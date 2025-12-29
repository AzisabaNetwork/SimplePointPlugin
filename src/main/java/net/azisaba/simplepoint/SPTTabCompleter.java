package net.azisaba.simplepoint;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.util.StringUtil;

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

        // 1. サブコマンドの補完: /spt <myp|reward|ranking>
        if (args.length == 1) {
            List<String> subCommands = Arrays.asList("myp", "reward", "ranking");
            StringUtil.copyPartialMatches(args[0], subCommands, completions);
        }

        // 2. ポイントIDの補完: /spt <subcommand> <ID>
        else if (args.length == 2) {
            // すべてのサブコマンド(myp, reward, ranking)が第2引数にポイントIDを必要とする
            List<String> pointIds = plugin.getPointManager().getPointNames();
            StringUtil.copyPartialMatches(args[1], pointIds, completions);
        }

        Collections.sort(completions);
        return completions;
    }
}