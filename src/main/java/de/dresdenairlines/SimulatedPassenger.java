package de.dresdenairlines;
import org.bukkit.entity.Villager;
public record SimulatedPassenger(Villager npc, String flightId, int seat) {}
