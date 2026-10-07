package de.dresdenairlines;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

public final class Commands implements CommandExecutor, TabCompleter {

    private final DresdenAirlines p;

    public Commands(DresdenAirlines p) {
        this.p = p;
    }

    // =========================================================
    // COMMANDS
    // =========================================================

    @Override
    public boolean onCommand(
            CommandSender sender,
            Command command,
            String label,
            String[] args
    ) {

        if (!(sender instanceof Player player)) {
            sender.sendMessage("Dieser Befehl kann nur von einem Spieler ausgeführt werden.");
            return true;
        }

        // =====================================================
        // FLIGHT
        // =====================================================

        if (command.getName().equalsIgnoreCase("flight")) {

            if (args.length == 0) {
                p.flightGUI.open(player);
                return true;
            }

            switch (args[0].toLowerCase()) {\n\n                case "livery" -> {\n                    if (airline == null) {\n                        player.sendMessage(ChatColor.RED + "Erstelle zuerst eine Airline.");\n                        return true;\n                    }\n                    if (args.length == 1) {\n                        p.liveryGUI.open(player);\n                        return true;\n                    }\n                    if (args.length >= 5 && args[1].equalsIgnoreCase("set")) {\n                        try {\n                            airline.livery.primary = parseHex(args[2]);\n                            airline.livery.secondary = parseHex(args[3]);\n                            airline.livery.accent = parseHex(args[4]);\n                            p.storage.save();\n                            player.sendMessage(ChatColor.GREEN + "Lackierung gespeichert: #" + LiveryGUI.hex(airline.livery.primary) + " / #" + LiveryGUI.hex(airline.livery.secondary) + " / #" + LiveryGUI.hex(airline.livery.accent));\n                        } catch (IllegalArgumentException ex) {\n                            player.sendMessage(ChatColor.RED + "Ungültige HEX-Farbe. Beispiel: #123456");\n                        }\n                        return true;\n                    }\n                    player.sendMessage(ChatColor.YELLOW + "/airline livery");\n                    player.sendMessage(ChatColor.YELLOW + "/airline livery set <primär> <sekundär> <akzent>");\n                    return true;\n                }

                case "money" -> {

                    player.sendMessage(
                            ChatColor.GOLD +
                                    "Kontostand: " +
                                    ChatColor.WHITE +
                                    p.passengers.money(player.getUniqueId()) +
                                    " $"
                    );

                    return true;
                }

                case "book" -> {

                    if (args.length < 2) {
                        player.sendMessage(
                                ChatColor.RED +
                                        "/flight book <Ziel>"
                        );
                        return true;
                    }

                    String error = p.passengers.bookNext(
                            player,
                            args[1].toUpperCase()
                    );

                    if (error != null) {
                        player.sendMessage(error);
                    }

                    return true;
                }

                case "give" -> {

                    if (!player.hasPermission("dresdenairlines.admin")) {
                        player.sendMessage(
                                ChatColor.RED +
                                        "Dafür hast du keine Berechtigung."
                        );
                        return true;
                    }

                    if (args.length < 3) {
                        player.sendMessage(
                                ChatColor.RED +
                                        "/flight give <Spieler> <Betrag>"
                        );
                        return true;
                    }

                    try {

                        Player target = Bukkit.getPlayerExact(args[1]);
                        double amount = Double.parseDouble(args[2]);

                        if (target == null || amount <= 0) {
                            player.sendMessage(
                                    ChatColor.RED +
                                            "Spieler oder Betrag ungültig."
                            );
                            return true;
                        }

                        p.passengers.wallet.deposit(
                                target.getUniqueId(),
                                amount
                        );

                        player.sendMessage(
                                ChatColor.GREEN +
                                        String.valueOf(amount) +
                                        " $ an " +
                                        target.getName() +
                                        " gegeben."
                        );

                        target.sendMessage(
                                ChatColor.GREEN +
                                        "Du hast " +
                                        amount +
                                        " $ erhalten."
                        );

                    } catch (Exception ex) {

                        player.sendMessage(
                                ChatColor.RED +
                                        "Betrag ungültig."
                        );
                    }

                    return true;
                }

                case "take" -> {

                    if (!player.hasPermission("dresdenairlines.admin")) {
                        player.sendMessage(
                                ChatColor.RED +
                                        "Dafür hast du keine Berechtigung."
                        );
                        return true;
                    }

                    if (args.length < 3) {
                        player.sendMessage(
                                ChatColor.RED +
                                        "/flight take <Spieler> <Betrag>"
                        );
                        return true;
                    }

                    try {

                        Player target = Bukkit.getPlayerExact(args[1]);
                        double amount = Double.parseDouble(args[2]);

                        if (target == null || amount <= 0) {
                            player.sendMessage(
                                    ChatColor.RED +
                                            "Spieler oder Betrag ungültig."
                            );
                            return true;
                        }

                        boolean success =
                                p.passengers.wallet.withdraw(
                                        target.getUniqueId(),
                                        amount
                                );

                        if (success) {

                            player.sendMessage(
                                    ChatColor.RED +
                                            String.valueOf(amount) +
                                            " $ von " +
                                            target.getName() +
                                            " genommen."
                            );

                            target.sendMessage(
                                    ChatColor.RED +
                                            "Dir wurden " +
                                            amount +
                                            " $ abgezogen."
                            );

                        } else {

                            player.sendMessage(
                                    ChatColor.RED +
                                            "Der Spieler hat nicht genug Geld."
                            );
                        }

                    } catch (Exception ex) {

                        player.sendMessage(
                                ChatColor.RED +
                                        "Betrag ungültig."
                        );
                    }

                    return true;
                }

                default -> {

                    player.sendMessage(
                            ChatColor.YELLOW +
                                    "/flight book <Ziel>"
                    );

                    player.sendMessage(
                            ChatColor.YELLOW +
                                    "/flight money"
                    );

                    player.sendMessage(
                            ChatColor.YELLOW +
                                    "/flight give <Spieler> <Betrag>"
                    );

                    player.sendMessage(
                            ChatColor.YELLOW +
                                    "/flight take <Spieler> <Betrag>"
                    );

                    return true;
                }
            }
        }


        // =====================================================
        // AIRLINE
        // =====================================================

        if (command.getName().equalsIgnoreCase("airline")) {

            if (args.length == 0) {
                p.airlineGUI.open(player);
                return true;
            }

            Airline airline =
                    p.storage.airlines.get(player.getUniqueId());

            switch (args[0].toLowerCase()) {

                // -------------------------------------------------
                // CREATE
                // -------------------------------------------------

                case "create" -> {

                    if (args.length < 2) {
                        player.sendMessage(
                                ChatColor.RED +
                                        "/airline create <Name> [Code]"
                        );
                        return true;
                    }

                    if (airline != null) {
                        player.sendMessage(
                                ChatColor.RED +
                                        "Du hast bereits eine Airline."
                        );
                        return true;
                    }

                    String code =
                            args.length > 2
                                    ? args[2].toUpperCase()
                                    : "AIR";

                    if (code.length() > 4) {
                        player.sendMessage(
                                ChatColor.RED +
                                        "Der Airline-Code darf maximal 4 Zeichen haben."
                        );
                        return true;
                    }

                    airline = new Airline(
                            player.getUniqueId(),
                            args[1],
                            code,
                            "DRE",
                            p.getConfig().getDouble(
                                    "economy.starting-money"
                            )
                    );

                    p.storage.airlines.put(
                            player.getUniqueId(),
                            airline
                    );

                    p.storage.save();

                    player.sendMessage(
                            ChatColor.GREEN +
                                    "Airline erfolgreich gegründet!"
                    );

                    player.sendMessage(
                            ChatColor.GRAY +
                                    "Name: " +
                                    ChatColor.WHITE +
                                    airline.name
                    );

                    player.sendMessage(
                            ChatColor.GRAY +
                                    "Code: " +
                                    ChatColor.WHITE +
                                    airline.code
                    );

                    return true;
                }


                // -------------------------------------------------
                // BUY
                // -------------------------------------------------

                case "buy" -> {

                    if (airline == null) {
                        player.sendMessage(
                                ChatColor.RED +
                                        "Erstelle zuerst eine Airline."
                        );
                        return true;
                    }

                    if (args.length < 2) {

                        player.sendMessage(
                                ChatColor.YELLOW +
                                        "Verfügbare Flugzeuge:"
                        );

                        player.sendMessage(
                                ChatColor.WHITE +
                                        "A220-300"
                        );

                        player.sendMessage(
                                ChatColor.WHITE +
                                        "A320"
                        );

                        player.sendMessage(
                                ChatColor.WHITE +
                                        "A330-300"
                        );

                        player.sendMessage(
                                ChatColor.WHITE +
                                        "A350-900"
                        );

                        player.sendMessage(
                                ChatColor.WHITE +
                                        "ERJ195"
                        );

                        return true;
                    }

                    Aircraft aircraft =
                            p.airlines.buy(
                                    airline,
                                    args[1]
                            );

                    if (aircraft == null) {

                        player.sendMessage(
                                ChatColor.RED +
                                        "Kauf nicht möglich."
                        );

                        player.sendMessage(
                                ChatColor.GRAY +
                                        "Möglicherweise hast du zu wenig Geld oder der Flugzeugtyp existiert nicht."
                        );

                    } else {

                        player.sendMessage(
                                ChatColor.GREEN +
                                        "Flugzeug gekauft!"
                        );

                        player.sendMessage(
                                ChatColor.GRAY +
                                        "Typ: " +
                                        ChatColor.WHITE +
                                        aircraft.type
                        );

                        player.sendMessage(
                                ChatColor.GRAY +
                                        "Registrierung: " +
                                        ChatColor.WHITE +
                                        aircraft.registration
                        );

                        player.sendMessage(
                                ChatColor.GRAY +
                                        "ID: " +
                                        ChatColor.WHITE +
                                        aircraft.id
                        );
                    }

                    return true;
                }


                // -------------------------------------------------
                // ROUTE
                // -------------------------------------------------

                case "route" -> {

                    if (airline == null) {
                        player.sendMessage(
                                ChatColor.RED +
                                        "Erstelle zuerst eine Airline."
                        );
                        return true;
                    }

                    if (args.length < 5) {

                        player.sendMessage(
                                ChatColor.RED +
                                        "/airline route <von> <nach> <Flugzeug-ID> <Preis>"
                        );

                        return true;
                    }

                    try {

                        String from =
                                args[1].toUpperCase();

                        String to =
                                args[2].toUpperCase();

                        String aircraftId =
                                args[3];

                        int price =
                                Integer.parseInt(args[4]);

                        boolean success =
                                p.airlines.addRoute(
                                        airline,
                                        from,
                                        to,
                                        aircraftId,
                                        price,
                                        300
                                );

                        if (success) {

                            player.sendMessage(
                                    ChatColor.GREEN +
                                            "Route erfolgreich eingerichtet!"
                            );

                            player.sendMessage(
                                    ChatColor.GRAY +
                                            from +
                                            " → " +
                                            to
                            );

                            player.sendMessage(
                                    ChatColor.GRAY +
                                            "Flugzeug: " +
                                            ChatColor.WHITE +
                                            aircraftId
                            );

                            player.sendMessage(
                                    ChatColor.GRAY +
                                            "Ticketpreis: " +
                                            ChatColor.WHITE +
                                            price +
                                            " $"
                            );

                        } else {

                            player.sendMessage(
                                    ChatColor.RED +
                                            "Route konnte nicht eingerichtet werden."
                            );

                            player.sendMessage(
                                    ChatColor.GRAY +
                                            "Prüfe Flughäfen, Flugzeug, Reichweite und Preis."
                            );
                        }

                    } catch (Exception ex) {

                        player.sendMessage(
                                ChatColor.RED +
                                        "Ungültige Eingabe."
                        );

                        player.sendMessage(
                                ChatColor.YELLOW +
                                        "/airline route <von> <nach> <Flugzeug-ID> <Preis>"
                        );
                    }

                    return true;
                }


                // -------------------------------------------------
                // FLEET
                // -------------------------------------------------

                case "fleet" -> {

                    if (airline == null) {
                        player.sendMessage(
                                ChatColor.RED +
                                        "Du besitzt keine Airline."
                        );
                        return true;
                    }

                    player.sendMessage(
                            ChatColor.GOLD +
                                    "=== Deine Flotte ==="
                    );

                    if (airline.fleet.isEmpty()) {

                        player.sendMessage(
                                ChatColor.GRAY +
                                        "Du besitzt noch keine Flugzeuge."
                        );

                        return true;
                    }

                    for (Aircraft aircraft : airline.fleet) {

                        player.sendMessage(
                                ChatColor.AQUA +
                                        aircraft.id +
                                        ChatColor.GRAY +
                                        " | " +
                                        aircraft.type +
                                        ChatColor.DARK_GRAY +
                                        " | " +
                                        aircraft.registration +
                                        ChatColor.GRAY +
                                        " | " +
                                        aircraft.seats +
                                        " Sitze"
                        );
                    }

                    return true;
                }


                // -------------------------------------------------
                // ROUTES
                // -------------------------------------------------

                case "routes" -> {

                    if (airline == null) {
                        player.sendMessage(
                                ChatColor.RED +
                                        "Du besitzt keine Airline."
                        );
                        return true;
                    }

                    player.sendMessage(
                            ChatColor.GOLD +
                                    "=== Deine Routen ==="
                    );

                    if (airline.routes.isEmpty()) {

                        player.sendMessage(
                                ChatColor.GRAY +
                                        "Du hast noch keine Routen."
                        );

                        return true;
                    }

                    for (Route route : airline.routes) {

                        player.sendMessage(
                                ChatColor.YELLOW +
                                        route.from +
                                        " → " +
                                        route.to +
                                        ChatColor.GRAY +
                                        " | Preis: " +
                                        ChatColor.WHITE +
                                        route.ticketPrice +
                                        " $"
                        );

                        player.sendMessage(
                                ChatColor.DARK_GRAY +
                                        "Flugzeug: " +
                                        route.aircraftId
                        );
                    }

                    return true;
                }


                // -------------------------------------------------
                // INFO
                // -------------------------------------------------

                case "info" -> {

                    if (airline == null) {
                        player.sendMessage(
                                ChatColor.RED +
                                        "Du besitzt keine Airline."
                        );
                        return true;
                    }

                    final Airline currentAirline = airline;

                    long active =
                            p.storage.flights.values()
                                    .stream()
                                    .filter(f ->
                                            f.airline == currentAirline &&
                                            f.status != FlightStatus.LANDED
                                    )
                                    .count();

                    long passengers =
                            p.storage.flights.values()
                                    .stream()
                                    .filter(f ->
                                            f.airline == currentAirline
                                    )
                                    .mapToLong(f ->
                                            f.booked + f.npcBooked
                                    )
                                    .sum();

                    player.sendMessage(
                            ChatColor.GOLD +
                                    "=== Airline-Informationen ==="
                    );

                    player.sendMessage(
                            ChatColor.AQUA +
                                    "Name: " +
                                    ChatColor.WHITE +
                                    currentAirline.name
                    );

                    player.sendMessage(
                            ChatColor.AQUA +
                                    "Code: " +
                                    ChatColor.WHITE +
                                    currentAirline.code
                    );

                    player.sendMessage(
                            ChatColor.AQUA +
                                    "Guthaben: " +
                                    ChatColor.GREEN +
                                    String.format(
                                            "%.0f",
                                            currentAirline.money
                                    ) +
                                    " $"
                    );

                    player.sendMessage(
                            ChatColor.AQUA +
                                    "Ruf: " +
                                    ChatColor.WHITE +
                                    String.format(
                                            "%.1f",
                                            currentAirline.reputation
                                    )
                    );

                    player.sendMessage(
                            ChatColor.AQUA +
                                    "Flotte: " +
                                    ChatColor.WHITE +
                                    currentAirline.fleet.size()
                    );

                    player.sendMessage(
                            ChatColor.AQUA +
                                    "Routen: " +
                                    ChatColor.WHITE +
                                    currentAirline.routes.size()
                    );

                    player.sendMessage(
                            ChatColor.AQUA +
                                    "Aktive Flüge: " +
                                    ChatColor.WHITE +
                                    active
                    );

                    player.sendMessage(
                            ChatColor.AQUA +
                                    "Passagiere: " +
                                    ChatColor.WHITE +
                                    passengers
                    );

                    return true;
                }


                // -------------------------------------------------
                // RANKING
                // -------------------------------------------------

                case "ranking" -> {

                    List<Airline> airlines =
                            new ArrayList<>(
                                    p.storage.airlines.values()
                            );

                    airlines.sort(
                            Comparator.comparingDouble(
                                    (Airline a) ->
                                            a.money +
                                            a.reputation * 10000 +
                                            a.fleet.size() * 1000
                            ).reversed()
                    );

                    player.sendMessage(
                            ChatColor.GOLD +
                                    "═══ Airline-Rangliste ═══"
                    );

                    if (airlines.isEmpty()) {

                        player.sendMessage(
                                ChatColor.GRAY +
                                        "Es gibt noch keine Airlines."
                        );

                        return true;
                    }

                    int rank = 1;

                    for (Airline a :
                            airlines.stream()
                                    .limit(10)
                                    .toList()) {

                        player.sendMessage(
                                ChatColor.YELLOW +
                                        String.valueOf(rank++) +
                                        ". " +
                                        ChatColor.WHITE +
                                        a.name +
                                        ChatColor.GRAY +
                                        " | " +
                                        ChatColor.GREEN +
                                        String.format(
                                                "%.0f",
                                                a.money
                                        ) +
                                        " $ " +
                                        ChatColor.GRAY +
                                        "| Ruf " +
                                        ChatColor.YELLOW +
                                        String.format(
                                                "%.1f",
                                                a.reputation
                                        ) +
                                        ChatColor.GRAY +
                                        " | Flotte " +
                                        ChatColor.WHITE +
                                        a.fleet.size()
                        );
                    }

                    return true;
                }


                default -> {

                    player.sendMessage(
                            ChatColor.YELLOW +
                                    "/airline create <Name> [Code]"
                    );

                    player.sendMessage(
                            ChatColor.YELLOW +
                                    "/airline buy <Typ>"
                    );

                    player.sendMessage(
                            ChatColor.YELLOW +
                                    "/airline route <Von> <Nach> <Flugzeug-ID> <Preis>"
                    );

                    player.sendMessage(
                            ChatColor.YELLOW +
                                    "/airline fleet"
                    );

                    player.sendMessage(
                            ChatColor.YELLOW +
                                    "/airline routes"
                    );

                    player.sendMessage(
                            ChatColor.YELLOW +
                                    "/airline info"
                    );

                    player.sendMessage(
                            ChatColor.YELLOW +
                                    "/airline ranking"
                    );

                    return true;
                }
            }
        }


        // =====================================================
        // AIRPORT
        // =====================================================

        if (command.getName().equalsIgnoreCase("airport")) {

            if (args.length == 0) {

                player.sendMessage(
                        ChatColor.YELLOW +
                                "/airport list"
                );

                player.sendMessage(
                        ChatColor.YELLOW +
                                "/airport locate [ID]"
                );

                player.sendMessage(
                        ChatColor.YELLOW +
                                "/airport info"
                );

                player.sendMessage(
                        ChatColor.YELLOW +
                                "/airport generate"
                );

                player.sendMessage(
                        ChatColor.YELLOW +
                                "/airport stations"
                );

                return true;
            }

            switch (args[0].toLowerCase()) {

                // -------------------------------------------------
                // LIST
                // -------------------------------------------------

                case "list" -> {

                    if (p.airports == null) {

                        player.sendMessage(
                                ChatColor.RED +
                                        "Der AirportManager ist noch nicht geladen."
                        );

                        return true;
                    }

                    if (p.airports.all().isEmpty()) {

                        player.sendMessage(
                                ChatColor.RED +
                                        "Es wurden noch keine Flughäfen generiert."
                        );

                        return true;
                    }

                    player.sendMessage(
                            ChatColor.GOLD +
                                    "=== Flughäfen ==="
                    );

                    for (Airport airport :
                            p.airports.all()) {

                        player.sendMessage(
                                ChatColor.YELLOW +
                                        airport.id() +
                                        ChatColor.WHITE +
                                        " - " +
                                        airport.name()
                        );
                    }

                    return true;
                }


                // -------------------------------------------------
                // LOCATE
                // -------------------------------------------------

                case "locate" -> {

                    if (p.airports == null) {

                        player.sendMessage(
                                ChatColor.RED +
                                        "Der AirportManager ist noch nicht geladen."
                        );

                        return true;
                    }

                    if (p.airports.all().isEmpty()) {

                        player.sendMessage(
                                ChatColor.RED +
                                        "Es wurden noch keine Flughäfen generiert."
                        );

                        return true;
                    }

                    try {

                        if (args.length < 2) {

                            Airport nearest =
                                    p.airports.nearest(
                                            player.getLocation()
                                    );

                            if (nearest == null) {

                                player.sendMessage(
                                        ChatColor.RED +
                                                "Kein Flughafen gefunden."
                                );

                                return true;
                            }

                            sendLocate(
                                    player,
                                    nearest
                            );

                            return true;
                        }

                        String search =
                                String.join(
                                        " ",
                                        Arrays.copyOfRange(
                                                args,
                                                1,
                                                args.length
                                        )
                                );

                        List<Airport> found =
                                p.airports.find(search);

                        if (found.isEmpty()) {

                            Airport exact =
                                    p.airports.get(
                                            args[1]
                                    );

                            if (exact != null) {

                                sendLocate(
                                        player,
                                        exact
                                );

                                return true;
                            }

                            player.sendMessage(
                                    ChatColor.RED +
                                            "Flughafen nicht gefunden: " +
                                            search
                            );

                            return true;
                        }

                        for (Airport airport :
                                found.stream()
                                        .limit(5)
                                        .toList()) {

                            sendLocate(
                                    player,
                                    airport
                            );
                        }

                    } catch (Exception ex) {

                        player.sendMessage(
                                ChatColor.RED +
                                        "Während der Ausführung des Befehls ist ein Fehler aufgetreten."
                        );

                        p.getLogger().warning(
                                "Fehler bei /airport locate: " +
                                        ex.getMessage()
                        );

                        ex.printStackTrace();
                    }

                    return true;
                }


                // -------------------------------------------------
                // INFO
                // -------------------------------------------------

                case "info" -> {

                    if (p.airports == null ||
                            p.airports.all().isEmpty()) {

                        player.sendMessage(
                                ChatColor.RED +
                                        "Es wurden noch keine Flughäfen generiert."
                        );

                        return true;
                    }

                    Airport airport =
                            p.airports.nearest(
                                    player.getLocation()
                            );

                    if (airport == null) {

                        player.sendMessage(
                                ChatColor.RED +
                                        "Kein Flughafen gefunden."
                        );

                        return true;
                    }

                    player.sendMessage(
                            ChatColor.GOLD +
                                    "=== Flughafen ==="
                    );

                    player.sendMessage(
                            ChatColor.YELLOW +
                                    "ID: " +
                                    ChatColor.WHITE +
                                    airport.id()
                    );

                    player.sendMessage(
                            ChatColor.YELLOW +
                                    "Name: " +
                                    ChatColor.WHITE +
                                    airport.name()
                    );

                    player.sendMessage(
                            ChatColor.YELLOW +
                                    "Position: " +
                                    ChatColor.WHITE +
                                    airport.center().getBlockX() +
                                    " " +
                                    airport.center().getBlockY() +
                                    " " +
                                    airport.center().getBlockZ()
                    );

                    player.sendMessage(
                            ChatColor.YELLOW +
                                    "Gates: " +
                                    ChatColor.WHITE +
                                    airport.gates().size()
                    );

                    return true;
                }


                // -------------------------------------------------
                // GENERATE
                // -------------------------------------------------

                case "generate" -> {

                    if (!player.hasPermission(
                            "dresdenairlines.admin"
                    )) {

                        player.sendMessage(
                                ChatColor.RED +
                                        "Dafür hast du keine Berechtigung."
                        );

                        return true;
                    }

                    try {

                        boolean created =
                                p.airports.randomGenerateAround(
                                        player.getLocation()
                                );

                        if (created) {
                            player.sendMessage(
                                    ChatColor.GREEN +
                                            "Flughafen erfolgreich generiert!"
                            );
                        } else {
                            player.sendMessage(
                                    ChatColor.YELLOW +
                                            "Kein neuer Flughafen wurde generiert. "
                                            + "Möglicherweise ist bereits ein Flughafen zu nah "
                                            + "oder das Gelände ist nicht geeignet."
                            );
                        }

                    } catch (Exception ex) {

                        player.sendMessage(
                                ChatColor.RED +
                                        "Flughafen konnte nicht generiert werden."
                        );

                        p.getLogger().warning(
                                "Fehler bei /airport generate: " +
                                        ex.getMessage()
                        );

                        ex.printStackTrace();
                    }

                    return true;
                }


                // -------------------------------------------------
                // STATIONS
                // -------------------------------------------------

                case "stations" -> {

                    if (p.stations == null) {

                        player.sendMessage(
                                ChatColor.RED +
                                        "Der Stations-Manager ist noch nicht geladen."
                        );

                        return true;
                    }

                    if (p.stations.stations.isEmpty()) {

                        player.sendMessage(
                                ChatColor.GRAY +
                                        "Es wurden noch keine Bahnhöfe gefunden."
                        );

                        return true;
                    }

                    p.transitGUI.open(player);
                    return true;
                }


                default -> {

                    player.sendMessage(
                            ChatColor.RED +
                                    "Unbekannter Unterbefehl."
                    );

                    player.sendMessage(
                            ChatColor.YELLOW +
                                    "/airport list"
                    );

                    player.sendMessage(
                            ChatColor.YELLOW +
                                    "/airport locate [ID]"
                    );

                    player.sendMessage(
                            ChatColor.YELLOW +
                                    "/airport info"
                    );

                    player.sendMessage(
                            ChatColor.YELLOW +
                                    "/airport generate"
                    );

                    player.sendMessage(
                            ChatColor.YELLOW +
                                    "/airport stations"
                    );

                    return true;
                }
            }
        }

        return true;
    }


