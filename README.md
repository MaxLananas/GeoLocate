# GeoLocate

A Paper plugin library that converts Minecraft block coordinates to real-world
latitude and longitude, with direct Google Maps links.

Built for Paper 1.21.10+ with Java 21.

## Features

- Converts any Minecraft X/Z coordinate to a real geographic location
- Supports Mercator and Linear projections
- Per-world configuration with custom bounding boxes
- Clickable Google Maps links in chat
- Developer API for other plugins
- Coordinate caching for performance
- Optional movement notifications

## Requirements

- Paper 1.21.10 or newer
- Java 21 or newer

## Installation

1. Download the latest jar from the [Releases](../../releases) page
2. Place the jar in your server's `plugins/` folder
3. Start the server to generate `config.yml`
4. Configure your world mappings in `plugins/GeoLocate/config.yml`
5. Run `/geoadmin reload`

## Commands

| Command | Description | Permission |
|---|---|---|
| `/geolocate` | Get your real-world location | `geolocate.use` |
| `/geolocate <player>` | Get another player's location | `geolocate.others` |
| `/geoadmin reload` | Reload configuration | `geolocate.admin` |
| `/geoadmin info` | Show plugin information | `geolocate.admin` |
| `/geoadmin clearcache` | Clear coordinate caches | `geolocate.admin` |

## API Usage

Add GeoLocate as a dependency and use the API:

```java
GeoLocateAPI api = GeoLocateAPI.get();

Optional<GeoPoint> point = api.getGeoPoint(player);
point.ifPresent(geo -> {
    player.sendMessage("Lat: " + geo.getLatitude());
    player.sendMessage("Lon: " + geo.getLongitude());
});

Optional<String> link = api.getGoogleMapsLink(player);
link.ifPresent(url -> player.sendMessage(url));
```

## Documentation

Full documentation is available in the [Wiki](../../wiki).

## License

MIT License
