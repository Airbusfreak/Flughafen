package de.dresdenairlines;
public class FlightManager {
 private final AirlineManager manager;
 FlightManager(DresdenAirlines p,AirportManager a){manager=p.airlines;}
 public void tick(){manager.economyTick();}
}
