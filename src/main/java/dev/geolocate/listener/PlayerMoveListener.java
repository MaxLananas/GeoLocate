package dev.geolocate.listener;

import dev.geolocate.GeoLocate;
import dev.geolocate.mapping.GeoPoint;
import dev.geolocate.util.GoogleMapsLink;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public final class PlayerMoveListener implements Listener {

    private final GeoLocate plugin;
    private final MiniMessage mm;
    private final Map<UUID, Location> lastNotifiedLocation;

    public PlayerMoveListener(GeoLocate plugin) {
        this.plugin = plugin;
        this.mm = MiniMessage.miniMessage();
        this.lastNotifiedLocation = new HashMap<>();
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerMove(PlayerMoveEvent event) {
        if (!plugin.getGeoConfig().isNotifyOnMove()) return;

        Location from = event.getFrom();
        Location to = event.getTo();

        if (sameBlock(from, to)) return;

        Player player = event.getPlayer();
        if (!player.hasPermission("geolocate.notify")) return;
        if (!plugin.getWorldMapper().isWorldMapped(player.getWorld().getName())) return;

        int notifyDistance = plugin.getGeoConfig().getNotifyDistance();
        Location last = lastNotifiedLocation.get(player.getUniqueId());

        if (last != null && to.distanceSquared(last) < (notifyDistance * notifyDistance)) return;

        lastNotifiedLocation.put(player.getUniqueId(), to.clone());

        Optional<GeoPoint> optPoint = plugin.getAPI().getGeoPoint(to);
        if (optPoint.isEmpty()) return;

        GeoPoint point = optPoint.get();
        String mapsLink = GoogleMapsLink.build(point, plugin.getGeoConfig().getGoogleMapsZoom());
        int dp = plugin.getGeoConfig().getDecimalPlaces();

        String prefix = plugin.getGeoConfig().getPrefix();
        String coordText = String.format("%." + dp + "f, %." + dp + "f", point.getLatitude(), point.getLongitude());

        Component message = mm.deserialize(
                prefix + "<gray>Location: <aqua>" + coordText
                        + " <dark_gray>| <click:open_url:'" + mapsLink + "'>"
                        + "<underlined><green>Maps</green></underlined></click>"
        ).hoverEvent(HoverEvent.showText(mm.deserialize("<gray>" + mapsLink)));

        player.sendMessage(message);
    }

    public void removePlayer(UUID uuid) {
        lastNotifiedLocation.remove(uuid);
    }

    private boolean sameBlock(Location a, Location b) {
        return a.getBlockX() == b.getBlockX()
                && a.getBlockY() == b.getBlockY()
                && a.getBlockZ() == b.getBlockZ();
    }
}
