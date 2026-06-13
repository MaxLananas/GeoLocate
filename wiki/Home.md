# GeoLocate Wiki

Welcome to the GeoLocate documentation.

GeoLocate is a Paper plugin that maps Minecraft block coordinates to real-world
geographic coordinates using configurable world bounding boxes and projections.

## Pages

- [Installation](Installation)
- [Configuration](Configuration)
- [Commands](Commands)
- [API Usage](API-Usage)

## How It Works

GeoLocate uses a bounding box system. You define what geographic area your
Minecraft world represents, and the library linearly or spherically maps
block coordinates within that area to latitude and longitude values.

For example, if you configure your world to represent the entire Earth,
then a player standing at X=0, Z=0 will be mapped to approximately 0°, 0°
(the Gulf of Guinea). A player at the world's maximum X coordinate will
be mapped to 180° longitude (the International Date Line).

The Mercator projection accounts for the distortion of the Earth's sphere,
giving more accurate results for navigation purposes. The Linear projection
is simpler and works well for flat custom worlds or small geographic regions.
