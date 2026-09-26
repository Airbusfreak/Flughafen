package de.dresdenairlines;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
public class Flight {
    public final String id; public final Airline airline; public final Aircraft aircraft; public final Airport from,to;
    public final int baseFare,capacity; public int ticketPrice; public int booked; public int npcBooked;
    public double demandScore; public int targetNpcPassengers;
    public final Map<UUID, Passenger> passengers = new LinkedHashMap<>();
    public FlightStatus status=FlightStatus.SCHEDULED;
    public long createdAt,boardingAt,departureAt,arrivalAt;
    public boolean routePreparing=false,routeReady=false; public String departureGateId=null,arrivalGateId=null; public int preparedChunks=0,totalRouteChunks=0; public String event=""; public long delayUntil=0; public boolean eventApplied=false;
    public Flight(String id, Airline airline, Aircraft aircraft, Airport from, Airport to, int ticketPrice, long now){this.id=id;this.airline=airline;this.aircraft=aircraft;this.from=from;this.to=to;this.baseFare=ticketPrice;this.ticketPrice=ticketPrice;this.capacity=aircraft.seats;this.createdAt=now;}
    public int available(){return Math.max(0,capacity-booked-npcBooked);}
    public double loadFactor(){return capacity==0?0:(double)booked/capacity;}
    public int prepPercent(){return totalRouteChunks==0?(routeReady?100:0):(int)Math.round(preparedChunks*100.0/totalRouteChunks);}
}
