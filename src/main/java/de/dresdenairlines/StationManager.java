package de.dresdenairlines;

import org.bukkit.*;
import org.bukkit.block.data.Rail;
import org.bukkit.entity.Minecart;
import org.bukkit.event.*;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.configuration.file.YamlConfiguration;
import java.io.File;
import java.util.*;

/** Generates persistent village railway stations and real Minecraft rail connections to airports. */
public final class StationManager implements Listener {
    private final DresdenAirlines plugin; private final File file; private YamlConfiguration data;
    public final Map<String,Station> stations=new LinkedHashMap<>();
    private final Set<String> villageChecks=new HashSet<>();
    StationManager(DresdenAirlines p){plugin=p;file=new File(p.getDataFolder(),"stations.yml");data=YamlConfiguration.loadConfiguration(file);}
    public void load(){stations.clear();data=YamlConfiguration.loadConfiguration(file);for(String id:data.getKeys(false))try{World w=Bukkit.getWorld(UUID.fromString(data.getString(id+".world-uuid")));if(w==null)w=Bukkit.getWorld(data.getString(id+".world"));if(w==null)continue;Location s=new Location(w,data.getDouble(id+".sx"),data.getDouble(id+".sy"),data.getDouble(id+".sz"));Location a=new Location(w,data.getDouble(id+".ax"),data.getDouble(id+".ay"),data.getDouble(id+".az"));stations.put(id,new Station(id,data.getString(id+".name",id),data.getString(id+".airport-id",""),s,a));}catch(Exception ex){plugin.getLogger().warning("Station load failed: "+id+" "+ex.getMessage());}}
    public void save(){YamlConfiguration y=new YamlConfiguration();for(Station s:stations.values()){String k=s.id();World w=s.station().getWorld();y.set(k+".name",s.name()); y.set(k+".airport-id",s.airportId());y.set(k+".world",w.getName());y.set(k+".world-uuid",w.getUID().toString());y.set(k+".sx",s.station().getX());y.set(k+".sy",s.station().getY());y.set(k+".sz",s.station().getZ());y.set(k+".ax",s.airportRail().getX());y.set(k+".ay",s.airportRail().getY());y.set(k+".az",s.airportRail().getZ());}try{y.save(file);data=y;}catch(Exception e){plugin.getLogger().warning("Station save failed: "+e.getMessage());}}
    public void scanVillage(World w,Location probe){if(!plugin.getConfig().getBoolean("railways.village-stations.enabled",true))return;if(!plugin.getConfig().getStringList("airports.allowed-worlds").isEmpty()&&!plugin.getConfig().getStringList("airports.allowed-worlds").contains(w.getName()))return;String key=w.getUID()+":"+(probe.getBlockX()>>6)+":"+(probe.getBlockZ()>>6);if(!villageChecks.add(key))return;try{var r=w.locateNearestStructure(probe,org.bukkit.generator.structure.StructureType.VILLAGE,plugin.getConfig().getInt("railways.village-stations.search-radius",96),false);if(r==null)return;Location v=r.getLocation();String vk=w.getUID()+":"+(v.getBlockX()>>6)+":"+(v.getBlockZ()>>6);String id="V"+Integer.toUnsignedString(vk.hashCode(),36).toUpperCase(Locale.ROOT); if(stations.containsKey(id))return;Airport ap=plugin.airports.nearest(v);if(ap==null)return;double max=plugin.getConfig().getDouble("railways.village-stations.max-distance",1800);if(ap.center().distance(v)>max)return;Location station=findStationSite(v,ap);if(station==null)return;Station st=new Station(id,"Dorf-Bahnhof → "+ap.id(),ap.id(),station,airportRailPoint(ap,station));stations.put(id,st);buildStation(st);save();}catch(Exception e){plugin.getLogger().fine("Village station skipped: "+e.getMessage());}}
    private Location findStationSite(Location v,Airport ap){World w=v.getWorld();double dx=ap.center().getX()-v.getX(),dz=ap.center().getZ()-v.getZ();double len=Math.max(1,Math.hypot(dx,dz));double ox=-dz/len*18,oz=dx/len*18;int x=v.getBlockX()+(int)Math.round(ox),z=v.getBlockZ()+(int)Math.round(oz);return new Location(w,x,w.getHighestBlockYAt(x,z)+1,z);}
    private Location airportRailPoint(Airport ap,Location village){World w=ap.center().getWorld();double dx=village.getX()-ap.center().getX(),dz=village.getZ()-ap.center().getZ();double len=Math.max(1,Math.hypot(dx,dz));int x=(int)Math.round(ap.center().getX()+dx/len*(ap.size()+20));int z=(int)Math.round(ap.center().getZ()+dz/len*(ap.size()+20));return new Location(w,x,w.getHighestBlockYAt(x,z)+1,z);}
    private void buildStation(Station s){Location l=s.station();World w=l.getWorld();fill(l.clone().add(-6,-1,-8),l.clone().add(6,2,8),Material.SMOOTH_STONE);fill(l.clone().add(-5,2,-6),l.clone().add(5,4,6),Material.GLASS);for(int x=-5;x<=5;x++)set(l.clone().add(x,0,-1),Material.POLISHED_BLACKSTONE);set(l.clone().add(0,2,0),Material.SEA_LANTERN);set(l.clone().add(0,3,0),Material.OAK_SIGN);set(l.clone().add(0,1,-7),Material.STONE);set(l.clone().add(0,1,-6),Material.STONE_BUTTON);buildRail(s);w.save();}
    private void buildRail(Station s){
        Location a=s.station().clone().add(0,0,-1), b=s.airportRail(); World w=a.getWorld();
        List<Location> path=new ArrayList<>(); int x=a.getBlockX(),z=a.getBlockZ(),tx=b.getBlockX(),tz=b.getBlockZ();
        int total=Math.max(1,Math.abs(tx-x)+Math.abs(tz-z)); int done=0;
        while(x!=tx){x+=Integer.compare(tx,x);done++;int y=(int)Math.round(a.getY()+(b.getY()-a.getY())*(done/(double)total));path.add(new Location(w,x,y,z));}
        while(z!=tz){z+=Integer.compare(tz,z);done++;int y=(int)Math.round(a.getY()+(b.getY()-a.getY())*(done/(double)total));path.add(new Location(w,x,y,z));}
        Location prev=a; org.bukkit.block.Block start=a.getBlock(); start.setType(Material.POWERED_RAIL); Rail sr=(Rail)start.getBlockData(); sr.setShape(Math.abs(tx-x)>=Math.abs(tz-z)?Rail.Shape.EAST_WEST:Rail.Shape.NORTH_SOUTH); start.setBlockData(sr); set(a.clone().add(0,-1,0),Material.REDSTONE_BLOCK);
        for(Location raw:path){int y=Math.max(prev.getBlockY()-1,Math.min(prev.getBlockY()+1,raw.getBlockY()));Location p=new Location(w,raw.getBlockX(),y,raw.getBlockZ());
            set(p.clone().add(0,-1,0),Material.STONE);org.bukkit.block.Block rb=p.getBlock();rb.setType(Material.POWERED_RAIL);Rail rd=(Rail)rb.getBlockData();int dx=p.getBlockX()-prev.getBlockX(),dz=p.getBlockZ()-prev.getBlockZ();
            if(dx!=0){if(y>prev.getBlockY())rd.setShape(Rail.Shape.ASCENDING_EAST);else if(y<prev.getBlockY())rd.setShape(Rail.Shape.ASCENDING_WEST);else rd.setShape(Rail.Shape.EAST_WEST);}
            else {if(y>prev.getBlockY())rd.setShape(Rail.Shape.ASCENDING_NORTH);else if(y<prev.getBlockY())rd.setShape(Rail.Shape.ASCENDING_SOUTH);else rd.setShape(Rail.Shape.NORTH_SOUTH);}
            rb.setBlockData(rd);set(p.clone().add(0,-1,0),Material.REDSTONE_BLOCK);prev=p;
        }
    }
    private void set(Location l,Material m){l.getBlock().setType(m);}private void fill(Location a,Location b,Material m){int x1=Math.min(a.getBlockX(),b.getBlockX()),x2=Math.max(a.getBlockX(),b.getBlockX()),y1=Math.min(a.getBlockY(),b.getBlockY()),y2=Math.max(a.getBlockY(),b.getBlockY()),z1=Math.min(a.getBlockZ(),b.getBlockZ()),z2=Math.max(a.getBlockZ(),b.getBlockZ());for(int x=x1;x<=x2;x++)for(int y=y1;y<=y2;y++)for(int z=z1;z<=z2;z++)set(new Location(a.getWorld(),x,y,z),m);}
    @EventHandler(ignoreCancelled=true) public void onChunkLoad(ChunkLoadEvent e){ Location probe=new Location(e.getWorld(),e.getChunk().getX()*16+8,64,e.getChunk().getZ()*16+8); plugin.getServer().getScheduler().runTaskLater(plugin,()->scanVillage(e.getWorld(),probe),40L); }
    public void tick(){if(!plugin.getConfig().getBoolean("railways.minecart-service.enabled",true))return;long interval=plugin.getConfig().getLong("railways.minecart-service.interval-seconds",120)*1000L;if(System.currentTimeMillis()%interval<1000)for(Station s:stations.values())spawnCart(s);}
    private void spawnCart(Station s){World w=s.station().getWorld();Location l=s.station().clone().add(0,0,-1);Minecart c=w.spawn(l,Minecart.class);c.setMaxSpeed(0.6);c.setCustomName("Lorenbahn → "+s.airportId());c.setCustomNameVisible(true);}
    @EventHandler(ignoreCancelled=true) public void onInteract(PlayerInteractEvent e){if(e.getHand()!=EquipmentSlot.HAND||e.getAction().isLeftClick())return;if(e.getClickedBlock()==null||e.getClickedBlock().getType()!=Material.STONE_BUTTON)return;Location l=e.getClickedBlock().getLocation();for(Station s:stations.values())if(s.station().distanceSquared(l)<36){spawnCart(s);e.getPlayer().sendMessage("§aLorenbahn fährt zum Flughafen.");break;}}
    public record Station(String id,String name,String airportId,Location station,Location airportRail){}
}
