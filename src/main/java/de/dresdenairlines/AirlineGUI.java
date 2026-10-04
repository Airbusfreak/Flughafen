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

import java.util.*;

/** Complete GUI-based airline management. */
public final class AirlineGUI implements Listener {
    final DresdenAirlines p;
    private final Map<UUID, String> routeFrom = new HashMap<>();
    private final Map<UUID, String> routeTo = new HashMap<>();
    private final Map<UUID, String> routeAircraft = new HashMap<>();

    AirlineGUI(DresdenAirlines p) { this.p = p; }

    private ItemStack item(Material m, String name, String... lore) {
        ItemStack i = new ItemStack(m);
        ItemMeta x = i.getItemMeta();
        x.setDisplayName(name);
        x.setLore(Arrays.asList(lore));
        i.setItemMeta(x);
        return i;
    }

    public void open(Player pl) {
        Airline a = p.storage.airlines.get(pl.getUniqueId());
        if (a == null) {
            Inventory inv = Bukkit.createInventory(null, 27, "✈ Airline – Gründung");
            inv.setItem(11, item(Material.PAPER, "§aAirline gründen",
                    "§7Erstellt deine persönliche Airline",
                    "§7Der Name wird aus deinem Spielernamen erstellt.",
                    "§eKlicke zum Gründen"));
            inv.setItem(15, item(Material.ARROW, "§cZurück"));
            pl.openInventory(inv);
            return;
        }

        Inventory inv = Bukkit.createInventory(null, 54, "✈ Airline: " + a.name);
        inv.setItem(4, item(Material.NETHER_STAR, "§b" + a.name + " §7[" + a.code + "]",
                "§7Guthaben: §a" + String.format("%.0f", a.money) + " $",
                "§7Ruf: §e" + String.format("%.1f", a.reputation) + "/100"));
        inv.setItem(10, item(Material.EMERALD, "§6Finanzen",
                "§7Kontostand: §a" + String.format("%.0f", a.money) + " $",
                "§7Ruf: §e" + String.format("%.1f", a.reputation) + "/100"));
        inv.setItem(12, item(Material.IRON_BLOCK, "§bFlotte",
                "§7Flugzeuge: §f" + a.fleet.size(), "§eKlicke zum Verwalten"));
        inv.setItem(14, item(Material.COMPASS, "§dRouten",
                "§7Routen: §f" + a.routes.size(), "§eKlicke zum Verwalten"));
        inv.setItem(16, item(Material.CLOCK, "§aFlugplan",
                "§7Aktive Flüge: §f" + activeFlights(a),
                "§7Automatische Flüge laufen weiter."));
        inv.setItem(28, item(Material.BOOK, "§fStatistik",
                "§7Flotte: §f" + a.fleet.size(),
                "§7Routen: §f" + a.routes.size(),
                "§7Ruf: §e" + String.format("%.1f", a.reputation) + "/100"));
        inv.setItem(30, item(Material.GOLD_INGOT, "§6Rangliste", "§eKlicke für die Airline-Rangliste"));
        inv.setItem(32, item(Material.PAPER, "§bFlugzeugtypen",
                "§7A220-300 · A320 · A330-300",
                "§7A350-900 · ERJ195"));
        inv.setItem(49, item(Material.ARROW, "§cHauptmenü"));
        pl.openInventory(inv);
    }

    private long activeFlights(Airline a) {
        return p.storage.flights.values().stream()
                .filter(f -> f.airline == a && f.status != FlightStatus.LANDED)
                .count();
    }

