package com.wuolp;

import com.wuolp.commands.LocatorBarCommand;
import com.wuolp.commands.LocatorBarTabCompleter;
import com.wuolp.listeners.PlayerListener;
import com.wuolp.managers.ConfigManager;
import com.wuolp.managers.LocatorBarManager;
import com.wuolp.managers.PluginManager;
import com.wuolp.updater.UpdateManager;
import java.io.File;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

public class LocatorBar extends JavaPlugin {

    private static LocatorBar instance;

    @Override
    public void onEnable() {
        instance = this;

        // Save default configuration if not already present
        saveDefaultConfig();

        // Initialize core managers
        PluginManager.getInstance().initialize(this);

        ConfigManager configManager = PluginManager.getInstance().getConfigManager();
        LocatorBarManager locatorBarManager = PluginManager.getInstance().getLocatorBarManager();
        UpdateManager updateManager = PluginManager.getInstance().getUpdateManager();

        // Register command and tab completer
        PluginCommand cmd = getCommand("locatorbar");
        if (cmd != null) {
            cmd.setExecutor(new LocatorBarCommand(this, locatorBarManager, configManager, updateManager));
            cmd.setTabCompleter(new LocatorBarTabCompleter());
        }

        // Register event listener
        getServer().getPluginManager().registerEvents(new PlayerListener(this, locatorBarManager, configManager), this);

        getLogger().info("=========================================");
        getLogger().info(" LocatorBar v" + getDescription().getVersion() + " has been enabled!");
        getLogger().info(" Hot-Updating & 1.21.11 LocatorBar active");
        getLogger().info("=========================================");
    }

    @Override
    public void onDisable() {
        // Cancel all tasks, shutdown updaters, and safely reset the game rule
        PluginManager.getInstance().shutdown();

        getLogger().info("LocatorBar has been safely disabled!");
    }

    public static LocatorBar getInstance() {
        return instance;
    }

    /**
     * Exposes the plugin's jar file on disk for live hot-reloading without server restarts.
     */
    public File getPluginFile() {
        return super.getFile();
    }
}