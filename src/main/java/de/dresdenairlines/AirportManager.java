package de.dresdenairlines;

import org.bukkit.*;
import org.bukkit.block.Biome;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.StructureType;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.*;

public class AirportManager implements Listener {

    private final DresdenAirlines plugin;

    public final Map<String, Airport> airports =
            new LinkedHashMap<>();

    private final Set<String> villageChecks =
            new HashSet<>();

    /** Grid cells already considered by the exploration-based generator. */
    private final Set<String> automaticCells =
            new HashSet<>();

    private boolean automaticGenerationBusy = false;

    private final File file;

    private YamlConfiguration data;

    public AirportManager(DresdenAirlines plugin) {

        this.plugin = plugin;

        this.file = new File(
                plugin.getDataFolder(),
                "airports.yml"
        );

        this.data =
                YamlConfiguration.loadConfiguration(file);
    }

    // =========================================================
    // LADEN
    // =========================================================

    public void load() {

        airports.clear();

        if (!file.exists()) {
            return;
        }

        data =
                YamlConfiguration.loadConfiguration(file);

        for (String id : data.getKeys(false)) {

            try {

                String worldName =
                        data.getString(
                                id + ".world"
                        );

                String worldUuid =
                        data.getString(
                                id + ".world-uuid"
                        );

                World world = null;

                if (worldUuid != null) {

                    try {

                        world =
                                Bukkit.getWorld(
                                        UUID.fromString(worldUuid)
                                );

                    } catch (IllegalArgumentException ignored) {
                    }
                }

                if (
                        world == null
                                &&
                        worldName != null
                ) {

                    world =
                            Bukkit.getWorld(
                                    worldName
                            );
                }

                if (world == null) {

                    plugin.getLogger().warning(
                            "Airport " +
                                    id +
                                    " konnte nicht geladen werden. " +
                                    "Welt nicht gefunden: " +
                                    worldName
                    );

                    continue;
                }

                double x =
                        data.getDouble(
                                id + ".x"
                        );

                double y =
                        data.getDouble(
                                id + ".y"
                        );

                double z =
                        data.getDouble(
                                id + ".z"
                        );

                int size =
                        data.getInt(
                                id + ".size",
                                45
                        );

                List<String> gates =
                        new ArrayList<>(
                                data.getStringList(
                                        id + ".gates"
                                )
                        );

                if (gates.isEmpty()) {

                    for (
                            int i = 1;
                            i <= Math.max(
                                    3,
                                    size / 15
                            );
                            i++
                    ) {

                        gates.add(
                                "G" + i
                        );
                    }
                }

                Airport airport =
                        new Airport(
                                id,
                                data.getString(
                                        id + ".name",
                                        id
                                ),
                                new Location(
                                        world,
                                        x,
                                        y,
                                        z
                                ),
                                size,
                                gates
                        );

                airport.level(
                        data.getInt(
                                id + ".level",
                                1
                        )
                );

                airports.put(
                        id.toUpperCase(Locale.ROOT),
                        airport
                );

            } catch (Exception ex) {

                plugin.getLogger().warning(
                        "Fehler beim Laden des Flughafens " +
                                id +
                                ": " +
                                ex.getMessage()
                );
            }
        }

        plugin.getLogger().info(
                "Loaded " +
                        airports.size() +
                        " airports."
        );
    }

    // =========================================================
    // SPEICHERN
    // =========================================================

    public void save() {

        YamlConfiguration yaml =
                new YamlConfiguration();

        for (Airport airport : airports.values()) {

            if (
                    airport == null
                            ||
                    airport.center() == null
                            ||
                    airport.center().getWorld() == null
            ) {
                continue;
            }

            String key =
                    airport.id();

            World world =
                    airport.center().getWorld();

            yaml.set(
                    key + ".name",
                    airport.name()
            );

            yaml.set(
                    key + ".world",
                    world.getName()
            );

            yaml.set(
                    key + ".world-uuid",
                    world.getUID().toString()
            );

            yaml.set(
                    key + ".x",
                    airport.center().getX()
            );

            yaml.set(
                    key + ".y",
                    airport.center().getY()
            );

            yaml.set(
                    key + ".z",
                    airport.center().getZ()
            );

            yaml.set(
                    key + ".size",
                    airport.size()
            );

            yaml.set(
                    key + ".gates",
                    airport.gates()
            );

            yaml.set(
                    key + ".level",
                    airport.level()
            );

            yaml.set(
                    key + ".generated",
                    true
            );

            yaml.set(
                    key + ".build-version",
                    1
            );
        }

        try {

            yaml.save(file);

            data = yaml;

        } catch (Exception ex) {

            plugin.getLogger().warning(
                    "Airport save failed: " +
                            ex.getMessage()
            );
        }
    }

    // =========================================================
    // ALLE FLUGHÄFEN
    // =========================================================

    public Collection<Airport> all() {

        return Collections.unmodifiableCollection(
                airports.values()
        );
    }

    // =========================================================
    // FLUGHAFEN NACH ID
    // =========================================================

    public Airport get(String id) {

        if (id == null) {
            return null;
        }

        return airports.get(
                id.toUpperCase(Locale.ROOT)
        );
    }

    // =========================================================
    // START-FLUGHÄFEN
    // =========================================================

    public void createInitial() {

        if (!airports.isEmpty()) {
            return;
        }

        World world =
                Bukkit.getWorlds()
                        .stream()
                        .filter(
                                this::worldAllowed
                        )
                        .findFirst()
                        .orElse(null);

        if (world == null) {

            plugin.getLogger().warning(
                    "Keine erlaubte Welt für die " +
                            "Flughafengenerierung gefunden."
            );

            return;
        }

        createInitialAirport(
                world,
                "DRE",
                "Dresden International",
                0,
                0,
                65
        );

        createInitialAirport(
                world,
                "LEJ",
                "Leipzig Regional",
                2600,
                0,
                45
        );

        createInitialAirport(
                world,
                "BER",
                "Berlin International",
                -2800,
                -2200,
                70
        );

        createInitialAirport(
                world,
                "MUC",
                "Munich International",
                4200,
                3100,
                75
        );

        save();
    }

    private void createInitialAirport(
            World world,
            String id,
            String name,
            int x,
            int z,
            int size
    ) {

        Location site =
                bestSite(
                        world,
                        x,
                        z,
                        size
                );

        if (site == null) {

            site =
                    bestSite(
                            world,
                            x,
                            z,
                            size,
                            512
                    );
        }

        if (site == null) {

            plugin.getLogger().warning(
                    "Kein geeigneter Bauplatz für " +
                            id +
                            " gefunden."
            );

            return;
        }

        createAirport(
                id,
                name,
                site,
                size
        );
    }

    // =========================================================
    // FLUGHAFEN ERSTELLEN
    // =========================================================

    public Airport createAirport(
            String id,
            String name,
            Location center,
            int size
    ) {

        if (
                center == null
                        ||
                center.getWorld() == null
        ) {

            plugin.getLogger().warning(
                    "Airport " +
                            id +
                            " konnte nicht erstellt werden: " +
                            "ungültige Position."
            );

            return null;
        }

        id =
                id.toUpperCase(
                        Locale.ROOT
                );

        Airport existing =
                airports.get(id);

        if (existing != null) {
            return existing;
        }

        List<String> gates =
                new ArrayList<>();

        for (
                int i = 1;
                i <= Math.max(
                        3,
                        size / 15
                );
                i++
        ) {

            gates.add(
                    "G" + i
            );
        }

        Airport airport =
                new Airport(
                        id,
                        name,
                        center,
                        size,
                        gates
                );

        airports.put(
                id,
                airport
        );

        build(
                airport
        );

        save();

        return airport;
    }

    // =========================================================
    // BLOCK-HILFSMETHODEN
    // =========================================================