    private void openFleet(Player pl) {
        Airline a = p.storage.airlines.get(pl.getUniqueId());
        Inventory inv = Bukkit.createInventory(null, 54, "✈ Airline – Flotte");
        String[] types = {"A220-300", "A320", "A330-300", "A350-900", "ERJ195"};
        Material[] mats = {Material.IRON_BLOCK, Material.QUARTZ_BLOCK, Material.LIGHT_BLUE_CONCRETE, Material.BLUE_CONCRETE, Material.SMOOTH_STONE};
        for (int i = 0; i < types.length; i++) {
            inv.setItem(10 + i * 2, item(mats[i], "§aKaufen: " + types[i], "§eKlicke zum Kaufen"));
        }
        int slot = 20;
        for (Aircraft aircraft : a.fleet) {
            if (slot >= 45) break;
            inv.setItem(slot++, item(Material.PAPER, "§b" + aircraft.type,
                    "§7ID: §f" + aircraft.id,
                    "§7Registrierung: §f" + aircraft.registration,
                    "§7Sitze: §f" + aircraft.seats));
        }
        inv.setItem(49, item(Material.ARROW, "§cZurück"));
        pl.openInventory(inv);
    }

    private void openRoutes(Player pl) {
        Inventory inv = Bukkit.createInventory(null, 54, "✈ Airline – Routen");
        int slot = 10;
        for (Airport airport : p.airports.all()) {
            if (slot >= 45) break;
            inv.setItem(slot++, item(Material.SMOOTH_QUARTZ,
                    "§bStart: " + airport.id(), "§7" + airport.name(), "§eKlicke als Abflughafen"));
        }
        inv.setItem(49, item(Material.ARROW, "§cZurück"));
        pl.openInventory(inv);
    }

    private void openDestination(Player pl, String from) {
        routeFrom.put(pl.getUniqueId(), from);
        Inventory inv = Bukkit.createInventory(null, 54, "✈ Route – Ziel auswählen");
        int slot = 10;
        for (Airport airport : p.airports.all()) {
            if (airport.id().equalsIgnoreCase(from) || slot >= 45) continue;
            inv.setItem(slot++, item(Material.MAP, "§bZiel: " + airport.id(), "§7" + airport.name(), "§eKlicke als Ziel"));
        }
        inv.setItem(49, item(Material.ARROW, "§cZurück"));
        pl.openInventory(inv);
    }

    private void openAircraft(Player pl, String to) {
        routeTo.put(pl.getUniqueId(), to);
        Airline a = p.storage.airlines.get(pl.getUniqueId());
        Inventory inv = Bukkit.createInventory(null, 54, "✈ Route – Flugzeug auswählen");
        int slot = 10;
        for (Aircraft aircraft : a.fleet) {
            if (slot >= 45) break;
            inv.setItem(slot++, item(Material.IRON_BLOCK, "§b" + aircraft.type,
                    "§7ID: §f" + aircraft.id, "§7Sitze: §f" + aircraft.seats, "§eKlicke zur Auswahl"));
        }
        inv.setItem(49, item(Material.ARROW, "§cZurück"));
        pl.openInventory(inv);
    }

    private void openPrice(Player pl, String aircraftId) {
        routeAircraft.put(pl.getUniqueId(), aircraftId);
        Inventory inv = Bukkit.createInventory(null, 27, "✈ Route – Ticketpreis");
        int[] prices = {50, 75, 100, 150, 200, 300, 500};
        for (int i = 0; i < prices.length; i++) {
            inv.setItem(10 + i, item(Material.GOLD_INGOT, "§a" + prices[i] + " $", "§eKlicke zum Festlegen"));
        }
        inv.setItem(22, item(Material.ARROW, "§cZurück"));
        pl.openInventory(inv);
    }

    private void openRanking(Player pl) {
        Inventory inv = Bukkit.createInventory(null, 54, "✈ Airline – Rangliste");
        List<Airline> list = new ArrayList<>(p.storage.airlines.values());
        list.sort(Comparator.comparingDouble((Airline a) -> a.reputation).reversed());
        int slot = 10;
        int rank = 1;
        for (Airline a : list) {
            if (slot >= 45) break;
            inv.setItem(slot++, item(Material.GOLD_INGOT, "§e#" + rank++ + " §b" + a.name,
                    "§7Code: §f" + a.code,
                    "§7Ruf: §e" + String.format("%.1f", a.reputation),
                    "§7Flotte: §f" + a.fleet.size(),
                    "§7Routen: §f" + a.routes.size()));
        }
        inv.setItem(49, item(Material.ARROW, "§cZurück"));
        pl.openInventory(inv);
    }

    private void main(Player pl) { p.flightGUI.open(pl); }

