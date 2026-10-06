package de.dresdenairlines;

import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.ItemMeta;
import java.util.*;

public class AirlineGUI implements Listener {
    final DresdenAirlines p;
    AirlineGUI(DresdenAirlines p){this.p=p;}

    ItemStack item(Material m,String name,String... lore){
        ItemStack i=new ItemStack(m);
        ItemMeta x=i.getItemMeta();
        x.displayName(net.kyori.adventure.text.Component.text(name));
        x.lore(Arrays.stream(lore).map(net.kyori.adventure.text.Component::text).toList());
        i.setItemMeta(x);
        return i;
    }

    public void open(Player pl){
        Airline a=p.storage.airlines.get(pl.getUniqueId());
        if(a==null){
            pl.sendMessage("§cNutze /airline create <Name> <Code>");
            return;
        }
        Inventory inv=Bukkit.createInventory(null,36,"Airline: "+a.name);
        inv.setItem(10,item(Material.EMERALD,"§aFinanzen",
                "§7Kontostand: §a"+String.format("%.0f",a.money)+" $",
                "§7Ruf: §f"+String.format("%.1f",a.reputation)+"/100"));
        inv.setItem(12,item(Material.ELYTRA,"§bFlotte",
                "§7Flugzeuge: §f"+a.fleet.size(),
                "§7Neue Flugzeuge werden über das Airline-System gekauft."));
        inv.setItem(14,item(Material.COMPASS,"§bRouten",
                "§7Routen: §f"+a.routes.size(),
                "§7Automatische Flugplanung auf deinen Routen."));
        inv.setItem(16,item(Material.CLOCK,"§eFlugplan",
                "§7Automatische Flüge werden regelmäßig geplant.",
                "§7Aktive Flüge: §f"+p.storage.flights.values().stream().filter(f->f.airline==a&&f.status!=FlightStatus.LANDED).count()));
        inv.setItem(20,item(Material.VILLAGER_SPAWN_EGG,"§6Mitarbeiter",
                "§7Angestellt: §f"+a.employees.size(),
                "§eKlicke zum Verwalten"));
        inv.setItem(22,item(Material.BOOK,"§dStatistik",
                "§7Flotte: §f"+a.fleet.size(),
                "§7Routen: §f"+a.routes.size(),
                "§7Ruf: §e"+String.format("%.1f",a.reputation)+"/100",
                "§7Kontostand: §a"+String.format("%.0f",a.money)+" $"));
        inv.setItem(24,item(Material.GOLD_BLOCK,"§6Rangliste",
                "§7Serverweite Airline-Rangliste"));
        inv.setItem(31,item(Material.BOOK,"§bSysteminfo",
                "§7Airbus & Embraer werden verwendet.",
                "§7Flüge haben Abfertigung, Gepäck und Zufallsereignisse."));
        pl.openInventory(inv);
    }

    @EventHandler
    public void click(InventoryClickEvent e){
        if(!e.getView().getTitle().startsWith("Airline:"))return;
        e.setCancelled(true);
        if(e.getWhoClicked() instanceof Player pl && e.getRawSlot()==20){
            p.employeeGUI.open(pl);
        }
    }
}
