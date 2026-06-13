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

    // Lazily computed and cached
    private GeoPoint cachedCentroid;
    private double   cachedArea = -1;

    public GeoRegion(String name, List<GeoPoint> vertices) {
        validate(vertices);
        this.id       = UUID.randomUUID().toString();
        this.name     = name;
        this.vertices = new ArrayList<>(vertices);
    }

    public GeoRegion(String id, String name, List<GeoPoint> vertices) {
        validate(vertices);
        this.id       = id;
        this.name     = name;
        this.vertices = new ArrayList<>(vertices);
    }

    private static void validate(List<GeoPoint> vertices) {
        if (vertices == null || vertices.size() < 3) {
            throw new IllegalArgumentException("A region requires at least 3 vertices.");
        }
    }

    /** Ray-casting point-in-polygon test. */
    public boolean contains(GeoPoint point) {
        int n = vertices.size();
        boolean inside = false;
        double px = point.longitude();
        double py = point.latitude();

        for (int i = 0, j = n - 1; i < n; j = i++) {
            double xi = vertices.get(i).longitude(), yi = vertices.get(i).latitude();
            double xj = vertices.get(j).longitude(), yj = vertices.get(j).latitude();
            if (((yi > py) != (yj > py)) && (px < (xj - xi) * (py - yi) / (yj - yi) + xi)) {
                inside = !inside;
            }
        }
        return inside;
    }

    public double getAreaSquareKm() {
        if (cachedArea >= 0) return cachedArea;
        int n = vertices.size();
        double area = 0;
        final double R = 6371.0;
        for (int i = 0; i < n; i++) {
            GeoPoint a = vertices.get(i);
            GeoPoint b = vertices.get((i + 1) % n);
            double lat1 = Math.toRadians(a.latitude());
            double lat2 = Math.toRadians(b.latitude());
            double dLon = Math.toRadians(b.longitude() - a.longitude());
            area += dLon * (2.0 + Math.sin(lat1) + Math.sin(lat2));
        }
        cachedArea = Math.abs(area * R * R / 2.0);
        return cachedArea;
    }

    public GeoPoint getCentroid() {
        if (cachedCentroid != null) return cachedCentroid;
        double latSum = 0, lonSum = 0;
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
            double d = distanceToSegment(point, vertices.get(i), vertices.get((i + 1) % n));
            if (d < minDist) minDist = d;
        }
        return minDist;
    }

    private static double distanceToSegment(GeoPoint p, GeoPoint a, GeoPoint b) {
        double ax = a.longitude(), ay = a.latitude();
        double bx = b.longitude(), by = b.latitude();
        double px = p.longitude(), py = p.latitude();
        double dx = bx - ax, dy = by - ay;
        double lenSq = dx * dx + dy * dy;
        double t = lenSq == 0 ? 0 : Math.max(0, Math.min(1, ((px - ax) * dx + (py - ay) * dy) / lenSq));
        return p.distanceTo(new GeoPoint(ay + t * dy, ax + t * dx));
    }

    public GeoPoint getBoundingBoxMin() {
        double minLat = Double.MAX_VALUE, minLon = Double.MAX_VALUE;
        for (GeoPoint v : vertices) {
            if (v.latitude()  < minLat) minLat = v.latitude();
            if (v.longitude() < minLon) minLon = v.longitude();
        }
        return new GeoPoint(minLat, minLon);
    }

    public GeoPoint getBoundingBoxMax() {
        double maxLat = -Double.MAX_VALUE, maxLon = -Double.MAX_VALUE;
        for (GeoPoint v : vertices) {
            if (v.latitude()  > maxLat) maxLat = v.latitude();
            if (v.longitude() > maxLon) maxLon = v.longitude();
        }
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

    public String           getId()       { return id; }
    public String           getName()     { return name; }
    public List<GeoPoint>   getVertices() { return Collections.unmodifiableList(vertices); }

    @Override public boolean equals(Object obj) {
        return obj instanceof GeoRegion other && id.equals(other.id);
    }
    @Override public int    hashCode()   { return Objects.hash(id); }
    @Override public String toString()   {
        return "GeoRegion{id=" + id + ", name=" + name + ", vertices=" + vertices.size() + "}";
    }
}