    private void set(
            Location location,
            Material material
    ) {

        location.getBlock().setType(
                material
        );
    }

    private void fill(
            Location a,
            Location b,
            Material material
    ) {

        int minX =
                Math.min(
                        a.getBlockX(),
                        b.getBlockX()
                );

        int maxX =
                Math.max(
                        a.getBlockX(),
                        b.getBlockX()
                );

        int minY =
                Math.min(
                        a.getBlockY(),
                        b.getBlockY()
                );

        int maxY =
                Math.max(
                        a.getBlockY(),
                        b.getBlockY()
                );

        int minZ =
                Math.min(
                        a.getBlockZ(),
                        b.getBlockZ()
                );

        int maxZ =
                Math.max(
                        a.getBlockZ(),
                        b.getBlockZ()
                );

        for (
                int x = minX;
                x <= maxX;
                x++
        ) {

            for (
                    int y = minY;
                    y <= maxY;
                    y++
            ) {

                for (
                        int z = minZ;
                        z <= maxZ;
                        z++
                ) {

                    set(
                            new Location(
                                    a.getWorld(),
                                    x,
                                    y,
                                    z
                            ),
                            material
                    );
                }
            }
        }
    }

    // =========================================================
    // FLUGHAFEN BAUEN
    // =========================================================

    /**
     * Rebuilds every physical gate stand and jet bridge of an airport.
     */
    public void rebuildGates(Airport airport) {
        if (airport == null || airport.center() == null || airport.center().getWorld() == null) {
            return;
        }
        for (String gateId : airport.gates()) {
            buildGate(airport, gateId);
        }
    }

    private void buildGate(Airport airport, String gateId) {
        Location gate = airport.gate(gateId);
        if (gate == null || gate.getWorld() == null) {
            return;
        }

        fill(
                gate.clone().add(-9, 0, -7),
                gate.clone().add(9, 0, 7),
                Material.LIGHT_GRAY_CONCRETE
        );

        fill(
                gate.clone().add(-1, 1, -6),
                gate.clone().add(1, 1, 6),
                Material.YELLOW_CONCRETE
        );

        set(gate.clone().add(0, 1, 0), Material.SEA_LANTERN);

        fill(
                gate.clone().add(-2, 1, 2),
                gate.clone().add(2, 2, 5),
                Material.GLASS
        );

        // Boarding podium, screen and enclosed jet bridge.
        fill(
                gate.clone().add(-3, 1, -4),
                gate.clone().add(3, 2, -2),
                Material.BLACK_CONCRETE
        );
        fill(
                gate.clone().add(-2, 3, -4),
                gate.clone().add(2, 4, -4),
                Material.LIGHT_BLUE_STAINED_GLASS
        );
        fill(
                gate.clone().add(-1, 1, 5),
                gate.clone().add(1, 3, 8),
                Material.SMOOTH_QUARTZ
        );
        fill(
                gate.clone().add(-1, 2, 6),
                gate.clone().add(1, 3, 7),
                Material.LIGHT_BLUE_STAINED_GLASS
        );
    }

    /** Rebuilds the project-style visual details for an existing airport. */
    public void rebuildDesign(Airport airport) {
        if (airport == null) {
            return;
        }
        rebuildGates(airport);
        buildRealisticAirportDetails(airport);
        buildAdvancedInteriorDetails(airport);
    }