    private void createAirline(Player pl) {
        if (p.storage.airlines.containsKey(pl.getUniqueId())) { open(pl); return; }
        String name = pl.getName() + " Airlines";
        Airline a = new Airline(pl.getUniqueId(), name, "AIR", "DRE",
                p.getConfig().getDouble("economy.starting-money"));
        p.storage.airlines.put(pl.getUniqueId(), a);
        p.storage.save();
        pl.sendMessage("§aAirline §b" + name + " §aerfolgreich gegründet!");
        open(pl);
    }

    @EventHandler
    public void click(InventoryClickEvent e) {
        String title = e.getView().getTitle();
        if (!title.startsWith("✈ Airline")) return;
        e.setCancelled(true);
        if (!(e.getWhoClicked() instanceof Player pl)) return;
        Airline a = p.storage.airlines.get(pl.getUniqueId());

        if (title.equals("✈ Airline – Gründung")) {
            if (e.getRawSlot() == 11) createAirline(pl);
            else if (e.getRawSlot() == 15) main(pl);
            return;
        }
        if (a == null) return;

        if (title.equals("✈ Airline: " + a.name)) {
            if (e.getRawSlot() == 12) openFleet(pl);
            else if (e.getRawSlot() == 14) openRoutes(pl);
            else if (e.getRawSlot() == 30) openRanking(pl);
            else if (e.getRawSlot() == 49) main(pl);
            return;
        }

        if (title.equals("✈ Airline – Flotte")) {
            if (e.getRawSlot() == 49) { open(pl); return; }
            String[] types = {"A220-300", "A320", "A330-300", "A350-900", "ERJ195"};
            for (int i = 0; i < types.length; i++) {
                if (e.getRawSlot() == 10 + i * 2) {
                    Aircraft aircraft = p.airlines.buy(a, types[i]);
                    pl.sendMessage(aircraft == null ? "§cKauf nicht möglich – prüfe Guthaben und Typ." : "§a" + types[i] + " erfolgreich gekauft!");
                    openFleet(pl);
                    return;
                }
            }
            return;
        }

        if (title.equals("✈ Airline – Routen")) {
            if (e.getRawSlot() == 49) { open(pl); return; }
            int index = e.getRawSlot() - 10;
            List<Airport> airports = new ArrayList<>(p.airports.all());
            if (index >= 0 && index < airports.size()) openDestination(pl, airports.get(index).id());
            return;
        }

        if (title.equals("✈ Route – Ziel auswählen")) {
            if (e.getRawSlot() == 49) { openRoutes(pl); return; }
            String from = routeFrom.get(pl.getUniqueId());
            if (from == null) return;
            List<Airport> airports = new ArrayList<>();
            for (Airport ap : p.airports.all()) if (!ap.id().equalsIgnoreCase(from)) airports.add(ap);
            int index = e.getRawSlot() - 10;
            if (index >= 0 && index < airports.size()) openAircraft(pl, airports.get(index).id());
            return;
        }

        if (title.equals("✈ Route – Flugzeug auswählen")) {
            if (e.getRawSlot() == 49) { openRoutes(pl); return; }
            int index = e.getRawSlot() - 10;
            if (index >= 0 && index < a.fleet.size()) openPrice(pl, a.fleet.get(index).id);
            return;
        }

        if (title.equals("✈ Route – Ticketpreis")) {
            if (e.getRawSlot() == 22) { openRoutes(pl); return; }
            int[] prices = {50, 75, 100, 150, 200, 300, 500};
            int index = e.getRawSlot() - 10;
            if (index >= 0 && index < prices.length) {
                String from = routeFrom.get(pl.getUniqueId());
                String to = routeTo.get(pl.getUniqueId());
                String aircraft = routeAircraft.get(pl.getUniqueId());
                boolean ok = p.airlines.addRoute(a, from, to, aircraft, prices[index], 300);
                pl.sendMessage(ok ? "§aRoute " + from + " → " + to + " eingerichtet!" : "§cRoute konnte nicht eingerichtet werden.");
                open(pl);
            }
        }
    }
}
