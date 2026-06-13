package dev.geolocate.border;

import dev.geolocate.config.GeoLocateConfig;
import dev.geolocate.mapping.GeoPoint;

public final class GeoBorderDetector {

    private final GeoLocateConfig.WorldConfig worldConfig;

    public GeoBorderDetector(GeoLocateConfig.WorldConfig worldConfig) {
        this.worldConfig = worldConfig;
    }

    public boolean isNearBorder(GeoPoint point, double thresholdDegrees) {
        double lat = point.latitude();
        double lon = point.longitude();
        double minLat = worldConfig.getBoundingBox().getMinLat();
        double maxLat = worldConfig.getBoundingBox().getMaxLat();
        double minLon = worldConfig.getBoundingBox().getMinLon();
        double maxLon = worldConfig.getBoundingBox().getMaxLon();

        return lat - minLat < thresholdDegrees
                || maxLat - lat < thresholdDegrees
                || lon - minLon < thresholdDegrees
                || maxLon - lon < thresholdDegrees;
    }

    public BorderSide getNearestBorderSide(GeoPoint point) {
        double lat = point.latitude();
        double lon = point.longitude();
        double minLat = worldConfig.getBoundingBox().getMinLat();
        double maxLat = worldConfig.getBoundingBox().getMaxLat();
        double minLon = worldConfig.getBoundingBox().getMinLon();
        double maxLon = worldConfig.getBoundingBox().getMaxLon();

        double distSouth = lat - minLat;
        double distNorth = maxLat - lat;
        double distWest = lon - minLon;
        double distEast = maxLon - lon;

        double min = Math.min(Math.min(distSouth, distNorth), Math.min(distWest, distEast));

        if (min == distSouth) return BorderSide.SOUTH;
        if (min == distNorth) return BorderSide.NORTH;
        if (min == distWest) return BorderSide.WEST;
        return BorderSide.EAST;
    }

    public double getDistanceToBorderMeters(GeoPoint point) {
        double lat = point.latitude();
        double lon = point.longitude();
        double minLat = worldConfig.getBoundingBox().getMinLat();
        double maxLat = worldConfig.getBoundingBox().getMaxLat();
        double minLon = worldConfig.getBoundingBox().getMinLon();
        double maxLon = worldConfig.getBoundingBox().getMaxLon();

        double distSouth = point.distanceTo(new GeoPoint(minLat, lon));
        double distNorth = point.distanceTo(new GeoPoint(maxLat, lon));
        double distWest = point.distanceTo(new GeoPoint(lat, minLon));
        double distEast = point.distanceTo(new GeoPoint(lat, maxLon));

        return Math.min(Math.min(distSouth, distNorth), Math.min(distWest, distEast));
    }

    public double getPercentageFromCenter(GeoPoint point) {
        double minLat = worldConfig.getBoundingBox().getMinLat();
        double maxLat = worldConfig.getBoundingBox().getMaxLat();
        double minLon = worldConfig.getBoundingBox().getMinLon();
        double maxLon = worldConfig.getBoundingBox().getMaxLon();

        double centerLat = (minLat + maxLat) / 2;
        double centerLon = (minLon + maxLon) / 2;
        GeoPoint center = new GeoPoint(centerLat, centerLon);
        GeoPoint corner = new GeoPoint(maxLat, maxLon);

        double distToCenter = point.distanceTo(center);
        double maxDist = center.distanceTo(corner);

        return Math.min(100.0, (distToCenter / maxDist) * 100.0);
    }

    public enum BorderSide {
        NORTH, SOUTH, EAST, WEST
    }
}
