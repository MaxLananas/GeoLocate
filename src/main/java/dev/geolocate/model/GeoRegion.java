package dev.geolocate.model;

import dev.geolocate.mapping.GeoPoint;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public final class GeoRegion {

    private final String id;
    private final String name;
    private final List<GeoPoint> vertices;
    private GeoPoint cachedCentroid;
    private double cachedArea = -1;

    public GeoRegion(String name, List<GeoPoint> vertices) {
        if (vertices.size() < 3) {
            throw new IllegalArgumentException("A region requires at least 3 vertices.");
        }
        this.id = UUID.randomUUID().toString();
        this.name = name;
        this.vertices = new ArrayList<>(vertices);
    }

    public GeoRegion(String id, String name, List<GeoPoint> vertices) {
        if (vertices.size() < 3) {
            throw new IllegalArgumentException("A region requires at least 3 vertices.");
        }
        this.id = id;
        this.name = name;
        this.vertices = new ArrayList<>(vertices);
    }

    public boolean contains(GeoPoint point) {
        int n = vertices.size();
        boolean inside = false;
        double px = point.longitude();
        double py = point.latitude();

        for (int i = 0, j = n - 1; i < n; j = i++) {
            double xi = vertices.get(i).longitude();
            double yi = vertices.get(i).latitude();
            double xj = vertices.get(j).longitude();
            double yj = vertices.get(j).latitude();

            boolean intersect = ((yi > py) != (yj > py))
                    && (px < (xj - xi) * (py - yi) / (yj - yi) + xi);
            if (intersect) inside = !inside;
        }

        return inside;
    }

    public double getAreaSquareKm() {
        if (cachedArea >= 0) return cachedArea;
        int n = vertices.size();
        double area = 0;
        double earthRadius = 6371.0;

        for (int i = 0; i < n; i++) {
            GeoPoint a = vertices.get(i);
            GeoPoint b = vertices.get((i + 1) % n);
            double lat1 = Math.toRadians(a.latitude());
            double lat2 = Math.toRadians(b.latitude());
            double dLon = Math.toRadians(b.longitude() - a.longitude());
            area += dLon * (2 + Math.sin(lat1) + Math.sin(lat2));
        }

        cachedArea = Math.abs(area * earthRadius * earthRadius / 2.0);
        return cachedArea;
    }

    public GeoPoint getCentroid() {
        if (cachedCentroid != null) return cachedCentroid;
        double latSum = 0;
        double lonSum = 0;
        for (GeoPoint v : vertices) {
            latSum += v.latitude();
            lonSum += v.longitude();
        }
        int n = vertices.size();
        cachedCentroid = new GeoPoint(latSum / n, lonSum / n);
        return cachedCentroid;
    }

    public double distanceToBorder(GeoPoint point) {
        double minDist = Double.MAX_VALUE;
        int n = vertices.size();
        for (int i = 0; i < n; i++) {
            GeoPoint a = vertices.get(i);
            GeoPoint b = vertices.get((i + 1) % n);
            double dist = distanceToSegment(point, a, b);
            if (dist < minDist) minDist = dist;
        }
        return minDist;
    }

    private double distanceToSegment(GeoPoint p, GeoPoint a, GeoPoint b) {
        double ax = a.longitude(), ay = a.latitude();
        double bx = b.longitude(), by = b.latitude();
        double px = p.longitude(), py = p.latitude();

        double dx = bx - ax, dy = by - ay;
        double lenSq = dx * dx + dy * dy;
        double t = lenSq == 0 ? 0 : Math.max(0, Math.min(1, ((px - ax) * dx + (py - ay) * dy) / lenSq));
        double closestLon = ax + t * dx;
        double closestLat = ay + t * dy;
        return p.distanceTo(new GeoPoint(closestLat, closestLon));
    }

    public GeoPoint getBoundingBoxMin() {
        double minLat = vertices.stream().mapToDouble(GeoPoint::latitude).min().orElse(0);
        double minLon = vertices.stream().mapToDouble(GeoPoint::longitude).min().orElse(0);
        return new GeoPoint(minLat, minLon);
    }

    public GeoPoint getBoundingBoxMax() {
        double maxLat = vertices.stream().mapToDouble(GeoPoint::latitude).max().orElse(0);
        double maxLon = vertices.stream().mapToDouble(GeoPoint::longitude).max().orElse(0);
        return new GeoPoint(maxLat, maxLon);
    }

    public boolean intersects(GeoRegion other) {
        for (GeoPoint v : other.vertices) {
            if (this.contains(v)) return true;
        }
        for (GeoPoint v : this.vertices) {
            if (other.contains(v)) return true;
        }
        return false;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public List<GeoPoint> getVertices() { return Collections.unmodifiableList(vertices); }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof GeoRegion other)) return false;
        return id.equals(other.id);
    }

    @Override
    public int hashCode() { return Objects.hash(id); }

    @Override
    public String toString() {
        return "GeoRegion{id=" + id + ", name=" + name + ", vertices=" + vertices.size() + "}";
    }
}
