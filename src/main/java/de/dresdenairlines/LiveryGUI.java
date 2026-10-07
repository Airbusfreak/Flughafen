package de.dresdenairlines;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.metadata.FixedMetadataValue;

import java.util.Arrays;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class LiveryGUI implements Listener {
    private static final String TITLE = "Airline-Lackierung";
    private static final String SELECTION = "livery-selection";
    private final DresdenAirlines p;
    private final Map<UUID, ItemDisplay> previews = new HashMap<>();

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
        ItemStack item = new ItemStack(Material.LIME_DYE);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name + " §f#" + hex(rgb));
        meta.setLore(List.of(
                "§7Bereich: §f" + role,
                "§7Linksklick: Farbe auswählen"
        ));
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack patternItem(Livery.Pattern pattern, Livery current) {
        boolean active = current.pattern == pattern;
        Material material = switch (pattern) {
            case CLASSIC -> Material.WHITE_BANNER;
            case MONOCHROME -> Material.GRAY_BANNER;
            case TWO_TONE -> Material.BLUE_BANNER;
            case TAIL_ACCENT -> Material.RED_BANNER;
        };
        String label = switch (pattern) {
            case CLASSIC -> "Klassisch";
            case MONOCHROME -> "Monochrom";
            case TWO_TONE -> "Zweifarbig";
            case TAIL_ACCENT -> "Heck-Akzent";
        };
        return item(material,
                (active ? "§a✓ " : "§7") + label,
                "§7" + patternDescription(pattern));
    }

    private String patternDescription(Livery.Pattern pattern) {
        return switch (pattern) {
            case CLASSIC -> "Primär + Sekundär + Akzent + Heck";
            case MONOCHROME -> "Alle Bereiche in der Primärfarbe";
            case TWO_TONE -> "Primärfarbe mit Sekundärfarbe";
            case TAIL_ACCENT -> "Primärfarbe + farbiges Heck";
        };
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
                "§7Muster: §f" + l.pattern,
                "§7Primär: §f#" + hex(l.primary),
                "§7Sekundär: §f#" + hex(l.secondary),
                "§7Akzent: §f#" + hex(l.accent),
                "§7Heck: §f#" + hex(l.tail)));

        inv.setItem(10, patternItem(Livery.Pattern.CLASSIC, l));
        inv.setItem(11, patternItem(Livery.Pattern.MONOCHROME, l));
        inv.setItem(12, patternItem(Livery.Pattern.TWO_TONE, l));
        inv.setItem(13, patternItem(Livery.Pattern.TAIL_ACCENT, l));

        inv.setItem(19, colorItem(l.primary, "§ePrimärfarbe", "primary"));
        inv.setItem(22, colorItem(l.secondary, "§eSekundärfarbe", "secondary"));
        inv.setItem(25, colorItem(l.accent, "§eAkzentfarbe", "accent"));
        inv.setItem(28, colorItem(l.tail, "§eHeckfarbe", "tail"));

        for (int i = 0; i < PALETTE.length; i++) {
            inv.setItem(36 + i, dye(PALETTE[i], "§f#" + hex(PALETTE[i])));
        }

        inv.setItem(31, item(Material.WRITABLE_BOOK, "§dEigene HEX-Farben",
                "§7/airline livery set <primär> <sekundär> <akzent>",
                "§7Zusätzlich: /airline livery tail <hex>",
                "§7Muster: /airline livery pattern <classic|mono|two-tone|tail>"));

        inv.setItem(34, item(Material.ENDER_EYE, "§b3D-Vorschau",
                "§7Das echte Minecraft-Flugzeugmodell wird",
                "§7vor dir in der Welt angezeigt."));

        inv.setItem(53, item(Material.EMERALD, "§aSpeichern",
                "§7Die Lackierung wird dauerhaft gespeichert."));

        player.openInventory(inv);
        showPreview(player, airline);
    }

    private ItemStack dye(int rgb, String name) {
        ItemStack item = new ItemStack(Material.LIME_DYE);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        meta.setLore(List.of("§7Klicke zuerst auf einen Farbbereich."));
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

        if (slot >= 10 && slot <= 13) {
            airline.livery.pattern = switch (slot) {
                case 10 -> Livery.Pattern.CLASSIC;
                case 11 -> Livery.Pattern.MONOCHROME;
                case 12 -> Livery.Pattern.TWO_TONE;
                default -> Livery.Pattern.TAIL_ACCENT;
            };
            p.storage.save();
            open(player);
            return;
        }

        if (slot == 19 || slot == 22 || slot == 25 || slot == 28) {
            String role = switch (slot) {
                case 19 -> "primary";
                case 22 -> "secondary";
                case 25 -> "accent";
                default -> "tail";
            };
            player.setMetadata(SELECTION, new FixedMetadataValue(p, role));
            player.sendMessage("§e" + role + " ausgewählt. Klicke jetzt auf eine Farbe.");
            return;
        }

        if (slot >= 36 && slot < 52) {
            String role = player.hasMetadata(SELECTION)
                    ? player.getMetadata(SELECTION).get(0).asString()
                    : "primary";
            int rgb = PALETTE[slot - 36];

            switch (role) {
                case "primary" -> airline.livery.primary = rgb;
                case "secondary" -> airline.livery.secondary = rgb;
                case "accent" -> airline.livery.accent = rgb;
                case "tail" -> airline.livery.tail = rgb;
            }

            p.storage.save();
            open(player);
            return;
        }

        if (slot == 34) {
            showPreview(player, airline);
            player.sendMessage("§b3D-Vorschau aktualisiert.");
            return;
        }

        if (slot == 53) {
            p.storage.save();
            player.sendMessage("§aLackierung gespeichert.");
            open(player);
        }
    }

    @EventHandler
    public void close(InventoryCloseEvent e) {
        if (TITLE.equals(e.getView().getTitle()) && e.getPlayer() instanceof Player player) {
            player.removeMetadata(SELECTION, p);
            removePreview(player.getUniqueId());
        }
    }

    private void showPreview(Player player, Airline airline) {
        removePreview(player.getUniqueId());

        String model = airline.fleet.isEmpty() ? "a320" : airline.fleet.get(0).model;
        String type = airline.fleet.isEmpty() ? "A320" : airline.fleet.get(0).type;

        org.bukkit.Location loc = player.getLocation().clone();
        org.bukkit.util.Vector direction = loc.getDirection().normalize();
        loc.add(direction.multiply(12));
        loc.add(0, 2.5, 0);
        loc.setYaw(player.getLocation().getYaw());
        loc.setPitch(0);

        ItemDisplay display = player.getWorld().spawn(loc, ItemDisplay.class);
        display.setPersistent(false);
        display.setItemStack(p.renderer.aircraftItem(model, type, airline.livery));
        display.setTransformation(new Transformation(
                new Vector3f(0, 0, 0),
                new AxisAngle4f(),
                new Vector3f(0.65f, 0.65f, 0.65f),
                new AxisAngle4f()
        ));
        display.setTeleportDuration(0);
        display.setViewRange(64);
        display.customName(net.kyori.adventure.text.Component.text("✈ Lackierungs-Vorschau"));
        display.setCustomNameVisible(true);

        previews.put(player.getUniqueId(), display);
    }

    private void removePreview(UUID uuid) {
        ItemDisplay display = previews.remove(uuid);
        if (display != null && !display.isDead()) {
            display.remove();
        }
    }

    static String hex(int rgb) {
        return String.format("%06X", rgb & 0xFFFFFF);
    }
}
