package de.dresdenairlines;

import java.util.UUID;

public class Employee {
    public final UUID id;
    public final String name;
    public final String role;
    public final int skill;
    public final double salary;
    public int level;
    public String assignedAirport;
    public long lastPaidAt;

    public Employee(UUID id, String name, String role, int skill, double salary, int level, String assignedAirport, long lastPaidAt) {
        this.id = id;
        this.name = name;
        this.role = role;
        this.skill = Math.max(1, Math.min(100, skill));
        this.salary = salary;
        this.level = Math.max(1, level);
        this.assignedAirport = assignedAirport == null ? "" : assignedAirport;
        this.lastPaidAt = lastPaidAt;
    }
}