    // =========================================================
    // LOCATE AUSGABE
    // =========================================================

    private void sendLocate(
            Player player,
            Airport airport
    ) {

        if (airport == null) {

            player.sendMessage(
                    ChatColor.RED +
                            "Flughafen nicht gefunden."
            );

            return;
        }

        try {

            Location location =
                    airport.center();

            if (location == null ||
                    location.getWorld() == null) {

                player.sendMessage(
                        ChatColor.RED +
                                "Der Flughafen besitzt keine gültige Position."
                );

                return;
            }

            double distance;

            if (player.getWorld() ==
                    location.getWorld()) {

                distance =
                        Math.sqrt(
                                player.getLocation()
                                        .distanceSquared(location)
                        );

            } else {

                distance = Double.NaN;
            }

            player.sendMessage(
                    ChatColor.GOLD +
                            "=== Flughafen gefunden ==="
            );

            player.sendMessage(
                    ChatColor.YELLOW +
                            "ID: " +
                            ChatColor.WHITE +
                            airport.id()
            );

            player.sendMessage(
                    ChatColor.YELLOW +
                            "Name: " +
                            ChatColor.WHITE +
                            airport.name()
            );

            player.sendMessage(
                    ChatColor.YELLOW +
                            "Position: " +
                            ChatColor.WHITE +
                            location.getBlockX() +
                            " " +
                            location.getBlockY() +
                            " " +
                            location.getBlockZ()
            );

            player.sendMessage(
                    ChatColor.YELLOW +
                            "Welt: " +
                            ChatColor.WHITE +
                            location.getWorld().getName()
            );

            if (!Double.isNaN(distance)) {

                player.sendMessage(
                        ChatColor.YELLOW +
                                "Entfernung: " +
                                ChatColor.WHITE +
                                Math.round(distance) +
                                " Blöcke"
                );
            }

        } catch (Exception ex) {

            player.sendMessage(
                    ChatColor.RED +
                            "Die Flughafenposition konnte nicht angezeigt werden."
            );

            p.getLogger().warning(
                    "Fehler beim Anzeigen des Flughafens: " +
                            ex.getMessage()
            );

            ex.printStackTrace();
        }
    }


