package dev.geolocate.command;

import dev.geolocate.GeoLocate;
import dev.geolocate.border.GeoBorderDetector;
import dev.geolocate.export.GeoExporter;
import dev.geolocate.history.PlayerGeoHistory;
import dev.geolocate.mapping.GeoPoint;
import dev.geolocate.model.DistanceMatrix;
import dev.geolocate.model.GeoPath;
import dev.geolocate.model.GeoRegion;
import dev.geolocate.model.GeoSnapshot;
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

import java.io.File;
import java.io.IOException;
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
        return switch (command.getName().toLowerCase()) {
            case "geoadmin" -> handleAdmin(sender, args);
            case "geoconvert" -> handleConvert(sender, args);
            case "geodistance" -> handleDistance(sender, args);
            case "geonotify" -> handleNotify(sender);
            case "geoactionbar" -> handleActionBar(sender);
            case "geopath" -> handlePath(sender, args);
            case "georegion" -> handleRegion(sender, args);
            case "geomatrix" -> handleMatrix(sender, args);
            case "geoexport" -> handleExport(sender, args);
            case "geostats" -> handleStats(sender);
            default -> handleGeoLocate(sender, args);
        };
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
            if (!sender.hasPermission("geolocate.others")) { sendMessage(sender, "no-permission"); return true; }
            target = Bukkit.getPlayerExact(args[0]);
            if (target == null) { sendMessage(sender, "player-not-found"); return true; }
        } else {
            if (!(sender instanceof Player p)) { sendMessage(sender, "player-only"); return true; }
            if (!sender.hasPermission("geolocate.use")) { sendMessage(sender, "no-permission"); return true; }
            target = p;
        }

        if (isOnCooldown(sender)) return true;
        if (!plugin.getWorldMapper().isWorldMapped(target.getWorld().getName())) { sendMessage(sender, "world-not-configured"); return true; }

        Optional<GeoPoint> optPoint = plugin.getAPI().getGeoPoint(target);
        if (optPoint.isEmpty()) { sendMessage(sender, "world-not-configured"); return true; }

        GeoPoint point = optPoint.get();
        String mapsLink = GoogleMapsLink.build(point, plugin.getGeoConfig().getGoogleMapsZoom());
        int dp = plugin.getGeoConfig().getDecimalPlaces();

        String prefix = plugin.getGeoConfig().getPrefix();
        boolean self = sender.equals(target);

        sender.sendMessage(mm.deserialize(prefix + (self ? "<white>Your real-world location:" : "<white>" + target.getName() + "'s real-world location:")));
        sender.sendMessage(mm.deserialize("  <gray>Latitude:  <aqua>" + formatCoord(point.latitude(), dp)));
        sender.sendMessage(mm.deserialize("  <gray>Longitude: <aqua>" + formatCoord(point.longitude(), dp)));
        sender.sendMessage(mm.deserialize("  <gray>DMS:       <aqua>" + point.formatDMS()));
        if (plugin.getGeoConfig().isShowAltitude()) {
            sender.sendMessage(mm.deserialize("  <gray>Altitude:  <aqua>" + point.altitude() + "m"));
        }

        List<GeoRegion> regions = plugin.getAPI().getRegionsAt(point);
        if (!regions.isEmpty()) {
            String names = String.join(", ", regions.stream().map(GeoRegion::getName).toList());
            sender.sendMessage(mm.deserialize("  <gray>Regions:   <gold>" + names));
        }

        Component linkComponent = mm.deserialize(
                prefix + "<white>Open in: "
                        + "<click:open_url:'" + mapsLink + "'><underlined><green>Google Maps</green></underlined></click>"
                        + "  <click:open_url:'" + GoogleMapsLink.buildOpenStreetMap(point, plugin.getGeoConfig().getGoogleMapsZoom()) + "'><underlined><aqua>OpenStreetMap</aqua></underlined></click>"
        ).hoverEvent(HoverEvent.showText(mm.deserialize("<gray>" + point.format(dp))));

        sender.sendMessage(linkComponent);
        return true;
    }

    private boolean handlePath(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) { sendMessage(sender, "player-only"); return true; }
        if (!sender.hasPermission("geolocate.use")) { sendMessage(sender, "no-permission"); return true; }

        String prefix = plugin.getGeoConfig().getPrefix();

        if (args.length == 0) {
            sender.sendMessage(mm.deserialize(prefix + "<gold>Path commands:"));
            sender.sendMessage(mm.deserialize("  <gray>/geopath info <white>- Show your path statistics"));
            sender.sendMessage(mm.deserialize("  <gray>/geopath clear <white>- Clear your history"));
            sender.sendMessage(mm.deserialize("  <gray>/geopath snap <white>- Record a snapshot now"));
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "info" -> {
                GeoPath path = plugin.getAPI().getPlayerPath(player);
                PlayerGeoHistory history = plugin.getAPI().getHistory(player);
                int dp = plugin.getGeoConfig().getDecimalPlaces();

                sender.sendMessage(mm.deserialize(prefix + "<gold>Your geo path"));
                sender.sendMessage(mm.deserialize("  <gray>Snapshots:   <white>" + history.size()));
                sender.sendMessage(mm.deserialize("  <gray>Total dist:  <white>" + formatDistance(history.getTotalDistanceTraveled())));
                if (!path.isEmpty()) {
                    GeoPoint first = path.getFirst();
                    GeoPoint last = path.getLast();
                    sender.sendMessage(mm.deserialize("  <gray>Start:       <aqua>" + first.format(dp)));
                    sender.sendMessage(mm.deserialize("  <gray>Current:     <aqua>" + last.format(dp)));
                    sender.sendMessage(mm.deserialize("  <gray>Bearing:     <white>" + String.format("%.1f", first.bearingTo(last)) + "° " + first.bearingCardinal(last)));
                }
            }
            case "clear" -> {
                plugin.getAPI().clearHistory(player.getUniqueId());
                sender.sendMessage(mm.deserialize(prefix + "<green>Path history cleared."));
            }
            case "snap" -> {
                plugin.getAPI().recordSnapshot(player);
                sender.sendMessage(mm.deserialize(prefix + "<green>Snapshot recorded."));
            }
            default -> sender.sendMessage(mm.deserialize(prefix + "<red>Unknown subcommand."));
        }
        return true;
    }

    private boolean handleRegion(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) { sendMessage(sender, "player-only"); return true; }
        if (!sender.hasPermission("geolocate.use")) { sendMessage(sender, "no-permission"); return true; }

        String prefix = plugin.getGeoConfig().getPrefix();

        if (args.length == 0) {
            sender.sendMessage(mm.deserialize(prefix + "<gold>Region commands:"));
            sender.sendMessage(mm.deserialize("  <gray>/georegion current <white>- Show regions you are in"));
            sender.sendMessage(mm.deserialize("  <gray>/georegion list <white>- List all registered regions"));
            sender.sendMessage(mm.deserialize("  <gray>/georegion info <name> <white>- Show region details"));
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "current" -> {
                List<GeoRegion> regions = plugin.getAPI().getRegionsAt(player);
                if (regions.isEmpty()) {
                    sender.sendMessage(mm.deserialize(prefix + "<yellow>You are not inside any registered geo-region."));
                } else {
                    sender.sendMessage(mm.deserialize(prefix + "<white>You are inside " + regions.size() + " region(s):"));
                    for (GeoRegion r : regions) {
                        sender.sendMessage(mm.deserialize("  <gold>" + r.getName() + " <gray>(" + r.getVertices().size() + " vertices, " + String.format("%.2f", r.getAreaSquareKm()) + " km²)"));
                    }
                }
            }
            case "list" -> {
                var all = plugin.getAPI().getRegionManager().getAllRegions();
                if (all.isEmpty()) {
                    sender.sendMessage(mm.deserialize(prefix + "<yellow>No regions registered."));
                } else {
                    sender.sendMessage(mm.deserialize(prefix + "<gold>Registered regions (" + all.size() + "):"));
                    for (GeoRegion r : all) {
                        sender.sendMessage(mm.deserialize("  <white>" + r.getName() + " <dark_gray>[" + r.getId().substring(0, 8) + "] <gray>" + String.format("%.2f", r.getAreaSquareKm()) + " km²"));
                    }
                }
            }
            case "info" -> {
                if (args.length < 2) { sender.sendMessage(mm.deserialize(prefix + "<red>Usage: /georegion info <name>")); return true; }
                String name = String.join(" ", java.util.Arrays.copyOfRange(args, 1, args.length));
                plugin.getAPI().getRegionManager().getRegionByName(name).ifPresentOrElse(r -> {
                    GeoPoint center = r.getCentroid();
                    int dp = plugin.getGeoConfig().getDecimalPlaces();
                    sender.sendMessage(mm.deserialize(prefix + "<gold>" + r.getName()));
                    sender.sendMessage(mm.deserialize("  <gray>ID:       <dark_gray>" + r.getId()));
                    sender.sendMessage(mm.deserialize("  <gray>Vertices: <white>" + r.getVertices().size()));
                    sender.sendMessage(mm.deserialize("  <gray>Area:     <white>" + String.format("%.4f", r.getAreaSquareKm()) + " km²"));
                    sender.sendMessage(mm.deserialize("  <gray>Centroid: <aqua>" + center.format(dp)));
                    sender.sendMessage(mm.deserialize("  <gray>DMS:      <aqua>" + center.formatDMS()));
                }, () -> sender.sendMessage(mm.deserialize(prefix + "<red>Region not found: " + name)));
            }
            default -> sender.sendMessage(mm.deserialize(prefix + "<red>Unknown subcommand."));
        }
        return true;
    }

    private boolean handleMatrix(CommandSender sender, String[] args) {
        if (!sender.hasPermission("geolocate.use")) { sendMessage(sender, "no-permission"); return true; }

        String prefix = plugin.getGeoConfig().getPrefix();
        DistanceMatrix matrix = plugin.getAPI().buildDistanceMatrix(Bukkit.getOnlinePlayers());

        if (matrix.getLabels().size() < 2) {
            sender.sendMessage(mm.deserialize(prefix + "<yellow>At least 2 online players in mapped worlds required."));
            return true;
        }

        sender.sendMessage(mm.deserialize(prefix + "<gold>Real-world distance matrix"));
        sender.sendMessage(mm.deserialize("  <gray>Players:  <white>" + matrix.getLabels().size()));
        sender.sendMessage(mm.deserialize("  <gray>Avg dist: <white>" + formatDistance(matrix.getAverageDistance())));

        if (args.length >= 1) {
            String target = args[0];
            Map<String, Double> distances = matrix.getDistancesFrom(target);
            if (distances.isEmpty()) {
                sender.sendMessage(mm.deserialize(prefix + "<red>Player not found in matrix: " + target));
                return true;
            }
            sender.sendMessage(mm.deserialize(prefix + "<white>Distances from <aqua>" + target + "<white>:"));
            distances.entrySet().stream()
                    .sorted(Map.Entry.comparingByValue())
                    .forEach(e -> sender.sendMessage(mm.deserialize("  <gray>" + e.getKey() + ": <white>" + formatDistance(e.getValue()))));
        } else {
            List<String> labels = matrix.getLabels();
            for (String label : labels) {
                String closest = matrix.getClosestTo(label);
                if (closest != null) {
                    double dist = matrix.getDistance(label, closest);
                    sender.sendMessage(mm.deserialize("  <aqua>" + label + " <gray>-> <white>" + closest + " <dark_gray>(" + formatDistance(dist) + ")"));
                }
            }
        }
        return true;
    }

    private boolean handleExport(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) { sendMessage(sender, "player-only"); return true; }
        if (!sender.hasPermission("geolocate.use")) { sendMessage(sender, "no-permission"); return true; }

        String prefix = plugin.getGeoConfig().getPrefix();

        if (args.length == 0) {
            sender.sendMessage(mm.deserialize(prefix + "<gold>Export formats: csv, geojson, gpx, kml"));
            sender.sendMessage(mm.deserialize("  <gray>Usage: /geoexport <format>"));
            return true;
        }

        String format = args[0].toLowerCase();
        String content;

        content = switch (format) {
            case "csv" -> plugin.getAPI().exportHistoryCSV(player);
            case "geojson" -> plugin.getAPI().exportHistoryGeoJSON(player);
            case "gpx" -> plugin.getAPI().exportPathGPX(player);
            case "kml" -> plugin.getAPI().exportPathKML(player);
            default -> null;
        };

        if (content == null) {
            sender.sendMessage(mm.deserialize(prefix + "<red>Unknown format. Use: csv, geojson, gpx, kml"));
            return true;
        }

        try {
            File file = GeoExporter.buildExportFile(plugin.getDataFolder(), player.getName(), format);
            GeoExporter.saveToFile(file, content);
            sender.sendMessage(mm.deserialize(prefix + "<green>Exported to: <white>" + file.getPath()));
        } catch (IOException e) {
            sender.sendMessage(mm.deserialize(prefix + "<red>Export failed: " + e.getMessage()));
            plugin.getLogger().severe("Export failed for " + player.getName() + ": " + e.getMessage());
        }

        return true;
    }

    private boolean handleStats(CommandSender sender) {
        if (!sender.hasPermission("geolocate.admin")) { sendMessage(sender, "no-permission"); return true; }

        String prefix = plugin.getGeoConfig().getPrefix();
        sender.sendMessage(mm.deserialize(prefix + "<gold>GeoLocate Statistics"));
        sender.sendMessage(mm.deserialize("  <gray>Mapped worlds:    <white>" + plugin.getWorldMapper().getConfiguredWorldCount()));
        sender.sendMessage(mm.deserialize("  <gray>Registered regions: <white>" + plugin.getAPI().getRegionManager().getRegionCount()));
        sender.sendMessage(mm.deserialize("  <gray>Heatmap cells:    <white>" + plugin.getAPI().getGlobalHeatmap().getUniqueCellCount()));
        sender.sendMessage(mm.deserialize("  <gray>Total recordings: <white>" + plugin.getAPI().getGlobalHeatmap().getTotalRecordings()));

        GeoPoint hotspot = plugin.getAPI().getGlobalHeatmap().getMostVisited();
        if (hotspot != null) {
            sender.sendMessage(mm.deserialize("  <gray>Top hotspot:      <aqua>" + hotspot.format(plugin.getGeoConfig().getDecimalPlaces())));
        }
        return true;
    }

    private boolean handleConvert(CommandSender sender, String[] args) {
        if (!sender.hasPermission("geolocate.use")) { sendMessage(sender, "no-permission"); return true; }
        if (!(sender instanceof Player player)) { sendMessage(sender, "player-only"); return true; }
        if (args.length < 2) {
            sender.sendMessage(mm.deserialize(plugin.getGeoConfig().getPrefix() + "<yellow>Usage: /geoconvert <latitude> <longitude>"));
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

        if (lat < -90 || lat > 90 || lon < -180 || lon > 180) { sendMessage(sender, "invalid-coordinates"); return true; }
        if (!plugin.getWorldMapper().isWorldMapped(player.getWorld().getName())) { sendMessage(sender, "world-not-configured"); return true; }
        if (isOnCooldown(sender)) return true;

        double[] mc = plugin.getWorldMapper().getConverter(player.getWorld().getName())
                .map(c -> c.convertToMinecraft(lat, lon))
                .orElse(null);

        if (mc == null) { sendMessage(sender, "world-not-configured"); return true; }

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
        if (!(sender instanceof Player self)) { sendMessage(sender, "player-only"); return true; }
        if (!sender.hasPermission("geolocate.use")) { sendMessage(sender, "no-permission"); return true; }
        if (args.length < 1) {
            sender.sendMessage(mm.deserialize(plugin.getGeoConfig().getPrefix() + "<yellow>Usage: /geodistance <player>"));
            return true;
        }

        Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null) { sendMessage(sender, "player-not-found"); return true; }
        if (!plugin.getWorldMapper().isWorldMapped(self.getWorld().getName())
                || !plugin.getWorldMapper().isWorldMapped(target.getWorld().getName())) {
            sendMessage(sender, "distance-same-world"); return true;
        }
        if (!self.getWorld().getName().equals(target.getWorld().getName())) {
            sendMessage(sender, "distance-same-world"); return true;
        }
        if (isOnCooldown(sender)) return true;

        Optional<GeoPoint> selfPoint = plugin.getAPI().getGeoPoint(self);
        Optional<GeoPoint> targetPoint = plugin.getAPI().getGeoPoint(target);

        if (selfPoint.isEmpty() || targetPoint.isEmpty()) { sendMessage(sender, "world-not-configured"); return true; }

        GeoPoint a = selfPoint.get();
        GeoPoint b = targetPoint.get();
        double distMeters = a.distanceTo(b);
        double rhumbMeters = a.rhumbDistanceTo(b);
        double bearing = a.bearingTo(b);
        String cardinal = a.bearingCardinal(b);
        GeoPoint mid = a.midpointTo(b);
        int dp = plugin.getGeoConfig().getDecimalPlaces();

        String prefix = plugin.getGeoConfig().getPrefix();
        sender.sendMessage(mm.deserialize(prefix + "<white>Distance to <aqua>" + target.getName() + "<white>:"));
        sender.sendMessage(mm.deserialize("  <gray>Great-circle: <white>" + formatDistance(distMeters)));
        sender.sendMessage(mm.deserialize("  <gray>Rhumb line:   <white>" + formatDistance(rhumbMeters)));
        sender.sendMessage(mm.deserialize("  <gray>Bearing:      <white>" + String.format("%.1f", bearing) + "° " + cardinal));
        sender.sendMessage(mm.deserialize("  <gray>Midpoint:     <aqua>" + mid.format(dp)));
        return true;
    }

    private boolean handleNotify(CommandSender sender) {
        if (!(sender instanceof Player player)) { sendMessage(sender, "player-only"); return true; }
        if (!sender.hasPermission("geolocate.notify.toggle")) { sendMessage(sender, "no-permission"); return true; }
        boolean now = plugin.getPreferenceStorage().toggleNotify(player.getUniqueId());
        sendMessage(sender, now ? "notifications-enabled" : "notifications-disabled");
        return true;
    }

    private boolean handleActionBar(CommandSender sender) {
        if (!(sender instanceof Player player)) { sendMessage(sender, "player-only"); return true; }
        if (!sender.hasPermission("geolocate.actionbar")) { sendMessage(sender, "no-permission"); return true; }
        if (!plugin.getGeoConfig().isActionBarEnabled()) {
            sender.sendMessage(mm.deserialize(plugin.getGeoConfig().getPrefix() + "<yellow>ActionBar display is disabled in config."));
            return true;
        }
        boolean now = plugin.getPreferenceStorage().toggleActionBar(player.getUniqueId());
        sender.sendMessage(mm.deserialize(plugin.getGeoConfig().getPrefix()
                + "<white>ActionBar display " + (now ? "<green>enabled" : "<red>disabled") + "<white>."));
        return true;
    }

    private boolean handleAdmin(CommandSender sender, String[] args) {
        if (!sender.hasPermission("geolocate.admin")) { sendMessage(sender, "no-permission"); return true; }
        if (args.length == 0) { sendAdminHelp(sender); return true; }

        switch (args[0].toLowerCase()) {
            case "reload" -> { plugin.reload(); sendMessage(sender, "reload-success"); }
            case "info" -> sendInfo(sender);
            case "clearcache" -> {
                plugin.getWorldMapper().clearAllCaches();
                sender.sendMessage(mm.deserialize(plugin.getGeoConfig().getPrefix() + "<green>All caches cleared."));
            }
            case "stats" -> handleStats(sender);
            default -> sendAdminHelp(sender);
        }
        return true;
    }

    private void sendAdminHelp(CommandSender sender) {
        String prefix = plugin.getGeoConfig().getPrefix();
        sender.sendMessage(mm.deserialize(prefix + "<gold>GeoLocate Admin Commands"));
        sender.sendMessage(mm.deserialize("  <gray>/geoadmin reload     <white>- Reload configuration"));
        sender.sendMessage(mm.deserialize("  <gray>/geoadmin info       <white>- Plugin information"));
        sender.sendMessage(mm.deserialize("  <gray>/geoadmin clearcache <white>- Clear coordinate caches"));
        sender.sendMessage(mm.deserialize("  <gray>/geoadmin stats      <white>- Runtime statistics"));
    }

    private void sendInfo(CommandSender sender) {
        String prefix = plugin.getGeoConfig().getPrefix();
        sender.sendMessage(mm.deserialize(prefix + "<gold>GeoLocate v" + plugin.getDescription().getVersion()));
        sender.sendMessage(mm.deserialize("  <gray>Worlds:    <white>" + plugin.getWorldMapper().getConfiguredWorldCount()));
        sender.sendMessage(mm.deserialize("  <gray>Regions:   <white>" + plugin.getAPI().getRegionManager().getRegionCount()));
        sender.sendMessage(mm.deserialize("  <gray>Decimals:  <white>" + plugin.getGeoConfig().getDecimalPlaces()));
        sender.sendMessage(mm.deserialize("  <gray>Zoom:      <white>" + plugin.getGeoConfig().getGoogleMapsZoom()));
        sender.sendMessage(mm.deserialize("  <gray>Cooldown:  <white>" + plugin.getGeoConfig().getCommandCooldownSeconds() + "s"));
        sender.sendMessage(mm.deserialize("  <gray>ActionBar: <white>" + (plugin.getGeoConfig().isActionBarEnabled() ? "<green>on" : "<red>off")));
    }

    private void sendMessage(CommandSender sender, String key) {
        sender.sendMessage(mm.deserialize(plugin.getGeoConfig().getPrefix() + plugin.getGeoConfig().getMessage(key)));
    }

    private String formatCoord(double value, int dp) {
        return String.format("%." + dp + "f", value);
    }

    private String formatDistance(double meters) {
        if (meters >= 1000) return String.format("%.2f km", meters / 1000);
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
                    List.of("reload", "info", "clearcache", "stats").stream()
                            .filter(s -> s.startsWith(args[0].toLowerCase()))
                            .forEach(suggestions::add);
                }
            }
            case "geodistance", "geolocate", "geomatrix" -> {
                if (args.length == 1 && sender.hasPermission("geolocate.others")) {
                    Bukkit.getOnlinePlayers().stream()
                            .map(Player::getName)
                            .filter(n -> n.toLowerCase().startsWith(args[0].toLowerCase()))
                            .forEach(suggestions::add);
                }
            }
            case "geoconvert" -> {
                if (args.length == 1) suggestions.add("<latitude>");
                if (args.length == 2) suggestions.add("<longitude>");
            }
            case "geopath" -> {
                if (args.length == 1) {
                    List.of("info", "clear", "snap").stream()
                            .filter(s -> s.startsWith(args[0].toLowerCase()))
                            .forEach(suggestions::add);
                }
            }
            case "georegion" -> {
                if (args.length == 1) {
                    List.of("current", "list", "info").stream()
                            .filter(s -> s.startsWith(args[0].toLowerCase()))
                            .forEach(suggestions::add);
                }
            }
            case "geoexport" -> {
                if (args.length == 1) {
                    List.of("csv", "geojson", "gpx", "kml").stream()
                            .filter(s -> s.startsWith(args[0].toLowerCase()))
                            .forEach(suggestions::add);
                }
            }
        }
        return suggestions;
    }
}
