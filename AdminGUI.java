package com.yourserver.giveaway;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

/** The /giveaway admin menu: click a key to add it as a prize. */
public final class AdminGUI {

    public static final int SIZE = 54;
    public static final int KEY_SLOTS = 45;      // slots 0-44 hold keys
    public static final int INFO_SLOT = 49;
    public static final int CANCEL_SLOT = 53;

    private AdminGUI() {
    }

    public static void open(GiveawayPlugin plugin, Player player, long durationSeconds) {
        List<KeyStore.Key> keys = plugin.getKeyStore().all();
        List<String> ids = new ArrayList<>();

        GiveawayAdminHolder holder = new GiveawayAdminHolder(ids, durationSeconds);
        Inventory inv = Bukkit.createInventory(holder, SIZE, Component.text("Giveaway Admin - Keys", NamedTextColor.GOLD));
        holder.setInventory(inv);

        for (int i = KEY_SLOTS; i < SIZE; i++) {
            inv.setItem(i, named(Material.GRAY_STAINED_GLASS_PANE, " ", List.of()));
        }

        for (int i = 0; i < keys.size() && i < KEY_SLOTS; i++) {
            KeyStore.Key key = keys.get(i);
            ids.add(key.id());
            ItemStack icon = key.icon().clone();
            ItemMeta meta = icon.getItemMeta();
            if (meta != null) {
                meta.lore(List.of(
                        Component.text("Key: " + key.id(), NamedTextColor.GRAY),
                        Component.text("Click to add as a giveaway prize", NamedTextColor.YELLOW)));
                icon.setItemMeta(meta);
            }
            inv.setItem(i, icon);
        }

        inv.setItem(INFO_SLOT, named(Material.PAPER, "Add a key prize", List.of(
                Component.text("Click a key above to add it.", NamedTextColor.GRAY),
                Component.text("Duration: " + TimeUtil.formatSeconds(durationSeconds), NamedTextColor.GRAY),
                Component.text("Keys saved: " + keys.size(), NamedTextColor.GRAY))));
        inv.setItem(CANCEL_SLOT, named(Material.BARRIER, "Cancel giveaway", List.of(
                Component.text("Stops the running giveaway.", NamedTextColor.GRAY))));

        player.openInventory(inv);
    }

    private static ItemStack named(Material material, String name, List<Component> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text(name));
            if (!lore.isEmpty()) {
                meta.lore(lore);
            }
            item.setItemMeta(meta);
        }
        return item;
    }
}