    // =========================================================
    // TAB COMPLETION
    // =========================================================

    @Override
    public List<String> onTabComplete(
            CommandSender sender,
            Command command,
            String alias,
            String[] args
    ) {

        List<String> result =
                new ArrayList<>();


        // =====================================================
        // AIRLINE TAB
        // =====================================================

        if (command.getName().equalsIgnoreCase("airline")) {

            if (args.length == 1) {

                result.add("create");
                result.add("buy");
                result.add("route");
                result.add("fleet");
                result.add("routes");
                result.add("info");
                result.add("ranking");
                result.add("livery");

                return filter(
                        result,
                        args[0]
                );
            }


            if (args[0].equalsIgnoreCase("buy") &&
                    args.length == 2) {

                result.add("A220-300");
                result.add("A320");
                result.add("A330-300");
                result.add("A350-900");
                result.add("ERJ195");

                return filter(
                        result,
                        args[1]
                );
            }


            if (args[0].equalsIgnoreCase("route")) {

                // Von
                if (args.length == 2) {

                    if (p.airports != null) {

                        for (Airport airport :
                                p.airports.all()) {

                            result.add(
                                    airport.id()
                            );
                        }
                    }

                    return filter(
                            result,
                            args[1]
                    );
                }


                // Nach
                if (args.length == 3) {

                    if (p.airports != null) {

                        for (Airport airport :
                                p.airports.all()) {

                            result.add(
                                    airport.id()
                            );
                        }
                    }

                    return filter(
                            result,
                            args[2]
                    );
                }


                // Flugzeug-ID
                if (args.length == 4) {

                    Airline airline =
                            p.storage.airlines.get(
                                    ((Player) sender)
                                            .getUniqueId()
                            );

                    if (airline != null) {

                        for (Aircraft aircraft :
                                airline.fleet) {

                            result.add(
                                    aircraft.id
                            );
                        }
                    }

                    return filter(
                            result,
                            args[3]
                    );
                }


                // Preis
                if (args.length == 5) {

                    result.add("100");
                    result.add("150");
                    result.add("200");
                    result.add("250");
                    result.add("300");
                    result.add("500");

                    return filter(
                            result,
                            args[4]
                    );
                }
            }
        }


        // =====================================================
        // AIRPORT TAB
        // =====================================================

        if (command.getName().equalsIgnoreCase("airport")) {

            if (args.length == 1) {

                result.add("list");
                result.add("locate");
                result.add("info");
                result.add("generate");
                result.add("stations");

                return filter(
                        result,
                        args[0]
                );
            }


            if (args.length >= 2 &&
                    args[0].equalsIgnoreCase("locate")) {

                if (p.airports != null) {

                    for (Airport airport :
                            p.airports.all()) {

                        result.add(
                                airport.id()
                        );

                        result.add(
                                airport.name()
                        );
                    }
                }

                return filter(
                        result,
                        args[args.length - 1]
                );
            }
        }


        // =====================================================
        // FLIGHT TAB
        // =====================================================

        if (command.getName().equalsIgnoreCase("flight")) {

            if (args.length == 1) {

                result.add("book");
                result.add("money");
                result.add("give");
                result.add("take");

                return filter(
                        result,
                        args[0]
                );
            }


            if (args.length == 2 &&
                    args[0].equalsIgnoreCase("book")) {

                if (p.airports != null) {

                    for (Airport airport :
                            p.airports.all()) {

                        result.add(
                                airport.id()
                        );
                    }
                }

                return filter(
                        result,
                        args[1]
                );
            }


            if (args.length == 2 &&
                    (
                            args[0].equalsIgnoreCase("give") ||
                            args[0].equalsIgnoreCase("take")
                    )) {

                for (Player online :
                        Bukkit.getOnlinePlayers()) {

                    result.add(
                            online.getName()
                    );
                }

                return filter(
                        result,
                        args[1]
                );
            }


            if (args.length == 3 &&
                    (
                            args[0].equalsIgnoreCase("give") ||
                            args[0].equalsIgnoreCase("take")
                    )) {

                result.add("100");
                result.add("500");
                result.add("1000");
                result.add("5000");
                result.add("10000");

                return filter(
                        result,
                        args[2]
                );
            }
        }


        return result;
    }


    // =========================================================
    // TAB FILTER
    // =========================================================

    private List<String> filter(
            List<String> values,
            String input
    ) {

        String lower =
                input.toLowerCase();

        return values.stream()
                .distinct()
                .filter(value ->
                        value.toLowerCase()
                                .startsWith(lower)
                )
                .toList();
    }
}
