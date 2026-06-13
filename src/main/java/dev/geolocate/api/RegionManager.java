package dev.geolocate.api;

import dev.geolocate.GeoLocate;
import dev.geolocate.event.PlayerEnterGeoRegionEvent;
import dev.geolocate.event.PlayerLeaveGeoRegionEvent;
import dev.geolocate.mapping.GeoPoint;
import dev.geolocate.model.GeoRegion;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public final class RegionManager {

    private final GeoLocate plugin;
    private final Map<String, GeoRegion> regions;
    private final Map<UUID, Set<String>> playerRegions;

    public RegionManager(GeoLocate plugin) {
        this.plugin = plugin;
        this.regions = new HashMap<>();
        this.playerRegions = new HashMap<>();
    }

    public void registerRegion(GeoRegion region) {
        regions.put(region.getId(), region);
    }

    public void unregisterRegion(String regionId) {
        regions.remove(regionId);
    }

    public Optional<GeoRegion> getRegion(String regionId) {
        return Optional.ofNullable(regions.get(regionId));
    }

    public Optional<GeoRegion> getRegionByName(String name) {
        return regions.values().stream()
                .filter(r -> r.getName().equalsIgnoreCase(name))
                .findFirst();
    }

    public List<GeoRegion> getRegionsContaining(GeoPoint point) {
        List<GeoRegion> result = new ArrayList<>();
        for (GeoRegion region : regions.values()) {
            if (region.contains(point)) result.add(region);
        }
        return result;
    }

    public void updatePlayer(Player player, GeoPoint point) {
        UUID uuid = player.getUniqueId();
        Set<String> previous = playerRegions.getOrDefault(uuid, new HashSet<>());
        Set<String> current = new HashSet<>();

        for (GeoRegion region : regions.values()) {
            if (region.contains(point)) {
                current.add(region.getId());
                if (!previous.contains(region.getId())) {
                    PlayerEnterGeoRegionEvent event = new PlayerEnterGeoRegionEvent(player, region, point);
                    plugin.getServer().getPluginManager().callEvent(event);
                }
            }
        }

        for (String id : previous) {
            if (!current.contains(id)) {
                GeoRegion region = regions.get(id);
                if (region != null) {
                    PlayerLeaveGeoRegionEvent event = new PlayerLeaveGeoRegionEvent(player, region, point);
                    plugin.getServer().getPluginManager().callEvent(event);
                }
            }
        }

        playerRegions.put(uuid, current);
    }

    public Set<String> getPlayerRegionIds(UUID uuid) {
        return Collections.unmodifiableSet(playerRegions.getOrDefault(uuid, new HashSet<>()));
    }

    public List<GeoRegion> getPlayerRegions(UUID uuid) {
        List<GeoRegion> result = new ArrayList<>();
        for (String id : playerRegions.getOrDefault(uuid, new HashSet<>())) {
            GeoRegion r = regions.get(id);
            if (r != null) result.add(r);
        }
        return result;
    }

    public void removePlayer(UUID uuid) {
        playerRegions.remove(uuid);
    }

    public Collection<GeoRegion> getAllRegions() {
        return Collections.unmodifiableCollection(regions.values());
    }

    public int getRegionCount() {
        return regions.size();
    }

    public void clear() {
        regions.clear();
        playerRegions.clear();
    }
}
