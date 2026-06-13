package dev.geolocate.model;

import dev.geolocate.mapping.GeoPoint;

import java.time.Instant;
import java.util.UUID;

public final class GeoSnapshot {

    private final UUID playerUuid;
    private final String playerName;
    private final GeoPoint point;
    private final String worldName;
    private final double minecraftX;
    private final double minecraftY;
    private final double minecraftZ;
    private final Instant timestamp;

    public GeoSnapshot(
            UUID playerUuid,
            String playerName,
            GeoPoint point,
            String worldName,
            double minecraftX,
            double minecraftY,
            double minecraftZ
    ) {
        this.playerUuid = playerUuid;
        this.playerName = playerName;
        this.point = point;
        this.worldName = worldName;
        this.minecraftX = minecraftX;
        this.minecraftY = minecraftY;
        this.minecraftZ = minecraftZ;
        this.timestamp = Instant.now();
    }

    public String toCSVLine() {
        return String.join(",",
                playerUuid.toString(),
                playerName,
                String.valueOf(point.latitude()),
                String.valueOf(point.longitude()),
                String.valueOf(point.altitude()),
                worldName,
                String.valueOf(minecraftX),
                String.valueOf(minecraftY),
                String.valueOf(minecraftZ),
                timestamp.toString()
        );
    }

    public String toJSON() {
        return String.format(
                "{\"uuid\":\"%s\",\"name\":\"%s\",\"lat\":%s,\"lon\":%s,\"alt\":%s,\"world\":\"%s\",\"x\":%s,\"y\":%s,\"z\":%s,\"timestamp\":\"%s\"}",
                playerUuid, playerName,
                point.latitude(), point.longitude(), point.altitude(),
                worldName,
                minecraftX, minecraftY, minecraftZ,
                timestamp
        );
    }

    public UUID getPlayerUuid() { return playerUuid; }
    public String getPlayerName() { return playerName; }
    public GeoPoint getPoint() { return point; }
    public String getWorldName() { return worldName; }
    public double getMinecraftX() { return minecraftX; }
    public double getMinecraftY() { return minecraftY; }
    public double getMinecraftZ() { return minecraftZ; }
    public Instant getTimestamp() { return timestamp; }
}
