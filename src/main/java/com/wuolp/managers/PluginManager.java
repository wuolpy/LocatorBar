package com.wuolp.managers;

import com.wuolp.LocatorBar;
import com.wuolp.updater.UpdateManager;

public class PluginManager {

    private static PluginManager instance;

    private ConfigManager configManager;
    private LocatorBarManager locatorBarManager;
    private UpdateManager updateManager;

    public static PluginManager getInstance() {
        if (instance == null) {
            instance = new PluginManager();
        }
        return instance;
    }

    public void initialize(LocatorBar plugin) {
        this.configManager = new ConfigManager(plugin);
        this.configManager.loadConfig();

        this.locatorBarManager = new LocatorBarManager(plugin, configManager);
        this.locatorBarManager.initialize();

        this.updateManager = new UpdateManager(plugin, configManager);
        this.updateManager.initialize();
    }

    public void shutdown() {
        if (locatorBarManager != null) {
            locatorBarManager.shutdown();
        }
        if (updateManager != null) {
            updateManager.shutdown();
        }
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public LocatorBarManager getLocatorBarManager() {
        return locatorBarManager;
    }

    public UpdateManager getUpdateManager() {
        return updateManager;
    }
}