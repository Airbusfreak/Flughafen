package de.dresdenairlines;

import org.bukkit.*;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.util.Vector;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import java.util.*;

public final class PassengerManager implements Listener {
    final DresdenAirlines plugin; final PlayerWallet wallet;
    private final Map<String,List<SimulatedPassenger>> npcPassengers=new HashMap<>();
    PassengerManager(DresdenAirlines plugin){this.plugin=plugin; this.wallet=new PlayerWallet(plugin);}

    public List<Flight> departures(){
        List<Flight> out=new ArrayList<>();
        for(Flight f:plugin.storage.flights.values()) if(f.status==FlightStatus.SCHEDULED||f.status==FlightStatus.BOARDING) out.add(f);
        out.sort(Comparator.comparingLong(f->f.departureAt)); return out;
    }
    public int currentFare(Flight f){
        double load=f.capacity==0?0:(double)(f.booked+f.npcBooked)/f.capacity;
        double multiplier=1.0 + Math.max(-0.20,Math.min(0.75,(f.demandScore-0.50)*0.90 + (load-0.55)*0.55));
        return Math.max(10,(int)Math.round(f.baseFare*multiplier));
    }
    public String demandText(Flight f){return String.format(Locale.US,"%.0f%%",Math.min(100,f.demandScore*100));}
    public String book(Player player, String destination){
        Flight f=departures().stream().filter(x->x.to.id().equalsIgnoreCase(destination)).findFirst().orElse(null);
        if(f==null) return "§cKein buchbarer Flug zu "+destination+" gefunden."; return book(player,f);
    }
    public String book(Player player, Flight f){
        if(f.status!=FlightStatus.SCHEDULED&&f.status!=FlightStatus.BOARDING) return "§cDieser Flug ist nicht mehr buchbar.";
        if(f.available()<=0) return "§cDieser Flug ist ausgebucht.";
        if(f.passengers.containsKey(player.getUniqueId())) return "§eDu hast bereits einen Platz auf diesem Flug.";
        int fare=currentFare(f);
        if(wallet.balance(player.getUniqueId())<fare) return "§cDu hast nicht genug Geld. Ticketpreis: "+fare+" $ | Kontostand: "+money(player.getUniqueId());
        if(!wallet.withdraw(player.getUniqueId(),fare)) return "§cTicketkauf fehlgeschlagen.";
        Passenger pp=new Passenger(player.getUniqueId(),f.id,f.to.id(),System.currentTimeMillis()); f.passengers.put(player.getUniqueId(),pp); f.booked++;
        f.ticketPrice=fare;
        player.sendMessage("§aTicket gekauft: §f"+f.from.id()+" §7→ §f"+f.to.id()+" §7für §a"+fare+" $§7. Flug: §b"+f.id);
        player.sendMessage("§7Nachfrage: §e"+demandText(f)+" §7| Gehe zu §f"+f.from.id()+" §7zum Boarding."); plugin.storage.save(); return null;
    }
    public String bookNext(Player player, String destination){
        Flight f=departures().stream().filter(x->x.to.id().equalsIgnoreCase(destination)).findFirst().orElse(null);
        if(f==null){for(Airline a:plugin.storage.airlines.values()) for(Route r:a.routes) if(r.to.equalsIgnoreCase(destination)){f=plugin.airlines.schedule(a,r); if(f!=null) break;}}
        return f==null?"§cKein Flug zu diesem Ziel verfügbar.":book(player,f);
    }
    public String money(UUID u){return String.format(Locale.US,"%.0f $",wallet.balance(u));}

    public void tick(){
        for(Flight f:new ArrayList<>(plugin.storage.flights.values())){
            if(f.status==FlightStatus.SCHEDULED||f.status==FlightStatus.BOARDING) simulateBoarding(f);
            if(f.status==FlightStatus.DEPARTED||f.status==FlightStatus.CRUISE||f.status==FlightStatus.APPROACH){
                Location plane=plugin.renderer.locationOf(f);
                if(plane!=null) for(Passenger pp:f.passengers.values()){
                    Player pl=Bukkit.getPlayer(pp.player); if(pl==null) continue;
                    if(!pp.boarded && f.departureGateId!=null && pl.getLocation().distanceSquared(f.from.gate(f.departureGateId))<2500){pp.boarded=true;pl.sendMessage("§aDu bist an Bord von §f"+f.id+"§a. Gute Reise!");}
                    if(pp.boarded){Location cabin=plane.clone().add(0,2.2,0);cabin.setYaw(pl.getLocation().getYaw());cabin.setPitch(pl.getLocation().getPitch());pl.teleport(cabin);}
                }
            }
            if(f.status==FlightStatus.LANDED){
                Location gate=f.to.gate(f.arrivalGateId!=null?f.arrivalGateId:f.to.gates().get(0)).clone().add(0,1,0);
                for(Passenger pp:f.passengers.values()){Player pl=Bukkit.getPlayer(pp.player);if(pl!=null&&pp.boarded){pl.teleport(gate);pl.sendMessage("§aWillkommen in §f"+f.to.name()+"§a!");}}
                cleanupNpcs(f.id); f.passengers.clear();
            }
        }
    }

