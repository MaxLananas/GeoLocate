# API Usage

GeoLocate exposes a clean API for other plugins to consume.

## Setup

Add GeoLocate as a dependency in your `plugin.yml`:

```yaml
depend:
  - GeoLocate
```

## Getting a GeoPoint

```java
import dev.geolocate.api.GeoLocateAPI;
import dev.geolocate.mapping.GeoPoint;

public class MyPlugin extends JavaPlugin {

    public void someMethod(Player player) {
        GeoLocateAPI api = GeoLocateAPI.get();

        api.getGeoPoint(player).ifPresent(point -> {
            double lat = point.getLatitude();
            double lon = point.getLongitude();
            double alt = point.getAltitude();

            player.sendMessage("You are at " + lat + ", " + lon);
        });
    }
}
```

## Getting a Google Maps Link

```java
GeoLocateAPI api = GeoLocateAPI.get();

api.getGoogleMapsLink(player).ifPresent(link -> {
    player.sendMessage("View on Maps: " + link);
});
```

## Converting a Specific Location

```java
GeoLocateAPI api = GeoLocateAPI.get();

Location loc = new Location(world, 1500, 64, -3000);
Optional<GeoPoint> point = api.getGeoPoint(loc);
```

## Checking if a World is Mapped

```java
GeoLocateAPI api = GeoLocateAPI.get();

if (api.isWorldMapped(player.getWorld())) {
    // safe to request coordinates
}
```

## GeoPoint Methods

| Method | Return | Description |
|---|---|---|
| `getLatitude()` | `double` | Latitude in decimal degrees |
| `getLongitude()` | `double` | Longitude in decimal degrees |
| `getAltitude()` | `double` | Normalized Minecraft Y level |
| `isValid()` | `boolean` | Whether the point is within valid bounds |
| `distanceTo(GeoPoint)` | `double` | Great-circle distance in meters |
| `format(int)` | `String` | Formatted coordinate string |

## GoogleMapsLink Utility

```java
import dev.geolocate.util.GoogleMapsLink;

String link = GoogleMapsLink.build(point, 15);
String simple = GoogleMapsLink.buildSimple(point);
String labeled = GoogleMapsLink.buildWithLabel(point, "My Base");
```
