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

    // Pre-computed ranges — avoids subtraction on every projection call
    private final double latRange;
    private final double lonRange;
    private final double xRange;
    private final double zRange;

    public WorldBoundingBox(
            double minLat, double maxLat,
            double minLon, double maxLon,
            double minX,   double maxX,
            double minZ,   double maxZ,
            double offsetX, double offsetZ) {
        this.minLat  = minLat;
        this.maxLat  = maxLat;
        this.minLon  = minLon;
        this.maxLon  = maxLon;
        this.minX    = minX;
        this.maxX    = maxX;
        this.minZ    = minZ;
        this.maxZ    = maxZ;
        this.offsetX = offsetX;
        this.offsetZ = offsetZ;

        this.latRange = maxLat - minLat;
        this.lonRange = maxLon - minLon;
        this.xRange   = maxX   - minX;
        this.zRange   = maxZ   - minZ;
    }

    public double getMinLat()  { return minLat; }
    public double getMaxLat()  { return maxLat; }
    public double getMinLon()  { return minLon; }
    public double getMaxLon()  { return maxLon; }
    public double getMinX()    { return minX; }
    public double getMaxX()    { return maxX; }
    public double getMinZ()    { return minZ; }
    public double getMaxZ()    { return maxZ; }
    public double getOffsetX() { return offsetX; }
    public double getOffsetZ() { return offsetZ; }

    /** Pre-computed — no subtraction at call site. */
    public double getLatRange() { return latRange; }
    public double getLonRange() { return lonRange; }
    public double getXRange()   { return xRange; }
    public double getZRange()   { return zRange; }
}
