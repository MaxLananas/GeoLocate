package dev.geolocate.mapping;

import org.bukkit.Location;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.StampedLock;

public final class CoordinateConverter {

    private final WorldBoundingBox box;
    private final MapProjection projection;
    private final int decimalPlaces;
    private final int cacheSize;
    private final ConcurrentHashMap<Long, GeoPoint> cache;
    private final StampedLock lock;
    private final double scale;

    public CoordinateConverter(WorldBoundingBox box, MapProjection projection, int decimalPlaces, int cacheSize) {
        this.box = box;
        this.projection = projection;
        this.decimalPlaces = decimalPlaces;
        this.cacheSize = cacheSize;
        this.cache = new ConcurrentHashMap<>(cacheSize);
        this.lock = new StampedLock();
        this.scale = Math.pow(10, decimalPlaces);
    }

    public GeoPoint convert(Location location) {
        return convert(location.getX(), location.getY(), location.getZ());
    }

    public GeoPoint convert(double x, double y, double z) {
        long cacheKey = buildCacheKey(x, z);

        long stamp = lock.tryOptimisticRead();
        GeoPoint cached = cache.get(cacheKey);
        if (lock.validate(stamp) && cached != null) {
            return new GeoPoint(cached.getLatitude(), cached.getLongitude(), normalizeAltitude(y));
        }

        stamp = lock.readLock();
        try {
            cached = cache.get(cacheKey);
            if (cached != null) {
                return new GeoPoint(cached.getLatitude(), cached.getLongitude(), normalizeAltitude(y));
            }
        } finally {
            lock.unlockRead(stamp);
        }

        GeoPoint computed = round(projection.toGeoPoint(x, z, box));

        stamp = lock.writeLock();
        try {
            if (cache.size() >= cacheSize) {
                cache.clear();
            }
            cache.putIfAbsent(cacheKey, computed);
        } finally {
            lock.unlockWrite(stamp);
        }

        return new GeoPoint(computed.getLatitude(), computed.getLongitude(), normalizeAltitude(y));
    }

    public double[] convertToMinecraft(double lat, double lon) {
        return projection.toMinecraft(lat, lon, box);
    }

    private GeoPoint round(GeoPoint point) {
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
        long stamp = lock.writeLock();
        try {
            cache.clear();
        } finally {
            lock.unlockWrite(stamp);
        }
    }

    public int getCacheSize() {
        return cache.size();
    }
}
