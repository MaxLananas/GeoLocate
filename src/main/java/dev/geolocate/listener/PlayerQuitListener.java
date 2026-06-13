package dev.geolocate.listener;

import dev.geolocate.GeoLocate;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.UUID;

public final class PlayerQuitListener implements Listener {

    private final GeoLocate plugin;

    public PlayerQuitListener(GeoLocate plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        plugin.getPreferenceStorage().saveAll();
        plugin.getAPI().removePlayer(uuid);
    }
}
