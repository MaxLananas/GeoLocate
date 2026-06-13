package dev.geolocate.event;

import dev.geolocate.mapping.GeoPoint;
import dev.geolocate.model.GeoRegion;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public final class PlayerEnterGeoRegionEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Player player;
    private final GeoRegion region;
    private final GeoPoint geoPoint;

    public PlayerEnterGeoRegionEvent(Player player, GeoRegion region, GeoPoint geoPoint) {
        this.player = player;
        this.region = region;
        this.geoPoint = geoPoint;
    }

    public Player getPlayer() { return player; }
    public GeoRegion getRegion() { return region; }
    public GeoPoint getGeoPoint() { return geoPoint; }

    @Override
    public HandlerList getHandlers() { return HANDLERS; }
    public static HandlerList getHandlerList() { return HANDLERS; }
}