    /**
     * Detailed modern airport terminal interior.
     *
     * Design direction is inspired by the referenced Zayed International
     * Airport build: large open halls, strong glass facade, layered
     * circulation, gates/pier, shops, lounges, signage and service areas.
     * The geometry remains deliberately block-friendly so it can be generated
     * by the Paper plugin without external schematics.
     */
    private void buildRealisticAirportDetails(Airport airport) {
        Location c = airport.center();
        World w = c.getWorld();
        int x = c.getBlockX();
        int y = c.getBlockY();
        int z = c.getBlockZ();
        int level = Math.max(1, airport.level());

        int tw = plugin.getConfig().getInt("airports.terminal-width", 65);
        int td = plugin.getConfig().getInt("airports.terminal-depth", 48);
        int tz = z + 18;
        int floor = y + 1;
        int ceiling = y + 9;

        // =====================================================
        // MAIN HALL – tall, open, modern glass terminal
        // =====================================================
        fill(new Location(w, x - tw / 2 + 2, floor, tz + 2),
             new Location(w, x + tw / 2 - 2, floor, tz + td - 2),
             Material.POLISHED_ANDESITE);

        // Dark entrance strip / carpet.
        fill(new Location(w, x - tw / 2 + 3, floor + 1, tz + 1),
             new Location(w, x + tw / 2 - 3, floor + 1, tz + 4),
             Material.BLACK_CONCRETE);

        // High ceiling beams create the large-airport feeling.
        for (int px = x - tw / 2 + 5; px <= x + tw / 2 - 5; px += 10) {
            fill(new Location(w, px, y + 2, tz + 5),
                 new Location(w, px + 1, ceiling, tz + 6),
                 Material.SMOOTH_QUARTZ);
            fill(new Location(w, px, ceiling, tz + 2),
                 new Location(w, px + 1, ceiling, tz + td - 2),
                 Material.SMOOTH_QUARTZ);
        }

        // Glass facade with slim vertical mullions.
        fill(new Location(w, x - tw / 2 + 2, y + 2, tz),
             new Location(w, x + tw / 2 - 2, y + 8, tz),
             Material.LIGHT_BLUE_STAINED_GLASS);
        for (int px = x - tw / 2 + 2; px <= x + tw / 2 - 2; px += 5) {
            fill(new Location(w, px, y + 2, tz),
                 new Location(w, px, y + 8, tz),
                 Material.SMOOTH_QUARTZ);
        }

        // Entrance canopy and revolving-door-like entrances.
        fill(new Location(w, x - 11, y + 8, tz - 5),
             new Location(w, x + 11, y + 8, tz - 1),
             Material.SMOOTH_QUARTZ);
        fill(new Location(w, x - 8, y + 3, tz - 1),
             new Location(w, x + 8, y + 6, tz),
             Material.GLASS);
        for (int px = x - 6; px <= x + 6; px += 4) {
            fill(new Location(w, px, y + 2, tz),
                 new Location(w, px + 1, y + 6, tz),
                 Material.IRON_BARS);
        }

        // =====================================================
        // CHECK-IN – realistic islands with queue lanes and screens
        // =====================================================
        int checkInZ = tz + 7;
        for (int px = x - tw / 2 + 7; px <= x + tw / 2 - 11; px += 8) {
            fill(new Location(w, px, floor + 1, checkInZ),
                 new Location(w, px + 4, floor + 2, checkInZ + 1),
                 Material.GRAY_CONCRETE);
            fill(new Location(w, px + 1, floor + 3, checkInZ - 1),
                 new Location(w, px + 3, floor + 4, checkInZ - 1),
                 Material.BLACK_CONCRETE);
            set(new Location(w, px + 2, floor + 5, checkInZ),
                Material.LIGHT_BLUE_STAINED_GLASS);

            // Queue posts.
            for (int q = 0; q < 3; q++) {
                set(new Location(w, px, floor + 1, checkInZ + 3 + q * 2), Material.IRON_BARS);
                set(new Location(w, px + 4, floor + 1, checkInZ + 3 + q * 2), Material.IRON_BARS);
            }
        }

        // Large departure information wall.
        fill(new Location(w, x - 13, y + 4, tz + 2),
             new Location(w, x + 13, y + 7, tz + 2),
             Material.BLACK_CONCRETE);
        for (int px = x - 11; px <= x + 11; px += 4) {
            fill(new Location(w, px, y + 5, tz + 1),
                 new Location(w, px + 2, y + 6, tz + 1),
                 Material.CYAN_CONCRETE);
        }

        // =====================================================
        // SECURITY – multiple lanes, trays and queue barriers
        // =====================================================
        int secZ = tz + 21;
        fill(new Location(w, x - 24, y + 1, secZ - 2),
             new Location(w, x + 24, y + 1, secZ - 1),
             Material.BLACK_CONCRETE);
        for (int px = x - 22; px <= x + 16; px += 8) {
            fill(new Location(w, px, y + 2, secZ),
                 new Location(w, px + 5, y + 2, secZ + 5),
                 Material.IRON_BLOCK);
            fill(new Location(w, px + 1, y + 3, secZ + 1),
                 new Location(w, px + 4, y + 3, secZ + 1),
                 Material.IRON_BARS);
            set(new Location(w, px + 2, y + 3, secZ + 3), Material.SEA_LANTERN);
            for (int q = 0; q < 2; q++) {
                set(new Location(w, px, y + 2, secZ + 7 + q * 2), Material.IRON_BARS);
                set(new Location(w, px + 5, y + 2, secZ + 7 + q * 2), Material.IRON_BARS);
            }
        }

        // =====================================================
        // RETAIL / FOOD COURT – shops facing the passenger flow
        // =====================================================
        int shopZ = tz + 12;
        for (int i = -2; i <= 2; i++) {
            int sx = x + i * 11;
            fill(new Location(w, sx - 4, floor + 1, shopZ),
                 new Location(w, sx + 4, floor + 5, shopZ + 5),
                 Material.SMOOTH_QUARTZ);
            fill(new Location(w, sx - 3, floor + 2, shopZ - 1),
                 new Location(w, sx + 3, floor + 4, shopZ - 1),
                 Material.TINTED_GLASS);
            fill(new Location(w, sx - 2, floor + 5, shopZ - 1),
                 new Location(w, sx + 2, floor + 5, shopZ - 1),
                 Material.BLACK_CONCRETE);
            set(new Location(w, sx, floor + 6, shopZ - 1), Material.SEA_LANTERN);
        }

        // =====================================================
        // RESTROOMS / SERVICE ROOMS
        // =====================================================
        int serviceX = x + tw / 2 - 9;
        fill(new Location(w, serviceX - 5, floor + 1, tz + 27),
             new Location(w, serviceX + 5, floor + 4, tz + 34),
             Material.QUARTZ_BLOCK);
        for (int px = serviceX - 4; px <= serviceX + 4; px += 4) {
            set(new Location(w, px, floor + 2, tz + 27), Material.IRON_DOOR);
            set(new Location(w, px, floor + 2, tz + 34), Material.IRON_DOOR);
        }

        // =====================================================
        // DEPARTURE LOUNGE – seating islands, tables and plants
        // =====================================================
        int loungeZ = tz + 30;
        for (int px = x - tw / 2 + 8; px <= x + tw / 2 - 12; px += 9) {
            for (int row = 0; row < 2; row++) {
                set(new Location(w, px, floor + 1, loungeZ + row * 5), Material.DARK_OAK_STAIRS);
                set(new Location(w, px + 1, floor + 1, loungeZ + row * 5), Material.DARK_OAK_STAIRS);
            }
            set(new Location(w, px + 1, floor + 2, loungeZ + 2), Material.DARK_OAK_SLAB);
            set(new Location(w, px + 2, floor + 2, loungeZ + 2), Material.SEA_LANTERN);
        }

        // =====================================================
        // BAGGAGE RECLAIM – two belts with claim pillars
        // =====================================================
        int bagZ = tz + 38;
        for (int bx : new int[]{x - 13, x + 13}) {
            fill(new Location(w, bx - 7, floor + 1, bagZ),
                 new Location(w, bx + 7, floor + 1, bagZ + 6),
                 Material.BLACK_CONCRETE);
            fill(new Location(w, bx - 5, floor + 2, bagZ + 1),
                 new Location(w, bx + 5, floor + 2, bagZ + 5),
                 Material.GRAY_CONCRETE);
            fill(new Location(w, bx - 5, floor + 3, bagZ + 2),
                 new Location(w, bx + 5, floor + 3, bagZ + 3),
                 Material.IRON_BARS);
            for (int p = -5; p <= 5; p += 5) {
                set(new Location(w, bx + p, floor + 4, bagZ), Material.CYAN_CONCRETE);
            }
        }

        // =====================================================
        // ARRIVALS HALL – benches, information wall and doors
        // =====================================================
        int arrZ = tz + td - 5;
        fill(new Location(w, x - tw / 2 + 4, floor + 1, arrZ),
             new Location(w, x + tw / 2 - 4, floor + 1, arrZ + 3),
             Material.LIGHT_GRAY_CONCRETE);
        for (int px = x - tw / 2 + 7; px <= x + tw / 2 - 10; px += 7) {
            set(new Location(w, px, floor + 2, arrZ), Material.OAK_STAIRS);
            set(new Location(w, px + 1, floor + 2, arrZ), Material.OAK_STAIRS);
        }

        // =====================================================
        // GATE LOUNGES / PIERS
        // =====================================================
        for (String gateId : airport.gates()) {
            Location gate = airport.gate(gateId);
            if (gate == null) continue;

            int gx = gate.getBlockX();
            int gz = gate.getBlockZ();

            // Larger gate waiting room.
            fill(new Location(w, gx - 5, floor + 1, gz + 9),
                 new Location(w, gx + 5, floor + 9, gz + 20),
                 Material.SMOOTH_QUARTZ);
            fill(new Location(w, gx - 4, floor + 2, gz + 9),
                 new Location(w, gx + 4, floor + 7, gz + 9),
                 Material.LIGHT_BLUE_STAINED_GLASS);
            fill(new Location(w, gx - 4, floor + 8, gz + 9),
                 new Location(w, gx + 4, floor + 8, gz + 20),
                 Material.SMOOTH_QUARTZ);

            for (int px = gx - 3; px <= gx + 2; px += 3) {
                set(new Location(w, px, floor + 1, gz + 14), Material.DARK_OAK_STAIRS);
                set(new Location(w, px + 1, floor + 1, gz + 14), Material.DARK_OAK_STAIRS);
                set(new Location(w, px, floor + 2, gz + 15), Material.SEA_LANTERN);
            }

            // Gate number display.
            fill(new Location(w, gx - 2, floor + 6, gz + 9),
                 new Location(w, gx + 2, floor + 7, gz + 9),
                 Material.BLACK_CONCRETE);
        }

        // =====================================================
        // LIGHTING / WAYFINDING
        // =====================================================
        for (int px = x - tw / 2 + 6; px <= x + tw / 2 - 6; px += 8) {
            for (int pz = tz + 5; pz <= tz + td - 5; pz += 8) {
                set(new Location(w, px, ceiling - 1, pz), Material.SEA_LANTERN);
            }
        }

        // Planters break up the large hall.
        for (int px = x - tw / 2 + 7; px <= x + tw / 2 - 8; px += 14) {
            fill(new Location(w, px, floor + 1, tz + 16),
                 new Location(w, px + 2, floor + 1, tz + 18),
                 Material.SMOOTH_STONE);
            set(new Location(w, px + 1, floor + 2, tz + 17), Material.OAK_LOG);
            fill(new Location(w, px, floor + 3, tz + 16),
                 new Location(w, px + 2, floor + 4, tz + 18),
                 Material.OAK_LEAVES);
        }

        // =====================================================
        // LEVEL EXPANSIONS
        // =====================================================
        if (level >= 2) {
            // Upper departure gallery.
            fill(new Location(w, x - tw / 2 + 5, y + 10, tz + 5),
                 new Location(w, x + tw / 2 - 5, y + 11, tz + td - 3),
                 Material.SMOOTH_QUARTZ);
            fill(new Location(w, x - tw / 2 + 6, y + 8, tz + 6),
                 new Location(w, x + tw / 2 - 6, y + 9, tz + 6),
                 Material.GLASS);
            for (int px = x - tw / 2 + 8; px <= x + tw / 2 - 10; px += 10) {
                set(new Location(w, px, y + 9, tz + 6), Material.SEA_LANTERN);
            }

            // Parking deck.
            fill(new Location(w, x - tw / 2 - 24, y, z + 55),
                 new Location(w, x + tw / 2 + 24, y, z + 76),
                 Material.GRAY_CONCRETE);
            for (int px = x - tw / 2 - 20; px <= x + tw / 2 + 20; px += 6) {
                set(new Location(w, px, y + 1, z + 60), Material.WHITE_CONCRETE);
            }
        }

        if (level >= 3) {
            // Central high concourse / skylight.
            fill(new Location(w, x - 13, y + 10, tz + 8),
                 new Location(w, x + 13, y + 10, tz + td - 6),
                 Material.SMOOTH_QUARTZ);
            fill(new Location(w, x - 10, y + 11, tz + 9),
                 new Location(w, x + 10, y + 11, tz + td - 7),
                 Material.LIGHT_BLUE_STAINED_GLASS);
        }

        if (level >= 4) {
            // International wing.
            int wingX = x + tw / 2 + 12;
            fill(new Location(w, wingX, y, tz + 5),
                 new Location(w, wingX + 30, y + 9, tz + td + 8),
                 Material.SMOOTH_QUARTZ);
            fill(new Location(w, wingX + 1, y + 2, tz + 6),
                 new Location(w, wingX + 1, y + 7, tz + td + 6),
                 Material.LIGHT_BLUE_STAINED_GLASS);
            for (int pz = tz + 9; pz <= tz + td; pz += 6) {
                set(new Location(w, wingX + 1, y + 4, pz), Material.SEA_LANTERN);
            }
        }

        if (level >= 5) {
            // Satellite pier.
            int pierZ = z + 72;
            fill(new Location(w, x - 7, y, pierZ),
                 new Location(w, x + 7, y + 8, pierZ + 42),
                 Material.SMOOTH_QUARTZ);
            fill(new Location(w, x - 5, y + 2, pierZ),
                 new Location(w, x + 5, y + 7, pierZ + 42),
                 Material.LIGHT_BLUE_STAINED_GLASS);
            for (int pz = pierZ + 5; pz < pierZ + 40; pz += 6) {
                set(new Location(w, x, y + 6, pz), Material.SEA_LANTERN);
            }
        }

        // Exterior landscaping and pedestrian forecourt.
        fill(new Location(w, x - tw / 2 - 14, y, tz - 12),
             new Location(w, x + tw / 2 + 14, y, tz - 8),
             Material.BLACK_CONCRETE);
        for (int px = x - tw / 2 - 8; px <= x + tw / 2 + 8; px += 12) {
            set(new Location(w, px, y + 1, tz - 10), Material.OAK_LOG);
            fill(new Location(w, px - 1, y + 2, tz - 11),
                 new Location(w, px + 1, y + 4, tz - 9),
                 Material.OAK_LEAVES);
        }
    }


