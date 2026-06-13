package dev.geolocate.mapping;

import java.util.Objects;

public final class GeoPoint {

    private final double latitude;
    private final double longitude;
    private final double altitude;

    public GeoPoint(double latitude, double longitude, double altitude) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.altitude = altitude;
    }

    public GeoPoint(double latitude, double longitude) {
        this(latitude, longitude, 0.0);
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public double getAltitude() {
        return altitude;
    }

    public boolean isValid() {
        return latitude >= -90.0 && latitude <= 90.0
                && longitude >= -180.0 && longitude <= 180.0;
    }

    public double distanceTo(GeoPoint other) {
        double earthRadius = 6371000.0;
        double dLat = Math.toRadians(other.latitude - this.latitude);
        double dLon = Math.toRadians(other.longitude - this.longitude);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(this.latitude))
                * Math.cos(Math.toRadians(other.latitude))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return earthRadius * c;
    }

    public String format(int decimalPlaces) {
        String fmt = "%." + decimalPlaces + "f";
        return String.format(fmt + ", " + fmt, latitude, longitude);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof GeoPoint other)) return false;
        return Double.compare(latitude, other.latitude) == 0
                && Double.compare(longitude, other.longitude) == 0
                && Double.compare(altitude, other.altitude) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(latitude, longitude, altitude);
    }

    @Override
    public String toString() {
        return "GeoPoint{lat=" + latitude + ", lon=" + longitude + ", alt=" + altitude + "}";
    }
}
