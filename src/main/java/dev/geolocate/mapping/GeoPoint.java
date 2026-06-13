package dev.geolocate.mapping;

public record GeoPoint(double latitude, double longitude, double altitude) {

    private static final double EARTH_RADIUS_METERS = 6_371_000.0;

    public GeoPoint(double latitude, double longitude) {
        this(latitude, longitude, 0.0);
    }

    public boolean isValid() {
        return latitude >= -90.0 && latitude <= 90.0
                && longitude >= -180.0 && longitude <= 180.0;
    }

    public double distanceTo(GeoPoint other) {
        double lat1 = Math.toRadians(this.latitude);
        double lat2 = Math.toRadians(other.latitude);
        double dLat = lat2 - lat1;
        double dLon = Math.toRadians(other.longitude - this.longitude);

        double sinDLat = Math.sin(dLat * 0.5);
        double sinDLon = Math.sin(dLon * 0.5);
        double a = sinDLat * sinDLat
                 + Math.cos(lat1) * Math.cos(lat2) * sinDLon * sinDLon;
        return EARTH_RADIUS_METERS * 2.0 * Math.atan2(Math.sqrt(a), Math.sqrt(1.0 - a));
    }

    public double distanceToKm(GeoPoint other) {
        return distanceTo(other) / 1000.0;
    }

    public double bearingTo(GeoPoint other) {
        double lat1 = Math.toRadians(this.latitude);
        double lat2 = Math.toRadians(other.latitude);
        double dLon = Math.toRadians(other.longitude - this.longitude);
        double cosLat2 = Math.cos(lat2);
        double x = Math.sin(dLon) * cosLat2;
        double y = Math.cos(lat1) * Math.sin(lat2)
                 - Math.sin(lat1) * cosLat2 * Math.cos(dLon);
        return (Math.toDegrees(Math.atan2(x, y)) + 360.0) % 360.0;
    }

    public String bearingCardinal(GeoPoint other) {
        double bearing = bearingTo(other);
        String[] dirs = {"N", "NE", "E", "SE", "S", "SW", "W", "NW"};
        return dirs[(int) Math.round(bearing / 45.0) % 8];
    }

    public GeoPoint midpointTo(GeoPoint other) {
        double lat1 = Math.toRadians(this.latitude);
        double lon1 = Math.toRadians(this.longitude);
        double lat2 = Math.toRadians(other.latitude);
        double dLon = Math.toRadians(other.longitude - this.longitude);

        double bx  = Math.cos(lat2) * Math.cos(dLon);
        double by  = Math.cos(lat2) * Math.sin(dLon);
        double cLat1PlusBx = Math.cos(lat1) + bx;

        double lat3 = Math.atan2(
                Math.sin(lat1) + Math.sin(lat2),
                Math.sqrt(cLat1PlusBx * cLat1PlusBx + by * by));
        double lon3 = lon1 + Math.atan2(by, cLat1PlusBx);

        return new GeoPoint(Math.toDegrees(lat3), Math.toDegrees(lon3));
    }

    public GeoPoint destinationPoint(double distanceMeters, double bearingDegrees) {
        double angDist  = distanceMeters / EARTH_RADIUS_METERS;
        double bearRad  = Math.toRadians(bearingDegrees);
        double lat1     = Math.toRadians(this.latitude);
        double lon1     = Math.toRadians(this.longitude);
        double cosAngDist = Math.cos(angDist);
        double sinAngDist = Math.sin(angDist);
        double cosLat1    = Math.cos(lat1);
        double sinLat1    = Math.sin(lat1);

        double lat2 = Math.asin(sinLat1 * cosAngDist + cosLat1 * sinAngDist * Math.cos(bearRad));
        double lon2 = lon1 + Math.atan2(
                Math.sin(bearRad) * sinAngDist * cosLat1,
                cosAngDist - sinLat1 * Math.sin(lat2));

        return new GeoPoint(Math.toDegrees(lat2), Math.toDegrees(lon2));
    }

    public double rhumbDistanceTo(GeoPoint other) {
        double lat1 = Math.toRadians(this.latitude);
        double lat2 = Math.toRadians(other.latitude);
        double dLat = lat2 - lat1;
        double dLon = Math.abs(Math.toRadians(other.longitude - this.longitude));
        if (dLon > Math.PI) dLon = 2.0 * Math.PI - dLon;

        double dPhi = Math.log(
                Math.tan(lat2 * 0.5 + Math.PI * 0.25) /
                Math.tan(lat1 * 0.5 + Math.PI * 0.25));
        double q = Math.abs(dPhi) > 1e-10 ? dLat / dPhi : Math.cos(lat1);
        return Math.sqrt(dLat * dLat + q * q * dLon * dLon) * EARTH_RADIUS_METERS;
    }

    public double rhumbBearingTo(GeoPoint other) {
        double lat1 = Math.toRadians(this.latitude);
        double lat2 = Math.toRadians(other.latitude);
        double dLon = Math.toRadians(other.longitude - this.longitude);
        if (dLon >  Math.PI) dLon -= 2.0 * Math.PI;
        if (dLon < -Math.PI) dLon += 2.0 * Math.PI;
        double dPhi = Math.log(
                Math.tan(lat2 * 0.5 + Math.PI * 0.25) /
                Math.tan(lat1 * 0.5 + Math.PI * 0.25));
        return (Math.toDegrees(Math.atan2(dLon, dPhi)) + 360.0) % 360.0;
    }

    public boolean isWithinRadius(GeoPoint center, double radiusMeters) {
        return distanceTo(center) <= radiusMeters;
    }

    public GeoPoint withAltitude(double newAltitude)   { return new GeoPoint(latitude, longitude, newAltitude); }
    public GeoPoint withLatitude(double newLatitude)   { return new GeoPoint(newLatitude, longitude, altitude); }
    public GeoPoint withLongitude(double newLongitude) { return new GeoPoint(latitude, newLongitude, altitude); }

    public String format(int decimalPlaces) {
        String fmt = "%." + decimalPlaces + "f";
        return String.format(fmt + ", " + fmt, latitude, longitude);
    }

    public String formatDMS() {
        return toDMS(latitude, true) + " " + toDMS(longitude, false);
    }

    private static String toDMS(double decimal, boolean isLat) {
        String direction = isLat
                ? (decimal >= 0 ? "N" : "S")
                : (decimal >= 0 ? "E" : "W");
        decimal = Math.abs(decimal);
        int    degrees       = (int) decimal;
        double minutesDouble = (decimal - degrees) * 60.0;
        int    minutes       = (int) minutesDouble;
        double seconds       = (minutesDouble - minutes) * 60.0;
        return String.format("%d°%d'%.2f\"%s", degrees, minutes, seconds, direction);
    }

    public double[] toRadians() {
        return new double[]{Math.toRadians(latitude), Math.toRadians(longitude)};
    }

    // Convenience aliases kept for compatibility
    public double getLatitude()  { return latitude; }
    public double getLongitude() { return longitude; }
    public double getAltitude()  { return altitude; }
}
