package com.wuolp.managers;

import com.wuolp.LocatorBar;
import com.wuolp.utils.Utils;
import java.util.List;
import java.util.Random;
import org.bukkit.Bukkit;
import org.bukkit.GameRule;
import org.bukkit.World;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

public class LocatorBarManager {

    private final LocatorBar plugin;
    private final ConfigManager configManager;
    private final Random random = new Random();

    private boolean active = false;
    private int remainingActiveSeconds = 0;
    private int countdownToNextSeconds = 0;
    private BukkitTask mainTask;

    public LocatorBarManager(LocatorBar plugin, ConfigManager configManager) {
        this.plugin = plugin;
        this.configManager = configManager;
    }

    public void initialize() {
        // Ensure locator bar gamerule is turned off initially
        applyGameRule(false);

        // Schedule next random activation
        this.countdownToNextSeconds = calculateNextInterval();

        // Start scheduler task running every second (20 ticks)
        this.mainTask = new BukkitRunnable() {
            @Override
            public void run() {
                tick();
            }
        }.runTaskTimer(plugin, 20L, 20L);

        plugin.getLogger().info("LocatorBarManager initialized. Next trigger in " + countdownToNextSeconds + "s.");
    }

    public void shutdown() {
        if (mainTask != null) {
            mainTask.cancel();
            mainTask = null;
        }
        if (active) {
            applyGameRule(false);
            active = false;
        }
    }

    private void tick() {
        if (active) {
            remainingActiveSeconds--;

            if (configManager.isActionBarEnabled()) {
                String formattedTime = Utils.formatTime(Math.max(0, remainingActiveSeconds));
                String bar = configManager.getActionBarMessage().replace("{time_left}", formattedTime);
                Utils.broadcastActionBar(bar);
            }

            if (remainingActiveSeconds <= 0) {
                stop();
            }
        } else {
            if (configManager.isEnabled()) {
                countdownToNextSeconds--;
                if (countdownToNextSeconds <= 0) {
                    start(configManager.getDuration());
                }
            }
        }
    }

    public void start(int durationSeconds) {
        if (durationSeconds <= 0) {
            durationSeconds = configManager.getDuration();
        }

        this.active = true;
        this.remainingActiveSeconds = durationSeconds;

        // Apply GameRule to worlds
        applyGameRule(true);

        // Broadcast activation chat message
        List<String> messages = configManager.getActivatedMessages();
        Utils.broadcast(messages);

        // Screen title
        if (configManager.isTitleEnabled()) {
            Utils.broadcastTitle(
                configManager.getTitleText(),
                configManager.getSubtitleText(),
                configManager.getTitleFadeIn(),
                configManager.getTitleStay(),
                configManager.getTitleFadeOut()
            );
        }

        // Sound
        if (configManager.isSoundEnabled()) {
            Utils.broadcastSound(
                configManager.getSoundOn(),
                configManager.getSoundVolume(),
                configManager.getSoundPitch()
            );
        }
    }

    public void stop() {
        if (!active) {
            return;
        }

        this.active = false;
        this.remainingActiveSeconds = 0;

        // Revert GameRule in worlds
        applyGameRule(false);

        // Broadcast deactivation chat message
        List<String> messages = configManager.getDeactivatedMessages();
        Utils.broadcast(messages);

        // Sound
        if (configManager.isSoundEnabled()) {
            Utils.broadcastSound(
                configManager.getSoundOff(),
                configManager.getSoundVolume(),
                configManager.getSoundPitch()
            );
        }

        // Pick next random interval
        this.countdownToNextSeconds = calculateNextInterval();
    }

    public int calculateNextInterval() {
        int min = configManager.getMinInterval();
        int max = configManager.getMaxInterval();
        if (min <= 0) {
            min = 60;
        }
        if (max < min) {
            max = min;
        }
        int diff = max - min;
        return diff > 0 ? (min + random.nextInt(diff + 1)) : min;
    }

    public void resetTimer() {
        this.countdownToNextSeconds = calculateNextInterval();
    }

    @SuppressWarnings("unchecked")
    private GameRule<Boolean> getLocatorBarGameRule() {
        GameRule<Boolean> rule = GameRule.LOCATOR_BAR;
        if (rule == null) {
            GameRule<?> byName = GameRule.getByName("locator_bar");
            if (byName != null && byName.getType() == Boolean.class) {
                rule = (GameRule<Boolean>) byName;
            }
        }
        if (rule == null) {
            GameRule<?> byName = GameRule.getByName("locatorBar");
            if (byName != null && byName.getType() == Boolean.class) {
                rule = (GameRule<Boolean>) byName;
            }
        }
        return rule;
    }

    public void applyGameRule(boolean state) {
        GameRule<Boolean> rule = getLocatorBarGameRule();
        if (rule == null) {
            plugin.getLogger().warning("Could not find GameRule LOCATOR_BAR in current server build!");
            return;
        }

        boolean allWorlds = configManager.isAllWorlds();
        List<String> whitelist = configManager.getWhitelistedWorlds();

        for (World world : Bukkit.getWorlds()) {
            if (allWorlds || whitelist.contains(world.getName())) {
                world.setGameRule(rule, state);
            }
        }
    }

    // Getters and helper status methods
    public boolean isActive() {
        return active;
    }

    public int getRemainingActiveSeconds() {
        return remainingActiveSeconds;
    }

    public int getCountdownToNextSeconds() {
        return countdownToNextSeconds;
    }

    public void setCountdownToNextSeconds(int seconds) {
        this.countdownToNextSeconds = seconds;
    }
}
