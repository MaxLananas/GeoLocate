package dev.geolocate.api;

import dev.geolocate.GeoLocate;
import dev.geolocate.border.GeoBorderDetector;
import dev.geolocate.export.GeoExporter;
import dev.geolocate.heatmap.GeoHeatmap;
import dev.geolocate.history.PlayerGeoHistory;
import dev.geolocate.mapping.GeoPoint;
import dev.geolocate.model.DistanceMatrix;
import dev.geolocate.model.GeoPath;
import dev.geolocate.model.GeoRegion;
import dev.geolocate.model.GeoSnapshot;
import dev.geolocate.util.GoogleMapsLink;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public final class GeoLocateAPI {

    private static GeoLocateAPI instance;
    private final GeoLocate plugin;
    private final RegionManager regionManager;
    private final Map<UUID, PlayerGeoHistory> histories;
    private final GeoHeatmap globalHeatmap;

    public GeoLocateAPI(GeoLocate plugin) {
        this.plugin = plugin;
        this.regionManager = new RegionManager(plugin);
        this.histories = new HashMap<>();
        this.globalHeatmap = new GeoHeatmap(0.01);
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
        return getGeoPoint(location).map(p -> GoogleMapsLink.build(p, plugin.getGeoConfig().getGoogleMapsZoom()));
    }

    public Optional<String> getGoogleMapsLink(Player player) {
        return getGoogleMapsLink(player.getLocation());
    }

    public Optional<String> getGoogleMapsLink(GeoPoint point) {
        return Optional.of(GoogleMapsLink.build(point, plugin.getGeoConfig().getGoogleMapsZoom()));
    }

    public Optional<String> getOpenStreetMapLink(Player player) {
        return getGeoPoint(player).map(p -> GoogleMapsLink.buildOpenStreetMap(p, plugin.getGeoConfig().getGoogleMapsZoom()));
    }

    public Optional<String> getAppleMapsLink(Player player) {
        return getGeoPoint(player).map(p -> GoogleMapsLink.buildAppleMaps(p, plugin.getGeoConfig().getGoogleMapsZoom()));
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

    public RegionManager getRegionManager() {
        return regionManager;
    }

    public void registerRegion(GeoRegion region) {
        regionManager.registerRegion(region);
    }

    public List<GeoRegion> getRegionsAt(Player player) {
        return getGeoPoint(player)
                .map(regionManager::getRegionsContaining)
                .orElse(List.of());
    }

    public List<GeoRegion> getRegionsAt(GeoPoint point) {
        return regionManager.getRegionsContaining(point);
    }

    public PlayerGeoHistory getHistory(UUID uuid) {
        return histories.computeIfAbsent(uuid, id -> new PlayerGeoHistory(id, 500));
    }

    public PlayerGeoHistory getHistory(Player player) {
        return getHistory(player.getUniqueId());
    }

    public void recordSnapshot(Player player) {
        getGeoPoint(player).ifPresent(point -> {
            GeoSnapshot snapshot = new GeoSnapshot(
                    player.getUniqueId(),
                    player.getName(),
                    point,
                    player.getWorld().getName(),
                    player.getLocation().getX(),
                    player.getLocation().getY(),
                    player.getLocation().getZ()
            );
            getHistory(player).record(snapshot);
            globalHeatmap.record(point);
        });
    }

    public GeoPath getPlayerPath(Player player) {
        return getHistory(player).toPath();
    }

    public GeoPath getPlayerPath(UUID uuid) {
        return getHistory(uuid).toPath();
    }

    public GeoHeatmap getGlobalHeatmap() {
        return globalHeatmap;
    }

    public GeoHeatmap createHeatmap(double cellSizeDegrees) {
        return new GeoHeatmap(cellSizeDegrees);
    }

    public DistanceMatrix buildDistanceMatrix(Collection<? extends Player> players) {
        Map<String, GeoPoint> points = new HashMap<>();
        for (Player p : players) {
            getGeoPoint(p).ifPresent(gp -> points.put(p.getName(), gp));
        }
        return new DistanceMatrix(points);
    }

    public Optional<Double> getRealWorldDistance(Player a, Player b) {
        Optional<GeoPoint> pa = getGeoPoint(a);
        Optional<GeoPoint> pb = getGeoPoint(b);
        if (pa.isEmpty() || pb.isEmpty()) return Optional.empty();
        return Optional.of(pa.get().distanceTo(pb.get()));
    }

    public Optional<GeoBorderDetector> getBorderDetector(String worldName) {
        return Optional.ofNullable(plugin.getGeoConfig().getWorldConfig(worldName))
                .map(GeoBorderDetector::new);
    }

    public Optional<GeoBorderDetector> getBorderDetector(Player player) {
        return getBorderDetector(player.getWorld().getName());
    }

    public GeoExporter getExporter() {
        return null;
    }

    public String exportHistoryCSV(Player player) {
        return getHistory(player).toCSV();
    }

    public String exportHistoryGeoJSON(Player player) {
        return getHistory(player).toGeoJSON();
    }

    public String exportPathGPX(Player player) {
        return GeoExporter.pathToGPX(getPlayerPath(player));
    }

    public String exportPathKML(Player player) {
        return GeoExporter.pathToKML(getPlayerPath(player));
    }

    public void clearHistory(UUID uuid) {
        PlayerGeoHistory history = histories.get(uuid);
        if (history != null) history.clear();
    }

    public void removePlayer(UUID uuid) {
        histories.remove(uuid);
        regionManager.removePlayer(uuid);
    }
}
