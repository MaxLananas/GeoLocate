package dev.geolocate.model;

import dev.geolocate.mapping.GeoPoint;

import java.time.Instant;
import java.util.UUID;

public final class GeoSnapshot {

    private final UUID      playerUuid;
    private final String    playerName;
    private final GeoPoint  point;
    private final String    worldName;
    private final double    minecraftX;
    private final double    minecraftY;
    private final double    minecraftZ;
    private final Instant   timestamp;

    // Lazily built and cached
    private String cachedCSV;

    public GeoSnapshot(
            UUID playerUuid, String playerName, GeoPoint point,
            String worldName, double minecraftX, double minecraftY, double minecraftZ) {
        this.playerUuid  = playerUuid;
        this.playerName  = playerName;
        this.point       = point;
        this.worldName   = worldName;
        this.minecraftX  = minecraftX;
        this.minecraftY  = minecraftY;
        this.minecraftZ  = minecraftZ;
        this.timestamp   = Instant.now();
    }

    public String toCSVLine() {
        if (cachedCSV != null) return cachedCSV;
        cachedCSV = playerUuid + "," + playerName + ","
                + point.latitude() + "," + point.longitude() + "," + point.altitude() + ","
                + worldName + ","
                + minecraftX + "," + minecraftY + "," + minecraftZ + ","
                + timestamp;
        return cachedCSV;
    }

    public String toJSON() {
        return "{\"uuid\":\"" + playerUuid + "\",\"name\":\"" + playerName + "\","
                + "\"lat\":" + point.latitude() + ",\"lon\":" + point.longitude()
                + ",\"alt\":" + point.altitude() + ","
                + "\"world\":\"" + worldName + "\","
                + "\"x\":" + minecraftX + ",\"y\":" + minecraftY + ",\"z\":" + minecraftZ + ","
                + "\"timestamp\":\"" + timestamp + "\"}";
    }

    public UUID     getPlayerUuid()  { return playerUuid; }
    public String   getPlayerName()  { return playerName; }
    public GeoPoint getPoint()       { return point; }
    public String   getWorldName()   { return worldName; }
    public double   getMinecraftX()  { return minecraftX; }
    public double   getMinecraftY()  { return minecraftY; }
    public double   getMinecraftZ()  { return minecraftZ; }
    public Instant  getTimestamp()   { return timestamp; }
}
