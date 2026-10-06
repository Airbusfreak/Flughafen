package de.dresdenairlines;

import org.bukkit.*;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.NamespacedKey;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;
import net.kyori.adventure.text.Component;

import java.util.*;

public final class FlightRenderer {

    final DresdenAirlines p;
    final Map<String, ItemDisplay> entities = new HashMap<>();

    FlightRenderer(DresdenAirlines p) {
        this.p = p;
    }

    /**
     * Visual flight model based on the generated airport geometry.
     *
     * Ground:
     *   gate -> taxiway -> runway
     *
     * Air:
     *   rotation -> climb -> curved cruise -> descent -> 3 degree final
     *   -> flare/touchdown -> rollout -> gate
     *
     * This is a Minecraft simulation, not a certified aerodynamic model.
     * The attitude profile follows realistic A320-family reference values.
     */
    public Location locationOf(Flight f) {
        Pose pose = poseOf(f);
        return pose == null ? null : pose.location;
    }

    /**
     * Third-person cinematic passenger camera:
     * above and behind the aircraft, continuously looking at the aircraft.
     */
    public Location cameraLocationOf(Flight f) {
        Pose pose = poseOf(f);
        if (pose == null) return null;

        Vector forward = pose.location.getDirection().normalize();
        if (forward.lengthSquared() < 0.01) {
            forward = new Vector(0, 0, 1);
        }

        double distance = p.getConfig().getDouble(
                "passengers.cinematic-camera.distance-behind", 18
        );
        double height = p.getConfig().getDouble(
                "passengers.cinematic-camera.height-above-aircraft", 9
        );

        Location camera = pose.location.clone()
                .add(forward.clone().multiply(-distance))
                .add(0, height, 0);

        Location target = pose.location.clone().add(0, 1.5, 0);
        camera.setDirection(target.toVector().subtract(camera.toVector()).normalize());
        return camera;
    }

