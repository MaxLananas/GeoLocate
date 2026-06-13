# Configuration

The configuration file is located at `plugins/GeoLocate/config.yml`.

## World Mappings

Each world has its own section under `worlds:`. The world name must match
exactly the folder name of the world on your server.

```yaml
worlds:
  world:
    enabled: true
    projection: MERCATOR
    bounds:
      min-lat: -85.05112878
      max-lat: 85.05112878
      min-lon: -180.0
      max-lon: 180.0
    minecraft:
      min-x: -29999984
      max-x: 29999984
      min-z: -29999984
      max-z: 29999984
    offset:
      x: 0
      z: 0
```

### projection

Defines the mathematical model used to map coordinates.

- `MERCATOR` - Uses the Mercator projection. Recommended for worlds that
  represent geographic areas, as it accounts for spherical distortion.
- `LINEAR` - A simple linear interpolation. Good for flat maps, city builds,
  or any custom mapping where distortion is not a concern.

### bounds

The real-world geographic area your Minecraft world represents.

| Key | Description |
|---|---|
| `min-lat` | Southern boundary in degrees (-90 to 90) |
| `max-lat` | Northern boundary in degrees (-90 to 90) |
| `min-lon` | Western boundary in degrees (-180 to 180) |
| `max-lon` | Eastern boundary in degrees (-180 to 180) |

### minecraft

The block coordinate range of your world. These default to the Minecraft
world border limits. Adjust these if your world has a smaller defined area.

### offset

Shifts the origin point of the mapping. Useful if your world's center
does not correspond to the geographic center of the mapped area.

## Settings

```yaml
settings:
  google-maps-zoom: 15
  show-altitude: true
  notify-on-move: false
  notify-distance: 100
  cache-size: 500
  decimal-places: 6
```

| Key | Description |
|---|---|
| `google-maps-zoom` | Zoom level used in generated Google Maps links (1-21) |
| `show-altitude` | Whether to include Minecraft Y level in location output |
| `notify-on-move` | Send automatic location notifications as players move |
| `notify-distance` | Blocks a player must travel before receiving another notification |
| `cache-size` | Maximum number of coordinate results to cache per world |
| `decimal-places` | Precision of latitude and longitude values |

## Example: Mapping a City

If your Minecraft world represents Paris, France:

```yaml
worlds:
  world:
    enabled: true
    projection: LINEAR
    bounds:
      min-lat: 48.815573
      max-lat: 48.902145
      min-lon: 2.224199
      max-lon: 2.469920
    minecraft:
      min-x: -5000
      max-x: 5000
      min-z: -5000
      max-z: 5000
    offset:
      x: 0
      z: 0
```
