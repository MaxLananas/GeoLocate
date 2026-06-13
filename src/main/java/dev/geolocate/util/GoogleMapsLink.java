package dev.geolocate.util;

import dev.geolocate.mapping.GeoPoint;

public final class GoogleMapsLink {

    private static final String GOOGLE_MAPS_URL = "https://www.google.com/maps/@%s,%s,%dz";
    private static final String GOOGLE_MAPS_SIMPLE = "https://www.google.com/maps?q=%s,%s";
    private static final String GOOGLE_MAPS_LABEL = "https://www.google.com/maps?q=%s,%s(%s)";
    private static final String GOOGLE_MAPS_DIRECTIONS = "https://www.google.com/maps/dir/%s,%s/%s,%s";
    private static final String OSM_URL = "https://www.openstreetmap.org/?mlat=%s&mlon=%s&zoom=%d";
    private static final String APPLE_MAPS_URL = "https://maps.apple.com/?ll=%s,%s&z=%d";

    private GoogleMapsLink() {}

    public static String build(GeoPoint point, int zoom) {
        return String.format(GOOGLE_MAPS_URL,
                point.getLatitude(),
                point.getLongitude(),
                zoom);
    }

    public static String buildSimple(GeoPoint point) {
        return String.format(GOOGLE_MAPS_SIMPLE,
                point.getLatitude(),
                point.getLongitude());
    }

    public static String buildWithLabel(GeoPoint point, String label) {
        return String.format(GOOGLE_MAPS_LABEL,
                point.getLatitude(),
                point.getLongitude(),
                label.replace(" ", "+"));
    }

    public static String buildDirections(GeoPoint from, GeoPoint to) {
        return String.format(GOOGLE_MAPS_DIRECTIONS,
                from.getLatitude(),
                from.getLongitude(),
                to.getLatitude(),
                to.getLongitude());
    }

    public static String buildOpenStreetMap(GeoPoint point, int zoom) {
        return String.format(OSM_URL,
                point.getLatitude(),
                point.getLongitude(),
                zoom);
    }

    public static String buildAppleMaps(GeoPoint point, int zoom) {
        return String.format(APPLE_MAPS_URL,
                point.getLatitude(),
                point.getLongitude(),
                zoom);
    }
}
