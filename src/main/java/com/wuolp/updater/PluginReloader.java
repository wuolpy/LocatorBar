package com.wuolp.updater;

import com.wuolp.LocatorBar;
import com.wuolp.utils.Utils;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.SimpleCommandMap;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.SimplePluginManager;

public class PluginReloader {

    /**
     * Hot-reloads LocatorBar on the live server without requiring a server reboot.
     * If updateFile is provided, replaces the old jar file with the update before reloading.
     *
     * @param plugin The current plugin instance
     * @param updateFile The downloaded update file (or null to reload the existing jar from disk)
     * @param sender The command sender initiating the reload (can be null)
     * @return true if reload succeeded
     */
    public static boolean reloadPlugin(LocatorBar plugin, File updateFile, CommandSender sender) {
        if (!Bukkit.isPrimaryThread()) {
            Bukkit.getScheduler().runTask(plugin, () -> reloadPlugin(plugin, updateFile, sender));
            return true;
        }

        UUID senderUuid = (sender instanceof Player) ? ((Player) sender).getUniqueId() : null;
        boolean isConsole = (sender != null && !(sender instanceof Player));

        String prefix = "&8[&c&lLOCATOR&6&lBAR&8]&r ";
        sendMessageToSender(senderUuid, isConsole, prefix + "&eInitiating hot-update without restarting the server...");

        File targetJar = plugin.getPluginFile();
        String pluginName = plugin.getName();
        PluginManager pm = Bukkit.getPluginManager();

        try {
            // 1. Disable current plugin instance (runs onDisable and stops all plugin tasks)
            pm.disablePlugin(plugin);

            // 2. Unregister commands and clean internal SimplePluginManager registries
            if (pm instanceof SimplePluginManager) {
                SimplePluginManager spm = (SimplePluginManager) pm;

                // Remove from plugins list
                try {
                    Field pluginsField = SimplePluginManager.class.getDeclaredField("plugins");
                    pluginsField.setAccessible(true);
                    @SuppressWarnings("unchecked")
                    List<Plugin> plugins = (List<Plugin>) pluginsField.get(spm);
                    plugins.remove(plugin);
                } catch (Exception e) {
                    plugin.getLogger().warning("Failed to remove plugin from plugins list: " + e.getMessage());
                }

                // Remove from lookupNames map
                try {
                    Field lookupNamesField = SimplePluginManager.class.getDeclaredField("lookupNames");
                    lookupNamesField.setAccessible(true);
                    @SuppressWarnings("unchecked")
                    Map<String, Plugin> lookupNames = (Map<String, Plugin>) lookupNamesField.get(spm);
                    lookupNames.remove(pluginName);
                    lookupNames.remove(pluginName.toLowerCase(Locale.ENGLISH));
                } catch (Exception e) {
                    plugin.getLogger().warning("Failed to remove plugin from lookupNames: " + e.getMessage());
                }

                // Unregister plugin commands from CommandMap
                try {
                    Field commandMapField = SimplePluginManager.class.getDeclaredField("commandMap");
                    commandMapField.setAccessible(true);
                    SimpleCommandMap commandMap = (SimpleCommandMap) commandMapField.get(spm);

                    Field knownCommandsField = SimpleCommandMap.class.getDeclaredField("knownCommands");
                    knownCommandsField.setAccessible(true);
                    @SuppressWarnings("unchecked")
                    Map<String, Command> knownCommands = (Map<String, Command>) knownCommandsField.get(commandMap);

                    Iterator<Map.Entry<String, Command>> it = knownCommands.entrySet().iterator();
                    while (it.hasNext()) {
                        Map.Entry<String, Command> entry = it.next();
                        Command cmd = entry.getValue();
                        if (cmd instanceof PluginCommand) {
                            PluginCommand pc = (PluginCommand) cmd;
                            if (pc.getPlugin() == plugin) {
                                pc.unregister(commandMap);
                                it.remove();
                            }
                        }
                    }
                } catch (Exception e) {
                    plugin.getLogger().warning("Failed to unregister commands: " + e.getMessage());
                }

                // Remove from dependency graph if present
                try {
                    Field depGraphField = SimplePluginManager.class.getDeclaredField("dependencyGraph");
                    depGraphField.setAccessible(true);
                    Object depGraph = depGraphField.get(spm);
                    if (depGraph != null) {
                        Method removeNodeMethod = depGraph.getClass().getMethod("removeNode", Object.class);
                        removeNodeMethod.invoke(depGraph, pluginName);
                    }
                } catch (Exception ignored) {
                }
            }

            // 3. Close the PluginClassLoader to release open JAR file handles on Windows
            ClassLoader cl = plugin.getClass().getClassLoader();
            if (cl instanceof URLClassLoader) {
                try {
                    ((URLClassLoader) cl).close();
                } catch (IOException e) {
                    plugin.getLogger().warning("Could not cleanly close ClassLoader: " + e.getMessage());
                }
            }

            // Trigger garbage collection to release Windows file locks
            System.gc();

            // 4. Overwrite target jar with update file if supplied
            if (updateFile != null && updateFile.exists()) {
                try {
                    Files.copy(updateFile.toPath(), targetJar.toPath(), StandardCopyOption.REPLACE_EXISTING);
                    updateFile.delete();
                } catch (Exception e) {
                    plugin.getLogger().warning("Direct overwrite locked; using update jar directly: " + e.getMessage());
                    targetJar = updateFile;
                }
            }

            // 5. Load the plugin jar from disk
            Plugin newPlugin = pm.loadPlugin(targetJar);
            if (newPlugin == null) {
                sendMessageToSender(senderUuid, isConsole, prefix + "&cFailed to load updated plugin JAR!");
                return false;
            }

            // 6. Enable the newly loaded plugin instance
            pm.enablePlugin(newPlugin);

            String newVer = newPlugin.getDescription().getVersion();
            sendMessageToSender(senderUuid, isConsole, prefix + "&a&lHOT-UPDATE COMPLETE! &aLocatorBar v" + newVer + " is now active without restarting the server!");
            return true;

        } catch (Exception e) {
            plugin.getLogger().severe("Hot-update failed with error: " + e.getMessage());
            e.printStackTrace();
            sendMessageToSender(senderUuid, isConsole, prefix + "&cHot-update failed: &e" + e.getMessage());
            return false;
        }
    }

    private static void sendMessageToSender(UUID uuid, boolean isConsole, String message) {
        if (isConsole) {
            Bukkit.getConsoleSender().sendMessage(Utils.colorize(message));
        } else if (uuid != null) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null && p.isOnline()) {
                p.sendMessage(Utils.colorize(message));
            }
        }
    }
}
