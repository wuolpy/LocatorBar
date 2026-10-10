package com.wuolp.managers;

import com.wuolp.LocatorBar;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.bukkit.configuration.file.FileConfiguration;

public class ConfigManager {

    private final LocatorBar plugin;

    private boolean enabled;
    private int duration;
    private int minInterval;
    private int maxInterval;

    private boolean allWorlds;
    private List<String> whitelistedWorlds;

    private String prefix;
    private List<String> activatedMessages;
    private List<String> deactivatedMessages;

    private boolean titleEnabled;
    private String titleText;
    private String subtitleText;
    private int titleFadeIn;
    private int titleStay;
    private int titleFadeOut;

    private boolean actionBarEnabled;
    private String actionBarMessage;

    private boolean soundEnabled;
    private String soundOn;
    private String soundOff;
    private float soundVolume;
    private float soundPitch;

    // Updater fields
    private boolean updaterEnabled;
    private int updaterCheckInterval;
    private boolean updaterAutoDownload;
    private boolean updaterAutoReload;
    private String updaterMode;
    private String updaterGithubRepo;
    private String updaterDirectUrl;

    public ConfigManager(LocatorBar plugin) {
        this.plugin = plugin;
    }

    public void loadConfig() {
        plugin.reloadConfig();
        FileConfiguration config = plugin.getConfig();

        this.enabled = config.getBoolean("enabled", true);
        this.duration = config.getInt("duration", 60);
        this.minInterval = config.getInt("interval.min", 300);
        this.maxInterval = config.getInt("interval.max", 900);

        if (this.minInterval <= 0) {
            this.minInterval = 60;
        }
        if (this.maxInterval < this.minInterval) {
            this.maxInterval = this.minInterval;
        }

        this.allWorlds = config.getBoolean("worlds.all-worlds", true);
        this.whitelistedWorlds = config.getStringList("worlds.whitelist");

        this.prefix = config.getString("messages.prefix", "&8[&c&lLOCATOR&6&lBAR&8]&r ");

        if (config.isList("messages.activated")) {
            this.activatedMessages = config.getStringList("messages.activated");
        } else {
            String single = config.getString("messages.activated", "&e&lLOCATOR BAR IS ON! LET THE FIGHTERS FIGHT...");
            this.activatedMessages = Arrays.asList(single.split("\n"));
        }

        if (config.isList("messages.deactivated")) {
            this.deactivatedMessages = config.getStringList("messages.deactivated");
        } else {
            String single = config.getString("messages.deactivated", "&c&lLOCATOR BAR IS OFF! &7The fighters may rest.");
            this.deactivatedMessages = Arrays.asList(single.split("\n"));
        }

        this.titleEnabled = config.getBoolean("messages.title.enabled", true);
        this.titleText = config.getString("messages.title.title", "&c&lLOCATOR BAR IS ON!");
        this.subtitleText = config.getString("messages.title.subtitle", "&eLET THE FIGHTERS FIGHT...");
        this.titleFadeIn = config.getInt("messages.title.fade-in", 10);
        this.titleStay = config.getInt("messages.title.stay", 70);
        this.titleFadeOut = config.getInt("messages.title.fade-out", 20);

        this.actionBarEnabled = config.getBoolean("messages.action-bar.enabled", true);
        this.actionBarMessage = config.getString("messages.action-bar.message", "&c&lLOCATOR BAR ACTIVE &8| &e{time_left} &7remaining");

        this.soundEnabled = config.getBoolean("messages.sound.enabled", true);
        this.soundOn = config.getString("messages.sound.sound-on", "ENTITY_ENDER_DRAGON_GROWL");
        this.soundOff = config.getString("messages.sound.sound-off", "BLOCK_BEACON_DEACTIVATE");
        this.soundVolume = (float) config.getDouble("messages.sound.volume", 1.0);
        this.soundPitch = (float) config.getDouble("messages.sound.pitch", 1.0);

        // Updater
        this.updaterEnabled = config.getBoolean("updater.enabled", true);
        this.updaterCheckInterval = config.getInt("updater.check-interval", 3600);
        this.updaterAutoDownload = config.getBoolean("updater.auto-download", false);
        this.updaterAutoReload = config.getBoolean("updater.auto-reload", false);
        this.updaterMode = config.getString("updater.mode", "github");
        this.updaterGithubRepo = config.getString("updater.github-repo", "wuolp/LocatorBar");
        this.updaterDirectUrl = config.getString("updater.direct-url", "");
    }

    public void setDuration(int duration) {
        this.duration = duration;
        plugin.getConfig().set("duration", duration);
        plugin.saveConfig();
    }

    public void setMinInterval(int minInterval) {
        this.minInterval = minInterval;
        plugin.getConfig().set("interval.min", minInterval);
        plugin.saveConfig();
    }

    public void setMaxInterval(int maxInterval) {
        this.maxInterval = maxInterval;
        plugin.getConfig().set("interval.max", maxInterval);
        plugin.saveConfig();
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        plugin.getConfig().set("enabled", enabled);
        plugin.saveConfig();
    }

    // Getters
    public boolean isEnabled() {
        return enabled;
    }

    public int getDuration() {
        return duration;
    }

    public int getMinInterval() {
        return minInterval;
    }

    public int getMaxInterval() {
        return maxInterval;
    }

    public boolean isAllWorlds() {
        return allWorlds;
    }

    public List<String> getWhitelistedWorlds() {
        return whitelistedWorlds != null ? whitelistedWorlds : new ArrayList<>();
    }

    public String getPrefix() {
        return prefix;
    }

    public List<String> getActivatedMessages() {
        return activatedMessages;
    }

    public List<String> getDeactivatedMessages() {
        return deactivatedMessages;
    }

    public boolean isTitleEnabled() {
        return titleEnabled;
    }

    public String getTitleText() {
        return titleText;
    }

    public String getSubtitleText() {
        return subtitleText;
    }

    public int getTitleFadeIn() {
        return titleFadeIn;
    }

    public int getTitleStay() {
        return titleStay;
    }

    public int getTitleFadeOut() {
        return titleFadeOut;
    }

    public boolean isActionBarEnabled() {
        return actionBarEnabled;
    }

    public String getActionBarMessage() {
        return actionBarMessage;
    }

    public boolean isSoundEnabled() {
        return soundEnabled;
    }

    public String getSoundOn() {
        return soundOn;
    }

    public String getSoundOff() {
        return soundOff;
    }

    public float getSoundVolume() {
        return soundVolume;
    }

    public float getSoundPitch() {
        return soundPitch;
    }

    public boolean isUpdaterEnabled() {
        return updaterEnabled;
    }

    public int getUpdaterCheckInterval() {
        return updaterCheckInterval;
    }

    public boolean isUpdaterAutoDownload() {
        return updaterAutoDownload;
    }

    public boolean isUpdaterAutoReload() {
        return updaterAutoReload;
    }

    public String getUpdaterMode() {
        return updaterMode;
    }

    public String getUpdaterGithubRepo() {
        return updaterGithubRepo;
    }

    public String getUpdaterDirectUrl() {
        return updaterDirectUrl;
    }
}
