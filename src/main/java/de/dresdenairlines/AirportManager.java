package de.dresdenairlines;

import org.bukkit.*;
import org.bukkit.block.Biome;
import org.bukkit.event.*;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.StructureType;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.*;

public class AirportManager implements Listener {

    private final DresdenAirlines plugin;

    public final Map<String, Airport> airports = new LinkedHashMap<>();

    private final Set<String> villageChecks = new HashSet<>();
    private final Random random = new Random(41231);

    private final File file;
    private YamlConfiguration data;

    AirportManager(DresdenAirlines p) {
        plugin = p;

        file = new File(
                p.getDataFolder(),
                "airports.yml"
        );

        data = YamlConfiguration.loadConfiguration(file);
    }

    /**
     * Lädt alle gespeicherten Flughäfen.
     *
     * Die bereits gebauten Flughäfen werden nicht erneut gebaut.
     * Es werden nur Position, Name, Gates usw. aus airports.yml geladen.
     */
    public void load() {

        airports.clear();

        if (!file.exists()) {
            return;
        }

        data = YamlConfiguration.loadConfiguration(file);

        for (String id : data.getKeys(false)) {

            try {

                String worldName =
                        data.getString(id + ".world");

                String worldUuid =
                        data.getString(id + ".world-uuid");

                World w = null;

                if (worldUuid != null) {

                    try {

                        w = Bukkit.getWorld(
                                UUID.fromString(worldUuid)
                        );

                    } catch (IllegalArgumentException ignored) {
                    }
                }

                if (w == null && worldName != null) {
                    w = Bukkit.getWorld(worldName);
                }

                if (w == null) {

                    plugin.getLogger().warning(
                            "Airport " + id +
                                    " could not be loaded: world is not available (" +
                                    worldName + ")."
                    );

                    continue;
                }

                double x =
                        data.getDouble(id + ".x");

                double y =
                        data.getDouble(id + ".y");

                double z =
                        data.getDouble(id + ".z");

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
                            i <= Math.max(3, size / 15);
                            i++
                    ) {

                        gates.add("G" + i);
                    }
                }

                Airport a = new Airport(
                        id,
                        data.getString(
                                id + ".name",
                                id
                        ),
                        new Location(
                                w,
                                x,
                                y,
                                z
                        ),
                        size,
                        gates
                );

                a.level(
                        data.getInt(
                                id + ".level",
                                1
                        )
                );

                airports.put(id, a);

            } catch (Exception ex) {

                plugin.getLogger().warning(
                        "Could not load airport " +
                                id +
                                ": " +
                                ex.getMessage()
                );
            }
        }

        plugin.getLogger().info(
                "Loaded " +
                        airports.size() +
                        " persistent airports."
        );
    }

    /**
     * Speichert alle Flughäfen in airports.yml.
     */
    public void save() {

        YamlConfiguration y =
                new YamlConfiguration();

        for (Airport a : airports.values()) {

            String k = a.id();

            World w =
                    a.center().getWorld();

            y.set(
                    k + ".name",
                    a.name()
            );

            y.set(
                    k + ".world",
                    w.getName()
            );

            y.set(
                    k + ".world-uuid",
                    w.getUID().toString()
            );

            y.set(
                    k + ".x",
                    a.center().getX()
            );

            y.set(
                    k + ".y",
                    a.center().getY()
            );

            y.set(
                    k + ".z",
                    a.center().getZ()
            );

            y.set(
                    k + ".size",
                    a.size()
            );

            y.set(
                    k + ".gates",
                    a.gates()
            );

            y.set(
                    k + ".level",
                    a.level()
            );

            y.set(
                    k + ".generated",
                    true
            );

            y.set(
                    k + ".build-version",
                    1
            );
        }

        try {

            y.save(file);

            data = y;

        } catch (Exception ex) {

            plugin.getLogger().warning(
                    "Airport save failed: " +
                            ex.getMessage()
            );
        }
    }

    /**
     * Erstellt die vier Startflughäfen.
     */
    public void createInitial() {

        if (!airports.isEmpty()) {
            return;
        }

        World w =
                Bukkit.getWorlds()
                        .stream()
                        .filter(this::worldAllowed)
                        .findFirst()
                        .orElse(null);

        if (w == null) {

            plugin.getLogger().warning(
                    "No allowed world found for automatic airport generation. " +
                            "Check airports.allowed-worlds in config.yml."
            );

            return;
        }

        createInitialAirport(
                w,
                "DRE",
                "Dresden International",
                0,
                0,
                65
        );

        createInitialAirport(
                w,
                "LEJ",
                "Leipzig Regional",
                2600,
                0,
                45
        );

        createInitialAirport(
                w,
                "BER",
                "Berlin International",
                -2800,
                -2200,
                70
        );

        createInitialAirport(
                w,
                "MUC",
                "Munich International",
                4200,
                3100,
                75
        );

        save();
    }

    /**
     * Erstellt einen Startflughafen.
     */
    private void createInitialAirport(
            World w,
            String id,
            String name,
            int x,
            int z,
            int size
    ) {

        if (airports.containsKey(id)) {
            return;
        }

        Location site =
                bestSite(
                        w,
                        x,
                        z,
                        size
                );

        /*
         * Terralith oder stark hügeliges Gelände:
         * zweiter Versuch mit größerem Suchradius.
         */
        if (site == null) {

            site =
                    bestSite(
                            w,
                            x,
                            z,
                            Math.max(size, 35),
                            512
                    );
        }

        if (site == null) {

            plugin.getLogger().warning(
                    "Could not find a suitable site for airport " +
                            id +
                            " near " +
                            x +
                            "," +
                            z +
                            "."
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

    /**
     * Erstellt einen Flughafen und baut ihn direkt in die Welt.
     */
    public Airport createAirport(
            String id,
            String name,
            Location c,
            int size
    ) {

        id =
                id.toUpperCase(
                        Locale.ROOT
                );

        if (airports.containsKey(id)) {
            return airports.get(id);
        }

        List<String> gates =
                new ArrayList<>();

        for (
                int i = 1;
                i <= Math.max(3, size / 15);
                i++
        ) {

            gates.add(
                    "G" + i
            );
        }

        Airport a =
                new Airport(
                        id,
                        name,
                        c,
                        size,
                        gates
                );

        airports.put(
                id,
                a
        );

        build(a);

        save();

        return a;
    }

    /**
     * Setzt einen Block.
     */
    private void set(
            Location l,
            Material m
    ) {

        l.getBlock().setType(m);
    }

    /**
     * Füllt einen Bereich mit einem Block.
     */
    private void fill(
            Location a,
            Location b,
            Material m
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
                            m
                    );
                }
            }
        }
    }

    /**
     * Baut den kompletten Flughafen.
     */
    private void build(Airport a) {

        Location c =
                a.center();

        int y =
                c.getBlockY();

        World w =
                c.getWorld();

        int s =
                a.size();

        /*
         * Untergrund angleichen.
         */
        int padRadius =
                s + 20;

        for (
                int x =
                        c.getBlockX() -
                                padRadius;

                x <=
                        c.getBlockX() +
                                padRadius;

                x += 2
        ) {

            for (
                    int z =
                            c.getBlockZ() -
                                    padRadius;

                    z <=
                            c.getBlockZ() +
                                    padRadius;

                    z += 2
            ) {

                int top =
                        w.getHighestBlockYAt(
                                x,
                                z
                        );

                if (top < y) {

                    fill(
                            new Location(
                                    w,
                                    x,
                                    top + 1,
                                    z
                            ),
                            new Location(
                                    w,
                                    x,
                                    y - 1,
                                    z
                            ),
                            Material.STONE
                    );
                }
            }
        }

        /*
         * Vorfeld.
         */
        int apronDepth =
                Math.max(
                        45,
                        s
                );

        fill(
                new Location(
                        w,
                        c.getBlockX() - s,
                        y,
                        c.getBlockZ() - s / 3
                ),
                new Location(
                        w,
                        c.getBlockX() + s,
                        y,
                        c.getBlockZ() + s / 3
                ),
                Material.POLISHED_ANDESITE
        );

        /*
         * Start- und Landebahn.
         */
        int runwayHalf =
                Math.max(
                        90,
                        plugin.getConfig()
                                .getInt(
                                        "airports.runway-length",
                                        180
                                ) / 2
                );

        int rz =
                c.getBlockZ() -
                        s -
                        35;

        fill(
                new Location(
                        w,
                        c.getBlockX() - 7,
                        y,
                        rz - runwayHalf
                ),
                new Location(
                        w,
                        c.getBlockX() + 7,
                        y,
                        rz + runwayHalf
                ),
                Material.BLACK_CONCRETE
        );

        /*
         * Weiße Mittellinie.
         */
        for (
                int z =
                        rz - runwayHalf + 10;

                z <
                        rz + runwayHalf - 10;

                z += 12
        ) {

            fill(
                    new Location(
                            w,
                            c.getBlockX() - 1,
                            y + 1,
                            z
                    ),
                    new Location(
                            w,
                            c.getBlockX() + 1,
                            y + 1,
                            z + 5
                    ),
                    Material.WHITE_CONCRETE
            );
        }

        /*
         * Taxiway.
         */
        fill(
                new Location(
                        w,
                        c.getBlockX() - 4,
                        y,
                        c.getBlockZ() - s
                ),
                new Location(
                        w,
                        c.getBlockX() + 4,
                        y,
                        rz
                ),
                Material.GRAY_CONCRETE
        );

        /*
         * Taxiway-Beleuchtung.
         */
        for (
                int x =
                        c.getBlockX() - s;

                x <=
                        c.getBlockX() + s;

                x += 8
        ) {

            set(
                    new Location(
                            w,
                            x,
                            y + 1,
                            c.getBlockZ()
                    ),
                    Material.SEA_LANTERN
            );
        }

        /*
         * Terminal.
         */
        int tw =
                plugin.getConfig()
                        .getInt(
                                "airports.terminal-width",
                                55
                        );

        int td =
                plugin.getConfig()
                        .getInt(
                                "airports.terminal-depth",
                                35
                        );

        int tz =
                c.getBlockZ() +
                        18;

        fill(
                new Location(
                        w,
                        c.getBlockX() - tw / 2,
                        y,
                        tz
                ),
                new Location(
                        w,
                        c.getBlockX() + tw / 2,
                        y + 1,
                        tz + td
                ),
                Material.SMOOTH_QUARTZ
        );

        /*
         * Glasfront.
         */
        fill(
                new Location(
                        w,
                        c.getBlockX() - tw / 2,
                        y + 2,
                        tz
                ),
                new Location(
                        w,
                        c.getBlockX() + tw / 2,
                        y + 2,
                        tz
                ),
                Material.GLASS
        );

        fill(
                new Location(
                        w,
                        c.getBlockX() - tw / 2 + 2,
                        y + 2,
                        tz + td - 1
                ),
                new Location(
                        w,
                        c.getBlockX() + tw / 2 - 2,
                        y + 2,
                        tz + td - 1
                ),
                Material.GLASS
        );

        /*
         * Terminal-Innenraum:
         * Check-in.
         */
        int mid =
                c.getBlockX();

        for (
                int x =
                        mid - tw / 2 + 4;

                x <
                        mid + tw / 2 - 3;

                x += 5
        ) {

            fill(
                    new Location(
                            w,
                            x,
                            y + 1,
                            tz + 4
                    ),
                    new Location(
                            w,
                            x + 2,
                            y + 2,
                            tz + 5
                    ),
                    Material.SMOOTH_STONE
            );

            set(
                    new Location(
                            w,
                            x + 1,
                            y + 3,
                            tz + 4
                    ),
                    Material.LECTERN
            );
        }

        /*
         * Sitzplätze.
         */
        for (
                int x =
                        mid - tw / 2 + 5;

                x <
                        mid + tw / 2 - 4;

                x += 7
        ) {

            fill(
                    new Location(
                            w,
                            x,
                            y + 1,
                            tz + 12
                    ),
                    new Location(
                            w,
                            x + 2,
                            y + 1,
                            tz + 14
                    ),
                    Material.OAK_PLANKS
            );

            set(
                    new Location(
                            w,
                            x + 1,
                            y + 2,
                            tz + 12
                    ),
                    Material.OAK_SLAB
            );
        }

        /*
         * Sicherheitskontrolle.
         */
        fill(
                new Location(
                        w,
                        mid - tw / 2 + 5,
                        y + 1,
                        tz + 20
                ),
                new Location(
                        w,
                        mid - 2,
                        y + 2,
                        tz + 22
                ),
                Material.IRON_BLOCK
        );

        /*
         * Gepäckbereich.
         */
        fill(
                new Location(
                        w,
                        mid + 2,
                        y + 1,
                        tz + 20
                ),
                new Location(
                        w,
                        mid + tw / 2 - 5,
                        y + 2,
                        tz + 22
                ),
                Material.WHITE_CONCRETE
        );

        /*
         * Gates und Jet-Brücken.
         */
        for (
                int i = 0;
                i < a.gates().size();
                i++
        ) {

            Location g =
                    a.gate(
                            a.gates().get(i)
                    );

            fill(
                    g.clone().add(
                            -5,
                            0,
                            -3
                    ),
                    g.clone().add(
                            5,
                            0,
                            3
                    ),
                    Material.LIGHT_GRAY_CONCRETE
            );

            set(
                    g.clone().add(
                            0,
                            1,
                            0
                    ),
                    Material.SEA_LANTERN
            );

            fill(
                    g.clone().add(
                            5,
                            1,
                            0
                    ),
                    g.clone().add(
                            12,
                            2,
                            1
                    ),
                    Material.GLASS
            );
        }

        /*
         * Tower.
         */
        fill(
                new Location(
                        w,
                        c.getBlockX() +
                                tw / 2 +
                                10,
                        y,
                        c.getBlockZ() +
                                28
                ),
                new Location(
                        w,
                        c.getBlockX() +
                                tw / 2 +
                                14,
                        y + 18,
                        c.getBlockZ() +
                                32
                ),
                Material.GRAY_CONCRETE
        );

        fill(
                new Location(
                        w,
                        c.getBlockX() +
                                tw / 2 +
                                8,
                        y + 18,
                        c.getBlockZ() +
                                26
                ),
                new Location(
                        w,
                        c.getBlockX() +
                                tw / 2 +
                                16,
                        y + 20,
                        c.getBlockZ() +
                                34
                ),
                Material.LIGHT_BLUE_STAINED_GLASS
        );

        /*
         * Parkplatz.
         */
        fill(
                new Location(
                        w,
                        c.getBlockX() -
                                tw / 2 -
                                20,
                        y,
                        c.getBlockZ() +
                                22
                ),
                new Location(
                        w,
                        c.getBlockX() -
                                tw / 2 -
                                5,
                        y,
                        c.getBlockZ() +
                                45
                ),
                Material.BLACK_CONCRETE
        );

        /*
         * Zufahrtsstraße.
         */
        fill(
                new Location(
                        w,
                        c.getBlockX() -
                                tw / 2 -
                                35,
                        y,
                        c.getBlockZ() +
                                15
                ),
                new Location(
                        w,
                        c.getBlockX() -
                                tw / 2 -
                                20,
                        y,
                        c.getBlockZ() +
                                50
                ),
                Material.GRAY_CONCRETE
        );

        plugin.getLogger().info(
                "Airport generated: " +
                        a.id() +
                        " at " +
                        c.getBlockX() +
                        "," +
                        c.getBlockY() +
                        "," +
                        c.getBlockZ()
        );

        if (
                plugin.getConfig()
                        .getBoolean(
                                "airports.save-world-after-generation",
                                true
                        )
        ) {

            w.save();
        }
    }

    /**
     * Findet den nächstgelegenen Flughafen.
     */
    public Airport nearest(Location l) {

        return airports
                .values()
                .stream()
                .filter(
                        a ->
                                a.center()
                                        .getWorld()
                                        == l.getWorld()
                )
                .min(
                        Comparator.comparingDouble(
                                a ->
                                        a.center()
                                                .distanceSquared(l)
                        )
                )
                .orElse(null);
    }

    /**
     * Sucht Flughäfen anhand ID oder Name.
     */
    public List<Airport> find(String query) {

        String q =
                query.toLowerCase(
                        Locale.ROOT
                );

        return airports
                .values()
                .stream()
                .filter(
                        a ->
                                a.id()
                                        .equalsIgnoreCase(query)
                                        ||
                                a.name()
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

    /**
     * Wird beim Laden eines Chunks ausgeführt.
     *
     * Kann automatisch in der Nähe von Dörfern
     * einen Flughafen erzeugen.
     */
    @EventHandler(ignoreCancelled = true)
    public void onChunkLoad(
            ChunkLoadEvent e
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

        World w =
                e.getWorld();

        Chunk ch =
                e.getChunk();

        String key =
                w.getUID() +
                        ":" +
                        ch.getX() +
                        ":" +
                        ch.getZ();

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
                                        w,
                                        new Location(
                                                w,
                                                ch.getX() * 16 + 8,
                                                64,
                                                ch.getZ() * 16 + 8
                                        ),
                                        radius
                                ),
                        2L
                );
    }

    /**
     * Prüft, ob sich ein Dorf in der Nähe befindet
     * und erzeugt gegebenenfalls einen Regional-Flughafen.
     */
    private void tryVillageAirport(
            World w,
            Location probe,
            int radius
    ) {

        if (
                !worldAllowed(w)
        ) {

            return;
        }

        try {

            Location village =
                    w.locateNearestStructure(
                            probe,
                            StructureType.VILLAGE,
                            radius,
                            false
                    );

            if (village == null) {
                return;
            }

            String vk =
                    w.getUID() +
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

            if (
                    villageChecks.contains(
                            "V:" + vk
                    )
            ) {

                return;
            }

            villageChecks.add(
                    "V:" + vk
            );

            /*
             * Zufallswahrscheinlichkeit.
             */
            if (
                    new Random(
                            village.getWorld().getSeed()
                                    ^
                            ((long) village.getBlockX() << 32)
                                    ^
                            village.getBlockZ()
                    ).nextDouble()
                            >
                    plugin.getConfig()
                            .getDouble(
                                    "airports.village-generation.chance",
                                    0.16
                            )
            ) {

                return;
            }

            /*
             * Mindestabstand zu anderen Flughäfen.
             */
            double minDist =
                    plugin.getConfig()
                            .getDouble(
                                    "airports.minimum-distance",
                                    1800
                            );

            if (
                    airports.values()
                            .stream()
                            .anyMatch(
                                    a ->
                                            a.center()
                                                    .getWorld()
                                                    == w
                                                    &&
                                            a.center()
                                                    .distance(
                                                            village
                                                    )
                                                    < minDist
                            )
            ) {

                return;
            }

            int x =
                    village.getBlockX()
                            +
                    plugin.getConfig()
                            .getInt(
                                    "airports.village-generation.offset-x",
                                    90
                            );

            int z =
                    village.getBlockZ()
                            +
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
                            w,
                            x,
                            z,
                            size
                    );

            if (site == null) {
                return;
            }

            x =
                    site.getBlockX();

            z =
                    site.getBlockZ();

            int y =
                    site.getBlockY();

            String id =
                    uniqueRegionalId(
                            x,
                            z
                    );

            createAirport(
                    id,
                    "Village Regional Airport",
                    new Location(
                            w,
                            x,
                            y,
                            z
                    ),
                    size
            );

            if (
                    plugin.stations != null
            ) {

                plugin.stations.scanVillage(
                        w,
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

    /**
     * Prüft, ob eine Welt für Flughäfen zugelassen ist.
     */
    private boolean worldAllowed(
            World w
    ) {

        var worlds =
                plugin.getConfig()
                        .getStringList(
                                "airports.allowed-worlds"
                        );

        /*
         * Keine Liste = alle Welten.
         */
        if (
                worlds.isEmpty()
        ) {

            return true;
        }

        /*
         * "world" ist standardmäßig eingetragen.
         *
         * Wenn der Server eine andere Hauptwelt verwendet
         * und keine Welt namens "world" existiert,
         * wird die aktuelle Welt trotzdem erlaubt.
         */
        if (
                worlds.size() == 1
                        &&
                worlds.contains("world")
                        &&
                Bukkit.getWorld("world") == null
        ) {

            return true;
        }

        return worlds.contains(
                w.getName()
        );
    }

    /**
     * Sucht einen geeigneten Bauplatz.
     */
    private Location bestSite(
            World w,
            int cx,
            int cz,
            int size
    ) {

        return bestSite(
                w,
                cx,
                cz,
                size,
                Math.max(
                        size * 2,
                        plugin.getConfig()
                                .getInt(
                                        "airports.terrain-search-radius",
                                        192
                                )
                )
        );
    }

    /**
     * Sucht einen geeigneten Bauplatz
     * mit einem frei wählbaren Suchradius.
     */
    private Location bestSite(
            World w,
            int cx,
            int cz,
            int size,
            int search
    ) {

        int step =
                Math.max(
                        8,
                        plugin.getConfig()
                                .getInt(
                                        "airports.terrain-search-step",
                                        12
                                )
                );

        search =
                Math.max(
                        size * 2,
                        search
                );

        double best =
                Double.MAX_VALUE;

        Location chosen =
                null;

        for (
                int x =
                        cx - search;

                x <=
                        cx + search;

                x += step
        ) {

            for (
                    int z =
                            cz - search;

                    z <=
                            cz + search;

                    z += step
            ) {

                if (
                        !isSuitable(
                                w,
                                x,
                                z
                        )
                ) {

                    continue;
                }

                int half =
                        Math.max(
                                10,
                                size / 2
                        );

                int min =
                        999;

                int max =
                        -999;

                boolean water =
                        false;

                for (
                        int sx = -half;

                        sx <= half;

                        sx += Math.max(
                                4,
                                step
                        )
                ) {

                    for (
                            int sz = -half;

                            sz <= half;

                            sz += Math.max(
                                    4,
                                    step
                            )
                    ) {

                        int yy =
                                w.getHighestBlockYAt(
                                        x + sx,
                                        z + sz
                                );

                        min =
                                Math.min(
                                        min,
                                        yy
                                );

                        max =
                                Math.max(
                                        max,
                                        yy
                                );

                        Material top =
                                w.getBlockAt(
                                        x + sx,
                                        yy,
                                        z + sz
                                ).getType();

                        if (
                                top == Material.WATER
                                        ||
                                top == Material.LAVA
                        ) {

                            water = true;
                        }
                    }
                }

                int slope =
                        max - min;

                if (
                        water
                                ||
                        slope >
                                plugin.getConfig()
                                        .getInt(
                                                "airports.max-terrain-height-difference",
                                                8
                                        )
                ) {

                    continue;
                }

                double score =
                        Math.hypot(
                                x - cx,
                                z - cz
                        )
                                +
                        slope * 25;

                if (
                        score < best
                ) {

                    best =
                            score;

                    chosen =
                            new Location(
                                    w,
                                    x,
                                    max + 1,
                                    z
                            );
                }
            }
        }

        return chosen;
    }

    /**
     * Prüft das Biom.
     *
     * Ozeane, Flüsse, Sümpfe usw. werden ausgeschlossen.
     */
    private boolean isSuitable(
            World w,
            int x,
            int z
    ) {

        Biome b =
                w.getBiome(
                        x,
                        z
                );

        String n =
                b.getKey()
                        .getKey();

        return !n.contains("ocean")
                &&
                !n.contains("river")
                &&
                !n.contains("swamp")
                &&
                !n.contains("deep_");
    }

    /**
     * Erstellt eine eindeutige ID
     * für automatisch generierte Regional-Flughäfen.
     */
    private String uniqueRegionalId(
            int x,
            int z
    ) {

        String base =
                "REG"
                        +
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

        int i = 1;

        String out = id;

        while (
                airports.containsKey(out)
        ) {

            out =
                    id +
                            (i++);
        }

        return out;
    }

    /**
     * Wird von /airport generate verwendet.
     *
     * Sucht direkt in der Umgebung des Spielers
     * nach geeignetem Terrain.
     */
    public void randomGenerateAround(
            Location l
    ) {

        if (
                l == null
                        ||
                l.getWorld() == null
        ) {

            return;
        }

        /*
         * Welt überprüfen.
         */
        if (
                !worldAllowed(
                        l.getWorld()
                )
        ) {

            plugin.getLogger().warning(
                    "Airport generation blocked because world is not in airports.allowed-worlds: "
                            +
                            l.getWorld().getName()
            );

            return;
        }

        /*
         * Mindestabstand zu bereits vorhandenen Flughäfen.
         */
        double minDist =
                plugin.getConfig()
                        .getDouble(
                                "airports.minimum-distance",
                                1800
                        );

        if (
                airports.values()
                        .stream()
                        .anyMatch(
                                a ->
                                        a.center()
                                                .getWorld()
                                                == l.getWorld()
                                                &&
                                        a.center()
                                                .distance(l)
                                                < minDist
                        )
        ) {

            plugin.getLogger().info(
                    "Airport generation skipped: an airport is already within "
                            +
                            (int) minDist +
                            " blocks."
            );

            return;
        }

        int size =
                plugin.getConfig()
                        .getInt(
                                "airports.village-generation.size",
                                45
                        );

        /*
         * Für manuelle Generierung wird mindestens
         * ein Radius von 256 Blöcken verwendet.
         */
        Location site =
                bestSite(
                        l.getWorld(),
                        l.getBlockX(),
                        l.getBlockZ(),
                        size,
                        Math.max(
                                256,
                                plugin.getConfig()
                                        .getInt(
                                                "airports.terrain-search-radius",
                                                192
                                        )
                        )
                );

        if (
                site == null
        ) {

            plugin.getLogger().warning(
                    "No suitable terrain found for a new airport near "
                            +
                            l.getBlockX()
                            +
                            ","
                            +
                            l.getBlockZ()
                            +
                            "."
            );

            return;
        }

        String id =
                uniqueRegionalId(
                        site.getBlockX(),
                        site.getBlockZ()
                );

        createAirport(
                id,
                "Regional Airport",
                site,
                size
        );
    }
}