    private void simulateBoarding(Flight f){
        if(!plugin.getConfig().getBoolean("passengers.npc-simulation.enabled",true)) return;
        long now=System.currentTimeMillis();
        long secondsToDeparture=Math.max(0,(f.departureAt-now)/1000);
        if(f.status!=FlightStatus.BOARDING || secondsToDeparture>plugin.getConfig().getLong("passengers.npc-simulation.spawn-before-departure-seconds",30)) return;
        double priceFactor=(double)f.baseFare/Math.max(1,currentFare(f));
        double reputation=Math.max(.45,Math.min(1.25,f.airline.reputation/50.0));
        double timeFactor=0.90+0.20*Math.abs(Math.sin(now/3600000.0));
        double raw=f.capacity * 0.78 * priceFactor * reputation * timeFactor;
        f.demandScore=Math.max(0,Math.min(1.0,raw/f.capacity));
        f.targetNpcPassengers=Math.min(f.capacity-f.booked,Math.max(0,(int)Math.round(raw))-f.booked);
        int missing=Math.max(0,f.targetNpcPassengers-f.npcBooked);
        if(missing>0) spawnNpcBatch(f,Math.min(missing,plugin.getConfig().getInt("passengers.npc-simulation.max-spawn-per-tick",8)));
        // Offer/demand is recalculated while boarding: a fuller aircraft raises price, weak demand lowers it.
        f.ticketPrice=currentFare(f);
    }
    private void spawnNpcBatch(Flight f,int amount){
        List<SimulatedPassenger> list=npcPassengers.computeIfAbsent(f.id,k->new ArrayList<>());
        Location start=f.from.center().clone().add(0,1,8);
        for(int i=0;i<amount;i++){
            Villager v=(Villager)f.from.center().getWorld().spawnEntity(start.clone().add((i%5)*1.2,0,(i/5)*1.2), EntityType.VILLAGER);
            v.setAI(false); v.setInvulnerable(true); v.setSilent(true); v.setCollidable(false); v.setCustomNameVisible(true);
            v.setCustomName("§7Passagier → "+f.to.id());
            v.setPersistent(false); v.getPersistentDataContainer().set(plugin.npcKey,org.bukkit.persistence.PersistentDataType.STRING,f.id);
            list.add(new SimulatedPassenger(v,f.id,f.npcBooked+1)); f.npcBooked++;
        }
    }
    public void animateNpcs(){
        for(var e:new HashMap<>(npcPassengers).entrySet()){
            Flight f=plugin.storage.flights.get(e.getKey()); if(f==null||f.status==FlightStatus.LANDED||f.status==FlightStatus.DEPARTED){if(f!=null&&f.status==FlightStatus.DEPARTED) cleanupNpcs(f.id);continue;}
            Location gate=f.from.gate(f.from.gates().get(Math.min(f.from.gates().size()-1, e.getValue().size()%Math.max(1,f.from.gates().size()))));
            for(SimulatedPassenger sp:e.getValue()){Villager v=sp.npc();if(v.isDead())continue;Location cur=v.getLocation();Vector step=gate.toVector().subtract(cur.toVector());double len=step.length();if(len>0.7)v.teleport(cur.add(step.normalize().multiply(Math.min(0.8,len))));else v.teleport(gate.clone().add(0,1,0));}
        }
    }
    private void cleanupNpcs(String flightId){List<SimulatedPassenger> list=npcPassengers.remove(flightId);if(list!=null)for(SimulatedPassenger s:list)if(!s.npc().isDead())s.npc().remove();}
    @EventHandler public void onNpcDeath(EntityDeathEvent e){if(e.getEntity() instanceof Villager v)v.getPersistentDataContainer().remove(plugin.npcKey);}
}
