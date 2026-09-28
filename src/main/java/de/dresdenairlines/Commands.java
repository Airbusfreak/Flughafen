package de.dresdenairlines;

import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.bukkit.Location;

import java.util.*;

public final class Commands implements TabExecutor {

    final DresdenAirlines p;

    Commands(DresdenAirlines p) {
        this.p = p;

        // TAB-Vervollständigung für alle DresdenAirlines-Befehle
        if (p.getCommand("flight") != null) {
            p.getCommand("flight").setTabCompleter(this);
        }

        if (p.getCommand("airline") != null) {
            p.getCommand("airline").setTabCompleter(this);
        }

        if (p.getCommand("airport") != null) {
            p.getCommand("airport").setTabCompleter(this);
        }
    }

    @Override
    public boolean onCommand(
            CommandSender s,
            Command c,
            String l,
            String[] a
    ) {

        if (!(s instanceof Player pl)) {
            return true;
        }

        /*
         * =========================================================
         * /flight
         * =========================================================
         */
        if (c.getName().equalsIgnoreCase("flight")) {

            if (a.length == 0) {
                p.flightGUI.open(pl);
                return true;
            }

            if (a[0].equalsIgnoreCase("money")) {

                pl.sendMessage(
                        "§6Kontostand: §f"
                                + p.passengers.money(
                                        pl.getUniqueId()
                                )
                );

                return true;
            }

            if (
                    a[0].equalsIgnoreCase("book")
                            && a.length >= 2
            ) {

                String err =
                        p.passengers.bookNext(
                                pl,
                                a[1].toUpperCase()
                        );

                if (err != null) {
                    pl.sendMessage(err);
                }

                return true;
            }

            if (
                    a[0].equalsIgnoreCase("give")
                            && a.length >= 3
                            && pl.hasPermission(
                                    "dresdenairlines.admin"
                            )
            ) {

                try {

                    Player t =
                            org.bukkit.Bukkit.getPlayerExact(
                                    a[1]
                            );

                    double amount =
                            Double.parseDouble(a[2]);

                    if (
                            t == null
                                    || amount <= 0
                    ) {

                        pl.sendMessage(
                                "§cSpieler oder Betrag ungültig."
                        );

                        return true;
                    }

                    p.passengers.wallet.deposit(
                            t.getUniqueId(),
                            amount
                    );

                    pl.sendMessage(
                            "§a"
                                    + amount
                                    + " $ an "
                                    + t.getName()
                                    + " gegeben."
                    );

                    t.sendMessage(
                            "§aDu hast "
                                    + amount
                                    + " $ erhalten."
                    );

                } catch (Exception ex) {

                    pl.sendMessage(
                            "§cBetrag ungültig."
                    );
                }

                return true;
            }

            if (
                    a[0].equalsIgnoreCase("take")
                            && a.length >= 3
                            && pl.hasPermission(
                                    "dresdenairlines.admin"
                            )
            ) {

                try {

                    Player t =
                            org.bukkit.Bukkit.getPlayerExact(
                                    a[1]
                            );

                    double amount =
                            Double.parseDouble(a[2]);

                    if (
                            t == null
                                    || amount <= 0
                    ) {

                        pl.sendMessage(
                                "§cSpieler oder Betrag ungültig."
                        );

                        return true;
                    }

                    boolean ok =
                            p.passengers.wallet.withdraw(
                                    t.getUniqueId(),
                                    amount
                            );

                    pl.sendMessage(
                            ok
                                    ? "§c"
                                    + amount
                                    + " $ von "
                                    + t.getName()
                                    + " genommen."
                                    : "§cSpieler hat nicht genug Geld."
                    );

                    if (ok) {

                        t.sendMessage(
                                "§cDir wurden "
                                        + amount
                                        + " $ abgezogen."
                        );
                    }

                } catch (Exception ex) {

                    pl.sendMessage(
                            "§cBetrag ungültig."
                    );
                }

                return true;
            }

            pl.sendMessage(
                    "§b/flight §7– Flüge auswählen"
                            + " | §b/flight book <Ziel>"
                            + " §7– direkt buchen"
                            + " | §b/flight money"
            );

            return true;
        }

        /*
         * =========================================================
         * /airline
         * =========================================================
         */
        if (c.getName().equalsIgnoreCase("airline")) {

            if (a.length == 0) {

                p.airlineGUI.open(pl);
                return true;
            }

            Airline al =
                    p.storage.airlines.get(
                            pl.getUniqueId()
                    );

            switch (a[0].toLowerCase()) {

                /*
                 * /airline create
                 */
                case "create" -> {

                    if (a.length < 2) {

                        pl.sendMessage(
                                "§c/airline create <Name> [Code]"
                        );

                        return true;
                    }

                    if (al != null) {

                        pl.sendMessage(
                                "§cDu hast bereits eine Airline."
                        );

                        return true;
                    }

                    String code =
                            a.length > 2
                                    ? a[2].toUpperCase()
                                    : "AIR";

                    if (code.length() > 4) {

                        pl.sendMessage(
                                "§cCode max. 4 Zeichen."
                        );

                        return true;
                    }

                    al =
                            new Airline(
                                    pl.getUniqueId(),
                                    a[1],
                                    code,
                                    "DRE",
                                    p.getConfig().getDouble(
                                            "economy.starting-money"
                                    )
                            );

                    p.storage.airlines.put(
                            pl.getUniqueId(),
                            al
                    );

                    p.storage.save();

                    pl.sendMessage(
                            "§aAirline gegründet."
                    );

                    p.airlineGUI.open(pl);
                }

                /*
                 * /airline buy
                 */
                case "buy" -> {

                    if (al == null) {

                        pl.sendMessage(
                                "§cErstelle zuerst eine Airline."
                        );

                        return true;
                    }

                    if (a.length < 2) {

                        pl.sendMessage(
                                "§cTypen: A220-300, A320, A330-300, A350-900, ERJ195"
                        );

                        return true;
                    }

                    Aircraft ac =
                            p.airlines.buy(
                                    al,
                                    a[1]
                            );

                    pl.sendMessage(
                            ac == null
                                    ? "§cKauf nicht möglich (Geld/Typ)."
                                    : "§aGekauft: "
                                    + ac.type
                                    + " • "
                                    + ac.registration
                                    + " • "
                                    + ac.id
                    );
                }

                /*
                 * /airline route
                 */
                case "route" -> {

                    if (
                            al == null
                                    || a.length < 5
                    ) {

                        pl.sendMessage(
                                "§c/airline route <von> <nach> <Flugzeug-ID> <Preis>"
                        );

                        return true;
                    }

                    try {

                        boolean ok =
                                p.airlines.addRoute(
                                        al,
                                        a[1].toUpperCase(),
                                        a[2].toUpperCase(),
                                        a[3],
                                        Integer.parseInt(a[4]),
                                        300
                                );

                        pl.sendMessage(
                                ok
                                        ? "§aRoute eingerichtet."
                                        : "§cRoute konnte nicht eingerichtet werden."
                        );

                    } catch (Exception ex) {

                        pl.sendMessage(
                                "§cUngültige Eingabe."
                        );
                    }
                }

                /*
                 * /airline fleet
                 */
                case "fleet" -> {

                    if (al == null) {

                        pl.sendMessage(
                                "§cKeine Airline."
                        );

                        return true;
                    }

                    if (al.fleet.isEmpty()) {

                        pl.sendMessage(
                                "§eDeine Flotte ist noch leer."
                        );

                        return true;
                    }

                    pl.sendMessage(
                            "§b═══ Deine Flotte ═══"
                    );

                    for (Aircraft x : al.fleet) {

                        if (x == null) {
                            continue;
                        }

                        pl.sendMessage(
                                "§b"
                                        + x.id
                                        + " §7"
                                        + x.type
                                        + " §8"
                                        + x.registration
                                        + " §7"
                                        + x.seats
                                        + " Sitze"
                        );
                    }
                }

                /*
                 * /airline routes
                 */
                case "routes" -> {

                    if (al == null) {

                        pl.sendMessage(
                                "§cKeine Airline."
                        );

                        return true;
                    }

                    if (al.routes.isEmpty()) {

                        pl.sendMessage(
                                "§eDeine Airline hat noch keine Routen."
                        );

                        return true;
                    }

                    pl.sendMessage(
                            "§e═══ Deine Routen ═══"
                    );

                    for (Route r : al.routes) {

                        pl.sendMessage(
                                "§e"
                                        + r.from
                                        + " → "
                                        + r.to
                                        + " §7Preis "
                                        + r.ticketPrice
                                        + "$ §8"
                                        + r.aircraftId
                        );
                    }
                }

                /*
                 * /airline info
                 */
                case "info" -> {

                    if (al == null) {

                        pl.sendMessage(
                                "§cKeine Airline."
                        );

                        return true;
                    }

                    /*
                     * Finale Kopie für die Lambdas.
                     */
                    final Airline currentAirline = al;

                    long active =
                            p.storage.flights.values()
                                    .stream()
                                    .filter(
                                            f ->
                                                    f.airline
                                                            == currentAirline
                                                            && f.status
                                                            != FlightStatus.LANDED
                                    )
                                    .count();

                    long pax =
                            p.storage.flights.values()
                                    .stream()
                                    .filter(
                                            f ->
                                                    f.airline
                                                            == currentAirline
                                    )
                                    .mapToLong(
                                            f ->
                                                    f.booked
                                                            + f.npcBooked
                                    )
                                    .sum();

                    pl.sendMessage(
                            "§b"
                                    + currentAirline.name
                                    + " §7("
                                    + currentAirline.code
                                    + ") §f"
                                    + String.format(
                                            "%.0f",
                                            currentAirline.money
                                    )
                                    + "$ §7Ruf "
                                    + String.format(
                                            "%.1f",
                                            currentAirline.reputation
                                    )
                                    + " §7| Flotte "
                                    + currentAirline.fleet.size()
                                    + " | aktive Flüge "
                                    + active
                                    + " | Passagiere "
                                    + pax
                    );
                }

                /*
                 * /airline ranking
                 */
                case "ranking" -> {

                    List<Airline> list =
                            new ArrayList<>(
                                    p.storage.airlines.values()
                            );

                    list.sort(
                            Comparator.comparingDouble(
                                    (Airline x) ->
                                            x.money
                                                    + x.reputation * 10000
                                                    + x.fleet.size() * 1000
                            ).reversed()
                    );

                    pl.sendMessage(
                            "§6═══ Airline-Rangliste ═══"
                    );

                    int rank = 1;

                    for (
                            Airline x :
                            list.stream()
                                    .limit(10)
                                    .toList()
                    ) {

                        pl.sendMessage(
                                "§e"
                                        + rank++
                                        + ". §f"
                                        + x.name
                                        + " §7| §a"
                                        + String.format(
                                                "%.0f",
                                                x.money
                                        )
                                        + "$ §7| Ruf §e"
                                        + String.format(
                                                "%.1f",
                                                x.reputation
                                        )
                                        + " §7| Flotte §f"
                                        + x.fleet.size()
                        );
                    }
                }
            }

            return true;
        }

        /*
         * =========================================================
         * /airport
         * =========================================================
         */

        if (!c.getName().equalsIgnoreCase("airport")) {
            return true;
        }

        if (a.length == 0) {

            pl.sendMessage(
                    "§b/airport list"
                            + " §7| §b/airport locate [ID/Name]"
                            + " §7| §b/airport info"
                            + " §7| §b/airport generate"
                            + " §7| §b/airport stations"
            );

            return true;
        }

        /*
         * /airport stations
         */
        if (a[0].equalsIgnoreCase("stations")) {

            if (
                    p.stations == null
                            || p.stations.stations == null
                            || p.stations.stations.isEmpty()
            ) {

                pl.sendMessage(
                        "§eEs wurden noch keine Stationen gefunden."
                );

                return true;
            }

            pl.sendMessage(
                    "§b═══ Flughafen-Stationen ═══"
            );

            for (
                    StationManager.Station st :
                    p.stations.stations.values()
            ) {

                if (
                        st == null
                                || st.station() == null
                ) {
                    continue;
                }

                pl.sendMessage(
                        "§b"
                                + st.id()
                                + " §7"
                                + st.name()
                                + " §8["
                                + st.station().getBlockX()
                                + ", "
                                + st.station().getBlockY()
                                + ", "
                                + st.station().getBlockZ()
                                + "]"
                );
            }

            return true;
        }

        /*
         * /airport list
         */
        if (a[0].equalsIgnoreCase("list")) {

            try {

                if (
                        p.airports == null
                                || p.airports.airports == null
                                || p.airports.airports.isEmpty()
                ) {

                    pl.sendMessage(
                            "§eEs sind noch keine Flughäfen registriert."
                    );

                    return true;
                }

                pl.sendMessage(
                        "§b═══ Flughäfen ═══"
                );

                int count = 0;

                for (
                        Airport x :
                        p.airports.airports.values()
                ) {

                    if (
                            x == null
                                    || x.center() == null
                    ) {
                        continue;
                    }

                    Location center =
                            x.center();

                    pl.sendMessage(
                            "§e"
                                    + x.id()
                                    + " §7- "
                                    + x.name()
                                    + " §8["
                                    + center.getBlockX()
                                    + ", "
                                    + center.getBlockY()
                                    + ", "
                                    + center.getBlockZ()
                                    + "]"
                    );

                    count++;
                }

                if (count == 0) {

                    pl.sendMessage(
                            "§eEs wurden keine gültigen Flughäfen gefunden."
                    );
                }

            } catch (Exception ex) {

                pl.sendMessage(
                        "§cDie Flughafenliste konnte nicht geladen werden."
                );

                p.getLogger().warning(
                        "Fehler bei /airport list: "
                                + ex.getMessage()
                );

                ex.printStackTrace();
            }

            return true;
        }

        /*
         * /airport locate
         */
        if (a[0].equalsIgnoreCase("locate")) {

            if (a.length == 1) {

                Airport x =
                        p.airports.nearest(
                                pl.getLocation()
                        );

                if (x != null) {

                    sendLocate(
                            pl,
                            x
                    );

                } else {

                    pl.sendMessage(
                            "§cKein Flughafen gefunden."
                    );
                }

            } else {

                List<Airport> found =
                        p.airports.find(
                                String.join(
                                        " ",
                                        Arrays.copyOfRange(
                                                a,
                                                1,
                                                a.length
                                        )
                                )
                        );

                if (found.isEmpty()) {

                    pl.sendMessage(
                            "§cKein Flughafen gefunden."
                    );

                } else {

                    for (
                            Airport x :
                            found.stream()
                                    .limit(5)
                                    .toList()
                    ) {

                        sendLocate(
                                pl,
                                x
                        );
                    }
                }
            }

            return true;
        }

        /*
         * /airport info
         */
        if (a[0].equalsIgnoreCase("info")) {

            Airport x =
                    p.airports.nearest(
                            pl.getLocation()
                    );

            if (x != null) {

                pl.sendMessage(
                        p.airportLevels.info(x)
                );

                pl.sendMessage(
                        "§7Gates: §f"
                                + x.gates().size()
                );

            } else {

                pl.sendMessage(
                        "§cKein Flughafen in der Nähe gefunden."
                );
            }

            return true;
        }

        /*
         * /airport generate
         */
        if (
                a[0].equalsIgnoreCase("generate")
                        && pl.hasPermission(
                                "dresdenairlines.admin"
                        )
        ) {

            p.airports.randomGenerateAround(
                    pl.getLocation()
            );

            pl.sendMessage(
                    "§aRegion geprüft."
            );

            return true;
        }

        /*
         * Unbekannter /airport-Befehl
         */
        pl.sendMessage(
                "§cUnbekannter Befehl."
        );

        pl.sendMessage(
                "§7Möglichkeiten: §f"
                        + "list, locate, info, generate, stations"
        );

        return true;
    }

