package de.dresdenairlines;
import org.bukkit.Location;
import java.util.*;
public class Airport {
    private final String id,name; private final Location center; private final int size; private final List<String> gates; private int level=1;
    public Airport(String id,String name,Location center,int size,List<String> gates){this.id=id;this.name=name;this.center=center;this.size=size;this.gates=new ArrayList<>(gates);}
    public String id(){return id;} public String name(){return name;} public Location center(){return center;} public int size(){return size;} public List<String> gates(){return Collections.unmodifiableList(gates);}
    public int level(){return level;} public void level(int value){level=Math.max(1,value);}
    public void addGates(int amount){int start=gates.size()+1;for(int i=0;i<amount;i++)gates.add("G"+(start+i));}
    public Location gate(String gate){
        int i=gates.indexOf(gate);
        if(i<0)i=0;

        int level=Math.max(1,level());
        int perRow=level>=3 ? 6 : Math.max(3,gates.size());

        int row=i/perRow;
        int col=i%perRow;
        int rowCount=(gates.size()+perRow-1)/perRow;
        int columns=Math.min(perRow,gates.size());
        double centeredCol=col-(columns-1)/2.0;
        double x=center.getX()+centeredCol*11.0;

        // Higher-level airports get a second pier/stand row.
        double z=center.getZ()+10.0+row*22.0;

        return new Location(
                center.getWorld(),
                x,
                center.getY()+1,
                z
        );
    }}
