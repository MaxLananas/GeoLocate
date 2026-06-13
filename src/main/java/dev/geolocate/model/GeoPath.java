package dev.geolocate.model;

import dev.geolocate.mapping.GeoPoint;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class GeoPath {

    private final String name;
    private final List<GeoPoint> points;
    private double cachedTotalDistance = -1.0;

    public GeoPath(String name) {
        this.name   = name;
        this.points = new ArrayList<>();
    }

    public GeoPath(String name, List<GeoPoint> points) {
        this.name   = name;
        this.points = new ArrayList<>(points);
    }

    public void addPoint(GeoPoint point) {
        points.add(point);
        cachedTotalDistance = -1.0;
    }

    public void removePoint(int index) {
        points.remove(index);
        cachedTotalDistance = -1.0;
    }

    public double getTotalDistanceMeters() {
        if (cachedTotalDistance >= 0) return cachedTotalDistance;
        double total = 0;
        int sz = points.size();
        for (int i = 0; i < sz - 1; i++) {
            total += points.get(i).distanceTo(points.get(i + 1));
        }
        cachedTotalDistance = total;
        return total;
    }

    public GeoPoint getNearestPoint(GeoPoint target) {
        GeoPoint nearest = null;
        double minDist   = Double.MAX_VALUE;
        for (GeoPoint p : points) {
            double d = p.distanceTo(target);
            if (d < minDist) { minDist = d; nearest = p; }
        }
        return nearest;
    }

    public double getDistanceToPath(GeoPoint target) {
        double min = Double.MAX_VALUE;
        int sz = points.size();
        for (int i = 0; i < sz - 1; i++) {
            double d = distanceToSegment(target, points.get(i), points.get(i + 1));
            if (d < min) min = d;
        }
        return min;
    }

    private static double distanceToSegment(GeoPoint p, GeoPoint a, GeoPoint b) {
        double ax = a.longitude(), ay = a.latitude();
        double bx = b.longitude(), by = b.latitude();
        double px = p.longitude(), py = p.latitude();
        double dx = bx - ax, dy = by - ay;
        double lenSq = dx * dx + dy * dy;
        double t = lenSq == 0 ? 0
                : Math.max(0, Math.min(1, ((px - ax) * dx + (py - ay) * dy) / lenSq));
        return p.distanceTo(new GeoPoint(ay + t * dy, ax + t * dx));
    }

    public GeoPoint interpolate(double fractionFromStart) {
        if (points.isEmpty()) return null;
        if (fractionFromStart <= 0) return points.get(0);
        if (fractionFromStart >= 1) return points.get(points.size() - 1);

        double target      = getTotalDistanceMeters() * fractionFromStart;
        double accumulated = 0;
        int sz = points.size();

        for (int i = 0; i < sz - 1; i++) {
            GeoPoint a = points.get(i);
            GeoPoint b = points.get(i + 1);
            double segDist = a.distanceTo(b);
            if (accumulated + segDist >= target) {
                double t   = (target - accumulated) / segDist;
                double lat = a.latitude()  + t * (b.latitude()  - a.latitude());
                double lon = a.longitude() + t * (b.longitude() - a.longitude());
                return new GeoPoint(lat, lon);
            }
            accumulated += segDist;
        }
        return points.get(sz - 1);
    }

    public GeoPoint getBoundingBoxCenter() {
        if (points.isEmpty()) return null;
        double minLat = Double.MAX_VALUE, maxLat = -Double.MAX_VALUE;
        double minLon = Double.MAX_VALUE, maxLon = -Double.MAX_VALUE;
        for (GeoPoint p : points) {
            if (p.latitude()  < minLat) minLat = p.latitude();
            if (p.latitude()  > maxLat) maxLat = p.latitude();
            if (p.longitude() < minLon) minLon = p.longitude();
            if (p.longitude() > maxLon) maxLon = p.longitude();
        }
        return new GeoPoint((minLat + maxLat) * 0.5, (minLon + maxLon) * 0.5);
    }

    public boolean  isEmpty()   { return points.isEmpty(); }
    public int      size()      { return points.size(); }
    public String   getName()   { return name; }
    public List<GeoPoint> getPoints() { return Collections.unmodifiableList(points); }
    public GeoPoint getFirst()  { return points.isEmpty() ? null : points.get(0); }
    public GeoPoint getLast()   { return points.isEmpty() ? null : points.get(points.size() - 1); }
}