    /*
     * =========================================================
     * TAB COMPLETION
     * =========================================================
     */
    @Override
    public List<String> onTabComplete(
            CommandSender sender,
            Command command,
            String alias,
            String[] args
    ) {

        if (!(sender instanceof Player player)) {
            return Collections.emptyList();
        }

        String name =
                command.getName().toLowerCase();

        /*
         * ---------------------------------------------------------
         * /airline
         * ---------------------------------------------------------
         */
        if (name.equals("airline")) {

            if (args.length == 1) {

                return partial(
                        args[0],
                        "create",
                        "buy",
                        "route",
                        "fleet",
                        "routes",
                        "info",
                        "ranking"
                );
            }

            /*
             * /airline buy <Typ>
             */
            if (
                    args.length == 2
                            && args[0].equalsIgnoreCase("buy")
            ) {

                return partial(
                        args[1],
                        "A220-300",
                        "A320",
                        "A330-300",
                        "A350-900",
                        "ERJ195"
                );
            }

            /*
             * /airline route <Von>
             */
            if (
                    args.length == 2
                            && args[0].equalsIgnoreCase("route")
            ) {

                List<String> ids =
                        new ArrayList<>();

                for (
                        Airport airport :
                        p.airports.airports.values()
                ) {

                    if (
                            airport != null
                                    && airport.id() != null
                    ) {

                        ids.add(
                                airport.id()
                        );
                    }
                }

                return partial(
                        args[1],
                        ids.toArray(
                                new String[0]
                        )
                );
            }

            /*
             * /airline route <Von> <Nach>
             */
            if (
                    args.length == 3
                            && args[0].equalsIgnoreCase("route")
            ) {

                List<String> ids =
                        new ArrayList<>();

                for (
                        Airport airport :
                        p.airports.airports.values()
                ) {

                    if (
                            airport != null
                                    && airport.id() != null
                    ) {

                        ids.add(
                                airport.id()
                        );
                    }
                }

                return partial(
                        args[2],
                        ids.toArray(
                                new String[0]
                        )
                );
            }

            /*
             * /airline route <Von> <Nach> <Flugzeug-ID>
             */
            if (
                    args.length == 4
                            && args[0].equalsIgnoreCase("route")
            ) {

                List<String> ids =
                        new ArrayList<>();

                Airline al =
                        p.storage.airlines.get(
                                player.getUniqueId()
                        );

                if (al != null) {

                    for (
                            Aircraft ac :
                            al.fleet
                    ) {

                        if (
                                ac != null
                                        && ac.id != null
                        ) {

                            ids.add(
                                    ac.id
                            );
                        }
                    }
                }

                return partial(
                        args[3],
                        ids.toArray(
                                new String[0]
                        )
                );
            }

            /*
             * /airline route <Von> <Nach> <Flugzeug-ID> <Preis>
             */
            if (
                    args.length == 5
                            && args[0].equalsIgnoreCase("route")
            ) {

                return Collections.emptyList();
            }

            return Collections.emptyList();
        }

        /*
         * ---------------------------------------------------------
         * /airport
         * ---------------------------------------------------------
         */
        if (name.equals("airport")) {

            if (args.length == 1) {

                return partial(
                        args[0],
                        "list",
                        "locate",
                        "info",
                        "generate",
                        "stations"
                );
            }

            /*
             * /airport locate <Name/ID>
             * /airport info <Name/ID>
             */
            if (
                    args.length >= 2
                            && (
                            args[0].equalsIgnoreCase("locate")
                                    || args[0].equalsIgnoreCase("info")
                    )
            ) {

                List<String> ids =
                        new ArrayList<>();

                for (
                        Airport airport :
                        p.airports.airports.values()
                ) {

                    if (airport != null) {

                        if (
                                airport.id() != null
                        ) {

                            ids.add(
                                    airport.id()
                            );
                        }

                        if (
                                airport.name() != null
                        ) {

                            ids.add(
                                    airport.name()
                            );
                        }
                    }
                }

                return partial(
                        args[args.length - 1],
                        ids.toArray(
                                new String[0]
                        )
                );
            }

            return Collections.emptyList();
        }

        /*
         * ---------------------------------------------------------
         * /flight
         * ---------------------------------------------------------
         */
        if (name.equals("flight")) {

            if (args.length == 1) {

                return partial(
                        args[0],
                        "book",
                        "money",
                        "give",
                        "take"
                );
            }

            /*
             * /flight book <Ziel>
             */
            if (
                    args.length == 2
                            && args[0].equalsIgnoreCase("book")
            ) {

                List<String> ids =
                        new ArrayList<>();

                for (
                        Airport airport :
                        p.airports.airports.values()
                ) {

                    if (
                            airport != null
                                    && airport.id() != null
                    ) {

                        ids.add(
                                airport.id()
                        );
                    }
                }

                return partial(
                        args[1],
                        ids.toArray(
                                new String[0]
                        )
                );
            }
        }

        return Collections.emptyList();
    }

