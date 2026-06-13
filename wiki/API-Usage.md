# API Usage

GeoLocate exposes a comprehensive API for other plugins to consume.

## Setup

Add GeoLocate as a dependency in your `plugin.yml`:

```yaml
depend:
  - GeoLocate
```

## Basic Coordinate Lookup

```java
GeoLocateAPI api = GeoLocateAPI.get();

api.getGeoPoint(player).ifPresent(point -> {
    double lat = point.latitude();
    double lon = point.longitude();
    double alt = point.altitude();
    player.sendMessage("You are at " + lat + ", " + lon);
});
```

## Async Lookup

```java
api.getGeoPointAsync(player).thenAccept(opt -> {
    opt.ifPresent(point -> {
        // runs off the main thread
    });
});
```

## Map Links

```java
api.getGoogleMapsLink(player).ifPresent(url -> player.sendMessage(url));
api.getOpenStreetMapLink(player).ifPresent(url -> player.sendMessage(url));
api.getAppleMapsLink(player).ifPresent(url -> player.sendMessage(url));
```

## Regions

```java
// Define a region
List<GeoPoint> vertices = List.of(
    new GeoPoint(48.85, 2.29),
    new GeoPoint(48.86, 2.29),
    new GeoPoint(48.86, 2.31),
    new GeoPoint(48.85, 2.31)
);
GeoRegion paris = new GeoRegion("Paris Center", vertices);
api.registerRegion(paris);

// Check which regions a player is in
List<GeoRegion> current = api.getRegionsAt(player);

// Listen for region events
@EventHandler
public void onEnter(PlayerEnterGeoRegionEvent e) {
    e.getPlayer().sendMessage("You entered " + e.getRegion().getName());
}

@EventHandler
public void onLeave(PlayerLeaveGeoRegionEvent e) {
    e.getPlayer().sendMessage("You left " + e.getRegion().getName());
}
```

## Region Utilities

```java
GeoRegion region = new GeoRegion("Zone", vertices);

// Area in square kilometers
double area = region.getAreaSquareKm();

// Geographic center
GeoPoint center = region.getCentroid();

// Check if a specific point is inside
boolean inside = region.contains(new GeoPoint(48.855, 2.30));

// Distance from a point to the nearest border edge in meters
double borderDist = region.distanceToBorder(playerPoint);

// Check if two regions overlap
boolean overlaps = regionA.intersects(regionB);
```

## Player History

```java
PlayerGeoHistory history = api.getHistory(player);

// Record a snapshot right now
api.recordSnapshot(player);

// Get total distance traveled in meters
double distance = history.getTotalDistanceTraveled();

// Get farthest point from spawn
GeoPoint farthest = history.getFarthestPointFrom(new GeoPoint(0, 0)).orElse(null);

// Export as CSV or GeoJSON
String csv = api.exportHistoryCSV(player);
String geoJson = api.exportHistoryGeoJSON(player);
```

## Path Export

```java
// GPX (compatible with GPS devices, Google Earth)
String gpx = api.exportPathGPX(player);

// KML (compatible with Google Earth, Maps)
String kml = api.exportPathKML(player);

// Save to disk
File file = GeoExporter.buildExportFile(plugin.getDataFolder(), player.getName(), "gpx");
GeoExporter.saveToFile(file, gpx);
```

## GeoPath

```java
GeoPath path = api.getPlayerPath(player);

double totalMeters = path.getTotalDistanceMeters();
GeoPoint halfway = path.interpolate(0.5);
GeoPoint nearest = path.getNearestPoint(someTarget);
double distToPath = path.getDistanceToPath(somePoint);
```

## Distance Matrix

```java
// Compute real-world distances between all online players
DistanceMatrix matrix = api.buildDistanceMatrix(Bukkit.getOnlinePlayers());

double dist = matrix.getDistance("PlayerA", "PlayerB");
String closest = matrix.getClosestTo("PlayerA");
String farthest = matrix.getFarthestTo("PlayerA");
double avg = matrix.getAverageDistance();

Map<String, Double> all = matrix.getDistancesFrom("PlayerA");
```

## Heatmap

```java
GeoHeatmap heatmap = api.getGlobalHeatmap();

// Record a position manually
heatmap.record(point);

// Get top 5 most visited areas
List<GeoPoint> hotspots = heatmap.getHotspots(5);

// Most visited single cell
GeoPoint top = heatmap.getMostVisited();

// Export as JSON for web visualization
String json = heatmap.toJSON();
```

## Border Detection

```java
api.getBorderDetector(player).ifPresent(detector -> {
    boolean nearEdge = detector.isNearBorder(point, 5.0);
    GeoBorderDetector.BorderSide side = detector.getNearestBorderSide(point);
    double metersToEdge = detector.getDistanceToBorderMeters(point);
    double percentFromCenter = detector.getPercentageFromCenter(point);
});
```

## PlaceholderAPI Placeholders

| Placeholder | Description |
|---|---|
| `%geolocate_latitude%` | Player latitude |
| `%geolocate_longitude%` | Player longitude |
| `%geolocate_altitude%` | Player Minecraft Y level |
| `%geolocate_coords%` | Formatted `lat, lon` string |
| `%geolocate_maps_link%` | Google Maps URL |
| `%geolocate_world%` | Current world name |
| `%geolocate_is_mapped%` | `true` or `false` |

## GeoPoint Methods

| Method | Return | Description |
|---|---|---|
| `latitude()` | `double` | Latitude in decimal degrees |
| `longitude()` | `double` | Longitude in decimal degrees |
| `altitude()` | `double` | Normalized Minecraft Y level |
| `isValid()` | `boolean` | Whether the point is within valid bounds |
| `distanceTo(GeoPoint)` | `double` | Great-circle distance in meters |
| `format(int)` | `String` | Formatted coordinate string |
