package dev.geolocate.history;

import dev.geolocate.mapping.GeoPoint;
import dev.geolocate.model.GeoPath;
import dev.geolocate.model.GeoSnapshot;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public final class PlayerGeoHistory {

    private final UUID playerUuid;
    private final int maxEntries;
    private final Deque<GeoSnapshot> snapshots;

    public PlayerGeoHistory(UUID playerUuid, int maxEntries) {
        this.playerUuid = playerUuid;
        this.maxEntries = maxEntries;
        this.snapshots = new ArrayDeque<>();
    }

    public void record(GeoSnapshot snapshot) {
        if (snapshots.size() >= maxEntries) {
            snapshots.pollFirst();
        }
        snapshots.addLast(snapshot);
    }

    public Optional<GeoSnapshot> getLast() {
        return Optional.ofNullable(snapshots.peekLast());
    }

    public Optional<GeoSnapshot> getFirst() {
        return Optional.ofNullable(snapshots.peekFirst());
    }

    public List<GeoSnapshot> getAll() {
        return Collections.unmodifiableList(new ArrayList<>(snapshots));
    }

    public GeoPath toPath() {
        GeoPath path = new GeoPath("history-" + playerUuid);
        for (GeoSnapshot s : snapshots) {
            path.addPoint(s.getPoint());
        }
        return path;
    }

    public double getTotalDistanceTraveled() {
        return toPath().getTotalDistanceMeters();
    }

    public Optional<GeoPoint> getFarthestPointFrom(GeoPoint reference) {
        return snapshots.stream()
                .map(GeoSnapshot::getPoint)
                .max((a, b) -> Double.compare(
                        a.distanceTo(reference),
                        b.distanceTo(reference)
                ));
    }

    public List<GeoSnapshot> getSnapshotsInRegion(double minLat, double maxLat, double minLon, double maxLon) {
        List<GeoSnapshot> result = new ArrayList<>();
        for (GeoSnapshot s : snapshots) {
            GeoPoint p = s.getPoint();
            if (p.latitude() >= minLat && p.latitude() <= maxLat
                    && p.longitude() >= minLon && p.longitude() <= maxLon) {
                result.add(s);
            }
        }
        return result;
    }

    public String toCSV() {
        StringBuilder sb = new StringBuilder();
        sb.append("uuid,name,lat,lon,alt,world,x,y,z,timestamp\n");
        for (GeoSnapshot s : snapshots) {
            sb.append(s.toCSVLine()).append("\n");
        }
        return sb.toString();
    }

    public String toGeoJSON() {
        StringBuilder sb = new StringBuilder();
        sb.append("{\"type\":\"FeatureCollection\",\"features\":[");
        boolean first = true;
        for (GeoSnapshot s : snapshots) {
            if (!first) sb.append(",");
            sb.append("{\"type\":\"Feature\",\"geometry\":{\"type\":\"Point\",\"coordinates\":[");
            sb.append(s.getPoint().longitude()).append(",").append(s.getPoint().latitude());
            sb.append("]},\"properties\":{\"player\":\"").append(s.getPlayerName()).append("\"");
            sb.append(",\"timestamp\":\"").append(s.getTimestamp()).append("\"}}");
            first = false;
        }
        sb.append("]}");
        return sb.toString();
    }

    public void clear() {
        snapshots.clear();
    }

    public int size() { return snapshots.size(); }
    public UUID getPlayerUuid() { return playerUuid; }
    public int getMaxEntries() { return maxEntries; }
}
