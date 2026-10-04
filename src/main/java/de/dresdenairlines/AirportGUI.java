package de.dresdenairlines;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

/** GUI for airport management, information, locating and railway stations. */
public final class AirportGUI implements Listener {
    private final DresdenAirlines p;
    private final Map<UUID, String> selectedAirport = new HashMap<>();

    AirportGUI(DresdenAirlines p) {
        this.p = p;
    }

    private ItemStack item(Material material, String name, String... lore) {
        ItemStack i = new ItemStack(material);
        ItemMeta meta = i.getItemMeta();
        meta.displayName(Component.text(name));
        meta.lore(Arrays.stream(lore).map(Component::text).toList());
        i.setItemMeta(meta);
        return i;
    }

    public void open(Player player) {
        Inventory inv = Bukkit.createInventory(null, 54, "✈ DresdenAirlines – Flughäfen");

        inv.setItem(4, item(Material.SUNFLOWER, "§6Flughafen-Zentrale",
                "§7Alle Flughäfen verwalten",
                "§7Infos, Positionen und Bahnhöfe"));
        inv.setItem(49, item(Material.COMPASS, "§bFlughäfen neu laden",
                "§7Aktuelle Flughafenliste anzeigen"));
        inv.setItem(51, item(Material.EMERALD, "§aFlughafen generieren",
                "§7Sucht einen geeigneten Standort",
                "§7in deiner aktuellen Umgebung"));
        inv.setItem(53, item(Material.RAIL, "§eBahnhöfe",
                "§7Zeigt die generierten Dorf-Bahnhöfe"));

        int slot = 10;
        for (Airport airport : p.airports.all()) {
            if (slot >= 45) break;
            inv.setItem(slot, item(Material.SMOOTH_QUARTZ,
                    "§b✈ " + airport.id() + " – " + airport.name(),
                    "§7Level: §e" + airport.level(),
                    "§7Gates: §f" + airport.gates().size(),
                    "§7Passagiere: §f" + p.airportLevels.passengers(airport.id()),
                    "§7Nachfrage: §e" + String.format(Locale.US, "%.0f", p.airportLevels.demandIndex(airport)) + "%",
                    "§aKlicke für Details"));
            slot++;
        }
        player.openInventory(inv);
    }

    private void openInfo(Player player, Airport airport) {
        selectedAirport.put(player.getUniqueId(), airport.id());
        Inventory inv = Bukkit.createInventory(null, 27, "✈ Flughafen: " + airport.id());
        inv.setItem(4, item(Material.BEACON, "§b" + airport.name(),
                "§7ID: §f" + airport.id(),
                "§7Level: §e" + airport.level(),
                "§7Gates: §f" + airport.gates().size(),
                "§7Passagiere: §f" + p.airportLevels.passengers(airport.id()),
                "§7Nachfrage: §e" + String.format(Locale.US, "%.0f", p.airportLevels.demandIndex(airport)) + "%"));
        inv.setItem(11, item(Material.ENDER_PEARL, "§aZum Flughafen teleportieren",
                "§7Klicke, um zum Terminal zu fliegen"));
        inv.setItem(13, item(Material.MAP, "§fKoordinaten",
                "§7X: §f" + airport.center().getBlockX(),
                "§7Y: §f" + airport.center().getBlockY(),
                "§7Z: §f" + airport.center().getBlockZ(),
                "§7Welt: §f" + airport.center().getWorld().getName()));
        inv.setItem(15, item(Material.IRON_DOOR, "§eGates",
                "§7" + String.join(", ", airport.gates())));
        inv.setItem(22, item(Material.ARROW, "§cZurück"));
        player.openInventory(inv);
    }

    private void openStations(Player player) {
        Inventory inv = Bukkit.createInventory(null, 54, "🚆 DresdenAirlines – Bahnhöfe");
        int slot = 10;
        for (Station station : p.stations.stations.values()) {
            if (slot >= 45) break;
            inv.setItem(slot, item(Material.POWERED_RAIL,
                    "§e🚆 " + station.name(),
                    "§7ID: §f" + station.id(),
                    "§7Flughafen: §b" + station.airportId(),
                    "§aKlicke zum Teleportieren"));
            slot++;
        }
        inv.setItem(49, item(Material.ARROW, "§cZurück"));
        player.openInventory(inv);
    }

    @EventHandler
    public void click(InventoryClickEvent e) {
        String title = e.getView().getTitle();
        if (!title.startsWith("✈ DresdenAirlines – Flughäfen")
                && !title.startsWith("✈ Flughafen:")
                && !title.equals("🚆 DresdenAirlines – Bahnhöfe")) return;
        e.setCancelled(true);
        if (!(e.getWhoClicked() instanceof Player player)) return;

        if (title.equals("✈ DresdenAirlines – Flughäfen")) {
            if (e.getRawSlot() == 51) {
                Airport airport = p.airports.randomGenerateAround(player.getLocation());
                if (airport == null) {
                    player.sendMessage("§cIn deiner Umgebung konnte kein geeigneter Flughafenstandort gefunden werden.");
                } else {
                    player.sendMessage("§aFlughafen §b" + airport.id() + " §aerfolgreich generiert.");
                }
                open(player);
                return;
            }
            if (e.getRawSlot() == 53) {
                openStations(player);
                return;
            }
            if (e.getRawSlot() == 49) {
                p.airports.load();
                open(player);
                return;
            }
            if (e.getRawSlot() >= 10 && e.getRawSlot() < 45) {
                int index = e.getRawSlot() - 10;
                List<Airport> airports = new ArrayList<>(p.airports.all());
                if (index < airports.size()) openInfo(player, airports.get(index));
            }
            return;
        }

        if (title.equals("🚆 DresdenAirlines – Bahnhöfe")) {
            if (e.getRawSlot() == 49) open(player);
            else if (e.getRawSlot() >= 10 && e.getRawSlot() < 45) {
                int index = e.getRawSlot() - 10;
                List<Station> stations = new ArrayList<>(p.stations.stations.values());
                if (index < stations.size()) player.teleport(stations.get(index).station());
            }
            return;
        }

        if (title.startsWith("✈ Flughafen:")) {
            if (e.getRawSlot() == 22) {
                open(player);
                return;
            }
            if (e.getRawSlot() == 11) {
                Airport airport = p.airports.get(selectedAirport.get(player.getUniqueId()));
                if (airport != null) player.teleport(airport.center().clone().add(0, 2, 0));
            }
        }
    }
}
