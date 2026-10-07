package de.dresdenairlines;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;
import java.util.List;

public final class LiveryGUI implements Listener {
    private static final String TITLE = "Airline-Lackierung";
    private final DresdenAirlines p;

    private static final int[] PALETTE = {
            0xFFFFFF, 0x111111, 0x777777, 0xAA0000,
            0xFF5555, 0xFFAA00, 0xFFFF55, 0x00AA00,
            0x55FF55, 0x00AAAA, 0x55FFFF, 0x0000AA,
            0x5555FF, 0xAA00AA, 0xFF55FF, 0x8B5A2B
    };

    LiveryGUI(DresdenAirlines p) { this.p = p; }

    private ItemStack item(Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        meta.setLore(Arrays.asList(lore));
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack colorItem(int rgb, String name, String role) {
        Material mat = switch (role) {
            case "primary" -> Material.RED_DYE;
            case "secondary" -> Material.BLUE_DYE;
            default -> Material.YELLOW_DYE;
        };
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name + " §f#" + String.format("%06X", rgb));
        meta.setLore(List.of("§7Bereich: §f" + role, "§7Linksklick: Farbe setzen"));
        item.setItemMeta(meta);
        return item;
    }

    public void open(Player player) {
        Airline airline = p.storage.airlines.get(player.getUniqueId());
        if (airline == null) {
            player.sendMessage("§cErstelle zuerst eine Airline.");
            return;
        }
        Livery l = airline.livery;
        Inventory inv = Bukkit.createInventory(null, 54, TITLE);

        inv.setItem(4, item(Material.PAPER, "§bLackierungs-Editor",
                "§7Airline: §f" + airline.name,
                "§7Primär: §f#" + hex(l.primary),
                "§7Sekundär: §f#" + hex(l.secondary),
                "§7Akzent: §f#" + hex(l.accent)));

        inv.setItem(19, colorItem(l.primary, "§ePrimärfarbe", "primary"));
        inv.setItem(22, colorItem(l.secondary, "§eSekundärfarbe", "secondary"));
        inv.setItem(25, colorItem(l.accent, "§eAkzentfarbe", "accent"));

        for (int i = 0; i < PALETTE.length; i++) {
            int slot = 36 + i;
            inv.setItem(slot, dye(PALETTE[i], "§f#" + hex(PALETTE[i])));
        }

        inv.setItem(31, item(Material.WRITABLE_BOOK, "§dEigene HEX-Farben",
                "§7Primär: §f/airline livery set <primär> <sekundär> <akzent>",
                "§7Beispiel: §f/airline livery set #FFFFFF #123456 #FFCC00"));
        inv.setItem(49, item(Material.EMERALD, "§aSpeichern",
                "§7Die Lackierung ist automatisch gespeichert."));

        player.openInventory(inv);
    }

    private ItemStack dye(int rgb, String name) {
        ItemStack item = new ItemStack(Material.LIME_DYE);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        meta.setLore(List.of("§7Klicke zuerst auf einen Farbbereich oben."));
        item.setItemMeta(meta);
        return item;
    }

    @EventHandler
    public void click(InventoryClickEvent e) {
        if (!TITLE.equals(e.getView().getTitle())) return;
        e.setCancelled(true);
        if (!(e.getWhoClicked() instanceof Player player)) return;
        Airline airline = p.storage.airlines.get(player.getUniqueId());
        if (airline == null) return;

        int slot = e.getRawSlot();
        if (slot == 19 || slot == 22 || slot == 25) {
            player.sendMessage("§eLackierungsbereich ausgewählt. Klicke danach auf eine Farbe.");
            player.setMetadata("livery-selection", new org.bukkit.metadata.FixedMetadataValue(p,
                    slot == 19 ? "primary" : slot == 22 ? "secondary" : "accent"));
            return;
        }

        if (slot >= 36 && slot < 52) {
            String role = player.hasMetadata("livery-selection")
                    ? player.getMetadata("livery-selection").get(0).asString() : "primary";
            int rgb = PALETTE[slot - 36];
            if (role.equals("primary")) airline.livery.primary = rgb;
            else if (role.equals("secondary")) airline.livery.secondary = rgb;
            else airline.livery.accent = rgb;
            p.storage.save();
            open(player);
            return;
        }

        if (slot == 49) {
            p.storage.save();
            player.sendMessage("§aLackierung gespeichert.");
            open(player);
        }
    }

    static String hex(int rgb) { return String.format("%06X", rgb & 0xFFFFFF); }
}