    private Pose poseOf(Flight f) {
        if (f == null || f.from == null || f.to == null) return null;

        Location a = f.from.center();
        Location b = f.to.center();

        if (a.getWorld() == null || b.getWorld() == null || a.getWorld() != b.getWorld()) {
            return null;
        }

        long now = System.currentTimeMillis();

        if (f.status == FlightStatus.SCHEDULED || f.status == FlightStatus.BOARDING) {
            Location g = f.departureGateId == null ? a : f.from.gate(f.departureGateId);
            return new Pose(g.clone().add(0, 2.2, 0), 0f, 0f, 0f);
        }

        Location departureRunway = runwayCenter(f.from);
        Location departureRunwayEnd = departureRunway.clone()
                .add(0, 1, -runwayHalf(f.from));
        Location taxiEnd = departureRunway.clone().add(0, 2, 0);

        if (f.status == FlightStatus.TAXIING) {
            Location g = f.departureGateId == null ? a : f.from.gate(f.departureGateId);
            double t = clamp((now - (f.departureAt - 5000)) / 5000.0);
            t = smooth(t);

            Location loc = lerp(g.clone().add(0, 2, 0), taxiEnd, t);
            return poseToward(loc, taxiEnd, 0);
        }

        if (f.status == FlightStatus.LANDED) {
            Location g = f.arrivalGateId == null ? b : f.to.gate(f.arrivalGateId);
            return new Pose(g.clone().add(0, 2.2, 0), 180f, 0f, 0f);
        }

        Location arrivalRunway = runwayCenter(f.to);
        Location touchdown = arrivalRunway.clone()
                .add(0, 1.8, runwayHalf(f.to));

        if (f.status == FlightStatus.ROLLOUT) {
            Location taxiJoin = arrivalRunway.clone().add(0, 1.8, 0);
            double t = clamp((now - f.arrivalAt) / 8000.0);
            t = smooth(t);

            Location loc = lerp(touchdown, taxiJoin, t);
            return poseToward(loc, taxiJoin, 0);
        }

        if (f.status == FlightStatus.LANDING) {
            long landingStart = f.arrivalAt - 15000;
            double t = clamp((now - landingStart) / 15000.0);

            Location finalStart = b.clone().add(0, 45, 260);
            Location loc = lerp(finalStart, touchdown, smooth(t));

            // Normal final approach is approximately 3 degrees. The visual
            // pitch is held slightly nose-up for the landing configuration.
            float pitch = (float) lerp(4.0, 2.0, t);
            return poseToward(loc, touchdown, pitch);
        }

        long airborneStart = f.departureAt;
        long total = Math.max(1, f.arrivalAt - f.departureAt);
        double t = clamp((now - airborneStart) / (double) total);

        Location takeoffPoint = departureRunwayEnd.clone().add(0, 2, 0);
        Location climbPoint = takeoffPoint.clone().add(0, 70, -220);

        Vector direct = b.toVector().subtract(a.toVector());
        Vector horizontal = new Vector(direct.getX(), 0, direct.getZ());
        if (horizontal.lengthSquared() < 1) horizontal = new Vector(0, 0, 1);
        horizontal.normalize();

        Vector side = new Vector(-horizontal.getZ(), 0, horizontal.getX());
        double routeOffset = Math.min(180, Math.max(45, direct.length() * 0.08));

        Location cruiseStart = climbPoint.clone();
        Location cruiseEnd = touchdown.clone()
                .add(horizontal.clone().multiply(-320))
                .add(0, 110, 0);

        double climbEnd = Math.min(
                0.22,
                Math.max(0.12, 420.0 / Math.max(700, direct.length()))
        );
        double approachStart = 0.82;

        if (t < climbEnd) {
            double u = smooth(t / climbEnd);
            Location loc = lerp(takeoffPoint, cruiseStart, u);

            // A320 reference: rotation around 3 deg/s and roughly 15 deg
            // initial pitch on a normal two-engine takeoff.
            float pitch = (float) lerp(15.0, 6.0, u);
            return poseToward(loc, cruiseStart, pitch);
        }

        if (t < approachStart) {
            double u = smooth((t - climbEnd) / (approachStart - climbEnd));
            double curve = Math.sin(u * Math.PI) * routeOffset;

            Location p0 = cruiseStart;
            Location p3 = cruiseEnd;

            Location c1 = p0.clone()
                    .add(horizontal.clone().multiply(350))
                    .add(side.clone().multiply(curve * 0.55))
                    .add(0, 70, 0);

            Location c2 = p3.clone()
                    .subtract(horizontal.clone().multiply(350))
                    .add(side.clone().multiply(curve * 0.55))
                    .add(0, 45, 0);

            double bezierT = smooth(u);
            Location loc = cubicBezier(p0, c1, c2, p3, bezierT);
            Vector tangent = tangentBezier(p0, p1(c1), p2(c2), p3, bezierT);

            float pitch = (float) lerp(6.0, 2.5, u);
            float bank = (float) (Math.sin(u * Math.PI * 2)
                    * Math.min(25.0, routeOffset / 8.0));

            return poseToward(loc, tangent, pitch, bank);
        }

        double u = smooth((t - approachStart) / (1.0 - approachStart));
        Location approachStartPoint = cruiseEnd;
        Location loc = lerp(approachStartPoint, touchdown, u);

        float pitch = (float) lerp(2.5, 2.0, u);
        return poseToward(loc, touchdown, pitch);
    }

    // Small helpers make the runway geometry identical to AirportManager.
    private Location runwayCenter(Airport airport) {
        return airport.center().clone().add(0, 1, -airport.size() - 35);
    }

    private int runwayHalf(Airport airport) {
        return Math.max(
                90,
                p.getConfig().getInt("airports.runway-length", 180) / 2
        );
    }

    private Pose poseToward(Location loc, Location target, double pitch) {
        Vector delta = target.toVector().subtract(loc.toVector());
        return poseToward(loc, delta, pitch, 0);
    }

    private Pose poseToward(Location loc, Vector tangent, double pitch, double bank) {
        Vector horizontal = new Vector(tangent.getX(), 0, tangent.getZ());
        if (horizontal.lengthSquared() < 0.001) horizontal = new Vector(0, 0, 1);

        float yaw = (float) Math.toDegrees(
                Math.atan2(horizontal.getX(), horizontal.getZ())
        );

        loc.setYaw(yaw);
        loc.setPitch((float) pitch);
        return new Pose(loc, yaw, (float) pitch, (float) bank);
    }

