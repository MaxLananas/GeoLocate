package dev.geolocate.task;

import dev.geolocate.GeoLocate;
import dev.geolocate.mapping.GeoPoint;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Optional;

public final class ActionBarTask extends BukkitRunnable {

    private final GeoLocate plugin;
    private final MiniMessage mm;

    public ActionBarTask(GeoLocate plugin) {
        this.plugin = plugin;
        this.mm = MiniMessage.miniMessage();
    }

    @Override
    public void run() {
        if (!plugin.getGeoConfig().isActionBarEnabled()) return;

        for (Player player : plugin.getServer().getOnlinePlayers()) {
            if (!player.hasPermission("geolocate.actionbar")) continue;
            if (!plugin.getPreferenceStorage().hasActionBarEnabled(player.getUniqueId())) continue;
            if (!plugin.getWorldMapper().isWorldMapped(player.getWorld().getName())) continue;

            Optional<GeoPoint> optPoint = plugin.getAPI().getGeoPoint(player);
            if (optPoint.isEmpty()) continue;

            GeoPoint point = optPoint.get();
            int dp = plugin.getGeoConfig().getDecimalPlaces();

            String text = String.format(
                    "<dark_gray>[<gradient:#00C9FF:#92FE9D>GeoLocate</gradient>]</dark_gray> "
                            + "<gray>%." + dp + "f, %." + dp + "f</gray>",
                    point.getLatitude(),
                    point.getLongitude()
            );

            player.sendActionBar(mm.deserialize(text));
        }
    }
}
