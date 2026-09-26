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
import org.bukkit.NamespacedKey;
import java.util.*;
import net.kyori.adventure.text.Component;

public final class FlightGUI implements Listener {
    private final DresdenAirlines plugin;
    private final Map<UUID,Map<Integer,String>> selections=new HashMap<>();
    FlightGUI(DresdenAirlines plugin){this.plugin=plugin;}
    private ItemStack item(Material m,String name,String... lore){ItemStack i=new ItemStack(m);ItemMeta meta=i.getItemMeta();meta.displayName(Component.text(name));meta.lore(Arrays.stream(lore).map(Component::text).toList());i.setItemMeta(meta);return i;}
    public void open(Player p){
        List<Flight> fs=plugin.passengers.departures(); Inventory inv=Bukkit.createInventory(null,54,"✈ DresdenAirlines – Flüge"); Map<Integer,String> map=new HashMap<>();
        int slot=0;
        for(Flight f:fs){if(slot>=45)break; String key=f.id; map.put(slot,key);
            inv.setItem(slot,item(Material.PAPER,"§b✈ "+f.from.id()+" → "+f.to.id(),"§7Flug: §f"+f.id,"§7Flugzeug: §f"+f.aircraft.type,"§7Preis: §a"+plugin.passengers.currentFare(f)+" $","§7Freie Plätze: §f"+f.available()+" §7| Nachfrage: §e"+plugin.passengers.demandText(f),"§7Status: §f"+f.status,"§eKlicke zum Buchen")); slot++;}
        inv.setItem(49,item(Material.GOLD_INGOT,"§6Dein Geld","§7Kontostand: §f"+plugin.passengers.money(p.getUniqueId())));
        selections.put(p.getUniqueId(),map); p.openInventory(inv);
    }
    @EventHandler public void click(InventoryClickEvent e){
        if(!e.getView().getTitle().equals("✈ DresdenAirlines – Flüge"))return; e.setCancelled(true); if(!(e.getWhoClicked() instanceof Player p))return;
        String id=selections.getOrDefault(p.getUniqueId(),Map.of()).get(e.getRawSlot()); if(id==null)return;
        Flight f=plugin.storage.flights.get(id); if(f==null){p.sendMessage("§cFlug nicht mehr verfügbar.");p.closeInventory();return;}
        String err=plugin.passengers.book(p,f); if(err!=null)p.sendMessage(err); else p.closeInventory();
    }
}
