package de.dresdenairlines;

import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.ItemMeta;
import java.util.*;

public final class EmployeeGUI implements Listener {
    private final DresdenAirlines p;
    EmployeeGUI(DresdenAirlines p){this.p=p;}

    private ItemStack item(Material m,String n,String... l){
        ItemStack i=new ItemStack(m);
        ItemMeta x=i.getItemMeta();
        x.displayName(net.kyori.adventure.text.Component.text(n));
        x.lore(Arrays.stream(l).map(net.kyori.adventure.text.Component::text).toList());
        i.setItemMeta(x);
        return i;
    }

    private Material roleMaterial(EmployeeManager.Role r){
        return switch(r){
            case PILOT -> Material.ELYTRA;
            case GROUND -> Material.CHEST;
            case MECHANIC -> Material.ANVIL;
            case CABIN -> Material.CAKE;
            case ATC -> Material.ENDER_EYE;
        };
    }

    public void open(Player pl){
        Airline a=p.storage.airlines.get(pl.getUniqueId());
        if(a==null)return;

        Inventory inv=Bukkit.createInventory(null,54,"Mitarbeiter - "+a.name);
        int s=0;
        for(Employee e:a.employees){
            if(s>=27)break;
            inv.setItem(s++,item(Material.PLAYER_HEAD,e.name,
                    "§7Rolle: §f"+e.role,
                    "§7Level: §f"+e.level,
                    "§7Fähigkeit: §f"+e.skill+"%",
                    "§7Gehalt: §f"+String.format("%.0f",e.salary)+" $/Periode",
                    "§cShift-Klick: Entlassen"));
        }

        EmployeeManager.Role[] rs=EmployeeManager.Role.values();
        int[] slots={36,38,40,42,44};
        for(int i=0;i<rs.length;i++){
            EmployeeManager.Role r=rs[i];
            inv.setItem(slots[i],item(roleMaterial(r),
                    "§a"+r.display+" einstellen",
                    "§7Kosten: §f"+String.format("%.0f",r.cost)+" $",
                    "§7Gehalt: §f"+String.format("%.0f",r.salary)+" $/Periode",
                    "§eKlick zum Einstellen"));
        }

        inv.setItem(49,item(Material.ARROW,"§cZurück"));
        pl.openInventory(inv);
    }

    @EventHandler
    public void click(InventoryClickEvent e){
        if(!e.getView().getTitle().startsWith("Mitarbeiter - "))return;
        e.setCancelled(true);
        if(!(e.getWhoClicked() instanceof Player pl))return;
        Airline a=p.storage.airlines.get(pl.getUniqueId());
        if(a==null)return;

        if(e.getRawSlot()==49){
            p.airlineGUI.open(pl);
            return;
        }

        int[] slots={36,38,40,42,44};
        if(e.getClick().isLeftClick()){
            for(int i=0;i<slots.length;i++){
                if(e.getRawSlot()==slots[i]){
                    EmployeeManager.Role r=EmployeeManager.Role.values()[i];
                    Employee x=p.employees.hire(a,r.display);
                    pl.sendMessage(x==null?"§cEinstellung nicht möglich.":"§aMitarbeiter eingestellt: §f"+x.name+" §7("+x.role+")");
                    open(pl);
                    return;
                }
            }
        }

        if(e.getClick().isShiftClick()&&e.getRawSlot()>=0&&e.getRawSlot()<27){
            int n=e.getRawSlot();
            if(n<a.employees.size()){
                Employee x=a.employees.get(n);
                p.employees.fire(a,x.id);
                pl.sendMessage("§cMitarbeiter entfernt: §f"+x.name);
                open(pl);
            }
        }
    }
}
