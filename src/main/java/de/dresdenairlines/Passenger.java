package de.dresdenairlines;

import java.util.UUID;

public final class Passenger {
    public final UUID player;
    public final String flightId;
    public final String destination;
    public final long bookedAt;
    public boolean boarded;
    public Passenger(UUID player, String flightId, String destination, long bookedAt) {
        this.player = player; this.flightId = flightId; this.destination = destination; this.bookedAt = bookedAt;
    }
}
