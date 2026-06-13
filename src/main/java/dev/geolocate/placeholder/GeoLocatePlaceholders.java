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

    public GeoLocatePlaceholders(GeoLocate plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "geolocate";
    }

    @Override
    public @NotNull String getAuthor() {
        return "MaxLananas";
    }

    @Override
    public @NotNull String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onPlaceholderRequest(Player player, @NotNull String params) {
        if (player == null) return "";

        Optional<GeoPoint> optPoint = plugin.getAPI().getGeoPoint(player);
        if (optPoint.isEmpty()) return "N/A";

        GeoPoint point = optPoint.get();
        int dp = plugin.getGeoConfig().getDecimalPlaces();

        return switch (params.toLowerCase()) {
            case "latitude" -> String.format("%." + dp + "f", point.getLatitude());
            case "longitude" -> String.format("%." + dp + "f", point.getLongitude());
            case "altitude" -> String.valueOf(point.getAltitude());
            case "coords" -> String.format("%." + dp + "f, %." + dp + "f",
                    point.getLatitude(), point.getLongitude());
            case "maps_link" -> GoogleMapsLink.build(point, plugin.getGeoConfig().getGoogleMapsZoom());
            case "world" -> player.getWorld().getName();
            case "is_mapped" -> plugin.getWorldMapper().isWorldMapped(player.getWorld().getName()) ? "true" : "false";
            default -> "";
        };
    }
}
