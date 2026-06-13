package dev.geolocate.border;

import dev.geolocate.config.GeoLocateConfig;
import dev.geolocate.mapping.GeoPoint;
import dev.geolocate.mapping.WorldBoundingBox;

public final class GeoBorderDetector {

    // Cached bounding box values — avoids repeated virtual dispatch
    private final double minLat;
    private final double maxLat;
    private final double minLon;
    private final double maxLon;
    private final GeoPoint center;
    private final double maxDistFromCenter;

    public GeoBorderDetector(GeoLocateConfig.WorldConfig worldConfig) {
        WorldBoundingBox box = worldConfig.getBoundingBox();
        this.minLat = box.getMinLat();
        this.maxLat = box.getMaxLat();
        this.minLon = box.getMinLon();
        this.maxLon = box.getMaxLon();

        double centerLat = (minLat + maxLat) / 2.0;
        double centerLon = (minLon + maxLon) / 2.0;
        this.center = new GeoPoint(centerLat, centerLon);
        this.maxDistFromCenter = center.distanceTo(new GeoPoint(maxLat, maxLon));
    }

    public boolean isNearBorder(GeoPoint point, double thresholdDegrees) {
        double lat = point.latitude();
        double lon = point.longitude();
        return lat - minLat < thresholdDegrees
                || maxLat - lat < thresholdDegrees
                || lon - minLon < thresholdDegrees
                || maxLon - lon < thresholdDegrees;
    }

    public BorderSide getNearestBorderSide(GeoPoint point) {
        double lat = point.latitude();
        double lon = point.longitude();

        double distSouth = lat - minLat;
        double distNorth = maxLat - lat;
        double distWest  = lon - minLon;
        double distEast  = maxLon - lon;

        double min = Math.min(Math.min(distSouth, distNorth), Math.min(distWest, distEast));

        if (min == distSouth) return BorderSide.SOUTH;
        if (min == distNorth) return BorderSide.NORTH;
        if (min == distWest)  return BorderSide.WEST;
        return BorderSide.EAST;
    }

    public double getDistanceToBorderMeters(GeoPoint point) {
        double lat = point.latitude();
        double lon = point.longitude();

        double distSouth = point.distanceTo(new GeoPoint(minLat, lon));
        double distNorth = point.distanceTo(new GeoPoint(maxLat, lon));
        double distWest  = point.distanceTo(new GeoPoint(lat, minLon));
        double distEast  = point.distanceTo(new GeoPoint(lat, maxLon));

        return Math.min(Math.min(distSouth, distNorth), Math.min(distWest, distEast));
    }

    public double getPercentageFromCenter(GeoPoint point) {
        if (maxDistFromCenter == 0) return 0;
        double distToCenter = point.distanceTo(center);
        return Math.min(100.0, (distToCenter / maxDistFromCenter) * 100.0);
    }

    public enum BorderSide {
        NORTH, SOUTH, EAST, WEST
    }
}
