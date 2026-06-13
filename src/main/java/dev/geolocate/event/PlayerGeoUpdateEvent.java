package dev.geolocate.event;

import dev.geolocate.mapping.GeoPoint;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public final class PlayerGeoUpdateEvent extends Event implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Player player;
    private final GeoPoint previousPoint;
    private final GeoPoint newPoint;
    private final double distanceMeters;
    private boolean cancelled;

    public PlayerGeoUpdateEvent(Player player, GeoPoint previousPoint, GeoPoint newPoint) {
        this.player = player;
        this.previousPoint = previousPoint;
        this.newPoint = newPoint;
        this.distanceMeters = previousPoint != null ? previousPoint.distanceTo(newPoint) : 0;
    }

    public Player getPlayer() { return player; }
    public GeoPoint getPreviousPoint() { return previousPoint; }
    public GeoPoint getNewPoint() { return newPoint; }
    public double getDistanceMeters() { return distanceMeters; }

    @Override
    public boolean isCancelled() { return cancelled; }

    @Override
    public void setCancelled(boolean cancel) { this.cancelled = cancel; }

    @Override
    public HandlerList getHandlers() { return HANDLERS; }
    public static HandlerList getHandlerList() { return HANDLERS; }
}
