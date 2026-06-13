package dev.geolocate.mapping;

import org.bukkit.Location;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.LongAdder;
import java.util.concurrent.locks.StampedLock;

public final class CoordinateConverter {

    private final WorldBoundingBox box;
    private final MapProjection projection;
    private final int decimalPlaces;
    private final int cacheSize;
    private final ConcurrentHashMap<Long, GeoPoint> cache;
    private final StampedLock lock;
    private final double scale;

    private final LongAdder cacheHits;
    private final LongAdder cacheMisses;
    private final LongAdder totalConversions;

    public CoordinateConverter(WorldBoundingBox box, MapProjection projection, int decimalPlaces, int cacheSize) {
        this.box = box;
        this.projection = projection;
        this.decimalPlaces = decimalPlaces;
        this.cacheSize = cacheSize;
        this.cache = new ConcurrentHashMap<>(Math.min(cacheSize, 1024));
        this.lock = new StampedLock();
        this.scale = Math.pow(10, decimalPlaces);
        this.cacheHits = new LongAdder();
        this.cacheMisses = new LongAdder();
        this.totalConversions = new LongAdder();
    }

    public GeoPoint convert(Location location) {
        return convert(location.getX(), location.getY(), location.getZ());
    }

    public GeoPoint convert(double x, double y, double z) {
        totalConversions.increment();
        long cacheKey = buildCacheKey(x, z);

        long stamp = lock.tryOptimisticRead();
        GeoPoint cached = cache.get(cacheKey);
        if (lock.validate(stamp) && cached != null) {
            cacheHits.increment();
            return cached.altitude() == normalizeAltitude(y)
                    ? cached
                    : new GeoPoint(cached.latitude(), cached.longitude(), normalizeAltitude(y));
        }

        stamp = lock.readLock();
        try {
            cached = cache.get(cacheKey);
            if (cached != null) {
                cacheHits.increment();
                return new GeoPoint(cached.latitude(), cached.longitude(), normalizeAltitude(y));
            }
        } finally {
            lock.unlockRead(stamp);
        }

        cacheMisses.increment();
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

        return new GeoPoint(computed.latitude(), computed.longitude(), normalizeAltitude(y));
    }

    public void warmUp(int sampleStep) {
        double minX = box.getMinX();
        double maxX = box.getMaxX();
        double minZ = box.getMinZ();
        double maxZ = box.getMaxZ();

        for (double x = minX; x <= maxX; x += sampleStep) {
            for (double z = minZ; z <= maxZ; z += sampleStep) {
                if (cache.size() >= cacheSize) return;
                long key = buildCacheKey(x, z);
                final double fx = x;
                final double fz = z;
                cache.computeIfAbsent(key, k -> round(projection.toGeoPoint(fx, fz, box)));
            }
        }
    }

    public double[] convertToMinecraft(double lat, double lon) {
        return projection.toMinecraft(lat, lon, box);
    }

    private GeoPoint round(GeoPoint point) {
        double lat = Math.round(point.latitude() * scale) / scale;
        double lon = Math.round(point.longitude() * scale) / scale;
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

    public int getCacheSize() { return cache.size(); }
    public long getCacheHits() { return cacheHits.sum(); }
    public long getCacheMisses() { return cacheMisses.sum(); }
    public long getTotalConversions() { return totalConversions.sum(); }
    public double getCacheHitRate() {
        long total = cacheHits.sum() + cacheMisses.sum();
        return total == 0 ? 0 : (double) cacheHits.sum() / total * 100.0;
    }
}
