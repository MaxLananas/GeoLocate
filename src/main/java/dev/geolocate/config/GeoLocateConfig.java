package dev.geolocate.config;

import dev.geolocate.GeoLocate;
import dev.geolocate.mapping.MapProjection;
import dev.geolocate.mapping.WorldBoundingBox;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public final class GeoLocateConfig {

    private final GeoLocate plugin;
    private final Map<String, WorldConfig> worldConfigs;

    private int googleMapsZoom;
    private boolean showAltitude;
    private boolean notifyOnMove;
    private int notifyDistance;
    private int cacheSize;
    private int decimalPlaces;
    private String prefix;
    private Map<String, String> messages;

    public GeoLocateConfig(GeoLocate plugin) {
        this.plugin = plugin;
        this.worldConfigs = new HashMap<>();
        this.messages = new HashMap<>();
        load();
    }

    private void load() {
        FileConfiguration config = plugin.getConfig();

        googleMapsZoom = config.getInt("settings.google-maps-zoom", 15);
        showAltitude = config.getBoolean("settings.show-altitude", true);
        notifyOnMove = config.getBoolean("settings.notify-on-move", false);
        notifyDistance = config.getInt("settings.notify-distance", 100);
        cacheSize = config.getInt("settings.cache-size", 500);
        decimalPlaces = config.getInt("settings.decimal-places", 6);
        prefix = config.getString("messages.prefix", "[GeoLocate] ");

        loadMessages(config);
        loadWorlds(config);
    }

    private void loadMessages(FileConfiguration config) {
        ConfigurationSection section = config.getConfigurationSection("messages");
        if (section == null) return;
        for (String key : section.getKeys(false)) {
            messages.put(key, section.getString(key, ""));
        }
    }

    private void loadWorlds(FileConfiguration config) {
        worldConfigs.clear();
        ConfigurationSection worlds = config.getConfigurationSection("worlds");
        if (worlds == null) return;

        for (String worldName : worlds.getKeys(false)) {
            ConfigurationSection ws = worlds.getConfigurationSection(worldName);
            if (ws == null) continue;

            boolean enabled = ws.getBoolean("enabled", true);
            if (!enabled) continue;

            String projName = ws.getString("projection", "LINEAR");
            MapProjection projection = MapProjection.fromString(projName);

            double minLat = ws.getDouble("bounds.min-lat", -85.05112878);
            double maxLat = ws.getDouble("bounds.max-lat", 85.05112878);
            double minLon = ws.getDouble("bounds.min-lon", -180.0);
            double maxLon = ws.getDouble("bounds.max-lon", 180.0);
            double minX = ws.getDouble("minecraft.min-x", -29999984);
            double maxX = ws.getDouble("minecraft.max-x", 29999984);
            double minZ = ws.getDouble("minecraft.min-z", -29999984);
            double maxZ = ws.getDouble("minecraft.max-z", 29999984);
            double offsetX = ws.getDouble("offset.x", 0);
            double offsetZ = ws.getDouble("offset.z", 0);

            WorldBoundingBox box = new WorldBoundingBox(
                    minLat, maxLat, minLon, maxLon,
                    minX, maxX, minZ, maxZ,
                    offsetX, offsetZ
            );

            worldConfigs.put(worldName, new WorldConfig(worldName, projection, box));
        }
    }

    public WorldConfig getWorldConfig(String worldName) {
        return worldConfigs.get(worldName);
    }

    public Map<String, WorldConfig> getWorldConfigs() {
        return Collections.unmodifiableMap(worldConfigs);
    }

    public int getGoogleMapsZoom() { return googleMapsZoom; }
    public boolean isShowAltitude() { return showAltitude; }
    public boolean isNotifyOnMove() { return notifyOnMove; }
    public int getNotifyDistance() { return notifyDistance; }
    public int getCacheSize() { return cacheSize; }
    public int getDecimalPlaces() { return decimalPlaces; }
    public String getPrefix() { return prefix; }

    public String getMessage(String key) {
        return messages.getOrDefault(key, "");
    }

    public static final class WorldConfig {
        private final String worldName;
        private final MapProjection projection;
        private final WorldBoundingBox boundingBox;

        public WorldConfig(String worldName, MapProjection projection, WorldBoundingBox boundingBox) {
            this.worldName = worldName;
            this.projection = projection;
            this.boundingBox = boundingBox;
        }

        public String getWorldName() { return worldName; }
        public MapProjection getProjection() { return projection; }
        public WorldBoundingBox getBoundingBox() { return boundingBox; }
    }
}
