package com.wuolp.listeners;

import com.wuolp.LocatorBar;
import com.wuolp.managers.ConfigManager;
import com.wuolp.managers.LocatorBarManager;
import com.wuolp.utils.Utils;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class PlayerListener implements Listener {

    private final LocatorBar plugin;
    private final LocatorBarManager manager;
    private final ConfigManager configManager;

    public PlayerListener(LocatorBar plugin, LocatorBarManager manager, ConfigManager configManager) {
        this.plugin = plugin;
        this.manager = manager;
        this.configManager = configManager;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (manager.isActive()) {
            // Alert joining players that the Locator Bar is active
            if (configManager.isActionBarEnabled()) {
                String bar = configManager.getActionBarMessage()
                    .replace("{time_left}", Utils.formatTime(manager.getRemainingActiveSeconds()));
                Utils.sendActionBar(player, bar);
            }
        }
    }
}