    private double clamp(double v) {
        return Math.max(0, Math.min(1, v));
    }

    private double smooth(double t) {
        t = clamp(t);
        return t * t * (3 - 2 * t);
    }

    private Location lerp(Location a, Location b, double t) {
        return a.clone().add(
                b.toVector().subtract(a.toVector()).multiply(t)
        );
    }

    private Location cubicBezier(
            Location p0,
            Location p1,
            Location p2,
            Location p3,
            double t
    ) {
        double u = 1 - t;
        double uu = u * u;
        double tt = t * t;

        return new Location(
                p0.getWorld(),
                uu * u * p0.getX()
                        + 3 * uu * t * p1.getX()
                        + 3 * u * tt * p2.getX()
                        + tt * t * p3.getX(),
                uu * u * p0.getY()
                        + 3 * uu * t * p1.getY()
                        + 3 * u * tt * p2.getY()
                        + tt * t * p3.getY(),
                uu * u * p0.getZ()
                        + 3 * uu * t * p1.getZ()
                        + 3 * u * tt * p2.getZ()
                        + tt * t * p3.getZ()
        );
    }

    private Vector tangentBezier(
            Location p0,
            Location p1,
            Location p2,
            Location p3,
            double t
    ) {
        double u = 1 - t;

        return p1.toVector().subtract(p0.toVector()).multiply(3 * u * u)
                .add(p2.toVector().subtract(p1.toVector()).multiply(6 * u * t))
                .add(p3.toVector().subtract(p2.toVector()).multiply(3 * t * t));
    }

    private Location p1(Location location) {
        return location;
    }

    private Location p2(Location location) {
        return location;
    }

    public void tick() {
        Set<String> seen = new HashSet<>();

        for (Flight f : p.storage.flights.values()) {
            seen.add(f.id);

            if (f.status == FlightStatus.CANCELLED) {
                remove(f.id);
                continue;
            }

            Location loc = locationOf(f);
            if (loc == null) continue;

            ItemDisplay d = entities.get(f.id);

            if (d == null || d.isDead()) {
                d = loc.getWorld().spawn(loc, ItemDisplay.class);
                d.setPersistent(false);
                d.setItemStack(aircraftItem(f.aircraft.model, f.aircraft.type));
                d.setTeleportDuration(1);
                d.setInterpolationDuration(2);
                d.setInterpolationDelay(0);
                d.setViewRange(512);
                entities.put(f.id, d);
            } else {
                d.teleport(loc);
            }

            Pose pose = poseOf(f);
            d.setRotation(pose.yaw, pose.pitch);

            // Bank the display around its longitudinal axis.
            d.setTransformation(new Transformation(
                    new Vector3f(0, 0, 0),
                    new AxisAngle4f(
                            (float) Math.toRadians(pose.bank),
                            0, 0, 1
                    ),
                    new Vector3f(1, 1, 1),
                    new AxisAngle4f()
            ));

            d.customName(Component.text(
                    "✈ " + f.id + "  " + f.from.id() + " → " + f.to.id()
                            + " | " + f.status
                            + " | Gate " + (
                            f.status == FlightStatus.SCHEDULED
                                    || f.status == FlightStatus.BOARDING
                                    || f.status == FlightStatus.TAXIING
                                    ? f.departureGateId
                                    : f.arrivalGateId)
            ));
            d.setCustomNameVisible(true);
        }

        entities.entrySet().removeIf(e -> !seen.contains(e.getKey()));
    }

    private ItemStack aircraftItem(String model, String type) {
        ItemStack item = new ItemStack(Material.PAPER);
        ItemMeta m = item.getItemMeta();

        m.setItemModel(
                NamespacedKey.fromString("dresdenairlines:" + model)
        );

        m.displayName(Component.text("✈ " + type));
        item.setItemMeta(m);
        return item;
    }

    private void remove(String id) {
        ItemDisplay d = entities.remove(id);
        if (d != null) d.remove();
    }

    private static final class Pose {
        final Location location;
        final float yaw, pitch, bank;

        Pose(Location location, float yaw, float pitch, float bank) {
            this.location = location;
            this.yaw = yaw;
            this.pitch = pitch;
            this.bank = bank;
        }
    }
}
