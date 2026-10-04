package de.dresdenairlines;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import net.kyori.adventure.text.Component;

import java.util.*;

/** Main DresdenAirlines dashboard and passenger flight booking GUI. */
public final class FlightGUI implements Listener {
    private final DresdenAirlines plugin;
    private final Map<UUID, Map<Integer, String>> selections = new HashMap<>();

    FlightGUI(DresdenAirlines plugin) {
        this.plugin = plugin;
    }

    private ItemStack item(Material material, String name, String... lore) {
        ItemStack i = new ItemStack(material);
        ItemMeta meta = i.getItemMeta();
        meta.displayName(Component.text(name));
        meta.lore(Arrays.stream(lore).map(Component::text).toList());
        i.setItemMeta(meta);
        return i;
    }

    /** Main GUI: the player can reach every major plugin area from here. */
    public void open(Player p) {
        Inventory inv = Bukkit.createInventory(null, 54, "✈ DresdenAirlines – Hauptmenü");
        inv.setItem(10, item(Material.PAPER, "§b✈ Flüge buchen",
                "§7Abflüge ansehen", "§7Tickets direkt per Klick buchen"));
        inv.setItem(12, item(Material.GOLD_INGOT, "§6Mein Geld",
                "§7Kontostand: §f" + plugin.passengers.money(p.getUniqueId()) + " $"));
        inv.setItem(14, item(Material.EMERALD, "§aMeine Airline",
                "§7Flotte, Routen, Finanzen und Flugplan"));
        inv.setItem(16, item(Material.SMOOTH_QUARTZ, "§fFlughäfen",
                "§7Infos, Teleport, Generierung und Bahnhöfe"));
        inv.setItem(28, item(Material.AIR, "§bSimulation",
                "§7Automatische Flüge, NPC-Passagiere",
                "§7Gepäck, Abfertigung und Events laufen automatisch"));
        inv.setItem(30, item(Material.IRON_BLOCK, "§eFlotte",
                "§7Airbus A220-300", "§7Airbus A320", "§7Airbus A330-300",
                "§7Airbus A350-900", "§7Embraer ERJ195"));
        inv.setItem(32, item(Material.COMPASS, "§dRouten & Ziele",
                "§7Flughäfen und Airline-Routen verwalten"));
        inv.setItem(34, item(Material.NETHER_STAR, "§6Server-Info",
                "§7Alles Wichtige ist über die GUIs erreichbar"));
        p.openInventory(inv);
    }

    public void openFlights(Player p) {
        List<Flight> fs = plugin.passengers.departures();
        Inventory inv = Bukkit.createInventory(null, 54, "✈ DresdenAirlines – Flüge");
        Map<Integer, String> map = new HashMap<>();
        int slot = 0;
        for (Flight f : fs) {
            if (slot >= 45) break;
            String key = f.id;
            map.put(slot, key);
            inv.setItem(slot, item(Material.PAPER,
                    "§b✈ " + f.from.id() + " → " + f.to.id(),
                    "§7Flug: §f" + f.id,
                    "§7Flugzeug: §f" + f.aircraft.type,
                    "§7Preis: §a" + plugin.passengers.currentFare(f) + " $",
                    "§7Freie Plätze: §f" + f.available() + " §7| Nachfrage: §e" + plugin.passengers.demandText(f),
                    "§7Status: §f" + f.status,
                    "§eKlicke zum Buchen"));
            slot++;
        }
        inv.setItem(45, item(Material.ARROW, "§cHauptmenü"));
        inv.setItem(49, item(Material.GOLD_INGOT, "§6Dein Geld",
                "§7Kontostand: §f" + plugin.passengers.money(p.getUniqueId()) + " $"));
        selections.put(p.getUniqueId(), map);
        p.openInventory(inv);
    }

    @EventHandler
    public void click(InventoryClickEvent e) {
        String title = e.getView().getTitle();
        if (!title.equals("✈ DresdenAirlines – Hauptmenü") && !title.equals("✈ DresdenAirlines – Flüge")) return;
        e.setCancelled(true);
        if (!(e.getWhoClicked() instanceof Player p)) return;

        if (title.equals("✈ DresdenAirlines – Hauptmenü")) {
            switch (e.getRawSlot()) {
                case 10 -> openFlights(p);
                case 14 -> plugin.airlineGUI.open(p);
                case 16 -> plugin.airportGUI.open(p);
                default -> { }
            }
            return;
        }

        if (e.getRawSlot() == 45) {
            open(p);
            return;
        }
        String id = selections.getOrDefault(p.getUniqueId(), Map.of()).get(e.getRawSlot());
        if (id == null) return;
        Flight f = plugin.storage.flights.get(id);
        if (f == null) {
            p.sendMessage("§cFlug nicht mehr verfügbar.");
            p.closeInventory();
            return;
        }
        String err = plugin.passengers.book(p, f);
        if (err != null) p.sendMessage(err); else p.closeInventory();
    }
}
