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

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class PlayerMoveListener implements Listener {

    private final GeoLocate plugin;
    private final MiniMessage mm;

    // Packed [chunkX, chunkZ] per player – avoids allocation compared to long[]
    private final ConcurrentHashMap<UUID, long[]> lastNotifiedChunk;
    private final ConcurrentHashMap<UUID, Long>   lastNotifiedTime;

    public PlayerMoveListener(GeoLocate plugin) {
        this.plugin             = plugin;
        this.mm                 = MiniMessage.miniMessage();
        this.lastNotifiedChunk  = new ConcurrentHashMap<>();
        this.lastNotifiedTime   = new ConcurrentHashMap<>();
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerMove(PlayerMoveEvent event) {
        Location from = event.getFrom();
        Location to   = event.getTo();

        // Fast-path: ignore Y-only movement (jumping, falling)
        if (from.getBlockX() == to.getBlockX() && from.getBlockZ() == to.getBlockZ()) return;

        Player player = event.getPlayer();
        UUID   uuid   = player.getUniqueId();

        if (!plugin.getGeoConfig().isNotifyOnMove()) return;
        if (!plugin.getPreferenceStorage().hasNotifyEnabled(uuid)) return;
        if (!player.hasPermission("geolocate.notify")) return;
        if (!plugin.getWorldMapper().isWorldMapped(player.getWorld().getName())) return;

        int notifyDistance = plugin.getGeoConfig().getNotifyDistance();

        long currentChunkX = to.getBlockX() >> 4;
        long currentChunkZ = to.getBlockZ() >> 4;

        long[] lastChunk = lastNotifiedChunk.get(uuid);
        if (lastChunk != null) {
            long dCX = currentChunkX - lastChunk[0];
            long dCZ = currentChunkZ - lastChunk[1];
            long chunksNeeded = Math.max(1L, notifyDistance >> 4);
            if (dCX * dCX + dCZ * dCZ < chunksNeeded * chunksNeeded) return;
        }

        long now      = System.currentTimeMillis();
        Long lastTime = lastNotifiedTime.get(uuid);
        if (lastTime != null && now - lastTime < 2000L) return;

        Optional<GeoPoint> optPoint = plugin.getAPI().getGeoPoint(to);
        if (optPoint.isEmpty()) return;

        // Update state before building message
        lastNotifiedChunk.put(uuid, new long[]{currentChunkX, currentChunkZ});
        lastNotifiedTime.put(uuid, now);

        GeoPoint point    = optPoint.get();
        int      dp       = plugin.getGeoConfig().getDecimalPlaces();
        int      zoom     = plugin.getGeoConfig().getGoogleMapsZoom();
        String   mapsLink = GoogleMapsLink.build(point, zoom);
        String   prefix   = plugin.getGeoConfig().getPrefix();
        String   coordFmt = "%." + dp + "f, %." + dp + "f";
        String   coordText = String.format(coordFmt, point.latitude(), point.longitude());

        Component message = mm.deserialize(
                prefix + "<gray>Location: <aqua>" + coordText
                        + " <dark_gray>| <click:open_url:'" + mapsLink + "'>"
                        + "<underlined><green>Maps</green></underlined></click>"
        ).hoverEvent(HoverEvent.showText(mm.deserialize("<gray>" + mapsLink)));

        player.sendMessage(message);
    }

    public void removePlayer(UUID uuid) {
        lastNotifiedChunk.remove(uuid);
        lastNotifiedTime.remove(uuid);
    }
}
