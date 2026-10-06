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

import java.util.ArrayList;
import java.util.List;

public final class AirportGUI implements Listener {
    private final DresdenAirlines p;

    AirportGUI(DresdenAirlines p){this.p=p;}

    private ItemStack item(Material material,String name,String... lore){
        ItemStack i=new ItemStack(material);
        ItemMeta meta=i.getItemMeta();
        meta.setDisplayName(name);
        meta.setLore(java.util.Arrays.asList(lore));
        i.setItemMeta(meta);
        return i;
    }

    public void open(Player player){
        Inventory inv=Bukkit.createInventory(null,54,"DresdenAirlines - Flughäfen");
        inv.setItem(4,item(Material.SUNFLOWER,"§6Flughafen-Zentrale",
                "§7Klicke einen Flughafen an, um dorthin zu reisen."));

        List<Airport> airports=new ArrayList<>(p.airports.all());
        int slot=10;
        for(Airport airport:airports){
            if(slot>=45)break;
            inv.setItem(slot,item(Material.SMOOTH_QUARTZ,
                    "§bFlughafen "+airport.id()+" - "+airport.name(),
                    "§7Level: §e"+airport.level(),
                    "§7Gates: §f"+airport.gates().size(),
                    "§7X: §f"+airport.center().getBlockX(),
                    "§7Y: §f"+airport.center().getBlockY(),
                    "§7Z: §f"+airport.center().getBlockZ(),
                    "§eKlicke zum Teleportieren"));
            slot++;
        }
        inv.setItem(49,item(Material.ARROW,"§cZurück"));
        inv.setItem(51,item(Material.MINECART,"§bNahverkehr",
                "§7Automatischer, kostenloser Dorf-Shuttle",
                "§7Keine Spieler können Linien betreiben."));
        player.openInventory(inv);
    }

    @EventHandler
    public void click(InventoryClickEvent e){
        if(!e.getView().getTitle().equals("DresdenAirlines - Flughäfen"))return;
        e.setCancelled(true);
        if(!(e.getWhoClicked() instanceof Player player))return;
        if(e.getRawSlot()==51){p.transitGUI.open(player);return;}
        if(e.getRawSlot()==49){p.flightGUI.open(player);return;}
        if(e.getRawSlot()<10||e.getRawSlot()>=45)return;
        int index=e.getRawSlot()-10;
        List<Airport> airports=new ArrayList<>(p.airports.all());
        if(index>=0&&index<airports.size()){
            Airport airport=airports.get(index);
            player.teleport(airport.center().clone().add(0,2,0));
            player.sendMessage("§aDu bist jetzt am Flughafen §b"+airport.id()+"§a.");
            player.closeInventory();
        }
    }
}