    /**
     * Fine interior pass: passenger circulation, immigration, restrooms,
     * retail, seating, service access and realistic terminal furniture.
     */
    private void buildAdvancedInteriorDetails(Airport airport) {
        Location c = airport.center();
        World w = c.getWorld();
        int x = c.getBlockX();
        int y = c.getBlockY();
        int z = c.getBlockZ();
        int tw = plugin.getConfig().getInt("airports.terminal-width", 65);
        int td = plugin.getConfig().getInt("airports.terminal-depth", 48);
        int tz = z + 18;
        int floor = y + 1;

        // ---------------- LAND SIDE: ticketing / customer service ----------------
        int ticketZ = tz + 6;
        fill(new Location(w, x - 23, floor + 1, ticketZ),
             new Location(w, x - 5, floor + 1, ticketZ + 2),
             Material.SMOOTH_QUARTZ);
        for (int px = x - 21; px <= x - 7; px += 4) {
            fill(new Location(w, px, floor + 2, ticketZ),
                 new Location(w, px + 2, floor + 3, ticketZ + 1),
                 Material.GRAY_CONCRETE);
            set(new Location(w, px + 1, floor + 4, ticketZ), Material.BLACK_CONCRETE);
            set(new Location(w, px + 1, floor + 5, ticketZ), Material.LIGHT_BLUE_STAINED_GLASS);
        }

        // Information desk with passenger-facing counter.
        fill(new Location(w, x + 7, floor + 1, ticketZ),
             new Location(w, x + 15, floor + 2, ticketZ + 2),
             Material.DARK_OAK_PLANKS);
        fill(new Location(w, x + 8, floor + 3, ticketZ),
             new Location(w, x + 14, floor + 4, ticketZ),
             Material.BLACK_CONCRETE);
        for (int px = x + 9; px <= x + 13; px += 2) {
            set(new Location(w, px, floor + 5, ticketZ), Material.SEA_LANTERN);
        }

        // ---------------- SECURITY DETAIL ----------------
        int securityZ = tz + 20;
        for (int lane = 0; lane < 5; lane++) {
            int lx = x - 20 + lane * 10;
            fill(new Location(w, lx, floor + 1, securityZ),
                 new Location(w, lx + 6, floor + 1, securityZ + 1),
                 Material.IRON_BLOCK);
            // conveyor / scanner
            fill(new Location(w, lx + 1, floor + 2, securityZ + 1),
                 new Location(w, lx + 4, floor + 2, securityZ + 3),
                 Material.BLACK_CONCRETE);
            // tray return
            fill(new Location(w, lx + 1, floor + 2, securityZ + 4),
                 new Location(w, lx + 4, floor + 2, securityZ + 4),
                 Material.LIGHT_GRAY_CONCRETE);
            // walk-through detector
            fill(new Location(w, lx, floor + 2, securityZ + 5),
                 new Location(w, lx + 5, floor + 4, securityZ + 5),
                 Material.IRON_BARS);
        }

        // Queue serpentine barriers.
        for (int q = 0; q < 6; q++) {
            int qx = x - 25 + q * 10;
            set(new Location(w, qx, floor + 1, securityZ - 5), Material.IRON_BARS);
            set(new Location(w, qx + 5, floor + 1, securityZ - 5), Material.IRON_BARS);
            set(new Location(w, qx, floor + 1, securityZ - 3), Material.IRON_BARS);
            set(new Location(w, qx + 5, floor + 1, securityZ - 3), Material.IRON_BARS);
        }

        // ---------------- AIRSIDE MAIN CONCOURSE ----------------
        int concourseZ = tz + 27;
        fill(new Location(w, x - 28, floor, concourseZ),
             new Location(w, x + 28, floor, concourseZ + 3),
             Material.LIGHT_GRAY_CONCRETE);
        for (int px = x - 25; px <= x + 23; px += 8) {
            // charging table
            fill(new Location(w, px, floor + 1, concourseZ + 1),
                 new Location(w, px + 4, floor + 1, concourseZ + 2),
                 Material.DARK_OAK_PLANKS);
            set(new Location(w, px + 2, floor + 2, concourseZ + 1), Material.SEA_LANTERN);
            // paired seats
            set(new Location(w, px, floor + 1, concourseZ + 3), Material.DARK_OAK_STAIRS);
            set(new Location(w, px + 1, floor + 1, concourseZ + 3), Material.DARK_OAK_STAIRS);
        }

        // ---------------- RESTROOMS WITH INDIVIDUAL STALLS ----------------
        int wcX = x + 20;
        int wcZ = tz + 30;
        fill(new Location(w, wcX - 7, floor + 1, wcZ),
             new Location(w, wcX + 7, floor + 4, wcZ + 8),
             Material.QUARTZ_BLOCK);
        for (int stall = 0; stall < 4; stall++) {
            int sx = wcX - 5 + stall * 3;
            fill(new Location(w, sx, floor + 2, wcZ + 2),
                 new Location(w, sx + 1, floor + 2, wcZ + 5),
                 Material.LIGHT_GRAY_CONCRETE);
            set(new Location(w, sx, floor + 3, wcZ + 2), Material.IRON_DOOR);
            set(new Location(w, sx + 1, floor + 3, wcZ + 5), Material.SMOOTH_STONE);
        }
        // sinks / mirrors
        fill(new Location(w, wcX - 5, floor + 2, wcZ + 7),
             new Location(w, wcX + 5, floor + 2, wcZ + 7),
             Material.QUARTZ_SLAB);
        fill(new Location(w, wcX - 5, floor + 3, wcZ + 8),
             new Location(w, wcX + 5, floor + 4, wcZ + 8),
             Material.GLASS);

        // ---------------- SHOPS / CAFES ----------------
        for (int i = 0; i < 3; i++) {
            int sx = x - 17 + i * 12;
            int sz = tz + 29;
            fill(new Location(w, sx, floor + 1, sz),
                 new Location(w, sx + 8, floor + 4, sz + 5),
                 Material.SMOOTH_QUARTZ);
            fill(new Location(w, sx + 1, floor + 2, sz),
                 new Location(w, sx + 7, floor + 3, sz),
                 Material.GLASS);
            fill(new Location(w, sx + 2, floor + 1, sz + 4),
                 new Location(w, sx + 6, floor + 2, sz + 4),
                 Material.DARK_OAK_PLANKS);
            set(new Location(w, sx + 4, floor + 4, sz), Material.SEA_LANTERN);
            // shelves / display islands
            for (int shelf = 0; shelf < 3; shelf++) {
                set(new Location(w, sx + 2 + shelf * 2, floor + 2, sz + 2), Material.BARREL);
            }
        }

        // ---------------- IMMIGRATION / PASSPORT CONTROL ----------------
        int passportZ = tz + td - 10;
        fill(new Location(w, x - 22, floor + 1, passportZ),
             new Location(w, x + 22, floor + 1, passportZ + 2),
             Material.BLACK_CONCRETE);
        for (int booth = 0; booth < 6; booth++) {
            int bx = x - 21 + booth * 8;
            fill(new Location(w, bx, floor + 2, passportZ),
                 new Location(w, bx + 5, floor + 4, passportZ + 2),
                 Material.GLASS);
            fill(new Location(w, bx + 1, floor + 2, passportZ + 1),
                 new Location(w, bx + 4, floor + 2, passportZ + 1),
                 Material.DARK_OAK_PLANKS);
            set(new Location(w, bx + 2, floor + 3, passportZ + 1), Material.SEA_LANTERN);
        }

        // ---------------- BAGGAGE CART / SERVICE ZONE ----------------
        int serviceZ = tz + td - 3;
        for (int cart = 0; cart < 4; cart++) {
            int cx = x - 18 + cart * 12;
            fill(new Location(w, cx, floor + 1, serviceZ),
                 new Location(w, cx + 6, floor + 2, serviceZ + 2),
                 Material.IRON_BLOCK);
            set(new Location(w, cx + 1, floor + 1, serviceZ + 2), Material.IRON_BARS);
            set(new Location(w, cx + 5, floor + 1, serviceZ + 2), Material.IRON_BARS);
        }

        // ---------------- VERTICAL CIRCULATION ----------------
        // Escalator pair: one up, one down.
        for (int i = 0; i < 8; i++) {
            set(new Location(w, x - 6 + i, floor + 1 + i / 3, tz + 24 + i / 2), Material.POLISHED_ANDESITE);
            set(new Location(w, x + 7 + i, floor + 1 + i / 3, tz + 24 + i / 2), Material.POLISHED_ANDESITE);
        }
        fill(new Location(w, x - 8, floor + 1, tz + 24),
             new Location(w, x - 1, floor + 2, tz + 29),
             Material.GLASS);
        fill(new Location(w, x + 6, floor + 1, tz + 24),
             new Location(w, x + 13, floor + 2, tz + 29),
             Material.GLASS);

        // Elevator core.
        fill(new Location(w, x + 14, floor + 1, tz + 18),
             new Location(w, x + 18, floor + 7, tz + 23),
             Material.SMOOTH_QUARTZ);
        fill(new Location(w, x + 15, floor + 2, tz + 18),
             new Location(w, x + 17, floor + 5, tz + 18),
             Material.TINTED_GLASS);

        // ---------------- GATE LOUNGE FURNITURE / BOARDING AREA ----------------
        for (String gateId : airport.gates()) {
            Location gate = airport.gate(gateId);
            if (gate == null) continue;
            int gx = gate.getBlockX();
            int gz = gate.getBlockZ();

            // Waiting rows facing the gate.
            for (int row = 0; row < 3; row++) {
                for (int seat = -3; seat <= 3; seat += 2) {
                    set(new Location(w, gx + seat, floor + 1, gz + 12 + row * 3), Material.DARK_OAK_STAIRS);
                }
            }
            // Boarding queue posts and rope line.
            for (int p = -3; p <= 3; p += 3) {
                set(new Location(w, gx + p, floor + 1, gz + 18), Material.IRON_BARS);
                set(new Location(w, gx + p, floor + 2, gz + 18), Material.LIGHT_BLUE_STAINED_GLASS);
            }
            // Small gate service counter.
            fill(new Location(w, gx - 3, floor + 1, gz + 19),
                 new Location(w, gx + 3, floor + 2, gz + 20),
                 Material.GRAY_CONCRETE);
            set(new Location(w, gx, floor + 3, gz + 19), Material.SEA_LANTERN);
        }
    }

