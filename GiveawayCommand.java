package com.yourserver.giveaway;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class GiveawayCommand implements CommandExecutor, TabCompleter {

    private final GiveawayPlugin plugin;
    private final MiniMessage mm = MiniMessage.miniMessage();

    public GiveawayCommand(GiveawayPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        String name = command.getName();
        if (name.equalsIgnoreCase("giveawaygui")) {
            return handleAdmin(sender, args);
        }
        if (name.equalsIgnoreCase("giveaway")) {
            // "/giveaway.adminkeyadd <id> <command>" is the same as "/giveaway adminkeyadd <id> <command>"
            String used = label.contains(":") ? label.substring(label.indexOf(':') + 1) : label;
            if (used.equalsIgnoreCase("giveaway.adminkeyadd")) {
                String[] withSub = new String[args.length + 1];
                withSub[0] = "adminkeyadd";
                System.arraycopy(args, 0, withSub, 1, args.length);
                args = withSub;
            }
            return handlePlayer(sender, args);
        }
        return false;
    }

    private boolean handleAdmin(CommandSender sender, String[] args) {
        // Bukkit permission checks already return true for ops automatically;
        // granting this node via a permissions plugin lets non-ops use it too.
        if (!sender.hasPermission("giveaway.admin")) {
            sender.sendMessage(mm.deserialize("<red>You do not have permission to use this command.</red>"));
            return true;
        }
        if (args.length == 0) {
            sender.sendMessage(mm.deserialize("<yellow>Usage: /giveawaygui [add [time]|cancel|info]</yellow>"));
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "add" -> {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage(mm.deserialize("<red>Only a player can add an item - you must be holding it.</red>"));
                    return true;
                }
                if (args.length < 2) {
                    sender.sendMessage(mm.deserialize("<yellow>Usage: /giveawaygui add [time] (e.g. 30s, 5m, 1h)</yellow>"));
                    return true;
                }
                long seconds = TimeUtil.parseSeconds(args[1]);
                if (seconds <= 0) {
                    sender.sendMessage(mm.deserialize("<red>Invalid time format. Use things like 30s, 5m, 1h, 1d.</red>"));
                    return true;
                }
                ItemStack held = player.getInventory().getItemInMainHand();
                if (held.getType().isAir()) {
                    sender.sendMessage(mm.deserialize("<red>You must be holding the item you want to give away.</red>"));
                    return true;
                }

                ItemStack toGive = held.clone();
                boolean added = plugin.getGiveawayManager().addPrize(toGive, player.getUniqueId(), seconds);
                if (!added) {
                    sender.sendMessage(mm.deserialize("<red>The giveaway is full (max "
                            + GiveawayManager.PRIZE_SLOTS.length + " items).</red>"));
                    return true;
                }
                // The item is actually taken from the host's hand so it can't be duplicated.
                player.getInventory().setItemInMainHand(new ItemStack(Material.AIR));
                sender.sendMessage(mm.deserialize("<green>Added " + toGive.getType().name() + " x" + toGive.getAmount()
                        + " to the giveaway. Ends in " + TimeUtil.formatSeconds(plugin.getGiveawayManager().getSecondsLeft())
                        + ".</green>"));
            }
            case "cancel" -> {
                if (!plugin.getGiveawayManager().isActive()) {
                    sender.sendMessage(mm.deserialize("<red>There is no active giveaway to cancel.</red>"));
                    return true;
                }
                plugin.getGiveawayManager().cancel();
                sender.sendMessage(mm.deserialize("<green>Giveaway cancelled. Prizes were returned to their hosts.</green>"));
                plugin.getGiveawayManager().broadcast("<gray>The giveaway has been cancelled by an admin.</gray>");
            }
            case "info" -> {
                if (!plugin.getGiveawayManager().isActive()) {
                    sender.sendMessage(mm.deserialize("<yellow>No giveaway is currently running.</yellow>"));
                } else {
                    sender.sendMessage(mm.deserialize("<green>Giveaway active! Prizes: "
                            + plugin.getGiveawayManager().getPrizes().size()
                            + " | Entries: " + plugin.getGiveawayManager().getParticipants().size()
                            + " | Time left: " + TimeUtil.formatSeconds(plugin.getGiveawayManager().getSecondsLeft())
                            + "</green>"));
                }
            }
            default -> sender.sendMessage(mm.deserialize("<yellow>Usage: /giveawaygui [add [time]|cancel|info]</yellow>"));
        }
        return true;
    }

    private static final String KEY_PERM = "giveaway.adminkeyadd";

    private boolean handlePlayer(CommandSender sender, String[] args) {
        if (args.length == 0) {
            return openGiveaway(sender);
        }
        switch (args[0].toLowerCase()) {
            case "admin" -> openAdminMenu(sender, args);
            case "adminkeyadd" -> keyAdd(sender, args);
            case "adminkeyremove" -> keyRemove(sender, args);
            case "adminkeylist" -> keyList(sender);
            default -> sender.sendMessage(mm.deserialize("<yellow>Usage: /giveaway</yellow>"));
        }
        return true;
    }

    private boolean openGiveaway(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(mm.deserialize("<red>Only players can use this command.</red>"));
            return true;
        }
        if (!plugin.getGiveawayManager().isActive()) {
            player.sendMessage(mm.deserialize("<yellow>There is no giveaway running right now.</yellow>"));
            return true;
        }
        player.openInventory(plugin.getGiveawayManager().buildGUI());
        return true;
    }

    /** /giveaway admin [time] - menu with all saved keys; click one to add it as a prize. */
    private void openAdminMenu(CommandSender sender, String[] args) {
        if (!sender.hasPermission("giveaway.admin")) {
            sender.sendMessage(mm.deserialize("<red>You do not have permission to use this command.</red>"));
            return;
        }
        if (!(sender instanceof Player player)) {
            sender.sendMessage(mm.deserialize("<red>Only a player can open the admin menu.</red>"));
            return;
        }
        long seconds = TimeUtil.parseSeconds(plugin.getConfig().getString("default-duration", "5m"));
        if (seconds <= 0) {
            seconds = 300;
        }
        if (args.length >= 2) {
            long custom = TimeUtil.parseSeconds(args[1]);
            if (custom <= 0) {
                sender.sendMessage(mm.deserialize("<red>Invalid time format. Use things like 30s, 5m, 1h, 1d.</red>"));
                return;
            }
            seconds = custom;
        }
        AdminGUI.open(plugin, player, seconds);
    }

    /** /giveaway adminkeyadd <id> <console command with %player%> - creates a key prize. */
    private void keyAdd(CommandSender sender, String[] args) {
        if (!sender.hasPermission(KEY_PERM)) {
            sender.sendMessage(mm.deserialize("<red>You do not have permission to use this command.</red>"));
            return;
        }
        if (args.length < 3) {
            sender.sendMessage(Component.text("Usage: /giveaway adminkeyadd <id> <console command, use %player% for the winner>",
                    NamedTextColor.YELLOW));
            sender.sendMessage(Component.text("Hold an item to use as the icon (empty hand = tripwire hook).",
                    NamedTextColor.GRAY));
            return;
        }
        String id = args[1];
        if (!id.matches("[A-Za-z0-9_-]{1,32}")) {
            sender.sendMessage(Component.text("Key id may only use letters, numbers, - and _ (max 32).", NamedTextColor.RED));
            return;
        }
        String keyCommand = String.join(" ", Arrays.copyOfRange(args, 2, args.length));
        if (keyCommand.startsWith("/")) {
            keyCommand = keyCommand.substring(1);
        }

        ItemStack icon = new ItemStack(Material.TRIPWIRE_HOOK);
        if (sender instanceof Player player) {
            ItemStack held = player.getInventory().getItemInMainHand();
            if (!held.getType().isAir()) {
                icon = held.clone();
                icon.setAmount(1);
            }
        }
        ItemMeta meta = icon.getItemMeta();
        if (meta != null && !meta.hasDisplayName()) {
            meta.displayName(Component.text(id, NamedTextColor.GOLD));
            icon.setItemMeta(meta);
        }

        boolean replaced = plugin.getKeyStore().add(id, icon, keyCommand);
        sender.sendMessage(Component.text((replaced ? "Updated" : "Created") + " key '" + id.toLowerCase()
                + "'. Winner command: /" + keyCommand, NamedTextColor.GREEN));
        if (!keyCommand.contains("%player%")) {
            sender.sendMessage(Component.text("Warning: the command has no %player%, so it will not know who won.",
                    NamedTextColor.YELLOW));
        }
        sender.sendMessage(Component.text("Use /giveaway admin to add it to a giveaway.", NamedTextColor.GRAY));
    }

    private void keyRemove(CommandSender sender, String[] args) {
        if (!sender.hasPermission(KEY_PERM)) {
            sender.sendMessage(mm.deserialize("<red>You do not have permission to use this command.</red>"));
            return;
        }
        if (args.length < 2) {
            sender.sendMessage(Component.text("Usage: /giveaway adminkeyremove <id>", NamedTextColor.YELLOW));
            return;
        }
        boolean removed = plugin.getKeyStore().remove(args[1]);
        sender.sendMessage(Component.text(removed ? "Removed key '" + args[1].toLowerCase() + "'."
                : "No key with that id.", removed ? NamedTextColor.GREEN : NamedTextColor.RED));
    }

    private void keyList(CommandSender sender) {
        if (!sender.hasPermission(KEY_PERM)) {
            sender.sendMessage(mm.deserialize("<red>You do not have permission to use this command.</red>"));
            return;
        }
        List<KeyStore.Key> keys = plugin.getKeyStore().all();
        if (keys.isEmpty()) {
            sender.sendMessage(Component.text("No keys yet. Create one with /giveaway adminkeyadd <id> <command>",
                    NamedTextColor.YELLOW));
            return;
        }
        sender.sendMessage(Component.text("Giveaway keys (" + keys.size() + "):", NamedTextColor.GREEN));
        for (KeyStore.Key key : keys) {
            sender.sendMessage(Component.text("- " + key.id() + " -> /" + key.command(), NamedTextColor.GRAY));
        }
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, String[] args) {
        String name = command.getName();
        if (name.equalsIgnoreCase("giveawaygui") && args.length == 1) {
            return filter(Arrays.asList("add", "cancel", "info"), args[0]);
        }
        if (name.equalsIgnoreCase("giveawaygui") && args.length == 2 && args[0].equalsIgnoreCase("add")) {
            return filter(Arrays.asList("30s", "1m", "5m", "10m", "1h"), args[1]);
        }
        if (name.equalsIgnoreCase("giveaway") && sender.hasPermission("giveaway.admin")) {
            if (args.length == 1) {
                return filter(Arrays.asList("admin", "adminkeyadd", "adminkeyremove", "adminkeylist"), args[0]);
            }
            if (args.length == 2 && args[0].equalsIgnoreCase("adminkeyremove")) {
                List<String> ids = new ArrayList<>();
                for (KeyStore.Key key : plugin.getKeyStore().all()) {
                    ids.add(key.id());
                }
                return filter(ids, args[1]);
            }
            if (args.length == 2 && args[0].equalsIgnoreCase("admin")) {
                return filter(Arrays.asList("30s", "1m", "5m", "10m", "1h"), args[1]);
            }
        }
        return new ArrayList<>();
    }

    private List<String> filter(List<String> options, String input) {
        List<String> result = new ArrayList<>();
        for (String option : options) {
            if (option.toLowerCase().startsWith(input.toLowerCase())) {
                result.add(option);
            }
        }
        return result;
    }
}