    /*
     * =========================================================
     * TAB-HILFSMETHODE
     * =========================================================
     */
    private List<String> partial(
            String input,
            String... values
    ) {

        List<String> result =
                new ArrayList<>();

        String lower =
                input == null
                        ? ""
                        : input.toLowerCase();

        for (String value : values) {

            if (
                    value != null
                            && value
                            .toLowerCase()
                            .startsWith(lower)
            ) {

                result.add(value);
            }
        }

        return result;
    }

    /*
     * =========================================================
     * /airport locate Ausgabe
     * =========================================================
     */
    private void sendLocate(
            Player pl,
            Airport x
    ) {

        Location c =
                x.center();

        double d =
                pl.getWorld() == c.getWorld()
                        ? Math.sqrt(
                                pl.getLocation()
                                        .distanceSquared(c)
                        )
                        : Double.NaN;

        pl.sendMessage(
                "§aFlughafen gefunden: §f"
                        + x.name()
                        + " §7("
                        + x.id()
                        + ") §8X="
                        + c.getBlockX()
                        + " Y="
                        + c.getBlockY()
                        + " Z="
                        + c.getBlockZ()
                        + (
                                Double.isNaN(d)
                                        ? ""
                                        : " §7Entfernung: §e"
                                        + Math.round(d)
                                        + " Blöcke"
                        )
        );
    }
}