    private void build(
            Airport airport
    ) {

        Location center =
                airport.center();

        World world =
                center.getWorld();

        int y =
                center.getBlockY();

        int size =
                airport.size();

        int x =
                center.getBlockX();

        int z =
                center.getBlockZ();

        // -----------------------------------------------------
        // Gelände vorbereiten
        // -----------------------------------------------------

        int radius =
                size + 20;

        for (
                int px = x - radius;
                px <= x + radius;
                px++
        ) {

            for (
                    int pz = z - radius;
                    pz <= z + radius;
                    pz++
            ) {

                int top =
                        world.getHighestBlockYAt(
                                px,
                                pz
                        );

                if (top < y) {

                    fill(
                            new Location(
                                    world,
                                    px,
                                    top + 1,
                                    pz
                            ),
                            new Location(
                                    world,
                                    px,
                                    y - 1,
                                    pz
                            ),
                            Material.STONE
                    );
                }
            }
        }

        // -----------------------------------------------------
        // Vorfeld
        // -----------------------------------------------------

        fill(
                new Location(
                        world,
                        x - size,
                        y,
                        z - size / 3
                ),
                new Location(
                        world,
                        x + size,
                        y,
                        z + size / 3
                ),
                Material.POLISHED_ANDESITE
        );

        // -----------------------------------------------------
        // Runway
        // -----------------------------------------------------

        int runwayHalf =
                Math.max(
                        90,
                        plugin.getConfig()
                                .getInt(
                                        "airports.runway-length",
                                        180
                                ) / 2
                );

        int runwayZ =
                z - size - 35;

        fill(
                new Location(
                        world,
                        x - 7,
                        y,
                        runwayZ - runwayHalf
                ),
                new Location(
                        world,
                        x + 7,
                        y,
                        runwayZ + runwayHalf
                ),
                Material.BLACK_CONCRETE
        );

        // Runway-Mittelmarkierungen

        for (
                int pz =
                        runwayZ -
                                runwayHalf +
                                10;

                pz <
                        runwayZ +
                                runwayHalf -
                                10;

                pz += 12
        ) {

            fill(
                    new Location(
                            world,
                            x - 1,
                            y + 1,
                            pz
                    ),
                    new Location(
                            world,
                            x + 1,
                            y + 1,
                            pz + 5
                    ),
                    Material.WHITE_CONCRETE
            );
        }

        // -----------------------------------------------------
        // Taxiway
        // -----------------------------------------------------

        fill(
                new Location(
                        world,
                        x - 4,
                        y,
                        z - size
                ),
                new Location(
                        world,
                        x + 4,
                        y,
                        runwayZ
                ),
                Material.GRAY_CONCRETE
        );

        // Taxiway-Lichter

        for (
                int px = x - size;
                px <= x + size;
                px += 8
        ) {

            set(
                    new Location(
                            world,
                            px,
                            y + 1,
                            z
                    ),
                    Material.SEA_LANTERN
            );
        }

        // -----------------------------------------------------
        // Terminal
        // -----------------------------------------------------

        int terminalWidth =
                plugin.getConfig()
                        .getInt(
                                "airports.terminal-width",
                                55
                        );

        int terminalDepth =
                plugin.getConfig()
                        .getInt(
                                "airports.terminal-depth",
                                35
                        );

        int terminalZ =
                z + 18;

        fill(
                new Location(
                        world,
                        x - terminalWidth / 2,
                        y,
                        terminalZ
                ),
                new Location(
                        world,
                        x + terminalWidth / 2,
                        y + 1,
                        terminalZ +
                                terminalDepth
                ),
                Material.SMOOTH_QUARTZ
        );

        // Glasfront

        fill(
                new Location(
                        world,
                        x - terminalWidth / 2,
                        y + 2,
                        terminalZ
                ),
                new Location(
                        world,
                        x + terminalWidth / 2,
                        y + 2,
                        terminalZ
                ),
                Material.GLASS
        );

        // Tall level-1 terminal shell – the interior detail system uses a
        // realistic 8-block clear height rather than a tiny 3-block box.
        fill(
                new Location(world, x - terminalWidth / 2, y + 8, terminalZ),
                new Location(world, x + terminalWidth / 2, y + 8, terminalZ + terminalDepth),
                Material.SMOOTH_QUARTZ
        );

        fill(
                new Location(world, x - terminalWidth / 2, y + 2, terminalZ),
                new Location(world, x - terminalWidth / 2, y + 8, terminalZ + terminalDepth),
                Material.SMOOTH_QUARTZ
        );

        fill(
                new Location(world, x + terminalWidth / 2, y + 2, terminalZ),
                new Location(world, x + terminalWidth / 2, y + 8, terminalZ + terminalDepth),
                Material.SMOOTH_QUARTZ
        );

        // Entrance canopy.
        fill(
                new Location(world, x - 8, y + 3, terminalZ - 3),
                new Location(world, x + 8, y + 3, terminalZ + 1),
                Material.SMOOTH_QUARTZ
        );

        fill(
                new Location(world, x - 6, y + 2, terminalZ - 1),
                new Location(world, x + 6, y + 2, terminalZ),
                Material.GLASS
        );

        // -----------------------------------------------------
        // Check-In
        // -----------------------------------------------------

        for (
                int px =
                        x -
                                terminalWidth / 2 +
                                4;

                px <
                        x +
                                terminalWidth / 2 -
                                3;

                px += 5
        ) {

            fill(
                    new Location(
                            world,
                            px,
                            y + 1,
                            terminalZ + 4
                    ),
                    new Location(
                            world,
                            px + 2,
                            y + 2,
                            terminalZ + 5
                    ),
                    Material.SMOOTH_STONE
            );

            set(
                    new Location(
                            world,
                            px + 1,
                            y + 3,
                            terminalZ + 4
                    ),
                    Material.LECTERN
            );
        }

        // -----------------------------------------------------
        // Sitze
        // -----------------------------------------------------

        for (
                int px =
                        x -
                                terminalWidth / 2 +
                                5;

                px <
                        x +
                                terminalWidth / 2 -
                                4;

                px += 7
        ) {

            set(
                    new Location(
                            world,
                            px,
                            y + 1,
                            terminalZ + 12
                    ),
                    Material.OAK_PLANKS
            );

            set(
                    new Location(
                            world,
                            px + 1,
                            y + 1,
                            terminalZ + 12
                    ),
                    Material.OAK_PLANKS
            );

            set(
                    new Location(
                            world,
                            px,
                            y + 2,
                            terminalZ + 12
                    ),
                    Material.OAK_SLAB
            );
        }

        // -----------------------------------------------------
        // Sicherheitskontrolle
        // -----------------------------------------------------

        fill(
                new Location(
                        world,
                        x -
                                terminalWidth / 2 +
                                5,
                        y + 1,
                        terminalZ + 20
                ),
                new Location(
                        world,
                        x - 2,
                        y + 2,
                        terminalZ + 22
                ),
                Material.IRON_BLOCK
        );

        // -----------------------------------------------------
        // Gepäckbereich
        // -----------------------------------------------------

        fill(
                new Location(
                        world,
                        x + 2,
                        y + 1,
                        terminalZ + 20
                ),
                new Location(
                        world,
                        x +
                                terminalWidth / 2 -
                                5,
                        y + 2,
                        terminalZ + 22
                ),
                Material.WHITE_CONCRETE
        );

        // -----------------------------------------------------
        // Gates
        // -----------------------------------------------------

        rebuildGates(airport);
        buildRealisticAirportDetails(airport);
        buildAdvancedInteriorDetails(airport);

        // -----------------------------------------------------
        // Tower
        // -----------------------------------------------------

        fill(
                new Location(
                        world,
                        x +
                                terminalWidth / 2 +
                                10,
                        y,
                        z + 28
                ),
                new Location(
                        world,
                        x +
                                terminalWidth / 2 +
                                14,
                        y + 18,
                        z + 32
                ),
                Material.GRAY_CONCRETE
        );

        fill(
                new Location(
                        world,
                        x +
                                terminalWidth / 2 +
                                8,
                        y + 18,
                        z + 26
                ),
                new Location(
                        world,
                        x +
                                terminalWidth / 2 +
                                16,
                        y + 20,
                        z + 34
                ),
                Material.LIGHT_BLUE_STAINED_GLASS
        );

        // -----------------------------------------------------
        // Parkplatz
        // -----------------------------------------------------

        fill(
                new Location(
                        world,
                        x -
                                terminalWidth / 2 -
                                20,
                        y,
                        z + 22
                ),
                new Location(
                        world,
                        x -
                                terminalWidth / 2 -
                                5,
                        y,
                        z + 45
                ),
                Material.BLACK_CONCRETE
        );

        // -----------------------------------------------------
        // Zufahrtsstraße
        // -----------------------------------------------------

        fill(
                new Location(
                        world,
                        x -
                                terminalWidth / 2 -
                                35,
                        y,
                        z + 15
                ),
                new Location(
                        world,
                        x -
                                terminalWidth / 2 -
                                20,
                        y,
                        z + 50
                ),
                Material.GRAY_CONCRETE
        );

        plugin.getLogger().info(
                "Airport generated: " +
                        airport.id() +
                        " at " +
                        x +
                        "," +
                        y +
                        "," +
                        z
        );

        if (
                plugin.getConfig()
                        .getBoolean(
                                "airports.save-world-after-generation",
                                true
                        )
        ) {

            world.save();
        }
    }

