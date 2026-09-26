package de.dresdenairlines;
import java.util.*;
public class Airline {
    public final UUID owner;
    public String name,code,homeAirport;
    public double money,reputation=50;
    public final List<Aircraft> fleet=new ArrayList<>();
    public final List<Route> routes=new ArrayList<>();
    public Airline(UUID owner,String name,String code,String home,double money){
        this.owner=owner;this.name=name;this.code=code;this.homeAirport=home;this.money=money;
    }
}
