package dev.geolocate.heatmap;

import dev.geolocate.mapping.GeoPoint;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public final class GeoHeatmap {

    private final double cellSizeDegrees;
    private final Map<Long, AtomicInteger> cells;

    public GeoHeatmap(double cellSizeDegrees) {
        this.cellSizeDegrees = cellSizeDegrees;
        this.cells = new HashMap<>();
    }

    public void record(GeoPoint point) {
        long key = toKey(point);
        cells.computeIfAbsent(key, k -> new AtomicInteger(0)).incrementAndGet();
    }

    public int getDensity(GeoPoint point) {
        AtomicInteger count = cells.get(toKey(point));
        return count == null ? 0 : count.get();
    }

    public List<GeoPoint> getHotspots(int topN) {
        List<Map.Entry<Long, AtomicInteger>> sorted = new ArrayList<>(cells.entrySet());
        sorted.sort(Comparator.comparingInt(e -> -e.getValue().get()));

        List<GeoPoint> result = new ArrayList<>();
        for (int i = 0; i < Math.min(topN, sorted.size()); i++) {
            result.add(fromKey(sorted.get(i).getKey()));
        }
        return result;
    }

    public GeoPoint getMostVisited() {
        return cells.entrySet().stream()
                .max(Comparator.comparingInt(e -> e.getValue().get()))
                .map(e -> fromKey(e.getKey()))
                .orElse(null);
    }

    public int getTotalRecordings() {
        return cells.values().stream().mapToInt(AtomicInteger::get).sum();
    }

    public int getUniqueCellCount() {
        return cells.size();
    }

    public void reset() {
        cells.clear();
    }

    public String toJSON() {
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        boolean first = true;
        for (Map.Entry<Long, AtomicInteger> entry : cells.entrySet()) {
            if (!first) sb.append(",");
            GeoPoint center = fromKey(entry.getKey());
            sb.append(String.format("{\"lat\":%s,\"lon\":%s,\"count\":%d}",
                    center.latitude(), center.longitude(), entry.getValue().get()));
            first = false;
        }
        sb.append("]");
        return sb.toString();
    }

    private long toKey(GeoPoint point) {
        int latCell = (int) Math.floor(point.latitude() / cellSizeDegrees);
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
