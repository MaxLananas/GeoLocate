package dev.geolocate.mapping;

public final class WorldBoundingBox {

    private final double minLat;
    private final double maxLat;
    private final double minLon;
    private final double maxLon;
    private final double minX;
    private final double maxX;
    private final double minZ;
    private final double maxZ;
    private final double offsetX;
    private final double offsetZ;

    public WorldBoundingBox(
            double minLat, double maxLat,
            double minLon, double maxLon,
            double minX, double maxX,
            double minZ, double maxZ,
            double offsetX, double offsetZ
    ) {
        this.minLat = minLat;
        this.maxLat = maxLat;
        this.minLon = minLon;
        this.maxLon = maxLon;
        this.minX = minX;
        this.maxX = maxX;
        this.minZ = minZ;
        this.maxZ = maxZ;
        this.offsetX = offsetX;
        this.offsetZ = offsetZ;
    }

    public double getMinLat() { return minLat; }
    public double getMaxLat() { return maxLat; }
    public double getMinLon() { return minLon; }
    public double getMaxLon() { return maxLon; }
    public double getMinX() { return minX; }
    public double getMaxX() { return maxX; }
    public double getMinZ() { return minZ; }
    public double getMaxZ() { return maxZ; }
    public double getOffsetX() { return offsetX; }
    public double getOffsetZ() { return offsetZ; }

    public double getLatRange() { return maxLat - minLat; }
    public double getLonRange() { return maxLon - minLon; }
    public double getXRange() { return maxX - minX; }
    public double getZRange() { return maxZ - minZ; }
}
