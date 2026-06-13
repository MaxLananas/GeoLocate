package dev.geolocate.util;

import dev.geolocate.mapping.GeoPoint;

public final class GoogleMapsLink {

    private static final String BASE_URL = "https://www.google.com/maps?q=";
    private static final String DETAILED_URL = "https://www.google.com/maps/@%s,%s,%dz";

    private GoogleMapsLink() {}

    public static String build(GeoPoint point, int zoom) {
        String lat = String.valueOf(point.getLatitude());
        String lon = String.valueOf(point.getLongitude());
        return String.format(DETAILED_URL, lat, lon, zoom);
    }

    public static String buildSimple(GeoPoint point) {
        return BASE_URL + point.getLatitude() + "," + point.getLongitude();
    }

    public static String buildWithLabel(GeoPoint point, String label) {
        return BASE_URL + point.getLatitude() + "," + point.getLongitude()
                + "(" + label.replace(" ", "+") + ")";
    }

    public static String buildDirections(GeoPoint from, GeoPoint to) {
        return "https://www.google.com/maps/dir/"
                + from.getLatitude() + "," + from.getLongitude()
                + "/"
                + to.getLatitude() + "," + to.getLongitude();
    }
}
