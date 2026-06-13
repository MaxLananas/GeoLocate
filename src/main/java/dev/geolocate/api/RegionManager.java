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
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class RegionManager {

    private final GeoLocate plugin;
    // Region ID -> GeoRegion (lock-free reads)
    private final ConcurrentHashMap<String, GeoRegion> regions;
    // Player UUID -> Set of region IDs the player is currently inside
    private final ConcurrentHashMap<UUID, Set<String>> playerRegions;

    public RegionManager(GeoLocate plugin) {
        this.plugin = plugin;
        this.regions = new ConcurrentHashMap<>();
        this.playerRegions = new ConcurrentHashMap<>();
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
        for (GeoRegion region : regions.values()) {
            if (region.getName().equalsIgnoreCase(name)) return Optional.of(region);
        }
        return Optional.empty();
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

        // Use a ConcurrentHashMap key-set as a concurrent Set
        Set<String> previous = playerRegions.getOrDefault(uuid, Collections.emptySet());
        Set<String> current = ConcurrentHashMap.newKeySet();

        for (GeoRegion region : regions.values()) {
            if (region.contains(point)) {
                String id = region.getId();
                current.add(id);
                if (!previous.contains(id)) {
                    plugin.getServer().getPluginManager()
                            .callEvent(new PlayerEnterGeoRegionEvent(player, region, point));
                }
            }
        }

        for (String id : previous) {
            if (!current.contains(id)) {
                GeoRegion region = regions.get(id);
                if (region != null) {
                    plugin.getServer().getPluginManager()
                            .callEvent(new PlayerLeaveGeoRegionEvent(player, region, point));
                }
            }
        }

        playerRegions.put(uuid, current);
    }

    public Set<String> getPlayerRegionIds(UUID uuid) {
        return Collections.unmodifiableSet(
                playerRegions.getOrDefault(uuid, Collections.emptySet())
        );
    }

    public List<GeoRegion> getPlayerRegions(UUID uuid) {
        Set<String> ids = playerRegions.getOrDefault(uuid, Collections.emptySet());
        List<GeoRegion> result = new ArrayList<>(ids.size());
        for (String id : ids) {
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