    // =========================================================
    // NÄCHSTER FLUGHAFEN
    // =========================================================

    public Airport nearest(
            Location location
    ) {

        if (location == null) {
            return null;
        }

        return airports
                .values()
                .stream()
                .filter(
                        airport ->
                                airport != null
                                        &&
                                airport.center() != null
                                        &&
                                airport.center().getWorld()
                                        == location.getWorld()
                )
                .min(
                        Comparator.comparingDouble(
                                airport ->
                                        airport.center()
                                                .distanceSquared(
                                                        location
                                                )
                        )
                )
                .orElse(null);
    }

    // =========================================================
    // SUCHE
    // =========================================================

    public List<Airport> find(
            String query
    ) {

        if (query == null) {
            return List.of();
        }

        String q =
                query.toLowerCase(
                        Locale.ROOT
                );

        return airports
                .values()
                .stream()
                .filter(
                        airport ->
                                airport.id()
                                        .equalsIgnoreCase(
                                                query
                                        )
                                        ||
                                airport.name()
                                        .toLowerCase(
                                                Locale.ROOT
                                        )
                                        .contains(q)
                )
                .sorted(
                        Comparator.comparing(
                                Airport::id
                        )
                )
                .toList();
    }

    // =========================================================
    // CHUNK LOAD / AUTOMATISCHE GENERIERUNG
    // =========================================================

