package de.dresdenairlines;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.inventory.AnvilInventory;
import org.bukkit.inventory.Inventory;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.view.AnvilView;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class FlightDetailsGUI implements Listener {
    private final DresdenAirlines p;
    private final Map<UUID,String> editingPrice = new HashMap<>();

    FlightDetailsGUI(DresdenAirlines p){this.p=p;}

    private ItemStack item(Material m,String name,String... lore){
        ItemStack i=new ItemStack(m);
        ItemMeta x=i.getItemMeta();
        x.displayName(net.kyori.adventure.text.Component.text(name));
        x.lore(Arrays.stream(lore).map(net.kyori.adventure.text.Component::text).toList());
        i.setItemMeta(x);
        return i;
    }

    private boolean canEdit(Player player, Flight f){
        return player.hasPermission("dresdenairlines.admin")
                || f.airline.owner.equals(player.getUniqueId());
    }

    public void open(Player player, Flight f){
        if(f==null)return;
        boolean owner=canEdit(player,f);
        Inventory inv=Bukkit.createInventory(null,54,"Flugdetails - "+f.id);

        inv.setItem(4,item(Material.ELYTRA,"§bFlug "+f.id,
                "§7Airline: §f"+f.airline.name+" §8("+f.airline.code+")",
                "§7Flugzeug: §f"+f.aircraft.type,
                "§7Status: §f"+f.status));

        inv.setItem(19,item(Material.COMPASS,"§bRoute",
                "§7Start: §f"+f.from.id(),
                "§7Ziel: §f"+f.to.id(),
                "§7Entfernung: §f"+Math.round(f.from.center().distance(f.to.center()))+" Blöcke"));

        inv.setItem(21,item(Material.EMERALD,"§aPreis",
                "§7Aktueller Preis: §f"+p.passengers.currentFare(f)+" $",
                "§7Grundpreis: §f"+f.baseFare+" $",
                owner?"§eKlicke zum Ändern":"§8Nur der Airline-Besitzer darf ändern"));

        inv.setItem(23,item(Material.CHEST,"§6Auslastung",
                "§7Gebucht: §f"+f.booked,
                "§7NPC: §f"+f.npcBooked,
                "§7Freie Plätze: §f"+f.available(),
                "§7Nachfrage: §e"+p.passengers.demandText(f)));

        inv.setItem(25,item(Material.IRON_DOOR,"§bGate",
                "§7Abflug: §f"+(f.departureGateId==null?"noch nicht zugewiesen":f.departureGateId),
                "§7Ankunft: §f"+(f.arrivalGateId==null?"noch nicht zugewiesen":f.arrivalGateId)));

        if(owner){
            inv.setItem(37,item(Material.ANVIL,"§ePreis ändern",
                    "§7Neuen Grundpreis eingeben",
                    "§7Nur Besitzer oder Admin"));
            inv.setItem(40,item(Material.BARRIER,"§cFlug stornieren",
                    "§7Storniert diesen konkreten Flug",
                    "§7Gates werden freigegeben"));
        }else{
            inv.setItem(40,item(Material.EMERALD,"§aTicket buchen",
                    "§7Preis: §f"+p.passengers.currentFare(f)+" $",
                    "§eKlicke zum Buchen"));
        }

        inv.setItem(49,item(Material.ARROW,"§cZurück zu den Flügen"));
        player.openInventory(inv);
    }

    private void openPriceEditor(Player player,Flight f){
        if(!canEdit(player,f)){
            player.sendMessage("§cNur der Besitzer dieser Airline oder ein Admin darf den Flug ändern.");
            return;
        }
        editingPrice.put(player.getUniqueId(),f.id);
        Inventory inv=Bukkit.createInventory(null, InventoryType.ANVIL, "Neuer Flugpreis");
        inv.setItem(0,item(Material.EMERALD,""+f.baseFare,"§7Gib den neuen Grundpreis ein."));
        player.openInventory(inv);
    }

    @EventHandler
    public void prepare(PrepareAnvilEvent e){
        if(!e.getView().getTitle().equals("Neuer Flugpreis"))return;
        ItemStack first=e.getInventory().getItem(0);
        if(first==null)return;
        if(!(e.getView() instanceof AnvilView view))return;
        String text=view.getRenameText();
        if(text==null||text.isBlank()||!text.matches("\\d{1,6}")){e.setResult(null);return;}
        int value;
        try{value=Integer.parseInt(text);}catch(NumberFormatException ex){e.setResult(null);return;}
        if(value<10||value>1000000){e.setResult(null);return;}
        ItemStack result=first.clone();
        ItemMeta meta=result.getItemMeta();
        meta.displayName(net.kyori.adventure.text.Component.text("§a"+value+" $"));
        result.setItemMeta(meta);
        e.setResult(result);
        view.setRepairCost(0);
    }

    @EventHandler
    public void click(InventoryClickEvent e){
        String title=e.getView().getTitle();
        if(title.startsWith("Flugdetails - ")){
            e.setCancelled(true);
            if(!(e.getWhoClicked() instanceof Player player))return;
            String id=title.substring("Flugdetails - ".length());
            Flight f=p.storage.flights.get(id);
            if(f==null){player.closeInventory();return;}

            if(e.getRawSlot()==49){p.flightGUI.open(player);return;}

            if(e.getRawSlot()==21){
                if(canEdit(player,f))openPriceEditor(player,f);
                else player.sendMessage("§cNur der Besitzer dieser Airline oder ein Admin darf den Preis ändern.");
                return;
            }

            if(e.getRawSlot()==37){
                if(canEdit(player,f))openPriceEditor(player,f);
                else player.sendMessage("§cNur der Besitzer dieser Airline oder ein Admin darf den Preis ändern.");
                return;
            }

            if(e.getRawSlot()==40){
                if(canEdit(player,f)){
                    p.storage.flights.remove(f.id);
                    p.gates.releaseAll(f);
                    f.status=FlightStatus.CANCELLED;
                    p.storage.save();
                    player.sendMessage("§cFlug "+f.id+" wurde storniert.");
                    p.flightGUI.open(player);
                }else{
                    String err=p.passengers.book(player,f);
                    if(err!=null)player.sendMessage(err);
                    else player.closeInventory();
                }
            }
            return;
        }

        if(title.equals("Neuer Flugpreis")){
            e.setCancelled(true);
            if(!(e.getWhoClicked() instanceof Player player))return;
            if(e.getRawSlot()!=2)return;
            Flight f=p.storage.flights.get(editingPrice.get(player.getUniqueId()));
            if(f==null){player.closeInventory();return;}
            if(!canEdit(player,f)){player.closeInventory();return;}
            if(!(e.getView() instanceof AnvilView view))return;
            String text=view.getRenameText();
            if(text==null||!text.matches("\\d{1,6}")){player.sendMessage("§cBitte eine Zahl zwischen 10 und 1.000.000 eingeben.");return;}
            int value=Integer.parseInt(text);
            if(value<10||value>1000000){player.sendMessage("§cDer Preis muss zwischen 10 und 1.000.000 $ liegen.");return;}
            f.baseFare=value;
            f.ticketPrice=p.passengers.currentFare(f);
            p.storage.save();
            editingPrice.remove(player.getUniqueId());
            player.sendMessage("§aNeuer Grundpreis für "+f.id+": §f"+value+" $");
            open(player,f);
        }
    }
}
