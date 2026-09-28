package de.dresdenairlines;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public final class Commands implements CommandExecutor, TabCompleter {

    private final DresdenAirlines p;

    public Commands(DresdenAirlines p) {
        this.p = p;
    }

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

        if (command.getName().equalsIgnoreCase("airport")) {

            if (args.length == 0) {
                player.sendMessage(ChatColor.YELLOW +
                        "/airport list, locate, info, generate, stations");
                return true;
            }

            switch (args[0].toLowerCase()) {

                case "list" -> {
                    if (p.airports == null) {
                        player.sendMessage(ChatColor.RED +
                                "Der AirportManager ist noch nicht geladen.");
                        return true;
                    }

                    if (p.airports.all().isEmpty()) {
                        player.sendMessage(ChatColor.RED +
                                "Es wurden noch keine Flughäfen generiert.");
                        return true;
                    }

                    player.sendMessage(ChatColor.GOLD + "=== Flughäfen ===");

                    p.airports.all().forEach(a ->
                            player.sendMessage(
                                    ChatColor.YELLOW + a.id() +
                                            ChatColor.WHITE + " - " +
                                            a.name()
                            )
                    );

                    return true;
                }

                case "locate" -> {

                    if (p.airports == null) {
                        player.sendMessage(ChatColor.RED +
                                "Der AirportManager ist noch nicht geladen.");
                        return true;
                    }

                    if (p.airports.all().isEmpty()) {
                        player.sendMessage(ChatColor.RED +
                                "Es wurden noch keine Flughäfen generiert.");
                        return true;
                    }

                    String searchId = args.length >= 2
                            ? args[1]
                            : null;

                    try {

                        if (searchId == null) {

                            var nearest = p.airports.nearest(player.getLocation());

                            if (nearest == null) {
                                player.sendMessage(ChatColor.RED +
                                        "Kein Flughafen gefunden.");
                                return true;
                            }

                            sendLocate(player, nearest);
                            return true;
                        }

                        var airport = p.airports.get(searchId);

                        if (airport == null) {
                            player.sendMessage(ChatColor.RED +
                                    "Flughafen nicht gefunden: " +
                                    searchId);
                            return true;
                        }

                        sendLocate(player, airport);

                    } catch (Exception ex) {

                        player.sendMessage(ChatColor.RED +
                                "Während der Ausführung des Befehls ist ein Fehler aufgetreten.");

                        p.getLogger().warning(
                                "Fehler bei /airport locate: " +
                                        ex.getMessage()
                        );

                        ex.printStackTrace();
                    }

                    return true;
                }

                case "info" -> {

                    if (p.airports == null ||
                            p.airports.all().isEmpty()) {

                        player.sendMessage(ChatColor.RED +
                                "Es wurden noch keine Flughäfen generiert.");
                        return true;
                    }

                    var airport = p.airports.nearest(player.getLocation());

                    if (airport == null) {
                        player.sendMessage(ChatColor.RED +
                                "Kein Flughafen gefunden.");
                        return true;
                    }

                    player.sendMessage(ChatColor.GOLD +
                            "=== Flughafen ===");

                    player.sendMessage(
                            ChatColor.YELLOW + "ID: " +
                                    ChatColor.WHITE + airport.id()
                    );

                    player.sendMessage(
                            ChatColor.YELLOW + "Name: " +
                                    ChatColor.WHITE + airport.name()
                    );

                    player.sendMessage(
                            ChatColor.YELLOW + "Position: " +
                                    ChatColor.WHITE +
                                    airport.center().getBlockX() +
                                    " " +
                                    airport.center().getBlockY() +
                                    " " +
                                    airport.center().getBlockZ()
                    );

                    return true;
                }

                case "generate" -> {

                    try {

                        p.airports.randomGenerateAround(
                                player.getLocation()
                        );

                        player.sendMessage(ChatColor.GREEN +
                                "Flughafen-Generierung gestartet.");

                    } catch (Exception ex) {

                        player.sendMessage(ChatColor.RED +
                                "Flughafen konnte nicht generiert werden.");

                        p.getLogger().warning(
                                "Fehler bei /airport generate: " +
                                        ex.getMessage()
                        );

                        ex.printStackTrace();
                    }

                    return true;
                }

                case "stations" -> {

                    if (p.stations == null) {
                        player.sendMessage(ChatColor.RED +
                                "Der Stations-Manager ist noch nicht geladen.");
                        return true;
                    }

                    player.sendMessage(
                            ChatColor.GOLD +
                                    "Bahnhofssystem ist aktiv."
                    );

                    return true;
                }

                default -> {
                    player.sendMessage(ChatColor.RED +
                            "Unbekannter Unterbefehl.");

                    player.sendMessage(ChatColor.YELLOW +
                            "/airport list, locate, info, generate, stations");

                    return true;
                }
            }
        }

        return true;
    }

    private void sendLocate(Player player, Object airport) {

        if (airport == null) {
            player.sendMessage(ChatColor.RED +
                    "Flughafen nicht gefunden.");
            return;
        }

        try {

            var a = (de.dresdenairlines.Airport) airport;

            Location loc = a.center();

            if (loc == null || loc.getWorld() == null) {
                player.sendMessage(ChatColor.RED +
                        "Der Flughafen besitzt keine gültige Position.");
                return;
            }

            double distance =
                    player.getLocation().distance(loc);

            player.sendMessage(ChatColor.GOLD +
                    "=== Flughafen gefunden ===");

            player.sendMessage(
                    ChatColor.YELLOW + "ID: " +
                            ChatColor.WHITE + a.id()
            );

            player.sendMessage(
                    ChatColor.YELLOW + "Name: " +
                            ChatColor.WHITE + a.name()
            );

            player.sendMessage(
                    ChatColor.YELLOW + "Entfernung: " +
                            ChatColor.WHITE +
                            Math.round(distance) +
                            " Blöcke"
            );

            player.sendMessage(
                    ChatColor.YELLOW + "Position: " +
                            ChatColor.WHITE +
                            loc.getBlockX() +
                            " " +
                            loc.getBlockY() +
                            " " +
                            loc.getBlockZ()
            );

            player.sendMessage(
                    ChatColor.YELLOW + "Welt: " +
                            ChatColor.WHITE +
                            loc.getWorld().getName()
            );

        } catch (Exception ex) {

            player.sendMessage(ChatColor.RED +
                    "Die Flughafenposition konnte nicht angezeigt werden.");

            p.getLogger().warning(
                    "Fehler beim Anzeigen des Flughafens: " +
                            ex.getMessage()
            );

            ex.printStackTrace();
        }
    }

    // =========================
    // TAB COMPLETION
    // =========================

    @Override
    public List<String> onTabComplete(
            CommandSender sender,
            Command command,
            String alias,
            String[] args
    ) {

        List<String> result = new ArrayList<>();

        if (command.getName().equalsIgnoreCase("airport")) {

            if (args.length == 1) {

                result.add("list");
                result.add("locate");
                result.add("info");
                result.add("generate");
                result.add("stations");

                return filter(result, args[0]);
            }

            if (args.length == 2 &&
                    args[0].equalsIgnoreCase("locate")) {

                if (p.airports != null) {

                    p.airports.all().forEach(
                            a -> result.add(a.id())
                    );
                }

                return filter(result, args[1]);
            }
        }

        return result;
    }

    private List<String> filter(
            List<String> values,
            String input
    ) {

        String lower = input.toLowerCase();

        return values.stream()
                .filter(v -> v.toLowerCase().startsWith(lower))
                .toList();
    }
}