    @EventHandler(
            ignoreCancelled = true
    )
    public void onChunkLoad(
            ChunkLoadEvent event
    ) {

        if (
                !plugin.getConfig()
                        .getBoolean(
                                "airports.village-generation.enabled",
                                true
                        )
        ) {

            return;
        }

        World world =
                event.getWorld();

        Chunk chunk =
                event.getChunk();

        String key =
                world.getUID() +
                        ":" +
                        chunk.getX() +
                        ":" +
                        chunk.getZ();

        if (
                !villageChecks.add(key)
        ) {

            return;
        }

        int radius =
                plugin.getConfig()
                        .getInt(
                                "airports.village-generation.search-radius",
                                96
                        );

        Bukkit.getScheduler()
                .runTaskLater(
                        plugin,
                        () ->
                                tryVillageAirport(
                                        world,
                                        new Location(
                                                world,
                                                chunk.getX() * 16 + 8,
                                                64,
                                                chunk.getZ() * 16 + 8
                                        ),
                                        radius
                                ),
                        2L
                );
    }

    private void tryVillageAirport(
            World world,
            Location probe,
            int radius
    ) {

        if (
                !worldAllowed(world)
        ) {

            return;
        }

        try {

            Location village =
                    world.locateNearestStructure(
                            probe,
                            StructureType.VILLAGE,
                            radius,
                            false
                    );

            if (village == null) {
                return;
            }

            String villageKey =
                    world.getUID() +
                            ":" +
                            Math.floorDiv(
                                    village.getBlockX(),
                                    64
                            ) +
                            ":" +
                            Math.floorDiv(
                                    village.getBlockZ(),
                                    64
                            );

            String checkKey =
                    "V:" +
                            villageKey;

            if (
                    villageChecks.contains(
                            checkKey
                    )
            ) {

                return;
            }

            villageChecks.add(
                    checkKey
            );

            double chance =
                    plugin.getConfig()
                            .getDouble(
                                    "airports.village-generation.chance",
                                    0.16
                            );

            Random random =
                    new Random(
                            world.getSeed()
                                    ^
                            ((long)
                                    village.getBlockX()
                                    << 32)
                                    ^
                            village.getBlockZ()
                    );

            if (
                    random.nextDouble()
                            >
                            chance
            ) {

                return;
            }

            double minimumDistance =
                    plugin.getConfig()
                            .getDouble(
                                    "airports.minimum-distance",
                                    1800
                            );

            boolean tooClose =
                    airports
                            .values()
                            .stream()
                            .anyMatch(
                                    airport ->
                                            airport.center()
                                                    .getWorld()
                                                    == world
                                                    &&
                                            airport.center()
                                                    .distance(
                                                            village
                                                    )
                                                    <
                                                    minimumDistance
                            );

            if (tooClose) {
                return;
            }

            int offsetX =
                    plugin.getConfig()
                            .getInt(
                                    "airports.village-generation.offset-x",
                                    90
                            );

            int offsetZ =
                    plugin.getConfig()
                            .getInt(
                                    "airports.village-generation.offset-z",
                                    90
                            );

            int size =
                    plugin.getConfig()
                            .getInt(
                                    "airports.village-generation.size",
                                    45
                            );

            Location site =
                    bestSite(
                            world,
                            village.getBlockX()
                                    + offsetX,
                            village.getBlockZ()
                                    + offsetZ,
                            size
                    );

            if (site == null) {
                return;
            }

            String id =
                    uniqueRegionalId(
                            site.getBlockX(),
                            site.getBlockZ()
                    );

            createAirport(
                    id,
                    "Village Regional Airport",
                    site,
                    size
            );

            if (
                    plugin.stations != null
            ) {

                plugin.stations.scanVillage(
                        world,
                        village
                );
            }

        } catch (Exception ex) {

            plugin.getLogger().fine(
                    "Village airport check skipped: " +
                            ex.getMessage()
            );
        }
    }

    // =========================================================
    // WELT PRÜFEN
    // =========================================================

    private boolean worldAllowed(
            World world
    ) {

        if (world == null) {
            return false;
        }

        List<String> allowed =
                plugin.getConfig()
                        .getStringList(
                                "airports.allowed-worlds"
                        );

        if (
                allowed == null
                        ||
                allowed.isEmpty()
        ) {

            return true;
        }

        /*
         * Wenn in der Standardconfig "world"
         * steht, aber die Welt anders heißt,
         * erlauben wir die vorhandene Hauptwelt.
         */
        if (
                allowed.size() == 1
                        &&
                allowed.contains("world")
                        &&
                Bukkit.getWorld("world") == null
        ) {

            return true;
        }

        return allowed.contains(
                world.getName()
        );
    }

    // =========================================================
    // TERRAIN-SUCHE
    // =========================================================

    private Location bestSite(
            World world,
            int centerX,
            int centerZ,
            int size
    ) {

        int radius =
                plugin.getConfig()
                        .getInt(
                                "airports.terrain-search-radius",
                                192
                        );

        return bestSite(
                world,
                centerX,
                centerZ,
                size,
                Math.max(
                        radius,
                        size * 2
                )
        );
    }

