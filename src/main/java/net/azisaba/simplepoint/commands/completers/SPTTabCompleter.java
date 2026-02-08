package net.azisaba.simplepoint.commands.completers;

import net.azisaba.simplepoint.SimplePointPlugin;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.util.StringUtil;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class SPTTabCompleter implements TabCompleter {
    private final SimplePointPlugin plugin;

    public SPTTabCompleter(SimplePointPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        // 1. サブコマンドの補完: /spt <サブコマンド>
        if (args.length == 1) {
            List<String> subCommands = Arrays.asList("myinfo","myp", "reward", "ranking", "teaminfo", "teamreward", "help");
            StringUtil.copyPartialMatches(args[0], subCommands, completions);
        }

        // 2. 第2引数の補完
        else if (args.length == 2) {
            String sub = args[0].toLowerCase();

            // ポイント系: 個人のポイントIDを補完
            if (Arrays.asList("myp", "reward", "ranking").contains(sub)) {
                List<String> pointIds = plugin.getPointManager().getPointNames();
                StringUtil.copyPartialMatches(args[1], pointIds, completions);
            }

            // チーム系: グループ名を補完
            else if (Arrays.asList("teaminfo", "teamreward").contains(sub)) {
                File teamDir = new File(plugin.getDataFolder(), "teams/team");
                if (teamDir.exists() && teamDir.list() != null) {
                    List<String> groups = Arrays.asList(teamDir.list());
                    StringUtil.copyPartialMatches(args[1], groups, completions);
                }
            }
        }

        Collections.sort(completions);
        return completions;
    }
}