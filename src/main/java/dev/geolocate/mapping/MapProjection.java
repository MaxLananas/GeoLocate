package dev.geolocate.mapping;

public enum MapProjection {

    LINEAR {
        @Override
        public GeoPoint toGeoPoint(double x, double z, WorldBoundingBox box) {
            double adjustedX = x - box.getOffsetX();
            double adjustedZ = z - box.getOffsetZ();

            double xNorm = (adjustedX - box.getMinX()) / box.getXRange();
            double zNorm = (adjustedZ - box.getMinZ()) / box.getZRange();

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
            double adjustedX = x - box.getOffsetX();
            double adjustedZ = z - box.getOffsetZ();

            double xNorm = (adjustedX - box.getMinX()) / box.getXRange();
            double zNorm = (adjustedZ - box.getMinZ()) / box.getZRange();

            double lon = box.getMinLon() + xNorm * box.getLonRange();

            double minLatRad = Math.toRadians(box.getMinLat());
            double maxLatRad = Math.toRadians(box.getMaxLat());

            double minMerc = mercatorY(minLatRad);
            double maxMerc = mercatorY(maxLatRad);
            double mercRange = maxMerc - minMerc;

            double mercY = maxMerc - zNorm * mercRange;
            double latRad = 2.0 * Math.atan(Math.exp(mercY)) - Math.PI / 2.0;
            double lat = Math.toDegrees(latRad);

            return new GeoPoint(clampLat(lat), clampLon(lon));
        }

        @Override
        public double[] toMinecraft(double lat, double lon, WorldBoundingBox box) {
            double xNorm = (lon - box.getMinLon()) / box.getLonRange();

            double minLatRad = Math.toRadians(box.getMinLat());
            double maxLatRad = Math.toRadians(box.getMaxLat());

            double minMerc = mercatorY(minLatRad);
            double maxMerc = mercatorY(maxLatRad);
            double mercRange = maxMerc - minMerc;

            double latRad = Math.toRadians(lat);
            double mercY = mercatorY(latRad);

            double zNorm = (maxMerc - mercY) / mercRange;

            double x = box.getMinX() + xNorm * box.getXRange() + box.getOffsetX();
            double z = box.getMinZ() + zNorm * box.getZRange() + box.getOffsetZ();

            return new double[]{x, z};
        }

        private double mercatorY(double latRad) {
            return Math.log(Math.tan(Math.PI / 4.0 + latRad / 2.0));
        }
    };

    public abstract GeoPoint toGeoPoint(double x, double z, WorldBoundingBox box);

    public abstract double[] toMinecraft(double lat, double lon, WorldBoundingBox box);

    protected double clampLat(double lat) {
        return Math.max(-90.0, Math.min(90.0, lat));
    }

    protected double clampLon(double lon) {
        double result = lon % 360.0;
        if (result > 180.0) result -= 360.0;
        if (result < -180.0) result += 360.0;
        return result;
    }

    public static MapProjection fromString(String name) {
        try {
            return valueOf(name.toUpperCase());
        } catch (IllegalArgumentException e) {
            return LINEAR;
        }
    }
}
