package dev.geolocate.util;

import dev.geolocate.mapping.GeoPoint;

import java.util.Locale;

public final class GoogleMapsLink {

    // Use Locale.ROOT so decimal separator is always '.' regardless of server locale
    private static final String GOOGLE_MAPS_URL       = "https://www.google.com/maps/@%.6f,%.6f,%dz";
    private static final String GOOGLE_MAPS_SIMPLE    = "https://www.google.com/maps?q=%.6f,%.6f";
    private static final String GOOGLE_MAPS_LABEL     = "https://www.google.com/maps?q=%.6f,%.6f(%s)";
    private static final String GOOGLE_MAPS_DIRECTIONS= "https://www.google.com/maps/dir/%.6f,%.6f/%.6f,%.6f";
    private static final String OSM_URL               = "https://www.openstreetmap.org/?mlat=%.6f&mlon=%.6f&zoom=%d";
    private static final String APPLE_MAPS_URL        = "https://maps.apple.com/?ll=%.6f,%.6f&z=%d";

    private GoogleMapsLink() {}

    public static String build(GeoPoint point, int zoom) {
        return String.format(Locale.ROOT, GOOGLE_MAPS_URL,
                point.latitude(), point.longitude(), zoom);
    }

    public static String buildSimple(GeoPoint point) {
        return String.format(Locale.ROOT, GOOGLE_MAPS_SIMPLE,
                point.latitude(), point.longitude());
    }

    public static String buildWithLabel(GeoPoint point, String label) {
        return String.format(Locale.ROOT, GOOGLE_MAPS_LABEL,
                point.latitude(), point.longitude(),
                label.replace(" ", "+"));
    }

    public static String buildDirections(GeoPoint from, GeoPoint to) {
        return String.format(Locale.ROOT, GOOGLE_MAPS_DIRECTIONS,
                from.latitude(), from.longitude(),
                to.latitude(),   to.longitude());
    }

    public static String buildOpenStreetMap(GeoPoint point, int zoom) {
        return String.format(Locale.ROOT, OSM_URL,
                point.latitude(), point.longitude(), zoom);
    }

    public static String buildAppleMaps(GeoPoint point, int zoom) {
        return String.format(Locale.ROOT, APPLE_MAPS_URL,
                point.latitude(), point.longitude(), zoom);
    }
}
