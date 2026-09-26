package de.dresdenairlines;

import org.bukkit.*;
import org.bukkit.configuration.file.YamlConfiguration;
import java.io.File;
import java.util.*;

/** Tracks airport demand and automatically expands busy airports. */
public final class AirportLevelManager {
    private final DresdenAirlines p; private final File file; private final YamlConfiguration data;
    private final Map<String,Long> passengers=new HashMap<>(); private final Map<String,Double> demand=new HashMap<>(); private final Set<String> countedFlights=new HashSet<>();
    AirportLevelManager(DresdenAirlines p){this.p=p;file=new File(p.getDataFolder(),"airport-levels.yml");data=YamlConfiguration.loadConfiguration(file);}
    public void load(){for(String id:data.getKeys(false)){passengers.put(id,data.getLong(id+".passengers",0));demand.put(id,data.getDouble(id+".demand",0));Airport a=p.airports.airports.get(id);if(a!=null)a.level(data.getInt(id+".level",1));}}
    public void save(){YamlConfiguration y=new YamlConfiguration();for(Airport a:p.airports.airports.values()){String id=a.id();y.set(id+".level",a.level());y.set(id+".passengers",passengers.getOrDefault(id,0L));y.set(id+".demand",demand.getOrDefault(id,0.0));}try{y.save(file);}catch(Exception e){p.getLogger().warning("Airport level save failed: "+e.getMessage());}}
    public void recordFlight(Flight f){if(countedFlights.add(f.id)){long pax=f.booked+f.npcBooked;passengers.merge(f.to.id(),pax,Long::sum);passengers.merge(f.from.id(),pax,Long::sum);demand.merge(f.from.id(),f.demandScore,Double::sum);demand.merge(f.to.id(),f.demandScore,Double::sum);checkLevel(f.from);checkLevel(f.to);}}
    public long passengers(String id){return passengers.getOrDefault(id,0L);} public double demand(String id){return demand.getOrDefault(id,0.0);}
    public double demandIndex(Airport a){long pax=passengers(a.id());double recent=demand.getOrDefault(a.id(),0.0);return Math.min(100,Math.min(70,pax/50.0)+Math.min(30,recent*2.0));}
    private int targetLevel(long pax){if(pax>=5000)return 5;if(pax>=2000)return 4;if(pax>=750)return 3;if(pax>=250)return 2;return 1;}
    private void checkLevel(Airport a){int target=targetLevel(passengers(a.id()));if(target<=a.level())return;while(a.level()<target){a.level(a.level()+1);upgrade(a);}p.airports.save();save();p.getLogger().info("Airport "+a.id()+" reached level "+a.level()+".");}
    private void upgrade(Airport a){World w=a.center().getWorld();int y=a.center().getBlockY(),cx=a.center().getBlockX(),cz=a.center().getBlockZ();int s=a.size();
        int before=a.gates().size(); if(a.level()==2){a.addGates(2);buildParking(w,cx,cz+55,y,14,8);}
        if(a.level()==3){a.addGates(2);fill(w,cx-s/2-12,y,cz+12,cx-s/2-4,y+4,Material.SMOOTH_QUARTZ);fill(w,cx+s/2+4,y,cz+12,cx+s/2+12,y+4,Material.SMOOTH_QUARTZ);}
        if(a.level()==4){a.addGates(3);fill(w,cx-s-20,y,cz-10,cx-s-8,y+1,Material.GRAY_CONCRETE);}
        if(a.level()==5){a.addGates(3);int rz=cz-s-35;fill(w,cx-7,y,rz-110,cx+7,y,rz-90,Material.BLACK_CONCRETE);for(int z=rz-108;z<=rz-92;z+=6)fill(w,cx-1,y+1,z,cx+1,y+2,z+2,Material.WHITE_CONCRETE);}
        if(a.gates().size()!=before)p.gates.rebuild(p.storage.flights.values());
    }
    private void buildParking(World w,int cx,int cz,int y,int width,int depth){fill(w,cx-width,y,cz-depth,cx+width,y,cz+depth,Material.GRAY_CONCRETE);for(int x=cx-width+2;x<cx+width;x+=4)fill(w,x,y+1,cz-depth+2,x+1,y+1,cz+depth-2,Material.WHITE_CONCRETE);}
    private void fill(World w,int x1,int y1,int z1,int x2,int y2,int z2,Material m){for(int x=Math.min(x1,x2);x<=Math.max(x1,x2);x++)for(int y=Math.min(y1,y2);y<=Math.max(y1,y2);y++)for(int z=Math.min(z1,z2);z<=Math.max(z1,z2);z++)w.getBlockAt(x,y,z).setType(m);}
    public String info(Airport a){return "§b"+a.name()+" §7Level §e"+a.level()+" §7| Passagiere: §f"+passengers(a.id())+" §7| Nachfrage: §e"+String.format(Locale.US,"%.0f",demandIndex(a))+"%";}
}
