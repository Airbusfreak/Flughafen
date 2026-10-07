package de.dresdenairlines;

import org.bukkit.*;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.NamespacedKey;
import org.bukkit.util.Transformation;\nimport org.bukkit.Color;\nimport org.bukkit.inventory.meta.components.CustomModelDataComponent;
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

    public Location locationOf(Flight f) {

        Location a = f.from.center();
        Location b = f.to.center();

        if (
                a.getWorld() == null
                        || b.getWorld() == null
                        || a.getWorld() != b.getWorld()
        ) {
            return null;
        }

        if (
                f.status == FlightStatus.SCHEDULED
                        || f.status == FlightStatus.BOARDING
        ) {
            Location g =
                    f.departureGateId == null
                            ? a
                            : f.from.gate(f.departureGateId);

            return g.clone().add(0, 2.2, 0);
        }

        if (f.status == FlightStatus.TAXIING) {

            Location g =
                    f.departureGateId == null
                            ? a
                            : f.from.gate(f.departureGateId);

            Location r =
                    a.clone().add(
                            0,
                            1,
                            -f.from.size() - 35
                    );

            double t =
                    Math.max(
                            0,
                            Math.min(
                                    1,
                                    (
                                            System.currentTimeMillis()
                                                    - (f.departureAt - 5000)
                                    ) / 5000.0
                            )
                    );

            return g.clone()
                    .add(
                            r.clone()
                                    .subtract(g)
                                    .multiply(t)
                    )
                    .add(0, 2, 0);
        }

        if (f.status == FlightStatus.LANDED) {

            Location g =
                    f.arrivalGateId == null
                            ? b
                            : f.to.gate(f.arrivalGateId);

            return g.clone().add(0, 2.2, 0);
        }

        double total =
                Math.max(
                        1,
                        f.arrivalAt - f.departureAt
                );

        double t =
                Math.max(
                        0,
                        Math.min(
                                1,
                                (
                                        System.currentTimeMillis()
                                                - f.departureAt
                                ) / (double) total
                        )
                );

        double x =
                a.getX()
                        + (b.getX() - a.getX()) * t;

        double z =
                a.getZ()
                        + (b.getZ() - a.getZ()) * t;

        double y =
                a.getY()
                        + 40
                        + Math.sin(t * Math.PI) * 100;

        float yaw =
                (float) Math.toDegrees(
                        Math.atan2(
                                b.getX() - a.getX(),
                                b.getZ() - a.getZ()
                        )
                );

        return new Location(
                a.getWorld(),
                x,
                y,
                z,
                yaw,
                0
        );
    }

    public void tick() {

        for (Flight f : p.storage.flights.values()) {

            if (
                    f.status == FlightStatus.LANDED
                            || f.status == FlightStatus.CANCELLED
            ) {
                remove(f.id);
                continue;
            }

            Location loc = locationOf(f);

            if (loc == null) {
                continue;
            }

            ItemDisplay d = entities.get(f.id);

            if (d == null || d.isDead()) {

                d =
                        loc.getWorld().spawn(
                                loc,
                                ItemDisplay.class
                        );

                d.setPersistent(false);

                d.setItemStack(
                        aircraftItem(
                                f.aircraft.model,
                                f.aircraft.type
                        )
                );

                /*
                 * Transformation:
                 * 1. Translation
                 * 2. Left rotation
                 * 3. Scale
                 * 4. Right rotation
                 */
                d.setTransformation(
                        new Transformation(
                                new Vector3f(0, 0, 0),
                                new AxisAngle4f(),
                                new Vector3f(1, 1, 1),
                                new AxisAngle4f()
                        )
                );

                d.setTeleportDuration(2);
                d.setViewRange(512);

                entities.put(
                        f.id,
                        d
                );

            } else {
                d.teleport(loc);
            }

            d.customName(
                    Component.text(
                            "✈ "
                                    + f.id
                                    + "  "
                                    + f.from.id()
                                    + " → "
                                    + f.to.id()
                                    + "  |  "
                                    + f.status
                                    + "  | Gate "
                                    + (
                                            f.status == FlightStatus.SCHEDULED
                                                    || f.status == FlightStatus.BOARDING
                                                    ? f.departureGateId
                                                    : f.arrivalGateId
                                    )
                    )
            );

            d.setCustomNameVisible(true);
        }
    }

    private ItemStack aircraftItem(
            String model,
            String type
    ) {

        ItemStack item =
                new ItemStack(
                        Material.PAPER
                );

        ItemMeta m =
                item.getItemMeta();

        m.setItemModel(
                NamespacedKey.fromString(
                        "dresdenairlines:" + model
                )
        );

        m.displayName(
                Component.text(
                        "✈ " + type
                )
        );

        CustomModelDataComponent cmd = m.getCustomModelDataComponent();\n        cmd.setColors(List.of(\n                Color.fromRGB(livery.primary),\n                Color.fromRGB(livery.secondary),\n                Color.fromRGB(livery.accent)\n        ));\n        m.setCustomModelDataComponent(cmd);\n\n        item.setItemMeta(m);

        return item;
    }

    void remove(String id) {

        ItemDisplay d =
                entities.remove(id);

        if (d != null) {
            d.remove();
        }
    }
}
