package dev.geolocate.storage;

import dev.geolocate.GeoLocate;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class PlayerPreferenceStorage {

    private final GeoLocate plugin;
    private final File file;
    private FileConfiguration data;

    private final Map<UUID, Boolean> notifyToggle;
    private final Map<UUID, Boolean> actionBarToggle;

    public PlayerPreferenceStorage(GeoLocate plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "players.yml");
        this.notifyToggle = new HashMap<>();
        this.actionBarToggle = new HashMap<>();
        load();
    }

    private void load() {
        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("Failed to create players.yml: " + e.getMessage());
            }
        }
        data = YamlConfiguration.loadConfiguration(file);

        if (data.isConfigurationSection("notify")) {
            for (String key : data.getConfigurationSection("notify").getKeys(false)) {
                try {
                    notifyToggle.put(UUID.fromString(key), data.getBoolean("notify." + key));
                } catch (IllegalArgumentException ignored) {}
            }
        }

        if (data.isConfigurationSection("actionbar")) {
            for (String key : data.getConfigurationSection("actionbar").getKeys(false)) {
                try {
                    actionBarToggle.put(UUID.fromString(key), data.getBoolean("actionbar." + key));
                } catch (IllegalArgumentException ignored) {}
            }
        }
    }

    public void saveAll() {
        notifyToggle.forEach((uuid, value) -> data.set("notify." + uuid, value));
        actionBarToggle.forEach((uuid, value) -> data.set("actionbar." + uuid, value));
        try {
            data.save(file);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save players.yml: " + e.getMessage());
        }
    }

    public boolean hasNotifyEnabled(UUID uuid) {
        return notifyToggle.getOrDefault(uuid, false);
    }

    public void setNotifyEnabled(UUID uuid, boolean enabled) {
        notifyToggle.put(uuid, enabled);
    }

    public boolean toggleNotify(UUID uuid) {
        boolean current = hasNotifyEnabled(uuid);
        notifyToggle.put(uuid, !current);
        return !current;
    }

    public boolean hasActionBarEnabled(UUID uuid) {
        return actionBarToggle.getOrDefault(uuid, false);
    }

    public void setActionBarEnabled(UUID uuid, boolean enabled) {
        actionBarToggle.put(uuid, enabled);
    }

    public boolean toggleActionBar(UUID uuid) {
        boolean current = hasActionBarEnabled(uuid);
        actionBarToggle.put(uuid, !current);
        return !current;
    }

    public void remove(UUID uuid) {
        notifyToggle.remove(uuid);
        actionBarToggle.remove(uuid);
    }
}
