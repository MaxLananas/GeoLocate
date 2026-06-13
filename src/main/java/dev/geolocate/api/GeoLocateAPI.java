package dev.geolocate.api;

import dev.geolocate.GeoLocate;
import dev.geolocate.mapping.GeoPoint;
import dev.geolocate.util.GoogleMapsLink;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public final class GeoLocateAPI {

    private static GeoLocateAPI instance;
    private final GeoLocate plugin;

    public GeoLocateAPI(GeoLocate plugin) {
        this.plugin = plugin;
        instance = this;
    }

    public static GeoLocateAPI get() {
        if (instance == null) {
            throw new IllegalStateException("GeoLocate is not loaded.");
        }
        return instance;
    }

    public Optional<GeoPoint> getGeoPoint(Location location) {
        return plugin.getWorldMapper().getGeoPoint(location);
    }

    public Optional<GeoPoint> getGeoPoint(Player player) {
        return getGeoPoint(player.getLocation());
    }

    public Optional<GeoPoint> getGeoPoint(World world, double x, double y, double z) {
        return plugin.getWorldMapper().getGeoPoint(world, x, y, z);
    }

    public CompletableFuture<Optional<GeoPoint>> getGeoPointAsync(Player player) {
        Location loc = player.getLocation();
        return CompletableFuture.supplyAsync(() -> getGeoPoint(loc));
    }

    public CompletableFuture<Optional<GeoPoint>> getGeoPointAsync(Location location) {
        return CompletableFuture.supplyAsync(() -> getGeoPoint(location));
    }

    public Optional<String> getGoogleMapsLink(Location location) {
        return getGeoPoint(location).map(point ->
                GoogleMapsLink.build(point, plugin.getGeoConfig().getGoogleMapsZoom())
        );
    }

    public Optional<String> getGoogleMapsLink(Player player) {
        return getGoogleMapsLink(player.getLocation());
    }

    public Optional<String> getGoogleMapsLink(GeoPoint point) {
        return Optional.of(GoogleMapsLink.build(point, plugin.getGeoConfig().getGoogleMapsZoom()));
    }

    public boolean isWorldMapped(String worldName) {
        return plugin.getWorldMapper().isWorldMapped(worldName);
    }

    public boolean isWorldMapped(World world) {
        return world != null && isWorldMapped(world.getName());
    }

    public void clearCache() {
        plugin.getWorldMapper().clearAllCaches();
    }
}
