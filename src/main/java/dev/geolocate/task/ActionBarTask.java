package dev.geolocate.task;

import dev.geolocate.GeoLocate;
import dev.geolocate.mapping.GeoPoint;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Collection;
import java.util.Optional;

public final class ActionBarTask extends BukkitRunnable {

    private static final String WRAPPER_FORMAT =
            "<dark_gray>[<gradient:#00C9FF:#92FE9D>GeoLocate</gradient>]</dark_gray> <gray>%s</gray>";

    private final GeoLocate plugin;
    private final MiniMessage mm;

    // Cached coord format string — rebuilt only when decimal places change
    private int    lastDp  = -1;
    private String coordFmt;

    public ActionBarTask(GeoLocate plugin) {
        this.plugin = plugin;
        this.mm     = MiniMessage.miniMessage();
    }

    @Override
    public void run() {
        if (!plugin.getGeoConfig().isActionBarEnabled()) return;

        Collection<? extends Player> players = plugin.getServer().getOnlinePlayers();
        if (players.isEmpty()) return;

        int dp = plugin.getGeoConfig().getDecimalPlaces();
        if (dp != lastDp) {
            coordFmt = "%." + dp + "f, %." + dp + "f";
            lastDp   = dp;
        }

        for (Player player : players) {
            if (!player.hasPermission("geolocate.actionbar")) continue;
            if (!plugin.getPreferenceStorage().hasActionBarEnabled(player.getUniqueId())) continue;
            if (!plugin.getWorldMapper().isWorldMapped(player.getWorld().getName())) continue;

            Optional<GeoPoint> optPoint = plugin.getAPI().getGeoPoint(player);
            if (optPoint.isEmpty()) continue;

            GeoPoint point  = optPoint.get();
            String coords   = String.format(coordFmt, point.latitude(), point.longitude());
            player.sendActionBar(mm.deserialize(String.format(WRAPPER_FORMAT, coords)));
        }
    }
}
