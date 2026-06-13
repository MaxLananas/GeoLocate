package dev.geolocate.storage;

import dev.geolocate.GeoLocate;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class PlayerPreferenceStorage {

    private final GeoLocate plugin;
    private final File      file;

    // ConcurrentHashMap for thread-safe toggle operations
    private final ConcurrentHashMap<UUID, Boolean> notifyToggle;
    private final ConcurrentHashMap<UUID, Boolean> actionBarToggle;

    public PlayerPreferenceStorage(GeoLocate plugin) {
        this.plugin          = plugin;
        this.file            = new File(plugin.getDataFolder(), "players.yml");
        this.notifyToggle    = new ConcurrentHashMap<>();
        this.actionBarToggle = new ConcurrentHashMap<>();
        load();
    }

    private void load() {
        if (!file.exists()) {
            try {
                file.getParentFile().mkdirs();
                file.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("Failed to create players.yml: " + e.getMessage());
            }
        }

        FileConfiguration data = YamlConfiguration.loadConfiguration(file);

        if (data.isConfigurationSection("notify")) {
            for (String key : data.getConfigurationSection("notify").getKeys(false)) {
                try { notifyToggle.put(UUID.fromString(key), data.getBoolean("notify." + key)); }
                catch (IllegalArgumentException ignored) {}
            }
        }
        if (data.isConfigurationSection("actionbar")) {
            for (String key : data.getConfigurationSection("actionbar").getKeys(false)) {
                try { actionBarToggle.put(UUID.fromString(key), data.getBoolean("actionbar." + key)); }
                catch (IllegalArgumentException ignored) {}
            }
        }
    }

    /** Synchronous save — call from onDisable or on player quit. */
    public void saveAll() {
        FileConfiguration data = new YamlConfiguration();
        notifyToggle.forEach((uuid, value)    -> data.set("notify."    + uuid, value));
        actionBarToggle.forEach((uuid, value) -> data.set("actionbar." + uuid, value));
        try {
            data.save(file);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save players.yml: " + e.getMessage());
        }
    }

    /** Asynchronous save — safe to call during normal gameplay. */
    public void saveAllAsync() {
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, this::saveAll);
    }

    public boolean hasNotifyEnabled(UUID uuid)  { return notifyToggle.getOrDefault(uuid, false); }
    public void setNotifyEnabled(UUID uuid, boolean enabled) { notifyToggle.put(uuid, enabled); }
    public boolean toggleNotify(UUID uuid) {
        return notifyToggle.merge(uuid, true, (old, ignored) -> !old);
    }

    public boolean hasActionBarEnabled(UUID uuid) { return actionBarToggle.getOrDefault(uuid, false); }
    public void setActionBarEnabled(UUID uuid, boolean enabled) { actionBarToggle.put(uuid, enabled); }
    public boolean toggleActionBar(UUID uuid) {
        return actionBarToggle.merge(uuid, true, (old, ignored) -> !old);
    }

    public void remove(UUID uuid) {
        notifyToggle.remove(uuid);
        actionBarToggle.remove(uuid);
    }
}
