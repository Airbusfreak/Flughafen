package de.dresdenairlines;

import org.bukkit.*;
import org.bukkit.entity.Minecart;
import org.bukkit.entity.EntityType;
import java.util.*;
import org.bukkit.util.Vector;

/** Ground operations, baggage carts and small random operational events. */
public final class SimulationManager {
    private final DresdenAirlines p; private final Random random=new Random(); private final Map<String,List<Minecart>> carts=new HashMap<>(); private long rankingTick=0;
    SimulationManager(DresdenAirlines p){this.p=p;}
    public void tick(){long now=System.currentTimeMillis();
        for(Flight f:new ArrayList<>(p.storage.flights.values())){
            if(f.status==FlightStatus.BOARDING && !f.eventApplied){applyEvent(f,now);spawnBaggage(f);}
            if(f.status==FlightStatus.TAXIING)moveBaggage(f,true);
            if(f.status==FlightStatus.APPROACH)spawnArrivalBaggage(f);
            if(f.status==FlightStatus.LANDED)cleanupBaggage(f);
        }
        if(now-rankingTick>60000){rankingTick=now;for(Airline a:p.storage.airlines.values())a.reputation=Math.max(0,Math.min(100,a.reputation));}
    }
    private void applyEvent(Flight f,long now){f.eventApplied=true;if(random.nextDouble()>0.22)return;String[] ev={"Gate-Wechsel","Technische Kontrolle","Gepäck-Nachprüfung","Verspätete Passagiere","Enteisung erforderlich"};f.event=ev[random.nextInt(ev.length)];long delay=switch(f.event){case "Gate-Wechsel"->5000;case "Technische Kontrolle"->12000;case "Gepäck-Nachprüfung"->8000;case "Verspätete Passagiere"->7000;default->10000;};f.delayUntil=now+delay;f.airline.reputation=Math.max(0,f.airline.reputation-(f.event.equals("Technische Kontrolle")?.10:.03));
        for(org.bukkit.entity.Player pl:Bukkit.getOnlinePlayers())pl.sendMessage("§e✈ "+f.id+" §7Ereignis: §f"+f.event+(delay>0?" §8("+(delay/1000)+"s Verzögerung)":""));
    }
    private void spawnBaggage(Flight f){if(carts.containsKey(f.id)||f.from.center().getWorld()==null)return;List<Minecart> list=new ArrayList<>();int n=Math.min(3,Math.max(1,(f.booked+f.npcBooked)/50));Location s=f.from.center().clone().add(0,1,18);for(int i=0;i<n;i++){Minecart m=(Minecart)f.from.center().getWorld().spawnEntity(s.clone().add(i*1.5,0,0),EntityType.CHEST_MINECART);m.setCustomName("§7Gepäckwagen "+f.id);m.setCustomNameVisible(true);m.setPersistent(false);list.add(m);}carts.put(f.id,list);}
    private void spawnArrivalBaggage(Flight f){if(carts.containsKey(f.id)||f.to.center().getWorld()==null)return;List<Minecart> list=new ArrayList<>();Location s=f.to.gate(f.arrivalGateId!=null?f.arrivalGateId:f.to.gates().get(0)).clone().add(0,1,0);Minecart m=(Minecart)f.to.center().getWorld().spawnEntity(s,EntityType.CHEST_MINECART);m.setCustomName("§7Ankunftsgepäck "+f.id);m.setCustomNameVisible(true);m.setPersistent(false);list.add(m);carts.put(f.id,list);}
    private void moveBaggage(Flight f,boolean toGate){List<Minecart> list=carts.get(f.id);if(list==null)return;Location target=f.from.gate(f.departureGateId!=null?f.departureGateId:f.from.gates().get(0));for(Minecart m:list){if(m.isDead())continue;Location cur=m.getLocation();Vector v=target.toVector().subtract(cur.toVector());double d=v.length();if(d>1){m.teleport(cur.add(v.normalize().multiply(Math.min(1.5,d))));}else m.setVelocity(new org.bukkit.util.Vector(0,0,0));}}
    private void cleanupBaggage(Flight f){List<Minecart> list=carts.remove(f.id);if(list!=null)for(Minecart m:list)if(!m.isDead())m.remove();}
}
