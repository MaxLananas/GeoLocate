package dev.geolocate.mapping;

import org.bukkit.Location;

import java.util.LinkedHashMap;
import java.util.Map;

public final class CoordinateConverter {

    private final WorldBoundingBox box;
    private final MapProjection projection;
    private final int decimalPlaces;
    private final Map<Long, GeoPoint> cache;
    private final int cacheSize;

    public CoordinateConverter(WorldBoundingBox box, MapProjection projection, int decimalPlaces, int cacheSize) {
        this.box = box;
        this.projection = projection;
        this.decimalPlaces = decimalPlaces;
        this.cacheSize = cacheSize;
        this.cache = new LinkedHashMap<>(cacheSize, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<Long, GeoPoint> eldest) {
                return size() > cacheSize;
            }
        };
    }

    public GeoPoint convert(Location location) {
        return convert(location.getX(), location.getY(), location.getZ());
    }

    public GeoPoint convert(double x, double y, double z) {
        long cacheKey = buildCacheKey(x, z);
        GeoPoint cached = cache.get(cacheKey);
        if (cached != null) {
            return new GeoPoint(cached.getLatitude(), cached.getLongitude(), normalizeAltitude(y));
        }

        GeoPoint point = projection.toGeoPoint(x, z, box);
        GeoPoint rounded = round(point);
        cache.put(cacheKey, rounded);

        return new GeoPoint(rounded.getLatitude(), rounded.getLongitude(), normalizeAltitude(y));
    }

    public double[] convertToMinecraft(double lat, double lon) {
        return projection.toMinecraft(lat, lon, box);
    }

    private GeoPoint round(GeoPoint point) {
        double scale = Math.pow(10, decimalPlaces);
        double lat = Math.round(point.getLatitude() * scale) / scale;
        double lon = Math.round(point.getLongitude() * scale) / scale;
        return new GeoPoint(lat, lon);
    }

    private double normalizeAltitude(double y) {
        return Math.round(y * 100.0) / 100.0;
    }

    private long buildCacheKey(double x, double z) {
        int ix = (int) Math.floor(x);
        int iz = (int) Math.floor(z);
        return ((long) ix << 32) | (iz & 0xFFFFFFFFL);
    }

    public void clearCache() {
        cache.clear();
    }

    public int getCacheSize() {
        return cache.size();
    }
}
