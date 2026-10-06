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
import java.util.Arrays;

public final class TransitGUI implements Listener {
    private final DresdenAirlines p;
    TransitGUI(DresdenAirlines p){this.p=p;}

    private ItemStack item(Material m,String name,String... lore){
        ItemStack i=new ItemStack(m);
        ItemMeta x=i.getItemMeta();
        x.setDisplayName(name);
        x.setLore(Arrays.asList(lore));
        i.setItemMeta(x);
        return i;
    }

    public void open(Player player){
        Inventory inv=Bukkit.createInventory(null,54,"DresdenAirlines - Nahverkehr");
        inv.setItem(4,item(Material.POWERED_RAIL,"§6Nahverkehr",
                "§7Automatischer Flughafen-Shuttle",
                "§aKostenlos §8• §7kein Spielerbetrieb",
                "§7Nur lokale Dorf ↔ Flughafen-Verbindungen"));

        int slot=10;
        for(StationManager.Station s:p.stations.stations.values()){
            if(slot>=45)break;
            long next=p.stations.secondsUntilNextDeparture(s);
            String nextText=next==0?"§ajetzt / beim nächsten Takt":("§e"+next+" s");
            inv.setItem(slot++,item(Material.MINECART,
                    "§b"+s.name(),
                    "§7Flughafen: §f"+s.airportId(),
                    "§7Nächster Shuttle-Takt: "+nextText,
                    "§7Hinweg: §fDorf → Flughafen",
                    "§7Rückweg: §fFlughafen → Dorf",
                    "§aTicket: KOSTENLOS",
                    "§8Die Linie wird automatisch betrieben."));
        }
        inv.setItem(49,item(Material.ARROW,"§cZurück"));
        player.openInventory(inv);
    }

    @EventHandler
    public void click(InventoryClickEvent e){
        if(!e.getView().getTitle().equals("DresdenAirlines - Nahverkehr"))return;
        e.setCancelled(true);
        if(!(e.getWhoClicked() instanceof Player player))return;
        if(e.getRawSlot()==49)p.airportGUI.open(player);
    }
}
