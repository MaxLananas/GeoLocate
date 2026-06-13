package dev.geolocate;

import dev.geolocate.api.GeoLocateAPI;
import dev.geolocate.api.WorldMapper;
import dev.geolocate.command.GeoLocateCommand;
import dev.geolocate.config.GeoLocateConfig;
import dev.geolocate.listener.PlayerMoveListener;
import org.bukkit.plugin.java.JavaPlugin;

public final class GeoLocate extends JavaPlugin {

    private static GeoLocate instance;
    private GeoLocateConfig geoConfig;
    private WorldMapper worldMapper;
    private GeoLocateAPI api;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();

        this.geoConfig = new GeoLocateConfig(this);
        this.worldMapper = new WorldMapper(this);
        this.api = new GeoLocateAPI(this);

        registerCommands();
        registerListeners();

        getLogger().info("GeoLocate v" + getDescription().getVersion() + " enabled.");
        getLogger().info("Loaded " + worldMapper.getConfiguredWorldCount() + " world mapping(s).");
    }

    @Override
    public void onDisable() {
        getLogger().info("GeoLocate disabled.");
        instance = null;
    }

    private void registerCommands() {
        GeoLocateCommand handler = new GeoLocateCommand(this);
        getCommand("geolocate").setExecutor(handler);
        getCommand("geolocate").setTabCompleter(handler);
        getCommand("geoadmin").setExecutor(handler);
        getCommand("geoadmin").setTabCompleter(handler);
    }

    private void registerListeners() {
        getServer().getPluginManager().registerEvents(new PlayerMoveListener(this), this);
    }

    public void reload() {
        reloadConfig();
        this.geoConfig = new GeoLocateConfig(this);
        this.worldMapper = new WorldMapper(this);
    }

    public static GeoLocate getInstance() {
        return instance;
    }

    public GeoLocateConfig getGeoConfig() {
        return geoConfig;
    }

    public WorldMapper getWorldMapper() {
        return worldMapper;
    }

    public GeoLocateAPI getAPI() {
        return api;
    }
}
