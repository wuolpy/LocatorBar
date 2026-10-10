package com.wuolp.commands;

import com.wuolp.LocatorBar;
import com.wuolp.managers.ConfigManager;
import com.wuolp.managers.LocatorBarManager;
import com.wuolp.updater.PluginReloader;
import com.wuolp.updater.UpdateManager;
import com.wuolp.utils.Utils;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class LocatorBarCommand implements CommandExecutor {

    private final LocatorBar plugin;
    private final LocatorBarManager manager;
    private final ConfigManager configManager;
    private final UpdateManager updateManager;

    public LocatorBarCommand(LocatorBar plugin, LocatorBarManager manager, ConfigManager configManager, UpdateManager updateManager) {
        this.plugin = plugin;
        this.manager = manager;
        this.configManager = configManager;
        this.updateManager = updateManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("locatorbar.admin")) {
            Utils.sendMessage(sender, "&cYou do not have permission to use this command.");
            return true;
        }

        String prefix = configManager.getPrefix();

        if (args.length == 0 || args[0].equalsIgnoreCase("help")) {
            sendHelp(sender, label);
            return true;
        }

        String sub = args[0].toLowerCase();
        switch (sub) {
            case "start":
                handleStart(sender, prefix, args);
                break;
            case "stop":
                handleStop(sender, prefix);
                break;
            case "status":
                handleStatus(sender, prefix);
                break;
            case "toggle":
                handleToggle(sender, prefix);
                break;
            case "set":
                handleSet(sender, label, prefix, args);
                break;
            case "reload":
                handleReload(sender, prefix);
                break;
            case "update":
                handleUpdate(sender, label, prefix, args);
                break;
            case "hotreload":
                handleHotReload(sender, prefix);
                break;
            default:
                Utils.sendMessage(sender, prefix + "&cUnknown subcommand. Type &e/" + label + " help &cfor available commands.");
                break;
        }

        return true;
    }

    private void handleStart(CommandSender sender, String prefix, String[] args) {
        int duration = configManager.getDuration();
        if (args.length >= 2) {
            try {
                duration = Integer.parseInt(args[1]);
                if (duration <= 0) {
                    Utils.sendMessage(sender, prefix + "&cDuration must be greater than 0 seconds.");
                    return;
                }
            } catch (NumberFormatException e) {
                Utils.sendMessage(sender, prefix + "&cInvalid duration number: &e" + args[1]);
                return;
            }
        }

        manager.start(duration);
        Utils.sendMessage(sender, prefix + "&aLocator Bar started for &e" + duration + "s&a!");
    }

    private void handleStop(CommandSender sender, String prefix) {
        if (!manager.isActive()) {
            Utils.sendMessage(sender, prefix + "&cLocator Bar is not currently active.");
            return;
        }

        manager.stop();
        Utils.sendMessage(sender, prefix + "&cLocator Bar has been manually stopped.");
    }

    private void handleStatus(CommandSender sender, String prefix) {
        Utils.sendMessage(sender, "&8&m--------------------------------------------------");
        Utils.sendMessage(sender, "&6&lLocatorBar System Status");
        Utils.sendMessage(sender, "&7- Version: &e" + plugin.getDescription().getVersion());
        Utils.sendMessage(sender, "&7- Active: " + (manager.isActive() ? "&a&lYES" : "&c&lNO"));
        if (manager.isActive()) {
            Utils.sendMessage(sender, "&7- Remaining Time: &e" + Utils.formatTime(manager.getRemainingActiveSeconds()));
        }
        Utils.sendMessage(sender, "&7- Auto Scheduler: " + (configManager.isEnabled() ? "&aENABLED" : "&cDISABLED"));
        if (configManager.isEnabled() && !manager.isActive()) {
            Utils.sendMessage(sender, "&7- Next Activation In: &e" + Utils.formatTime(manager.getCountdownToNextSeconds()));
        }
        Utils.sendMessage(sender, "&7- Configured Duration: &e" + configManager.getDuration() + "s");
        Utils.sendMessage(sender, "&7- Random Interval: &e" + configManager.getMinInterval() + "s &7to &e" + configManager.getMaxInterval() + "s");
        Utils.sendMessage(sender, "&7- Auto-Updater: " + (configManager.isUpdaterEnabled() ? "&aENABLED" : "&cDISABLED"));
        Utils.sendMessage(sender, "&8&m--------------------------------------------------");
    }

    private void handleToggle(CommandSender sender, String prefix) {
        boolean newState = !configManager.isEnabled();
        configManager.setEnabled(newState);

        if (newState) {
            manager.resetTimer();
        }

        Utils.sendMessage(sender, prefix + "&aAutomatic random scheduler is now " + (newState ? "&a&lENABLED" : "&c&lDISABLED") + " &aand saved to config!");
    }

    private void handleSet(CommandSender sender, String label, String prefix, String[] args) {
        if (args.length < 3) {
            Utils.sendMessage(sender, prefix + "&cUsage: &e/" + label + " set <duration|mininterval|maxinterval> <seconds>");
            return;
        }

        String target = args[1].toLowerCase();
        int seconds;
        try {
            seconds = Integer.parseInt(args[2]);
            if (seconds <= 0) {
                Utils.sendMessage(sender, prefix + "&cValue must be a positive integer greater than 0.");
                return;
            }
        } catch (NumberFormatException e) {
            Utils.sendMessage(sender, prefix + "&cInvalid number: &e" + args[2]);
            return;
        }

        switch (target) {
            case "duration":
                configManager.setDuration(seconds);
                Utils.sendMessage(sender, prefix + "&aDuration set to &e" + seconds + "s &aand saved to config!");
                break;
            case "mininterval":
                if (seconds > configManager.getMaxInterval()) {
                    Utils.sendMessage(sender, prefix + "&cMin interval (" + seconds + "s) cannot be greater than max interval (" + configManager.getMaxInterval() + "s). Adjust max interval first.");
                    return;
                }
                configManager.setMinInterval(seconds);
                manager.resetTimer();
                Utils.sendMessage(sender, prefix + "&aMinimum interval set to &e" + seconds + "s &aand saved to config!");
                break;
            case "maxinterval":
                if (seconds < configManager.getMinInterval()) {
                    Utils.sendMessage(sender, prefix + "&cMax interval (" + seconds + "s) cannot be less than min interval (" + configManager.getMinInterval() + "s). Adjust min interval first.");
                    return;
                }
                configManager.setMaxInterval(seconds);
                manager.resetTimer();
                Utils.sendMessage(sender, prefix + "&aMaximum interval set to &e" + seconds + "s &aand saved to config!");
                break;
            default:
                Utils.sendMessage(sender, prefix + "&cUnknown setting. Use &e<duration|mininterval|maxinterval>&c.");
                break;
        }
    }

    private void handleReload(CommandSender sender, String prefix) {
        configManager.loadConfig();
        manager.resetTimer();
        Utils.sendMessage(sender, prefix + "&aConfiguration reloaded from config.yml!");
    }

    private void handleUpdate(CommandSender sender, String label, String prefix, String[] args) {
        String action = (args.length >= 2) ? args[1].toLowerCase() : "check";

        if (action.equalsIgnoreCase("reload")) {
            handleHotReload(sender, prefix);
            return;
        }

        if (action.equalsIgnoreCase("download") || action.equalsIgnoreCase("apply")) {
            Utils.sendMessage(sender, prefix + "&eChecking for update before downloading...");
            updateManager.checkForUpdates(result -> {
                if (!result.isUpdateAvailable()) {
                    Utils.sendMessage(sender, prefix + "&aPlugin is already up to date! (&ev" + result.getCurrentVersion() + "&a)");
                    return;
                }

                if (result.getDownloadUrl() == null || result.getDownloadUrl().isEmpty()) {
                    Utils.sendMessage(sender, prefix + "&cNew version v" + result.getLatestVersion() + " found, but no direct download URL was found in the release assets.");
                    return;
                }

                updateManager.downloadAndApply(result, sender);
            });
            return;
        }

        // Default: check status
        Utils.sendMessage(sender, prefix + "&eChecking for updates...");
        updateManager.checkForUpdates(result -> {
            if (result.isUpdateAvailable()) {
                Utils.sendMessage(sender, prefix + "&aA new update is available: &6v" + result.getLatestVersion() + " &7(Current: &ev" + result.getCurrentVersion() + "&7)");
                Utils.sendMessage(sender, prefix + "&7Run &b/" + label + " update download &7to automatically download and apply without restarting the server!");
            } else {
                Utils.sendMessage(sender, prefix + "&aYou are running the latest version: &ev" + result.getCurrentVersion());
            }
        });
    }

    private void handleHotReload(CommandSender sender, String prefix) {
        Utils.sendMessage(sender, prefix + "&eReloading LocatorBar directly from disk without restarting the server...");
        PluginReloader.reloadPlugin(plugin, null, sender);
    }

    private void sendHelp(CommandSender sender, String label) {
        Utils.sendMessage(sender, "&8&m--------------------------------------------------");
        Utils.sendMessage(sender, "&6&lLocatorBar &7v" + plugin.getDescription().getVersion() + " &8| &eCommands");
        Utils.sendMessage(sender, "&e/" + label + " start [seconds] &7- Manually activate locator bar");
        Utils.sendMessage(sender, "&e/" + label + " stop &7- Manually stop the active locator bar");
        Utils.sendMessage(sender, "&e/" + label + " status &7- Check active state, timers & version");
        Utils.sendMessage(sender, "&e/" + label + " toggle &7- Enable/disable random auto-scheduler");
        Utils.sendMessage(sender, "&e/" + label + " set duration <seconds> &7- Set active duration");
        Utils.sendMessage(sender, "&e/" + label + " set mininterval <seconds> &7- Set minimum random interval");
        Utils.sendMessage(sender, "&e/" + label + " set maxinterval <seconds> &7- Set maximum random interval");
        Utils.sendMessage(sender, "&e/" + label + " reload &7- Reload config.yml");
        Utils.sendMessage(sender, "&e/" + label + " update &7- Check for updates");
        Utils.sendMessage(sender, "&e/" + label + " update download &7- Auto-download & update WITHOUT restart");
        Utils.sendMessage(sender, "&e/" + label + " hotreload &7- Hot-reload jar from disk WITHOUT restart");
        Utils.sendMessage(sender, "&e/" + label + " help &7- Display this help message");
        Utils.sendMessage(sender, "&8&m--------------------------------------------------");
    }
}