    private Location bestSite(
            World world,
            int centerX,
            int centerZ,
            int size,
            int searchRadius
    ) {
        if (world == null) {
            return null;
        }

        int step = Math.max(
                8,
                plugin.getConfig().getInt("airports.terrain-search-step", 12)
        );

        int maxHeightDifference = plugin.getConfig().getInt(
                "airports.max-terrain-height-difference", 8
        );

        int terminalWidth = plugin.getConfig().getInt(
                "airports.terminal-width", 55
        );

        int terminalDepth = plugin.getConfig().getInt(
                "airports.terminal-depth", 35
        );

        int runwayHalf = Math.max(
                90,
                plugin.getConfig().getInt("airports.runway-length", 180) / 2
        );

        // Validate the real build footprint: runway + taxiway + apron + terminal.
        int halfX = Math.max(size + 20, terminalWidth / 2 + 20);
        int minZ = -size - 35 - runwayHalf - 8;
        int maxZ = 18 + terminalDepth + 20;

        double bestScore = Double.MAX_VALUE;
        Location best = null;

        for (int x = centerX - searchRadius; x <= centerX + searchRadius; x += step) {
            for (int z = centerZ - searchRadius; z <= centerZ + searchRadius; z += step) {

                if (!isSuitable(world, x, z)) {
                    continue;
                }

                int minHeight = Integer.MAX_VALUE;
                int maxHeight = Integer.MIN_VALUE;
                boolean invalid = false;
                int sampleStep = Math.max(8, step);

                for (int sx = -halfX; sx <= halfX && !invalid; sx += sampleStep) {
                    for (int sz = minZ; sz <= maxZ; sz += sampleStep) {
                        int px = x + sx;
                        int pz = z + sz;

                        if (!isSuitable(world, px, pz)) {
                            invalid = true;
                            break;
                        }

                        int height = world.getHighestBlockYAt(px, pz);
                        Material top = world.getBlockAt(px, height, pz).getType();

                        if (top == Material.WATER || top == Material.LAVA) {
                            invalid = true;
                            break;
                        }

                        minHeight = Math.min(minHeight, height);
                        maxHeight = Math.max(maxHeight, height);

                        if (maxHeight - minHeight > maxHeightDifference) {
                            invalid = true;
                            break;
                        }
                    }
                }

                if (invalid || minHeight == Integer.MAX_VALUE) {
                    continue;
                }

                int[][] edges = {
                        {-halfX, minZ}, {0, minZ}, {halfX, minZ},
                        {-halfX, maxZ}, {0, maxZ}, {halfX, maxZ},
                        {-halfX, 0}, {halfX, 0}
                };

                for (int[] edge : edges) {
                    int px = x + edge[0];
                    int pz = z + edge[1];

                    if (!isSuitable(world, px, pz)) {
                        invalid = true;
                        break;
                    }

                    int h = world.getHighestBlockYAt(px, pz);
                    Material top = world.getBlockAt(px, h, pz).getType();

                    if (top == Material.WATER || top == Material.LAVA) {
                        invalid = true;
                        break;
                    }

                    minHeight = Math.min(minHeight, h);
                    maxHeight = Math.max(maxHeight, h);

                    if (maxHeight - minHeight > maxHeightDifference) {
                        invalid = true;
                        break;
                    }
                }

                if (invalid) {
                    continue;
                }

                double distance = Math.hypot(
                        x - centerX,
                        z - centerZ
                );

                double score =
                        distance
                                + (maxHeight - minHeight) * 40.0;

                if (score < bestScore) {
                    bestScore = score;
                    best = new Location(
                            world,
                            x,
                            maxHeight + 1,
                            z
                    );
                }
            }
        }

        return best;
    }

    // =========================================================
    // BIOM-PRÜFUNG
    // =========================================================

    private boolean isSuitable(
            World world,
            int x,
            int z
    ) {

        Biome biome =
                world.getBiome(
                        x,
                        z
                );

        String name =
                biome.getKey()
                        .getKey()
                        .toLowerCase(
                                Locale.ROOT
                        );

        if (
                name.contains("ocean")
                        ||
                name.contains("river")
                        ||
                name.contains("swamp")
                        ||
                name.contains("deep_")
        ) {

            return false;
        }

        return true;
    }

    // =========================================================
    // REGIONALE ID
    // =========================================================

    private String uniqueRegionalId(
            int x,
            int z
    ) {

        String base =
                "REG" +
                        Integer.toUnsignedString(
                                (x * 73428767)
                                        ^
                                (z * 912931),
                                36
                        ).toUpperCase(
                                Locale.ROOT
                        );

        String id =
                base.substring(
                        0,
                        Math.min(
                                7,
                                base.length()
                        )
                );

        String result =
                id;

        int number = 1;

        while (
                airports.containsKey(
                        result
                )
        ) {

            result =
                    id +
                            number++;
        }

        return result;
    }

    // =========================================================
    // /airport generate
    // =========================================================

    public boolean randomGenerateAround(
            Location location
    ) {
        if (location == null || location.getWorld() == null) {
            plugin.getLogger().warning("Airport generation: ungültige Position.");
            return false;
        }

        if (!worldAllowed(location.getWorld())) {
            plugin.getLogger().warning(
                    "Airport generation blockiert: Welt "
                            + location.getWorld().getName()
                            + " ist nicht erlaubt."
            );
            return false;
        }

        int size = plugin.getConfig().getInt(
                "airports.village-generation.size", 45
        );

        int searchRadius = Math.max(
                256,
                plugin.getConfig().getInt(
                        "airports.terrain-search-radius", 192
                )
        );

        return generateRegionalAirport(
                location,
                size,
                searchRadius,
                "Regional Airport"
        );
    }

    /**
     * Automatic airport generation while players explore the world.
     * Each grid cell is checked at most once per server session.
     */
    public void automaticGenerationTick() {
        if (!plugin.getConfig().getBoolean(
                "airports.automatic-generation.enabled", true
        ) || automaticGenerationBusy) {
            return;
        }

        int gridSize = Math.max(
                512,
                plugin.getConfig().getInt("airports.grid-size", 4096)
        );

        double chance = Math.max(
                0.0,
                Math.min(
                        1.0,
                        plugin.getConfig().getDouble(
                                "airports.generation-chance", 0.22
                        )
                )
        );

        for (org.bukkit.entity.Player player : Bukkit.getOnlinePlayers()) {
            Location loc = player.getLocation();

            if (!worldAllowed(loc.getWorld())) {
                continue;
            }

            long cellX = Math.floorDiv(loc.getBlockX(), gridSize);
            long cellZ = Math.floorDiv(loc.getBlockZ(), gridSize);

            String cellKey =
                    loc.getWorld().getUID()
                            + ":" + cellX
                            + ":" + cellZ;

            if (!automaticCells.add(cellKey)) {
                continue;
            }

            long seed =
                    loc.getWorld().getSeed()
                            ^ (cellX * 341873128712L)
                            ^ (cellZ * 132897987541L);

            if (new Random(seed).nextDouble() > chance) {
                continue;
            }

            automaticGenerationBusy = true;

            Bukkit.getScheduler().runTask(
                    plugin,
                    () -> {
                        try {
                            int size = plugin.getConfig().getInt(
                                    "airports.automatic-generation.size",
                                    plugin.getConfig().getInt(
                                            "airports.village-generation.size",
                                            45
                                    )
                            );

                            int radius = Math.max(
                                    256,
                                    plugin.getConfig().getInt(
                                            "airports.automatic-generation.search-radius",
                                            320
                                    )
                            );

                            generateRegionalAirport(
                                    loc,
                                    size,
                                    radius,
                                    "Regional Airport"
                            );
                        } finally {
                            automaticGenerationBusy = false;
                        }
                    }
            );

            return;
        }
    }

    private boolean generateRegionalAirport(
            Location location,
            int size,
            int searchRadius,
            String name
    ) {
        if (location == null || location.getWorld() == null) {
            return false;
        }

        World world = location.getWorld();

        double minimumDistance = plugin.getConfig().getDouble(
                "airports.minimum-distance", 1800
        );

        boolean tooClose = airports.values().stream().anyMatch(
                airport ->
                        airport != null
                                && airport.center() != null
                                && airport.center().getWorld() == world
                                && airport.center().distance(location)
                                < minimumDistance
        );

        if (tooClose) {
            plugin.getLogger().info(
                    "Airport generation skipped: another airport is within "
                            + (int) minimumDistance
                            + " blocks."
            );
            return false;
        }

        Location site = bestSite(
                world,
                location.getBlockX(),
                location.getBlockZ(),
                size,
                searchRadius
        );

        if (site == null) {
            plugin.getLogger().warning(
                    "Kein geeignetes Terrain für einen Flughafen gefunden."
            );
            return false;
        }

        String id = uniqueRegionalId(
                site.getBlockX(),
                site.getBlockZ()
        );

        Airport airport = createAirport(
                id,
                name,
                site,
                size
        );

        if (airport == null) {
            return false;
        }

        plugin.getLogger().info(
                "Regional airport created: "
                        + id
                        + " at "
                        + site.getBlockX()
                        + ","
                        + site.getBlockY()
                        + ","
                        + site.getBlockZ()
        );

        return true;
    }

}
