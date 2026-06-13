package dev.geolocate.mapping;

public record GeoPoint(double latitude, double longitude, double altitude) {

    private static final double EARTH_RADIUS_METERS = 6371000.0;
    private static final double EARTH_RADIUS_KM = 6371.0;

    public GeoPoint(double latitude, double longitude) {
        this(latitude, longitude, 0.0);
    }

    public boolean isValid() {
        return latitude >= -90.0 && latitude <= 90.0
                && longitude >= -180.0 && longitude <= 180.0;
    }

    public double distanceTo(GeoPoint other) {
        double dLat = Math.toRadians(other.latitude - this.latitude);
        double dLon = Math.toRadians(other.longitude - this.longitude);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(this.latitude))
                * Math.cos(Math.toRadians(other.latitude))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return EARTH_RADIUS_METERS * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }

    public double distanceToKm(GeoPoint other) {
        return distanceTo(other) / 1000.0;
    }

    public double bearingTo(GeoPoint other) {
        double lat1 = Math.toRadians(this.latitude);
        double lat2 = Math.toRadians(other.latitude);
        double dLon = Math.toRadians(other.longitude - this.longitude);
        double x = Math.sin(dLon) * Math.cos(lat2);
        double y = Math.cos(lat1) * Math.sin(lat2)
                - Math.sin(lat1) * Math.cos(lat2) * Math.cos(dLon);
        double bearing = Math.toDegrees(Math.atan2(x, y));
        return (bearing + 360) % 360;
    }

    public String bearingCardinal(GeoPoint other) {
        double bearing = bearingTo(other);
        String[] directions = {"N", "NE", "E", "SE", "S", "SW", "W", "NW", "N"};
        return directions[(int) Math.round(bearing / 45) % 8];
    }

    public GeoPoint midpointTo(GeoPoint other) {
        double lat1 = Math.toRadians(this.latitude);
        double lon1 = Math.toRadians(this.longitude);
        double lat2 = Math.toRadians(other.latitude);
        double dLon = Math.toRadians(other.longitude - this.longitude);

        double bx = Math.cos(lat2) * Math.cos(dLon);
        double by = Math.cos(lat2) * Math.sin(dLon);

        double lat3 = Math.atan2(
                Math.sin(lat1) + Math.sin(lat2),
                Math.sqrt((Math.cos(lat1) + bx) * (Math.cos(lat1) + bx) + by * by)
        );
        double lon3 = lon1 + Math.atan2(by, Math.cos(lat1) + bx);

        return new GeoPoint(Math.toDegrees(lat3), Math.toDegrees(lon3));
    }

    public GeoPoint destinationPoint(double distanceMeters, double bearingDegrees) {
        double angularDist = distanceMeters / EARTH_RADIUS_METERS;
        double bearingRad = Math.toRadians(bearingDegrees);
        double lat1 = Math.toRadians(this.latitude);
        double lon1 = Math.toRadians(this.longitude);

        double lat2 = Math.asin(
                Math.sin(lat1) * Math.cos(angularDist)
                        + Math.cos(lat1) * Math.sin(angularDist) * Math.cos(bearingRad)
        );
        double lon2 = lon1 + Math.atan2(
                Math.sin(bearingRad) * Math.sin(angularDist) * Math.cos(lat1),
                Math.cos(angularDist) - Math.sin(lat1) * Math.sin(lat2)
        );

        return new GeoPoint(Math.toDegrees(lat2), Math.toDegrees(lon2));
    }

    public double rhumbDistanceTo(GeoPoint other) {
        double lat1 = Math.toRadians(this.latitude);
        double lat2 = Math.toRadians(other.latitude);
        double dLat = lat2 - lat1;
        double dLon = Math.abs(Math.toRadians(other.longitude - this.longitude));
        if (dLon > Math.PI) dLon = 2 * Math.PI - dLon;

        double dPhi = Math.log(Math.tan(lat2 / 2 + Math.PI / 4) / Math.tan(lat1 / 2 + Math.PI / 4));
        double q = Math.abs(dPhi) > 1e-10 ? dLat / dPhi : Math.cos(lat1);
        return Math.sqrt(dLat * dLat + q * q * dLon * dLon) * EARTH_RADIUS_METERS;
    }

    public double rhumbBearingTo(GeoPoint other) {
        double lat1 = Math.toRadians(this.latitude);
        double lat2 = Math.toRadians(other.latitude);
        double dLon = Math.toRadians(other.longitude - this.longitude);
        if (dLon > Math.PI) dLon -= 2 * Math.PI;
        if (dLon < -Math.PI) dLon += 2 * Math.PI;
        double dPhi = Math.log(Math.tan(lat2 / 2 + Math.PI / 4) / Math.tan(lat1 / 2 + Math.PI / 4));
        double bearing = Math.toDegrees(Math.atan2(dLon, dPhi));
        return (bearing + 360) % 360;
    }

    public boolean isWithinRadius(GeoPoint center, double radiusMeters) {
        return distanceTo(center) <= radiusMeters;
    }

    public GeoPoint withAltitude(double newAltitude) {
        return new GeoPoint(this.latitude, this.longitude, newAltitude);
    }

    public GeoPoint withLatitude(double newLatitude) {
        return new GeoPoint(newLatitude, this.longitude, this.altitude);
    }

    public GeoPoint withLongitude(double newLongitude) {
        return new GeoPoint(this.latitude, newLongitude, this.altitude);
    }

    public String format(int decimalPlaces) {
        String fmt = "%." + decimalPlaces + "f";
        return String.format(fmt + ", " + fmt, latitude, longitude);
    }

    public String formatDMS() {
        return toDMS(latitude, true) + " " + toDMS(longitude, false);
    }

    private String toDMS(double decimal, boolean isLat) {
        String direction = isLat ? (decimal >= 0 ? "N" : "S") : (decimal >= 0 ? "E" : "W");
        decimal = Math.abs(decimal);
        int degrees = (int) decimal;
        double minutesDouble = (decimal - degrees) * 60;
        int minutes = (int) minutesDouble;
        double seconds = (minutesDouble - minutes) * 60;
        return String.format("%d°%d'%.2f\"%s", degrees, minutes, seconds, direction);
    }

    public double[] toRadians() {
        return new double[]{Math.toRadians(latitude), Math.toRadians(longitude)};
    }

    public double getLatitude() { return latitude; }
    public double getLongitude() { return longitude; }
    public double getAltitude() { return altitude; }
}
