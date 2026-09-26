package de.dresdenairlines;
import org.bukkit.Location;
import java.util.*;
public class Airport {
    private final String id,name; private final Location center; private final int size; private final List<String> gates; private int level=1;
    public Airport(String id,String name,Location center,int size,List<String> gates){this.id=id;this.name=name;this.center=center;this.size=size;this.gates=new ArrayList<>(gates);}
    public String id(){return id;} public String name(){return name;} public Location center(){return center;} public int size(){return size;} public List<String> gates(){return Collections.unmodifiableList(gates);}
    public int level(){return level;} public void level(int value){level=Math.max(1,value);}
    public void addGates(int amount){int start=gates.size()+1;for(int i=0;i<amount;i++)gates.add("G"+(start+i));}
    public Location gate(String gate){int i=gates.indexOf(gate);if(i<0)i=0;double x=center.getX()-18+i*12,z=center.getZ()-12;return new Location(center.getWorld(),x,center.getY()+1,z);}
}
