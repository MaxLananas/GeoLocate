package dev.geolocate.heatmap;

import dev.geolocate.mapping.GeoPoint;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.LongAdder;

public final class GeoHeatmap {

    private final double cellSizeDegrees;
    private final ConcurrentHashMap<Long, AtomicInteger> cells;
    // Maintained incrementally to avoid full iteration on getTotalRecordings()
    private final LongAdder totalRecordings;

    public GeoHeatmap(double cellSizeDegrees) {
        this.cellSizeDegrees  = cellSizeDegrees;
        this.cells            = new ConcurrentHashMap<>();
        this.totalRecordings  = new LongAdder();
    }

    public void record(GeoPoint point) {
        long key = toKey(point);
        cells.computeIfAbsent(key, k -> new AtomicInteger(0)).incrementAndGet();
        totalRecordings.increment();
    }

    public int getDensity(GeoPoint point) {
        AtomicInteger count = cells.get(toKey(point));
        return count == null ? 0 : count.get();
    }

    public List<GeoPoint> getHotspots(int topN) {
        List<Map.Entry<Long, AtomicInteger>> sorted = new ArrayList<>(cells.entrySet());
        sorted.sort(Comparator.comparingInt(e -> -e.getValue().get()));

        List<GeoPoint> result = new ArrayList<>(Math.min(topN, sorted.size()));
        for (int i = 0; i < Math.min(topN, sorted.size()); i++) {
            result.add(fromKey(sorted.get(i).getKey()));
        }
        return result;
    }

    public GeoPoint getMostVisited() {
        Map.Entry<Long, AtomicInteger> best = null;
        for (Map.Entry<Long, AtomicInteger> entry : cells.entrySet()) {
            if (best == null || entry.getValue().get() > best.getValue().get()) {
                best = entry;
            }
        }
        return best == null ? null : fromKey(best.getKey());
    }

    /** O(1) — maintained by {@link LongAdder} in {@link #record}. */
    public long getTotalRecordings() {
        return totalRecordings.sum();
    }

    public int getUniqueCellCount() {
        return cells.size();
    }

    public void reset() {
        cells.clear();
        totalRecordings.reset();
    }

    public String toJSON() {
        StringBuilder sb = new StringBuilder(cells.size() * 60 + 2);
        sb.append('[');
        boolean first = true;
        for (Map.Entry<Long, AtomicInteger> entry : cells.entrySet()) {
            if (!first) sb.append(',');
            GeoPoint center = fromKey(entry.getKey());
            sb.append("{\"lat\":").append(center.latitude())
              .append(",\"lon\":").append(center.longitude())
              .append(",\"count\":").append(entry.getValue().get())
              .append('}');
            first = false;
        }
        sb.append(']');
        return sb.toString();
    }

    private long toKey(GeoPoint point) {
        int latCell = (int) Math.floor(point.latitude()  / cellSizeDegrees);
        int lonCell = (int) Math.floor(point.longitude() / cellSizeDegrees);
        return ((long) latCell << 32) | (lonCell & 0xFFFFFFFFL);
    }

    private GeoPoint fromKey(long key) {
        int latCell = (int) (key >> 32);
        int lonCell = (int) (key & 0xFFFFFFFFL);
        double lat = (latCell + 0.5) * cellSizeDegrees;
        double lon = (lonCell + 0.5) * cellSizeDegrees;
        return new GeoPoint(lat, lon);
    }
}
