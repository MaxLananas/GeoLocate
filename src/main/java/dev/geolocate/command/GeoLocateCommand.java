package dev.geolocate.command;

import dev.geolocate.GeoLocate;
import dev.geolocate.mapping.GeoPoint;
import dev.geolocate.util.GoogleMapsLink;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public final class GeoLocateCommand implements CommandExecutor, TabCompleter {

    private final GeoLocate plugin;
    private final MiniMessage mm;
    private final Map<UUID, Long> cooldowns;

    public GeoLocateCommand(GeoLocate plugin) {
        this.plugin = plugin;
        this.mm = MiniMessage.miniMessage();
        this.cooldowns = new HashMap<>();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        switch (command.getName().toLowerCase()) {
            case "geoadmin" -> { return handleAdmin(sender, args); }
            case "geoconvert" -> { return handleConvert(sender, args); }
            case "geodistance" -> { return handleDistance(sender, args); }
            case "geonotify" -> { return handleNotify(sender); }
            case "geoactionbar" -> { return handleActionBar(sender); }
            default -> { return handleGeoLocate(sender, args); }
        }
    }

    private boolean isOnCooldown(CommandSender sender) {
        if (!(sender instanceof Player player)) return false;
        if (sender.hasPermission("geolocate.admin")) return false;
        int seconds = plugin.getGeoConfig().getCommandCooldownSeconds();
        if (seconds <= 0) return false;

        long now = System.currentTimeMillis();
        UUID uuid = player.getUniqueId();
        Long last = cooldowns.get(uuid);
        if (last != null && now - last < seconds * 1000L) {
            long remaining = (seconds * 1000L - (now - last)) / 1000L + 1;
            String msg = plugin.getGeoConfig().getPrefix()
                    + plugin.getGeoConfig().getMessage("cooldown")
                    .replace("<seconds>", String.valueOf(remaining));
            sender.sendMessage(mm.deserialize(msg));
            return true;
        }
        cooldowns.put(uuid, now);
        return false;
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

        if (isOnCooldown(sender)) return true;

        if (!plugin.getWorldMapper().isWorldMapped(target.getWorld().getName())) {
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

    private boolean handleConvert(CommandSender sender, String[] args) {
        if (!sender.hasPermission("geolocate.use")) {
            sendMessage(sender, "no-permission");
            return true;
        }
        if (!(sender instanceof Player player)) {
            sendMessage(sender, "player-only");
            return true;
        }
        if (args.length < 2) {
            sender.sendMessage(mm.deserialize(plugin.getGeoConfig().getPrefix()
                    + "<yellow>Usage: /geoconvert <latitude> <longitude>"));
            return true;
        }

        double lat, lon;
        try {
            lat = Double.parseDouble(args[0]);
            lon = Double.parseDouble(args[1]);
        } catch (NumberFormatException e) {
            sendMessage(sender, "invalid-coordinates");
            return true;
        }

        if (lat < -90 || lat > 90 || lon < -180 || lon > 180) {
            sendMessage(sender, "invalid-coordinates");
            return true;
        }

        if (!plugin.getWorldMapper().isWorldMapped(player.getWorld().getName())) {
            sendMessage(sender, "world-not-configured");
            return true;
        }

        if (isOnCooldown(sender)) return true;

        double[] mc = plugin.getWorldMapper().getConverter(player.getWorld().getName())
                .map(c -> c.convertToMinecraft(lat, lon))
                .orElse(null);

        if (mc == null) {
            sendMessage(sender, "world-not-configured");
            return true;
        }

        int dp = plugin.getGeoConfig().getDecimalPlaces();
        String msg = plugin.getGeoConfig().getMessage("convert-result")
                .replace("<lat>", String.format("%." + dp + "f", lat))
                .replace("<lon>", String.format("%." + dp + "f", lon))
                .replace("<x>", String.valueOf((int) mc[0]))
                .replace("<z>", String.valueOf((int) mc[1]));

        sender.sendMessage(mm.deserialize(plugin.getGeoConfig().getPrefix() + msg));
        return true;
    }

    private boolean handleDistance(CommandSender sender, String[] args) {
        if (!(sender instanceof Player self)) {
            sendMessage(sender, "player-only");
            return true;
        }
        if (!sender.hasPermission("geolocate.use")) {
            sendMessage(sender, "no-permission");
            return true;
        }

        Player target;
        if (args.length >= 1) {
            target = Bukkit.getPlayerExact(args[0]);
            if (target == null) {
                sendMessage(sender, "player-not-found");
                return true;
            }
        } else {
            sender.sendMessage(mm.deserialize(plugin.getGeoConfig().getPrefix()
                    + "<yellow>Usage: /geodistance <player>"));
            return true;
        }

        if (!plugin.getWorldMapper().isWorldMapped(self.getWorld().getName())
                || !plugin.getWorldMapper().isWorldMapped(target.getWorld().getName())) {
            sendMessage(sender, "distance-same-world");
            return true;
        }

        if (!self.getWorld().getName().equals(target.getWorld().getName())) {
            sendMessage(sender, "distance-same-world");
            return true;
        }

        if (isOnCooldown(sender)) return true;

        Optional<GeoPoint> selfPoint = plugin.getAPI().getGeoPoint(self);
        Optional<GeoPoint> targetPoint = plugin.getAPI().getGeoPoint(target);

        if (selfPoint.isEmpty() || targetPoint.isEmpty()) {
            sendMessage(sender, "world-not-configured");
            return true;
        }

        double distanceMeters = selfPoint.get().distanceTo(targetPoint.get());
        String formatted = formatDistance(distanceMeters);

        String msg = plugin.getGeoConfig().getMessage("distance-result")
                .replace("<player>", target.getName())
                .replace("<distance>", formatted);

        sender.sendMessage(mm.deserialize(plugin.getGeoConfig().getPrefix() + msg));
        return true;
    }

    private boolean handleNotify(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sendMessage(sender, "player-only");
            return true;
        }
        if (!sender.hasPermission("geolocate.notify.toggle")) {
            sendMessage(sender, "no-permission");
            return true;
        }

        boolean now = plugin.getPreferenceStorage().toggleNotify(player.getUniqueId());
        String key = now ? "notifications-enabled" : "notifications-disabled";
        sendMessage(sender, key);
        return true;
    }

    private boolean handleActionBar(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sendMessage(sender, "player-only");
            return true;
        }
        if (!sender.hasPermission("geolocate.actionbar")) {
            sendMessage(sender, "no-permission");
            return true;
        }
        if (!plugin.getGeoConfig().isActionBarEnabled()) {
            sender.sendMessage(mm.deserialize(plugin.getGeoConfig().getPrefix()
                    + "<yellow>ActionBar display is disabled in the config."));
            return true;
        }

        boolean now = plugin.getPreferenceStorage().toggleActionBar(player.getUniqueId());
        String status = now ? "<green>enabled" : "<red>disabled";
        sender.sendMessage(mm.deserialize(plugin.getGeoConfig().getPrefix()
                + "<white>ActionBar display " + status + "<white>."));
        return true;
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
            case "clearcache" -> {
                plugin.getWorldMapper().clearAllCaches();
                sender.sendMessage(mm.deserialize(plugin.getGeoConfig().getPrefix()
                        + "<green>All caches cleared."));
            }
            default -> sendAdminHelp(sender);
        }

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
        sender.sendMessage(mm.deserialize("  <gray>Latitude:  <aqua>" + formatCoord(point.getLatitude(), dp)));
        sender.sendMessage(mm.deserialize("  <gray>Longitude: <aqua>" + formatCoord(point.getLongitude(), dp)));

        if (showAlt) {
            sender.sendMessage(mm.deserialize("  <gray>Altitude:  <aqua>" + point.getAltitude() + "m"));
        }

        Component linkComponent = mm.deserialize(
                prefix + "<white>Open in Google Maps: <click:open_url:'" + mapsLink + "'>"
                        + "<underlined><green>Click here</green></underlined></click>"
        ).hoverEvent(HoverEvent.showText(mm.deserialize("<gray>" + mapsLink)));

        sender.sendMessage(linkComponent);
    }

    private void sendAdminHelp(CommandSender sender) {
        String prefix = plugin.getGeoConfig().getPrefix();
        sender.sendMessage(mm.deserialize(prefix + "<gold>GeoLocate Admin Commands"));
        sender.sendMessage(mm.deserialize("  <gray>/geoadmin reload     <white>- Reload configuration"));
        sender.sendMessage(mm.deserialize("  <gray>/geoadmin info       <white>- Show plugin info"));
        sender.sendMessage(mm.deserialize("  <gray>/geoadmin clearcache <white>- Clear coordinate caches"));
    }

    private void sendInfo(CommandSender sender) {
        String prefix = plugin.getGeoConfig().getPrefix();
        sender.sendMessage(mm.deserialize(prefix + "<gold>GeoLocate v" + plugin.getDescription().getVersion()));
        sender.sendMessage(mm.deserialize("  <gray>Worlds mapped:   <white>" + plugin.getWorldMapper().getConfiguredWorldCount()));
        sender.sendMessage(mm.deserialize("  <gray>Decimal places: <white>" + plugin.getGeoConfig().getDecimalPlaces()));
        sender.sendMessage(mm.deserialize("  <gray>Maps zoom:      <white>" + plugin.getGeoConfig().getGoogleMapsZoom()));
        sender.sendMessage(mm.deserialize("  <gray>Cooldown:       <white>" + plugin.getGeoConfig().getCommandCooldownSeconds() + "s"));
        sender.sendMessage(mm.deserialize("  <gray>ActionBar:      <white>" + (plugin.getGeoConfig().isActionBarEnabled() ? "<green>enabled" : "<red>disabled")));
    }

    private void sendMessage(CommandSender sender, String key) {
        String msg = plugin.getGeoConfig().getPrefix() + plugin.getGeoConfig().getMessage(key);
        sender.sendMessage(mm.deserialize(msg));
    }

    private String formatCoord(double value, int dp) {
        return String.format("%." + dp + "f", value);
    }

    private String formatDistance(double meters) {
        if (meters >= 1000) {
            return String.format("%.2f km", meters / 1000);
        }
        return String.format("%.0f m", meters);
    }

    public void removeCooldown(UUID uuid) {
        cooldowns.remove(uuid);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        List<String> suggestions = new ArrayList<>();

        switch (command.getName().toLowerCase()) {
            case "geoadmin" -> {
                if (args.length == 1) {
                    List.of("reload", "info", "clearcache").stream()
                            .filter(s -> s.startsWith(args[0].toLowerCase()))
                            .forEach(suggestions::add);
                }
            }
            case "geodistance", "geolocate" -> {
                if (args.length == 1 && sender.hasPermission("geolocate.others")) {
                    Bukkit.getOnlinePlayers().stream()
                            .map(Player::getName)
                            .filter(name -> name.toLowerCase().startsWith(args[0].toLowerCase()))
                            .forEach(suggestions::add);
                }
            }
            case "geoconvert" -> {
                if (args.length == 1) suggestions.add("<latitude>");
                if (args.length == 2) suggestions.add("<longitude>");
            }
        }

        return suggestions;
    }
}
