package de.dresdenairlines;
import org.bukkit.*; import java.util.*;
public final class RoutePreGenerator {
    private final DresdenAirlines plugin;
    public RoutePreGenerator(DresdenAirlines plugin){this.plugin=plugin;}
    public void prepare(Flight f){
        if(!plugin.getConfig().getBoolean("traffic.route-pregeneration.enabled",true)||f.routePreparing||f.routeReady)return;
        Location a=f.from.center(),b=f.to.center();
        if(a.getWorld()==null||b.getWorld()==null||a.getWorld()!=b.getWorld()){f.routeReady=true;return;}
        int radius=Math.max(0,plugin.getConfig().getInt("traffic.route-pregeneration.radius-chunks",2));
        int spacing=Math.max(1,plugin.getConfig().getInt("traffic.route-pregeneration.sample-spacing-chunks",2));
        int ax=a.getBlockX()>>4,az=a.getBlockZ()>>4,bx=b.getBlockX()>>4,bz=b.getBlockZ()>>4;
        int dx=bx-ax,dz=bz-az,steps=Math.max(1,(int)Math.ceil(Math.max(Math.abs(dx),Math.abs(dz))/(double)spacing));
        List<long[]> chunks=new ArrayList<>(); Set<Long> seen=new HashSet<>();
        for(int i=0;i<=steps;i++){double t=i/(double)steps;int cx=(int)Math.round(ax+dx*t),cz=(int)Math.round(az+dz*t);
            for(int ox=-radius;ox<=radius;ox++)for(int oz=-radius;oz<=radius;oz++)if(ox*ox+oz*oz<=radius*radius){int x=cx+ox,z=cz+oz;long k=(((long)x)<<32)^(z&0xffffffffL);if(seen.add(k))chunks.add(new long[]{x,z});}}
        f.totalRouteChunks=chunks.size();f.preparedChunks=0;f.routePreparing=true;next(f,chunks,0);
    }
    private void next(Flight f,List<long[]> chunks,int i){
        if(i>=chunks.size()){f.routePreparing=false;f.routeReady=true;f.preparedChunks=f.totalRouteChunks;return;}
        World w=f.from.center().getWorld(); if(w==null){f.routePreparing=false;f.routeReady=true;return;}
        long[] c=chunks.get(i);
        w.getChunkAtAsync((int)c[0],(int)c[1],true,false,chunk->{
            f.preparedChunks++; long delay=Math.max(1,plugin.getConfig().getLong("traffic.route-pregeneration.delay-ticks-between-chunks",1));
            plugin.getServer().getScheduler().runTaskLater(plugin,()->next(f,chunks,i+1),delay);
        });
    }
}
