package com.wuolp.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class Utils {

    private static final Pattern HEX_PATTERN_WITH_AMP = Pattern.compile("&#([a-fA-F0-9]{6})");
    private static final Pattern HEX_PATTERN = Pattern.compile("#([a-fA-F0-9]{6})");

    /**
     * Colorize a string supporting legacy codes and hex (#RRGGBB or &#RRGGBB).
     */
    public static String colorize(String msg) {
        if (msg == null) {
            return "";
        }

        // Handle &#RRGGBB format
        Matcher matcherAmp = HEX_PATTERN_WITH_AMP.matcher(msg);
        StringBuffer buffer = new StringBuffer();
        while (matcherAmp.find()) {
            String color = "#" + matcherAmp.group(1);
            matcherAmp.appendReplacement(buffer, ChatColor.of(color).toString());
        }
        matcherAmp.appendTail(buffer);
        msg = buffer.toString();

        // Handle #RRGGBB format
        Matcher matcher = HEX_PATTERN.matcher(msg);
        buffer = new StringBuffer();
        while (matcher.find()) {
            String color = "#" + matcher.group(1);
            matcher.appendReplacement(buffer, ChatColor.of(color).toString());
        }
        matcher.appendTail(buffer);
        msg = buffer.toString();

        return ChatColor.translateAlternateColorCodes('&', msg);
    }

    /**
     * Colorize a list of strings.
     */
    public static List<String> colorize(List<String> lines) {
        List<String> colored = new ArrayList<>();
        if (lines != null) {
            for (String line : lines) {
                colored.add(colorize(line));
            }
        }
        return colored;
    }

    /**
     * Send colored message to a command sender.
     */
    public static void sendMessage(CommandSender sender, String message) {
        if (sender != null && message != null && !message.isEmpty()) {
            sender.sendMessage(colorize(message));
        }
    }

    /**
     * Broadcast a colored message to all players and console.
     */
    public static void broadcast(String message) {
        if (message != null && !message.isEmpty()) {
            String colored = colorize(message);
            for (Player player : Bukkit.getOnlinePlayers()) {
                player.sendMessage(colored);
            }
            Bukkit.getConsoleSender().sendMessage(colored);
        }
    }

    /**
     * Broadcast a list of colored messages to all players and console.
     */
    public static void broadcast(List<String> messages) {
        if (messages != null) {
            for (String line : messages) {
                broadcast(line);
            }
        }
    }

    /**
     * Broadcast a title and subtitle to all online players.
     */
    public static void broadcastTitle(String title, String subtitle, int fadeIn, int stay, int fadeOut) {
        String coloredTitle = colorize(title);
        String coloredSubtitle = colorize(subtitle);
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.sendTitle(coloredTitle, coloredSubtitle, fadeIn, stay, fadeOut);
        }
    }

    /**
     * Send an action bar to a specific player.
     */
    public static void sendActionBar(Player player, String message) {
        if (player != null && message != null) {
            player.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(colorize(message)));
        }
    }

    /**
     * Broadcast an action bar to all online players.
     */
    public static void broadcastActionBar(String message) {
        if (message != null) {
            net.md_5.bungee.api.chat.BaseComponent[] components = TextComponent.fromLegacyText(colorize(message));
            for (Player player : Bukkit.getOnlinePlayers()) {
                player.spigot().sendMessage(ChatMessageType.ACTION_BAR, components);
            }
        }
    }

    /**
     * Play a sound safely to all online players.
     */
    public static void broadcastSound(String soundName, float volume, float pitch) {
        if (soundName == null || soundName.trim().isEmpty()) {
            return;
        }
        try {
            Sound sound = Sound.valueOf(soundName.trim().toUpperCase());
            for (Player player : Bukkit.getOnlinePlayers()) {
                player.playSound(player.getLocation(), sound, volume, pitch);
            }
        } catch (IllegalArgumentException e) {
            Bukkit.getLogger().warning("[LocatorBar] Invalid sound name configured: " + soundName);
        }
    }

    /**
     * Format seconds into human readable format, e.g. "2m 15s" or "45s".
     */
    public static String formatTime(int totalSeconds) {
        if (totalSeconds < 0) {
            totalSeconds = 0;
        }
        int minutes = totalSeconds / 60;
        int seconds = totalSeconds % 60;
        if (minutes > 0) {
            return minutes + "m " + seconds + "s";
        } else {
            return seconds + "s";
        }
    }
}