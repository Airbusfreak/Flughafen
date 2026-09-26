package de.dresdenairlines;
import org.bukkit.configuration.file.YamlConfiguration;
import java.io.File;
import java.util.*;
public class Storage {
    private final DresdenAirlines plugin; private final File file;
    public final Map<UUID,Airline> airlines=new HashMap<>();
    public final Map<String,Flight> flights=new LinkedHashMap<>();
    Storage(DresdenAirlines p){plugin=p;file=new File(p.getDataFolder(),"airlines.yml");}
    public void load(){
        if(!file.exists())return;
        var y=YamlConfiguration.loadConfiguration(file);
        for(String k:y.getKeys(false))try{
            UUID u=UUID.fromString(k);
            Airline a=new Airline(u,y.getString(k+".name","Airline"),y.getString(k+".code","AIR"),
                    y.getString(k+".home","DRE"),y.getDouble(k+".money",100000));
            a.reputation=y.getDouble(k+".reputation",50);
            airlines.put(u,a);
            for(String id:y.getStringList(k+".aircraft")){
                String[] q=id.split("\\|",-1);
                if(q.length>=6)a.fleet.add(new Aircraft(q[0],q[1],q[2],q[3],Integer.parseInt(q[4]),Integer.parseInt(q[5])));
            }
            for(String r:y.getStringList(k+".routes")){
                String[] q=r.split("\\|",-1);
                if(q.length>=5){Route rt=new Route(q[0],q[1],q[2],Integer.parseInt(q[3]),Integer.parseInt(q[4]));if(q.length>=6)rt.lastScheduledAt=Long.parseLong(q[5]);a.routes.add(rt);}
            }
        }catch(Exception ex){plugin.getLogger().warning("Could not load airline "+k+": "+ex.getMessage());}
    }
    public void save(){
        var y=new YamlConfiguration();
        for(var e:airlines.entrySet()){
            String k=e.getKey().toString();Airline a=e.getValue();
            y.set(k+".name",a.name);y.set(k+".code",a.code);y.set(k+".home",a.homeAirport);
            y.set(k+".money",a.money);y.set(k+".reputation",a.reputation);
            List<String> ac=new ArrayList<>();for(Aircraft x:a.fleet)
                ac.add(String.join("|",x.id,x.type,x.model,x.registration,""+x.seats,""+x.range));
            y.set(k+".aircraft",ac);
            List<String> rr=new ArrayList<>();for(Route r:a.routes)
                rr.add(String.join("|",r.from,r.to,r.aircraftId,""+r.ticketPrice,""+r.frequencySeconds,""+r.lastScheduledAt));
            y.set(k+".routes",rr);
        }
        try{y.save(file);}catch(Exception ex){plugin.getLogger().warning("Save failed: "+ex.getMessage());}
    }
}
