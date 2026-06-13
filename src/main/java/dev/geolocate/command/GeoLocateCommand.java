package dev.geolocate.command;

import dev.geolocate.GeoLocate;
import dev.geolocate.mapping.GeoPoint;
import dev.geolocate.util.GoogleMapsLink;
import dev.geolocate.util.MessageUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class GeoLocateCommand implements CommandExecutor, TabCompleter {

    private final GeoLocate plugin;
    private final MiniMessage mm;

    public GeoLocateCommand(GeoLocate plugin) {
        this.plugin = plugin;
        this.mm = MiniMessage.miniMessage();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("geoadmin")) {
            return handleAdmin(sender, args);
        }
        return handleGeoLocate(sender, args);
    }

    private boolean handleGeoLocate(CommandSender sender, String[] args) {
        Player target;

        if (args.length >= 1) {
            if (!sender.hasPermission("geolocate.others")) {
                sendMessage(sender, "no-permission");
                return true;
            }
            target = Bukkit.getPlayerExact(args[0]);
            if (target == null) {
                sendMessage(sender, "player-not-found");
                return true;
            }
        } else {
            if (!(sender instanceof Player player)) {
                sendMessage(sender, "player-only");
                return true;
            }
            if (!sender.hasPermission("geolocate.use")) {
                sendMessage(sender, "no-permission");
                return true;
            }
            target = player;
        }

        String worldName = target.getWorld().getName();
        if (!plugin.getWorldMapper().isWorldMapped(worldName)) {
            sendMessage(sender, "world-not-configured");
            return true;
        }

        Optional<GeoPoint> optPoint = plugin.getAPI().getGeoPoint(target);
        if (optPoint.isEmpty()) {
            sendMessage(sender, "world-not-configured");
            return true;
        }

        GeoPoint point = optPoint.get();
        String mapsLink = GoogleMapsLink.build(point, plugin.getGeoConfig().getGoogleMapsZoom());
        int dp = plugin.getGeoConfig().getDecimalPlaces();

        sendLocationResult(sender, target, point, mapsLink, dp);
        return true;
    }

    private void sendLocationResult(CommandSender sender, Player target, GeoPoint point, String mapsLink, int dp) {
        String prefix = plugin.getGeoConfig().getPrefix();
        boolean self = sender.equals(target);
        boolean showAlt = plugin.getGeoConfig().isShowAltitude();

        String header = prefix + (self
                ? "<white>Your real-world location:"
                : "<white>" + target.getName() + "'s real-world location:"
        );

        sender.sendMessage(mm.deserialize(header));

        String latLine = "  <gray>Latitude:  <aqua>" + formatCoord(point.getLatitude(), dp);
        String lonLine = "  <gray>Longitude: <aqua>" + formatCoord(point.getLongitude(), dp);
        sender.sendMessage(mm.deserialize(latLine));
        sender.sendMessage(mm.deserialize(lonLine));

        if (showAlt) {
            String altLine = "  <gray>Altitude:  <aqua>" + point.getAltitude() + "m";
            sender.sendMessage(mm.deserialize(altLine));
        }

        Component linkComponent = mm.deserialize(
                prefix + "<white>Open in Google Maps: <click:open_url:'" + mapsLink + "'>"
                        + "<underlined><green>Click here</green></underlined></click>"
        ).hoverEvent(HoverEvent.showText(mm.deserialize("<gray>" + mapsLink)));

        sender.sendMessage(linkComponent);
    }

    private boolean handleAdmin(CommandSender sender, String[] args) {
        if (!sender.hasPermission("geolocate.admin")) {
            sendMessage(sender, "no-permission");
            return true;
        }

        if (args.length == 0) {
            sendAdminHelp(sender);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "reload" -> {
                plugin.reload();
                sendMessage(sender, "reload-success");
            }
            case "info" -> sendInfo(sender);
            case "setbounds" -> sendSetBoundsHelp(sender);
            case "clearcache" -> {
                plugin.getWorldMapper().clearAllCaches();
                String prefix = plugin.getGeoConfig().getPrefix();
                sender.sendMessage(mm.deserialize(prefix + "<green>All caches cleared."));
            }
            default -> sendAdminHelp(sender);
        }

        return true;
    }

    private void sendAdminHelp(CommandSender sender) {
        String prefix = plugin.getGeoConfig().getPrefix();
        sender.sendMessage(mm.deserialize(prefix + "<gold>GeoLocate Admin Commands"));
        sender.sendMessage(mm.deserialize("  <gray>/geoadmin reload <white>- Reload configuration"));
        sender.sendMessage(mm.deserialize("  <gray>/geoadmin info <white>- Show plugin info"));
        sender.sendMessage(mm.deserialize("  <gray>/geoadmin clearcache <white>- Clear all coordinate caches"));
    }

    private void sendInfo(CommandSender sender) {
        String prefix = plugin.getGeoConfig().getPrefix();
        sender.sendMessage(mm.deserialize(prefix + "<gold>GeoLocate v" + plugin.getDescription().getVersion()));
        sender.sendMessage(mm.deserialize("  <gray>Configured worlds: <white>" + plugin.getWorldMapper().getConfiguredWorldCount()));
        sender.sendMessage(mm.deserialize("  <gray>Projection: <white>per-world (see config.yml)"));
        sender.sendMessage(mm.deserialize("  <gray>Decimal places: <white>" + plugin.getGeoConfig().getDecimalPlaces()));
        sender.sendMessage(mm.deserialize("  <gray>Google Maps zoom: <white>" + plugin.getGeoConfig().getGoogleMapsZoom()));
    }

    private void sendSetBoundsHelp(CommandSender sender) {
        String prefix = plugin.getGeoConfig().getPrefix();
        sender.sendMessage(mm.deserialize(prefix + "<yellow>To set custom bounds, edit config.yml and run /geoadmin reload."));
        sender.sendMessage(mm.deserialize("  <gray>Configure the <white>worlds.<worldname>.bounds</white> section."));
    }

    private void sendMessage(CommandSender sender, String key) {
        String prefix = plugin.getGeoConfig().getPrefix();
        String msg = plugin.getGeoConfig().getMessage(key);
        sender.sendMessage(mm.deserialize(prefix + msg));
    }

    private String formatCoord(double value, int dp) {
        return String.format("%." + dp + "f", value);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        List<String> suggestions = new ArrayList<>();

        if (command.getName().equalsIgnoreCase("geoadmin")) {
            if (args.length == 1) {
                List.of("reload", "info", "clearcache", "setbounds").stream()
                        .filter(s -> s.startsWith(args[0].toLowerCase()))
                        .forEach(suggestions::add);
            }
            return suggestions;
        }

        if (args.length == 1 && sender.hasPermission("geolocate.others")) {
            Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(name -> name.toLowerCase().startsWith(args[0].toLowerCase()))
                    .forEach(suggestions::add);
        }

        return suggestions;
    }
}
