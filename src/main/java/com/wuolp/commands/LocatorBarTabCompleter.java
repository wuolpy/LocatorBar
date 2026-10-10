package com.wuolp.commands;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.util.StringUtil;

public class LocatorBarTabCompleter implements TabCompleter {

    private static final List<String> ROOT_SUBS = Arrays.asList(
        "start", "stop", "status", "toggle", "set", "reload", "update", "hotreload", "help"
    );

    private static final List<String> SET_SUBS = Arrays.asList(
        "duration", "mininterval", "maxinterval"
    );

    private static final List<String> UPDATE_SUBS = Arrays.asList(
        "check", "download", "reload"
    );

    private static final List<String> DURATION_SUGGESTIONS = Arrays.asList(
        "30", "60", "120", "300"
    );

    private static final List<String> MIN_INTERVAL_SUGGESTIONS = Arrays.asList(
        "60", "180", "300", "600"
    );

    private static final List<String> MAX_INTERVAL_SUGGESTIONS = Arrays.asList(
        "300", "600", "900", "1800"
    );

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission("locatorbar.admin")) {
            return Collections.emptyList();
        }

        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            StringUtil.copyPartialMatches(args[0], ROOT_SUBS, completions);
            Collections.sort(completions);
            return completions;
        }

        if (args.length == 2) {
            if (args[0].equalsIgnoreCase("start")) {
                StringUtil.copyPartialMatches(args[1], DURATION_SUGGESTIONS, completions);
                Collections.sort(completions);
                return completions;
            }
            if (args[0].equalsIgnoreCase("set")) {
                StringUtil.copyPartialMatches(args[1], SET_SUBS, completions);
                Collections.sort(completions);
                return completions;
            }
            if (args[0].equalsIgnoreCase("update")) {
                StringUtil.copyPartialMatches(args[1], UPDATE_SUBS, completions);
                Collections.sort(completions);
                return completions;
            }
        }

        if (args.length == 3 && args[0].equalsIgnoreCase("set")) {
            if (args[1].equalsIgnoreCase("duration")) {
                StringUtil.copyPartialMatches(args[2], DURATION_SUGGESTIONS, completions);
                Collections.sort(completions);
                return completions;
            }
            if (args[1].equalsIgnoreCase("mininterval")) {
                StringUtil.copyPartialMatches(args[2], MIN_INTERVAL_SUGGESTIONS, completions);
                Collections.sort(completions);
                return completions;
            }
            if (args[1].equalsIgnoreCase("maxinterval")) {
                StringUtil.copyPartialMatches(args[2], MAX_INTERVAL_SUGGESTIONS, completions);
                Collections.sort(completions);
                return completions;
            }
        }

        return Collections.emptyList();
    }
}
