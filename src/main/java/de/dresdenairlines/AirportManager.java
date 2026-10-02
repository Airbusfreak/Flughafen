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

        fill(
                gate.clone().add(-1, 1, 5),
                gate.clone().add(1, 3, 8),
                Material.SMOOTH_QUARTZ
        );
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

        // Simple but complete level-1 terminal shell.
        fill(
                new Location(world, x - terminalWidth / 2, y + 3, terminalZ),
                new Location(world, x + terminalWidth / 2, y + 3, terminalZ + terminalDepth),
                Material.SMOOTH_QUARTZ
        );

        fill(
                new Location(world, x - terminalWidth / 2, y + 2, terminalZ),
                new Location(world, x - terminalWidth / 2, y + 3, terminalZ + terminalDepth),
                Material.SMOOTH_QUARTZ
        );

        fill(
                new Location(world, x + terminalWidth / 2, y + 2, terminalZ),
                new Location(world, x + terminalWidth / 2, y + 3, terminalZ + terminalDepth),
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
                "airports.automatic-generation", true
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
