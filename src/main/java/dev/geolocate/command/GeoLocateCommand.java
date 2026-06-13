package dev.geolocate.command;

import dev.geolocate.GeoLocate;
import dev.geolocate.export.GeoExporter;
import dev.geolocate.history.PlayerGeoHistory;
import dev.geolocate.mapping.GeoPoint;
import dev.geolocate.model.DistanceMatrix;
import dev.geolocate.model.GeoPath;
import dev.geolocate.model.GeoRegion;
import dev.geolocate.util.GoogleMapsLink;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
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
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class GeoLocateCommand implements CommandExecutor, TabCompleter {
    private static final List<String> ADMIN_SUBS  = List.of("reload", "info", "clearcache", "stats");
    private static final List<String> PATH_SUBS   = List.of("info", "clear", "snap");
    private static final List<String> REGION_SUBS = List.of("current", "list", "info");
    private static final List<String> EXPORT_SUBS = List.of("csv", "geojson", "gpx", "kml");

    /**
     * Primary gradient header: cyan -> green, used for the plugin name in
     * all outputs.
     */
    private static final String HEADER =
            "<dark_gray>[<gradient:#00C9FF:#92FE9D><bold>GeoLocate</bold></gradient>]</dark_gray> ";

    /** Thin separator line used between sections. */
    private static final String SEPARATOR =
            "<dark_gray>━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━</dark_gray>";

    /** Label style: muted gray for field names. */
    private static final String LBL = "<gray>";
    /** Value style: white for values. */
    private static final String VAL = "</gray><white>";
    private static final String END = "</white>";

    private final GeoLocate plugin;
    private final MiniMessage mm;
    private final ConcurrentHashMap<UUID, Long> cooldowns;

    public GeoLocateCommand(GeoLocate plugin) {
        this.plugin    = plugin;
        this.mm        = MiniMessage.miniMessage();
        this.cooldowns = new ConcurrentHashMap<>();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        return switch (command.getName().toLowerCase()) {
            case "geoadmin"     -> handleAdmin(sender, args);
            case "geoconvert"   -> handleConvert(sender, args);
            case "geodistance"  -> handleDistance(sender, args);
            case "geonotify"    -> handleNotify(sender);
            case "geoactionbar" -> handleActionBar(sender);
            case "geopath"      -> handlePath(sender, args);
            case "georegion"    -> handleRegion(sender, args);
            case "geomatrix"    -> handleMatrix(sender, args);
            case "geoexport"    -> handleExport(sender, args);
            case "geostats"     -> handleStats(sender);
            default             -> handleGeoLocate(sender, args);
        };
    }

    private boolean isOnCooldown(CommandSender sender) {
        if (!(sender instanceof Player player)) return false;
        if (sender.hasPermission("geolocate.admin")) return false;
        int seconds = plugin.getGeoConfig().getCommandCooldownSeconds();
        if (seconds <= 0) return false;

        long now       = System.currentTimeMillis();
        long cooldownMs = (long) seconds * 1000L;
        Long last      = cooldowns.get(player.getUniqueId());

        if (last != null && now - last < cooldownMs) {
            long remaining = (cooldownMs - (now - last)) / 1000L + 1;
            send(sender,
                HEADER + "<red>Please wait <white>" + remaining + "s</white> before using this command again.");
            return true;
        }
        cooldowns.put(player.getUniqueId(), now);
        return false;
    }

    private boolean handleGeoLocate(CommandSender sender, String[] args) {
        Player target;
        if (args.length >= 1) {
            if (!sender.hasPermission("geolocate.others")) { perm(sender); return true; }
            target = Bukkit.getPlayerExact(args[0]);
            if (target == null) { notFound(sender); return true; }
        } else {
            if (!(sender instanceof Player p)) { playerOnly(sender); return true; }
            if (!sender.hasPermission("geolocate.use")) { perm(sender); return true; }
            target = p;
        }

        if (isOnCooldown(sender)) return true;
        if (!plugin.getWorldMapper().isWorldMapped(target.getWorld().getName())) {
            notMapped(sender); return true;
        }

        Optional<GeoPoint> optPoint = plugin.getAPI().getGeoPoint(target);
        if (optPoint.isEmpty()) { notMapped(sender); return true; }

        GeoPoint point = optPoint.get();
        int      dp    = plugin.getGeoConfig().getDecimalPlaces();
        int      zoom  = plugin.getGeoConfig().getGoogleMapsZoom();
        String   mapsLink = GoogleMapsLink.build(point, zoom);
        String   osmLink  = GoogleMapsLink.buildOpenStreetMap(point, zoom);
        String   appleLink= GoogleMapsLink.buildAppleMaps(point, zoom);
        boolean  self     = sender.equals(target);

        String title = self
                ? HEADER + "<gradient:#00C9FF:#92FE9D>Your Location</gradient>"
                : HEADER + "<gradient:#00C9FF:#92FE9D>" + target.getName() + "'s Location</gradient>";

        sender.sendMessage(mm.deserialize(SEPARATOR));
        sender.sendMessage(mm.deserialize(title));
        sender.sendMessage(mm.deserialize(SEPARATOR));

        sender.sendMessage(mm.deserialize(
                LBL + "  Latitude  " + VAL + formatCoord(point.latitude(),  dp) + END));
        sender.sendMessage(mm.deserialize(
                LBL + "  Longitude " + VAL + formatCoord(point.longitude(), dp) + END));
        sender.sendMessage(mm.deserialize(
                LBL + "  DMS       " + VAL + point.formatDMS() + END));

        if (plugin.getGeoConfig().isShowAltitude()) {
            sender.sendMessage(mm.deserialize(
                    LBL + "  Altitude  " + VAL + point.altitude() + "m" + END));
        }

        List<GeoRegion> regions = plugin.getAPI().getRegionsAt(point);
        if (!regions.isEmpty()) {
            String names = String.join(", ", regions.stream().map(GeoRegion::getName).toList());
            sender.sendMessage(mm.deserialize(
                    LBL + "  Regions   <gold>" + names + "</gold>"));
        }

        sender.sendMessage(mm.deserialize(SEPARATOR));

        // Clickable map links with hover tooltips
        Component mapLine = mm.deserialize(
                "  <dark_gray>» </dark_gray>" +
                "<click:open_url:'" + mapsLink + "'>" +
                "<hover:show_text:'<gray>Open in Google Maps<newline><dark_gray>" + mapsLink + "'>" +
                "<gradient:#4285F4:#34A853>Google Maps</gradient></hover></click>" +
                "  <dark_gray>|</dark_gray>  " +
                "<click:open_url:'" + osmLink + "'>" +
                "<hover:show_text:'<gray>Open in OpenStreetMap<newline><dark_gray>" + osmLink + "'>" +
                "<gradient:#7EBC6F:#4A90D9>OpenStreetMap</gradient></hover></click>" +
                "  <dark_gray>|</dark_gray>  " +
                "<click:open_url:'" + appleLink + "'>" +
                "<hover:show_text:'<gray>Open in Apple Maps<newline><dark_gray>" + appleLink + "'>" +
                "<gradient:#FC3158:#FF6B6B>Apple Maps</gradient></hover></click>"
        );
        sender.sendMessage(mapLine);

        // Copy-to-clipboard button
        String coords = formatCoord(point.latitude(), dp) + ", " + formatCoord(point.longitude(), dp);
        Component copyBtn = mm.deserialize(
                "  <dark_gray>» </dark_gray>" +
                "<click:copy_to_clipboard:'" + coords + "'>" +
                "<hover:show_text:'<gray>Click to copy coordinates to clipboard'>" +
                "<dark_gray>[</dark_gray><white>Copy Coordinates</white><dark_gray>]</dark_gray>" +
                "</hover></click>"
        );
        sender.sendMessage(copyBtn);
        sender.sendMessage(mm.deserialize(SEPARATOR));
        return true;
    }

    private boolean handlePath(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) { playerOnly(sender); return true; }
        if (!sender.hasPermission("geolocate.use")) { perm(sender); return true; }

        if (args.length == 0) {
            sender.sendMessage(mm.deserialize(SEPARATOR));
            sender.sendMessage(mm.deserialize(HEADER + "<gradient:#00C9FF:#92FE9D>Path Commands</gradient>"));
            sender.sendMessage(mm.deserialize(SEPARATOR));
            helpEntry(sender, "/geopath info",  "Show your path statistics");
            helpEntry(sender, "/geopath clear", "Clear your movement history");
            helpEntry(sender, "/geopath snap",  "Record a snapshot immediately");
            sender.sendMessage(mm.deserialize(SEPARATOR));
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "info" -> {
                GeoPath path = plugin.getAPI().getPlayerPath(player);
                PlayerGeoHistory history = plugin.getAPI().getHistory(player);
                int dp = plugin.getGeoConfig().getDecimalPlaces();

                sender.sendMessage(mm.deserialize(SEPARATOR));
                sender.sendMessage(mm.deserialize(HEADER + "<gradient:#00C9FF:#92FE9D>Geo Path</gradient>"));
                sender.sendMessage(mm.deserialize(SEPARATOR));
                sender.sendMessage(mm.deserialize(LBL + "  Snapshots  " + VAL + history.size() + END));
                sender.sendMessage(mm.deserialize(LBL + "  Distance   " + VAL + formatDistance(history.getTotalDistanceTraveled()) + END));

                if (!path.isEmpty()) {
                    GeoPoint first = path.getFirst();
                    GeoPoint last  = path.getLast();
                    sender.sendMessage(mm.deserialize(LBL + "  Start      <aqua>" + first.format(dp)));
                    sender.sendMessage(mm.deserialize(LBL + "  Current    <aqua>" + last.format(dp)));
                    sender.sendMessage(mm.deserialize(LBL + "  Bearing    " + VAL
                            + String.format("%.1f", first.bearingTo(last)) + "° "
                            + first.bearingCardinal(last) + END));
                }
                sender.sendMessage(mm.deserialize(SEPARATOR));
            }
            case "clear" -> {
                plugin.getAPI().clearHistory(player.getUniqueId());
                success(sender, "Path history cleared.");
            }
            case "snap" -> {
                plugin.getAPI().recordSnapshot(player);
                success(sender, "Snapshot recorded.");
            }
            default -> error(sender, "Unknown subcommand. Use: info, clear, snap");
        }
        return true;
    }

    private boolean handleRegion(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) { playerOnly(sender); return true; }
        if (!sender.hasPermission("geolocate.use")) { perm(sender); return true; }

        if (args.length == 0) {
            sender.sendMessage(mm.deserialize(SEPARATOR));
            sender.sendMessage(mm.deserialize(HEADER + "<gradient:#00C9FF:#92FE9D>Region Commands</gradient>"));
            sender.sendMessage(mm.deserialize(SEPARATOR));
            helpEntry(sender, "/georegion current",      "Show regions you are currently in");
            helpEntry(sender, "/georegion list",         "List all registered geo-regions");
            helpEntry(sender, "/georegion info <name>",  "Show detailed region information");
            sender.sendMessage(mm.deserialize(SEPARATOR));
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "current" -> {
                List<GeoRegion> regions = plugin.getAPI().getRegionsAt(player);
                if (regions.isEmpty()) {
                    warn(sender, "You are not inside any registered geo-region.");
                } else {
                    sender.sendMessage(mm.deserialize(SEPARATOR));
                    sender.sendMessage(mm.deserialize(
                            HEADER + "<gradient:#00C9FF:#92FE9D>Active Regions</gradient> "
                            + "<dark_gray>(" + regions.size() + ")</dark_gray>"));
                    sender.sendMessage(mm.deserialize(SEPARATOR));
                    for (GeoRegion r : regions) {
                        sender.sendMessage(mm.deserialize(
                                "  <dark_gray>▸</dark_gray> <gold>" + r.getName() +
                                "</gold> <dark_gray>— <gray>" + r.getVertices().size() +
                                " vertices, " + String.format("%.2f", r.getAreaSquareKm()) + " km²"));
                    }
                    sender.sendMessage(mm.deserialize(SEPARATOR));
                }
            }
            case "list" -> {
                var all = plugin.getAPI().getRegionManager().getAllRegions();
                if (all.isEmpty()) {
                    warn(sender, "No geo-regions are currently registered.");
                } else {
                    sender.sendMessage(mm.deserialize(SEPARATOR));
                    sender.sendMessage(mm.deserialize(
                            HEADER + "<gradient:#00C9FF:#92FE9D>All Regions</gradient> "
                            + "<dark_gray>(" + all.size() + ")</dark_gray>"));
                    sender.sendMessage(mm.deserialize(SEPARATOR));
                    for (GeoRegion r : all) {
                        sender.sendMessage(mm.deserialize(
                                "  <dark_gray>▸</dark_gray> <white>" + r.getName() +
                                " <dark_gray>[" + r.getId().substring(0, 8) + "]</dark_gray>" +
                                " <gray>" + String.format("%.2f", r.getAreaSquareKm()) + " km²"));
                    }
                    sender.sendMessage(mm.deserialize(SEPARATOR));
                }
            }
            case "info" -> {
                if (args.length < 2) {
                    error(sender, "Usage: /georegion info <name>"); return true;
                }
                String name = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
                plugin.getAPI().getRegionManager().getRegionByName(name).ifPresentOrElse(r -> {
                    GeoPoint centroid = r.getCentroid();
                    int dp = plugin.getGeoConfig().getDecimalPlaces();
                    sender.sendMessage(mm.deserialize(SEPARATOR));
                    sender.sendMessage(mm.deserialize(
                            HEADER + "<gradient:#00C9FF:#92FE9D>" + r.getName() + "</gradient>"));
                    sender.sendMessage(mm.deserialize(SEPARATOR));
                    sender.sendMessage(mm.deserialize(LBL + "  ID        <dark_gray>" + r.getId()));
                    sender.sendMessage(mm.deserialize(LBL + "  Vertices  " + VAL + r.getVertices().size() + END));
                    sender.sendMessage(mm.deserialize(LBL + "  Area      " + VAL + String.format("%.4f", r.getAreaSquareKm()) + " km²" + END));
                    sender.sendMessage(mm.deserialize(LBL + "  Centroid  <aqua>" + centroid.format(dp)));
                    sender.sendMessage(mm.deserialize(LBL + "  DMS       <aqua>" + centroid.formatDMS()));
                    sender.sendMessage(mm.deserialize(SEPARATOR));
                }, () -> error(sender, "Region not found: " + name));
            }
            default -> error(sender, "Unknown subcommand. Use: current, list, info");
        }
        return true;
    }

    private boolean handleMatrix(CommandSender sender, String[] args) {
        if (!sender.hasPermission("geolocate.use")) { perm(sender); return true; }

        DistanceMatrix matrix = plugin.getAPI().buildDistanceMatrix(Bukkit.getOnlinePlayers());
        if (matrix.getLabels().size() < 2) {
            warn(sender, "At least 2 online players in mapped worlds are required."); return true;
        }

        sender.sendMessage(mm.deserialize(SEPARATOR));
        sender.sendMessage(mm.deserialize(
                HEADER + "<gradient:#00C9FF:#92FE9D>Distance Matrix</gradient>"));
        sender.sendMessage(mm.deserialize(SEPARATOR));
        sender.sendMessage(mm.deserialize(LBL + "  Players   " + VAL + matrix.getLabels().size() + END));
        sender.sendMessage(mm.deserialize(LBL + "  Avg dist  " + VAL + formatDistance(matrix.getAverageDistance()) + END));
        sender.sendMessage(mm.deserialize(SEPARATOR));

        if (args.length >= 1) {
            String target = args[0];
            Map<String, Double> distances = matrix.getDistancesFrom(target);
            if (distances.isEmpty()) { error(sender, "Player not found in matrix: " + target); return true; }
            sender.sendMessage(mm.deserialize(
                    "  <gray>Distances from <gradient:#00C9FF:#92FE9D>" + target + "</gradient><gray>:"));
            distances.entrySet().stream()
                    .sorted(Map.Entry.comparingByValue())
                    .forEach(e -> sender.sendMessage(mm.deserialize(
                            "  <dark_gray>▸ <gray>" + e.getKey() +
                            " <dark_gray>— <white>" + formatDistance(e.getValue()))));
        } else {
            for (String label : matrix.getLabels()) {
                String closest = matrix.getClosestTo(label);
                if (closest != null) {
                    sender.sendMessage(mm.deserialize(
                            "  <dark_gray>▸ <gradient:#00C9FF:#92FE9D>" + label +
                            "</gradient> <dark_gray>→ <white>" + closest +
                            " <dark_gray>(" + formatDistance(matrix.getDistance(label, closest)) + ")"));
                }
            }
        }
        sender.sendMessage(mm.deserialize(SEPARATOR));
        return true;
    }

    private boolean handleExport(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) { playerOnly(sender); return true; }
        if (!sender.hasPermission("geolocate.use")) { perm(sender); return true; }

        if (args.length == 0) {
            sender.sendMessage(mm.deserialize(SEPARATOR));
            sender.sendMessage(mm.deserialize(HEADER + "<gradient:#00C9FF:#92FE9D>Export History</gradient>"));
            sender.sendMessage(mm.deserialize(SEPARATOR));
            helpEntry(sender, "/geoexport csv",     "Export as CSV spreadsheet");
            helpEntry(sender, "/geoexport geojson", "Export as GeoJSON feature collection");
            helpEntry(sender, "/geoexport gpx",     "Export as GPX track (GPS devices)");
            helpEntry(sender, "/geoexport kml",     "Export as KML (Google Earth)");
            sender.sendMessage(mm.deserialize(SEPARATOR));
            return true;
        }

        String format = args[0].toLowerCase();
        String content = switch (format) {
            case "csv"     -> plugin.getAPI().exportHistoryCSV(player);
            case "geojson" -> plugin.getAPI().exportHistoryGeoJSON(player);
            case "gpx"     -> plugin.getAPI().exportPathGPX(player);
            case "kml"     -> plugin.getAPI().exportPathKML(player);
            default        -> null;
        };

        if (content == null) {
            error(sender, "Unknown format. Valid formats: csv, geojson, gpx, kml"); return true;
        }

        try {
            File file = GeoExporter.buildExportFile(plugin.getDataFolder(), player.getName(), format);
            GeoExporter.saveToFile(file, content);
            sender.sendMessage(mm.deserialize(
                    HEADER + "<green>Exported successfully.</green> <dark_gray>→ <gray>" + file.getPath()));
        } catch (IOException e) {
            error(sender, "Export failed: " + e.getMessage());
            plugin.getLogger().severe("Export failed for " + player.getName() + ": " + e.getMessage());
        }
        return true;
    }

    private boolean handleStats(CommandSender sender) {
        if (!sender.hasPermission("geolocate.admin")) { perm(sender); return true; }

        sender.sendMessage(mm.deserialize(SEPARATOR));
        sender.sendMessage(mm.deserialize(HEADER + "<gradient:#00C9FF:#92FE9D>Runtime Statistics</gradient>"));
        sender.sendMessage(mm.deserialize(SEPARATOR));
        sender.sendMessage(mm.deserialize(LBL + "  Mapped worlds     " + VAL + plugin.getWorldMapper().getConfiguredWorldCount() + END));
        sender.sendMessage(mm.deserialize(LBL + "  Regions           " + VAL + plugin.getAPI().getRegionManager().getRegionCount() + END));
        sender.sendMessage(mm.deserialize(LBL + "  Heatmap cells     " + VAL + plugin.getAPI().getGlobalHeatmap().getUniqueCellCount() + END));
        sender.sendMessage(mm.deserialize(LBL + "  Total recordings  " + VAL + plugin.getAPI().getGlobalHeatmap().getTotalRecordings() + END));

        GeoPoint hotspot = plugin.getAPI().getGlobalHeatmap().getMostVisited();
        if (hotspot != null) {
            sender.sendMessage(mm.deserialize(
                    LBL + "  Top hotspot       <aqua>" +
                    hotspot.format(plugin.getGeoConfig().getDecimalPlaces())));
        }
        sender.sendMessage(mm.deserialize(SEPARATOR));
        return true;
    }

    private boolean handleConvert(CommandSender sender, String[] args) {
        if (!sender.hasPermission("geolocate.use")) { perm(sender); return true; }
        if (!(sender instanceof Player player)) { playerOnly(sender); return true; }
        if (args.length < 2) {
            error(sender, "Usage: /geoconvert <latitude> <longitude>"); return true;
        }

        double lat, lon;
        try {
            lat = Double.parseDouble(args[0]);
            lon = Double.parseDouble(args[1]);
        } catch (NumberFormatException e) {
            error(sender, "Invalid coordinates. Expected decimal numbers."); return true;
        }

        if (lat < -90 || lat > 90 || lon < -180 || lon > 180) {
            error(sender, "Latitude must be -90 to 90. Longitude must be -180 to 180."); return true;
        }
        if (!plugin.getWorldMapper().isWorldMapped(player.getWorld().getName())) {
            notMapped(sender); return true;
        }
        if (isOnCooldown(sender)) return true;

        double[] mc = plugin.getWorldMapper()
                .getConverter(player.getWorld().getName())
                .map(c -> c.convertToMinecraft(lat, lon))
                .orElse(null);
        if (mc == null) { notMapped(sender); return true; }

        int dp = plugin.getGeoConfig().getDecimalPlaces();
        sender.sendMessage(mm.deserialize(SEPARATOR));
        sender.sendMessage(mm.deserialize(
                HEADER + "<gradient:#00C9FF:#92FE9D>Coordinate Conversion</gradient>"));
        sender.sendMessage(mm.deserialize(SEPARATOR));
        sender.sendMessage(mm.deserialize(
                LBL + "  Real-world  <aqua>" + String.format("%." + dp + "f", lat) +
                ", " + String.format("%." + dp + "f", lon)));
        sender.sendMessage(mm.deserialize(
                LBL + "  Minecraft   " + VAL + "X " + (int) mc[0] + "  Z " + (int) mc[1] + END));
        sender.sendMessage(mm.deserialize(SEPARATOR));
        return true;
    }

    private boolean handleDistance(CommandSender sender, String[] args) {
        if (!(sender instanceof Player self)) { playerOnly(sender); return true; }
        if (!sender.hasPermission("geolocate.use")) { perm(sender); return true; }
        if (args.length < 1) {
            error(sender, "Usage: /geodistance <player>"); return true;
        }

        Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null) { notFound(sender); return true; }

        String selfWorld   = self.getWorld().getName();
        String targetWorld = target.getWorld().getName();

        if (!plugin.getWorldMapper().isWorldMapped(selfWorld)
                || !plugin.getWorldMapper().isWorldMapped(targetWorld)
                || !selfWorld.equals(targetWorld)) {
            error(sender, "Both players must be in the same mapped world."); return true;
        }
        if (isOnCooldown(sender)) return true;

        Optional<GeoPoint> selfPoint   = plugin.getAPI().getGeoPoint(self);
        Optional<GeoPoint> targetPoint = plugin.getAPI().getGeoPoint(target);
        if (selfPoint.isEmpty() || targetPoint.isEmpty()) { notMapped(sender); return true; }

        GeoPoint a = selfPoint.get();
        GeoPoint b = targetPoint.get();
        int dp = plugin.getGeoConfig().getDecimalPlaces();

        sender.sendMessage(mm.deserialize(SEPARATOR));
        sender.sendMessage(mm.deserialize(
                HEADER + "<gradient:#00C9FF:#92FE9D>Distance to " + target.getName() + "</gradient>"));
        sender.sendMessage(mm.deserialize(SEPARATOR));
        sender.sendMessage(mm.deserialize(LBL + "  Great-circle  " + VAL + formatDistance(a.distanceTo(b)) + END));
        sender.sendMessage(mm.deserialize(LBL + "  Rhumb line    " + VAL + formatDistance(a.rhumbDistanceTo(b)) + END));
        sender.sendMessage(mm.deserialize(LBL + "  Bearing       " + VAL
                + String.format("%.1f", a.bearingTo(b)) + "° " + a.bearingCardinal(b) + END));
        sender.sendMessage(mm.deserialize(LBL + "  Midpoint      <aqua>" + a.midpointTo(b).format(dp)));
        sender.sendMessage(mm.deserialize(SEPARATOR));
        return true;
    }

    private boolean handleNotify(CommandSender sender) {
        if (!(sender instanceof Player player)) { playerOnly(sender); return true; }
        if (!sender.hasPermission("geolocate.notify.toggle")) { perm(sender); return true; }
        boolean now = plugin.getPreferenceStorage().toggleNotify(player.getUniqueId());
        if (now) {
            success(sender, "Move notifications <green>enabled</green>.");
        } else {
            send(sender, HEADER + "<red>Move notifications <bold>disabled</bold>.");
        }
        return true;
    }

    private boolean handleActionBar(CommandSender sender) {
        if (!(sender instanceof Player player)) { playerOnly(sender); return true; }
        if (!sender.hasPermission("geolocate.actionbar")) { perm(sender); return true; }
        if (!plugin.getGeoConfig().isActionBarEnabled()) {
            warn(sender, "ActionBar display is globally disabled in config.yml."); return true;
        }
        boolean now = plugin.getPreferenceStorage().toggleActionBar(player.getUniqueId());
        if (now) {
            success(sender, "ActionBar display <green>enabled</green>.");
        } else {
            send(sender, HEADER + "<red>ActionBar display <bold>disabled</bold>.");
        }
        return true;
    }

    private boolean handleAdmin(CommandSender sender, String[] args) {
        if (!sender.hasPermission("geolocate.admin")) { perm(sender); return true; }
        if (args.length == 0) { sendAdminHelp(sender); return true; }

        switch (args[0].toLowerCase()) {
            case "reload" -> {
                plugin.reload();
                success(sender, "Configuration reloaded successfully.");
            }
            case "info"       -> sendInfo(sender);
            case "clearcache" -> {
                plugin.getWorldMapper().clearAllCaches();
                success(sender, "All coordinate caches cleared.");
            }
            case "stats"      -> handleStats(sender);
            default           -> sendAdminHelp(sender);
        }
        return true;
    }

    private void sendAdminHelp(CommandSender sender) {
        sender.sendMessage(mm.deserialize(SEPARATOR));
        sender.sendMessage(mm.deserialize(HEADER + "<gradient:#00C9FF:#92FE9D>Admin Commands</gradient>"));
        sender.sendMessage(mm.deserialize(SEPARATOR));
        helpEntry(sender, "/geoadmin reload",     "Reload config.yml without restart");
        helpEntry(sender, "/geoadmin info",       "Display plugin state information");
        helpEntry(sender, "/geoadmin clearcache", "Flush all coordinate caches");
        helpEntry(sender, "/geoadmin stats",      "Show runtime statistics");
        sender.sendMessage(mm.deserialize(SEPARATOR));
    }

    private void sendInfo(CommandSender sender) {
       sender.sendMessage(mm.deserialize(SEPARATOR));
       sender.sendMessage(mm.deserialize(
             HEADER + "<gradient:#00C9FF:#92FE9D>GeoLocate</gradient> "
             + "<dark_gray>v" + plugin.getPluginVersion() + "</dark_gray>"));
       sender.sendMessage(mm.deserialize(SEPARATOR));
       sender.sendMessage(mm.deserialize(LBL + "  Worlds     " + VAL + plugin.getWorldMapper().getConfiguredWorldCount() + END));
       sender.sendMessage(mm.deserialize(LBL + "  Regions    " + VAL + plugin.getAPI().getRegionManager().getRegionCount() + END));
       sender.sendMessage(mm.deserialize(LBL + "  Decimals   " + VAL + plugin.getGeoConfig().getDecimalPlaces() + END));
       sender.sendMessage(mm.deserialize(LBL + "  Zoom       " + VAL + plugin.getGeoConfig().getGoogleMapsZoom() + END));
       sender.sendMessage(mm.deserialize(LBL + "  Cooldown   " + VAL + plugin.getGeoConfig().getCommandCooldownSeconds() + "s" + END));
       sender.sendMessage(mm.deserialize(LBL + "  ActionBar  " +
             (plugin.getGeoConfig().isActionBarEnabled() ? "<green>enabled" : "<red>disabled")));
       sender.sendMessage(mm.deserialize(SEPARATOR));
    }

    private void send(CommandSender sender, String miniMessage) {
        sender.sendMessage(mm.deserialize(miniMessage));
    }

    private void success(CommandSender sender, String text) {
        send(sender, HEADER + "<green>" + text + "</green>");
    }

    private void warn(CommandSender sender, String text) {
        send(sender, HEADER + "<yellow>" + text + "</yellow>");
    }

    private void error(CommandSender sender, String text) {
        send(sender, HEADER + "<red>" + text + "</red>");
    }

    private void perm(CommandSender sender) {
        error(sender, "You do not have permission to use this command.");
    }

    private void playerOnly(CommandSender sender) {
        error(sender, "This command can only be used by players.");
    }

    private void notFound(CommandSender sender) {
        error(sender, "Player not found or is not online.");
    }

    private void notMapped(CommandSender sender) {
        warn(sender, "This world does not have a geo-mapping configured.");
    }

    private void helpEntry(CommandSender sender, String cmd, String description) {
        sender.sendMessage(mm.deserialize(
                "  <dark_gray>▸ <gradient:#00C9FF:#92FE9D>" + cmd +
                "</gradient> <dark_gray>— <gray>" + description));
    }

    private static String formatCoord(double value, int dp) {
        return String.format("%." + dp + "f", value);
    }

    private static String formatDistance(double meters) {
        return meters >= 1000
                ? String.format("%.2f km", meters / 1000.0)
                : String.format("%.0f m", meters);
    }

    public void removeCooldown(UUID uuid) {
        cooldowns.remove(uuid);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        List<String> suggestions = new ArrayList<>();
        switch (command.getName().toLowerCase()) {
            case "geoadmin"    -> { if (args.length == 1) filterInto(ADMIN_SUBS,  args[0], suggestions); }
            case "geodistance", "geolocate", "geomatrix" -> {
                if (args.length == 1 && sender.hasPermission("geolocate.others")) {
                    Bukkit.getOnlinePlayers().stream()
                            .map(Player::getName)
                            .filter(n -> n.toLowerCase().startsWith(args[0].toLowerCase()))
                            .forEach(suggestions::add);
                }
            }
            case "geoconvert"  -> {
                if (args.length == 1) suggestions.add("<latitude>");
                else if (args.length == 2) suggestions.add("<longitude>");
            }
            case "geopath"     -> { if (args.length == 1) filterInto(PATH_SUBS,   args[0], suggestions); }
            case "georegion"   -> { if (args.length == 1) filterInto(REGION_SUBS, args[0], suggestions); }
            case "geoexport"   -> { if (args.length == 1) filterInto(EXPORT_SUBS, args[0], suggestions); }
        }
        return suggestions;
    }

    private static void filterInto(List<String> source, String prefix, List<String> target) {
        String lower = prefix.toLowerCase();
        for (String s : source) {
            if (s.startsWith(lower)) target.add(s);
        }
    }
}
