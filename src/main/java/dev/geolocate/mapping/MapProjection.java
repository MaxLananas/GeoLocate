package dev.geolocate.mapping;

public enum MapProjection {

    LINEAR {
        @Override
        public GeoPoint toGeoPoint(double x, double z, WorldBoundingBox box) {
            double xNorm = (x - box.getOffsetX() - box.getMinX()) / box.getXRange();
            double zNorm = (z - box.getOffsetZ() - box.getMinZ()) / box.getZRange();
            double lon = box.getMinLon() + xNorm * box.getLonRange();
            double lat = box.getMaxLat() - zNorm * box.getLatRange();
            return new GeoPoint(clampLat(lat), clampLon(lon));
        }

        @Override
        public double[] toMinecraft(double lat, double lon, WorldBoundingBox box) {
            double xNorm = (lon - box.getMinLon()) / box.getLonRange();
            double zNorm = (box.getMaxLat() - lat) / box.getLatRange();
            double x = box.getMinX() + xNorm * box.getXRange() + box.getOffsetX();
            double z = box.getMinZ() + zNorm * box.getZRange() + box.getOffsetZ();
            return new double[]{x, z};
        }
    },

    MERCATOR {
        @Override
        public GeoPoint toGeoPoint(double x, double z, WorldBoundingBox box) {
            double xNorm = (x - box.getOffsetX() - box.getMinX()) / box.getXRange();
            double zNorm = (z - box.getOffsetZ() - box.getMinZ()) / box.getZRange();

            double lon = box.getMinLon() + xNorm * box.getLonRange();

            double minMerc = mercY(Math.toRadians(box.getMinLat()));
            double maxMerc = mercY(Math.toRadians(box.getMaxLat()));
            double mercVal = maxMerc - zNorm * (maxMerc - minMerc);
            double lat = Math.toDegrees(2.0 * Math.atan(Math.exp(mercVal)) - HALF_PI);

            return new GeoPoint(clampLat(lat), clampLon(lon));
        }

        @Override
        public double[] toMinecraft(double lat, double lon, WorldBoundingBox box) {
            double xNorm = (lon - box.getMinLon()) / box.getLonRange();

            double minMerc = mercY(Math.toRadians(box.getMinLat()));
            double maxMerc = mercY(Math.toRadians(box.getMaxLat()));
            double mercRange = maxMerc - minMerc;
            double zNorm = (maxMerc - mercY(Math.toRadians(lat))) / mercRange;

            double x = box.getMinX() + xNorm * box.getXRange() + box.getOffsetX();
            double z = box.getMinZ() + zNorm * box.getZRange() + box.getOffsetZ();
            return new double[]{x, z};
        }

        private double mercY(double latRad) {
            return Math.log(Math.tan(QUARTER_PI + latRad / 2.0));
        }
    };

    private static final double HALF_PI = Math.PI / 2.0;
    private static final double QUARTER_PI = Math.PI / 4.0;

    public abstract GeoPoint toGeoPoint(double x, double z, WorldBoundingBox box);
    public abstract double[] toMinecraft(double lat, double lon, WorldBoundingBox box);

    protected double clampLat(double lat) {
        return Math.max(-90.0, Math.min(90.0, lat));
    }

    protected double clampLon(double lon) {
        double r = lon % 360.0;
        if (r > 180.0) r -= 360.0;
        if (r < -180.0) r += 360.0;
        return r;
    }

    public static MapProjection fromString(String name) {
        try {
            return valueOf(name.toUpperCase());
        } catch (IllegalArgumentException e) {
            return LINEAR;
        }
    }
}
