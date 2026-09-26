package de.dresdenairlines;
public class Route {
    public final String from,to,aircraftId;
    public int ticketPrice,frequencySeconds;
    public boolean enabled=true; public long lastScheduledAt=0;
    public Route(String from,String to,String aircraftId,int ticketPrice,int frequencySeconds){
        this.from=from;this.to=to;this.aircraftId=aircraftId;this.ticketPrice=ticketPrice;this.frequencySeconds=frequencySeconds;
    }
}
