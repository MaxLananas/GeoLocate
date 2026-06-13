package dev.geolocate;

import dev.geolocate.api.GeoLocateAPI;
import dev.geolocate.api.WorldMapper;
import dev.geolocate.command.GeoLocateCommand;
import dev.geolocate.config.GeoLocateConfig;
import dev.geolocate.listener.PlayerMoveListener;
import dev.geolocate.listener.PlayerQuitListener;
import dev.geolocate.placeholder.GeoLocatePlaceholders;
import dev.geolocate.task.ActionBarTask;
import dev.geolocate.storage.PlayerPreferenceStorage;
import org.bukkit.plugin.java.JavaPlugin;

public final class GeoLocate extends JavaPlugin {

    private static GeoLocate instance;
    private GeoLocateConfig geoConfig;
    private WorldMapper worldMapper;
    private GeoLocateAPI api;
    private PlayerPreferenceStorage preferenceStorage;
    private ActionBarTask actionBarTask;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();

        this.geoConfig = new GeoLocateConfig(this);
        this.worldMapper = new WorldMapper(this);
        this.preferenceStorage = new PlayerPreferenceStorage(this);
        this.api = new GeoLocateAPI(this);

        registerCommands();
        registerListeners();
        startTasks();
        registerPlaceholders();

        getLogger().info("GeoLocate v" + getDescription().getVersion() + " enabled.");
        getLogger().info("Loaded " + worldMapper.getConfiguredWorldCount() + " world mapping(s).");
    }

    @Override
    public void onDisable() {
        if (actionBarTask != null) {
            actionBarTask.cancel();
        }
        if (preferenceStorage != null) {
            preferenceStorage.saveAll();
        }
        getLogger().info("GeoLocate disabled.");
        instance = null;
    }

    private void registerCommands() {
        GeoLocateCommand handler = new GeoLocateCommand(this);
        getCommand("geolocate").setExecutor(handler);
        getCommand("geolocate").setTabCompleter(handler);
        getCommand("geoadmin").setExecutor(handler);
        getCommand("geoadmin").setTabCompleter(handler);
        getCommand("geoconvert").setExecutor(handler);
        getCommand("geoconvert").setTabCompleter(handler);
        getCommand("geodistance").setExecutor(handler);
        getCommand("geodistance").setTabCompleter(handler);
        getCommand("geonotify").setExecutor(handler);
        getCommand("geoactionbar").setExecutor(handler);
    }

    private void registerListeners() {
        getServer().getPluginManager().registerEvents(new PlayerMoveListener(this), this);
        getServer().getPluginManager().registerEvents(new PlayerQuitListener(this), this);
    }

    private void startTasks() {
        this.actionBarTask = new ActionBarTask(this);
        actionBarTask.runTaskTimer(this, 0L, geoConfig.getActionBarUpdateInterval());
    }

    private void registerPlaceholders() {
        if (getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new GeoLocatePlaceholders(this).register();
            getLogger().info("PlaceholderAPI integration enabled.");
        }
    }

    public void reload() {
        reloadConfig();
        this.geoConfig = new GeoLocateConfig(this);
        this.worldMapper = new WorldMapper(this);
        if (actionBarTask != null) {
            actionBarTask.cancel();
        }
        this.actionBarTask = new ActionBarTask(this);
        actionBarTask.runTaskTimer(this, 0L, geoConfig.getActionBarUpdateInterval());
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

    public PlayerPreferenceStorage getPreferenceStorage() {
        return preferenceStorage;
    }
}
