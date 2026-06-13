# Installation

## Requirements

- A Paper server running 1.21.10 or newer
- Java 21 or newer

## Steps

1. Download the latest `GeoLocate-x.x.x.jar` from the
   [releases page](../../releases)

2. Place the jar file in your server's `plugins/` directory

3. Start or restart your server

4. Open `plugins/GeoLocate/config.yml` and configure your world mappings

5. Run `/geoadmin reload` to apply your configuration without restarting

## As a Dependency

If you are a developer using GeoLocate as a library in your own plugin,
add it to your `plugin.yml`:

```yaml
depend:
  - GeoLocate
```

Then add it to your build file. With Gradle:

```kotlin
repositories {
    maven("https://repo.yourusername.dev/releases")
}

dependencies {
    compileOnly("dev.geolocate:geolocate:1.0.0")
}
```

Make sure GeoLocate is installed on the server alongside your plugin.
