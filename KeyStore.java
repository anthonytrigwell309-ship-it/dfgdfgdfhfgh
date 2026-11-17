package com.yourserver.giveaway;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Saved "key" prizes (plugins/GiveawayGUI/keys.yml).
 * A key = an icon shown in the giveaway menu + a console command that is run
 * for the winner when the giveaway ends. %player% in the command is replaced by the winner's name.
 */
public class KeyStore {

    /** One saved key prize. */
    public record Key(String id, ItemStack icon, String command) {
    }

    private final GiveawayPlugin plugin;
    private final File file;
    private final YamlConfiguration yml;

    public KeyStore(GiveawayPlugin plugin) {
        this.plugin = plugin;
        plugin.getDataFolder().mkdirs();
        this.file = new File(plugin.getDataFolder(), "keys.yml");
        this.yml = YamlConfiguration.loadConfiguration(file);
    }

    /** Returns true if an existing key with this id was replaced. */
    public synchronized boolean add(String id, ItemStack icon, String command) {
        String path = "keys." + id.toLowerCase();
        boolean replaced = yml.contains(path);
        yml.set(path + ".icon", icon);
        yml.set(path + ".command", command);
        save();
        return replaced;
    }

    public synchronized boolean remove(String id) {
        String path = "keys." + id.toLowerCase();
        if (!yml.contains(path)) {
            return false;
        }
        yml.set(path, null);
        save();
        return true;
    }

    public synchronized Key get(String id) {
        String path = "keys." + id.toLowerCase();
        ItemStack icon = yml.getItemStack(path + ".icon");
        String command = yml.getString(path + ".command");
        if (icon == null || command == null) {
            return null;
        }
        return new Key(id.toLowerCase(), icon, command);
    }

    public synchronized List<Key> all() {
        List<Key> result = new ArrayList<>();
        ConfigurationSection section = yml.getConfigurationSection("keys");
        if (section != null) {
            for (String id : section.getKeys(false)) {
                Key key = get(id);
                if (key != null) {
                    result.add(key);
                }
            }
        }
        result.sort(Comparator.comparing(Key::id));
        return result;
    }

    private void save() {
        try {
            yml.save(file);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save keys.yml: " + e.getMessage());
        }
    }
}
