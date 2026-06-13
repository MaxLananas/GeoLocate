package dev.geolocate.placeholder;

import dev.geolocate.GeoLocate;
import dev.geolocate.mapping.GeoPoint;
import dev.geolocate.util.GoogleMapsLink;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

public final class GeoLocatePlaceholders extends PlaceholderExpansion {

    private final GeoLocate plugin;
    // Cached format string — rebuilt only on reload
    private String fmtString;

    public GeoLocatePlaceholders(GeoLocate plugin) {
        this.plugin     = plugin;
        this.fmtString  = buildFmt(plugin.getGeoConfig().getDecimalPlaces());
    }

    private static String buildFmt(int dp) {
        return "%." + dp + "f";
    }

    @Override public @NotNull String getIdentifier() { return "geolocate"; }
    @Override public @NotNull String getAuthor()     { return "MaxLananas"; }
    @Override public @NotNull String getVersion()    { return plugin.getDescription().getVersion(); }
    @Override public boolean persist()               { return true; }

    @Override
    public String onPlaceholderRequest(Player player, @NotNull String params) {
        if (player == null) return "";

        Optional<GeoPoint> optPoint = plugin.getAPI().getGeoPoint(player);
        if (optPoint.isEmpty()) return "N/A";

        GeoPoint point = optPoint.get();
        int dp = plugin.getGeoConfig().getDecimalPlaces();
        // Refresh format string if decimal-places config changed at runtime
        if (fmtString.length() != dp + 3) fmtString = buildFmt(dp);

        return switch (params.toLowerCase()) {
            case "latitude"  -> String.format(fmtString, point.latitude());
            case "longitude" -> String.format(fmtString, point.longitude());
            case "altitude"  -> String.valueOf(point.altitude());
            case "coords"    -> String.format(fmtString + ", " + fmtString,
                                    point.latitude(), point.longitude());
            case "maps_link" -> GoogleMapsLink.build(point, plugin.getGeoConfig().getGoogleMapsZoom());
            case "world"     -> player.getWorld().getName();
            case "is_mapped" -> plugin.getWorldMapper().isWorldMapped(player.getWorld().getName())
                                    ? "true" : "false";
            default          -> "";
        };
    }
}
