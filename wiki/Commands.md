# Commands

## /geolocate

Shows your current real-world coordinates and a clickable Google Maps link.

**Permission:** `geolocate.use` (default: all players)

**Usage:** `/geolocate` or `/geo` or `/coords`

**Output example:**
```
[GeoLocate] Your real-world location:
  Latitude:  48.858844
  Longitude: 2.294351
  Altitude:  64.0m
[GeoLocate] Open in Google Maps: Click here
```

## /geolocate \<player\>

Shows another player's real-world coordinates.

**Permission:** `geolocate.others` (default: op)

**Usage:** `/geolocate Notch`

## /geoadmin

Administration commands for managing GeoLocate.

**Permission:** `geolocate.admin` (default: op)

### /geoadmin reload

Reloads `config.yml` without restarting the server.

### /geoadmin info

Displays information about the current plugin state including the number of
configured worlds and active settings.

### /geoadmin clearcache

Clears all cached coordinate calculations. Useful after changing world
configuration to ensure fresh results.

## Permissions

| Permission | Description | Default |
|---|---|---|
| `geolocate.use` | Use /geolocate for yourself | true |
| `geolocate.others` | Check other players' locations | op |
| `geolocate.admin` | Use /geoadmin commands | op |
| `geolocate.notify` | Receive move notifications | false |
String directions = GoogleMapsLink.buildDirections(pointA, pointB);
```
```
