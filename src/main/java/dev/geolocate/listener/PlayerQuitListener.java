package dev.geolocate.listener;

import dev.geolocate.GeoLocate;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

public final class PlayerQuitListener implements Listener {

    private final GeoLocate plugin;

    public PlayerQuitListener(GeoLocate plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        plugin.getPreferenceStorage().saveAll();
    }
}
