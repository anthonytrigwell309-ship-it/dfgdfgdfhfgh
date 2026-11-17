package com.yourserver.giveaway;

import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Arrays;

public class GiveawayGUIListener implements Listener {

    private final GiveawayPlugin plugin;
    private final MiniMessage mm = MiniMessage.miniMessage();

    public GiveawayGUIListener(GiveawayPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof GiveawayGUIHolder holder)) {
            return;
        }
        // Always cancel: nobody should be able to take items out of these menus.
        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        int slot = event.getRawSlot();

        if (holder instanceof GiveawayAdminHolder admin) {
            handleAdminClick(player, admin, slot);
            return;
        }

        if (slot < 0 || slot >= GiveawayManager.GUI_SIZE) {
            return;
        }
        boolean isPrizeSlot = Arrays.stream(GiveawayManager.PRIZE_SLOTS).anyMatch(s -> s == slot);
        if (!isPrizeSlot) {
            return;
        }
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType().isAir()) {
            return;
        }

        GiveawayManager manager = plugin.getGiveawayManager();
        if (!manager.isActive()) {
            player.sendMessage(mm.deserialize("<red>This giveaway has already ended.</red>"));
            return;
        }
        if (manager.hasJoined(player)) {
            player.sendMessage(mm.deserialize("<yellow>You have already entered this giveaway!</yellow>"));
            return;
        }
        manager.join(player);
        player.sendMessage(mm.deserialize("<green>You have entered the giveaway! Good luck!</green>"));
    }

    private void handleAdminClick(Player player, GiveawayAdminHolder admin, int slot) {
        if (!player.hasPermission("giveaway.admin")) {
            player.closeInventory();
            return;
        }
        GiveawayManager manager = plugin.getGiveawayManager();

        if (slot == AdminGUI.CANCEL_SLOT) {
            if (!manager.isActive()) {
                player.sendMessage(mm.deserialize("<red>There is no active giveaway to cancel.</red>"));
                return;
            }
            manager.cancel();
            manager.broadcast("<gray>The giveaway has been cancelled by an admin.</gray>");
            player.sendMessage(mm.deserialize("<green>Giveaway cancelled.</green>"));
            return;
        }

        if (slot < 0 || slot >= AdminGUI.KEY_SLOTS || slot >= admin.keyIds().size()) {
            return;
        }
        KeyStore.Key key = plugin.getKeyStore().get(admin.keyIds().get(slot));
        if (key == null) {
            player.sendMessage(mm.deserialize("<red>That key no longer exists.</red>"));
            return;
        }
        if (!manager.addKeyPrize(key, admin.durationSeconds())) {
            player.sendMessage(mm.deserialize("<red>The giveaway is full (max "
                    + GiveawayManager.PRIZE_SLOTS.length + " prizes).</red>"));
            return;
        }
        player.sendMessage(mm.deserialize("<green>Added key <white>" + key.id() + "</white> to the giveaway. Ends in "
                + TimeUtil.formatSeconds(manager.getSecondsLeft()) + ".</green>"));
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (event.getInventory().getHolder() instanceof GiveawayGUIHolder) {
            event.setCancelled(true);
        }
    }
}
