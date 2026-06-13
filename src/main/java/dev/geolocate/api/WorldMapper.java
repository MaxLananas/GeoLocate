package dev.geolocate.api;

import dev.geolocate.GeoLocate;
import dev.geolocate.config.GeoLocateConfig;
import dev.geolocate.mapping.CoordinateConverter;
import dev.geolocate.mapping.GeoPoint;
import org.bukkit.Location;
import org.bukkit.World;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public final class WorldMapper {

    private final GeoLocate plugin;
    private final Map<String, CoordinateConverter> converters;

    public WorldMapper(GeoLocate plugin) {
        this.plugin = plugin;
        this.converters = new HashMap<>();
        initialize();
    }

    private void initialize() {
        converters.clear();
        for (Map.Entry<String, GeoLocateConfig.WorldConfig> entry : plugin.getGeoConfig().getWorldConfigs().entrySet()) {
            GeoLocateConfig.WorldConfig wc = entry.getValue();
            CoordinateConverter converter = new CoordinateConverter(
                    wc.getBoundingBox(),
                    wc.getProjection(),
                    plugin.getGeoConfig().getDecimalPlaces(),
                    plugin.getGeoConfig().getCacheSize()
            );
            converters.put(entry.getKey(), converter);
        }
    }

    public Optional<GeoPoint> getGeoPoint(Location location) {
        if (location == null || location.getWorld() == null) return Optional.empty();
        String worldName = location.getWorld().getName();
        CoordinateConverter converter = converters.get(worldName);
        if (converter == null) return Optional.empty();
        return Optional.of(converter.convert(location));
    }

    public Optional<GeoPoint> getGeoPoint(World world, double x, double y, double z) {
        if (world == null) return Optional.empty();
        CoordinateConverter converter = converters.get(world.getName());
        if (converter == null) return Optional.empty();
        return Optional.of(converter.convert(x, y, z));
    }

    public boolean isWorldMapped(String worldName) {
        return converters.containsKey(worldName);
    }

    public int getConfiguredWorldCount() {
        return converters.size();
    }

    public void clearCache(String worldName) {
        CoordinateConverter converter = converters.get(worldName);
        if (converter != null) converter.clearCache();
    }

    public void clearAllCaches() {
        converters.values().forEach(CoordinateConverter::clearCache);
    }
}
