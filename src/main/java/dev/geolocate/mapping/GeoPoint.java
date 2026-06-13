package dev.geolocate.mapping;

public record GeoPoint(double latitude, double longitude, double altitude) {

    public GeoPoint(double latitude, double longitude) {
        this(latitude, longitude, 0.0);
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
        return earthRadius * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }

    public String format(int decimalPlaces) {
        String fmt = "%." + decimalPlaces + "f";
        return String.format(fmt + ", " + fmt, latitude, longitude);
    }

    public double getLatitude() { return latitude; }
    public double getLongitude() { return longitude; }
    public double getAltitude() { return altitude; }
}
