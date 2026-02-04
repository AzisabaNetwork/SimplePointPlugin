package net.azisaba.simplepoint;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;
import java.io.File;
import java.util.*;

public class SPPTTabCompleter implements TabCompleter {
    private final SimplePointPlugin plugin;

    public SPPTTabCompleter(SimplePointPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            StringUtil.copyPartialMatches(args[0], Arrays.asList(
                    "create", "teamcreate", "join", "setpoint", "setpoint_value",
                    "info", "rewardgui", "toggle", "multiplier", "member", "delete", "help"
            ), completions);
            return completions;
        }

        String sub = args[0].toLowerCase();

        // 第2引数: グループ名の補完 (teams/team フォルダ内のフォルダ名)
        if (args.length == 2) {
            File teamDir = new File(plugin.getDataFolder(), "teams/team");
            if (teamDir.exists() && teamDir.list() != null) {
                StringUtil.copyPartialMatches(args[1], Arrays.asList(teamDir.list()), completions);
            }
            return completions;
        }

        // 第3引数: チームID または ポイントID または 倍率
        if (args.length == 3) {
            if (sub.equals("setpoint")) {
                completions.addAll(plugin.getPointManager().getPointNames());
            } else if (Arrays.asList("join", "info", "member", "delete", "setpoint_value", "teamcreate").contains(sub)) {
                File groupDir = new File(plugin.getDataFolder(), "teams/team/" + args[1]);
                if (groupDir.exists() && groupDir.list() != null) {
                    for (String f : groupDir.list()) completions.add(f.replace(".yml", ""));
                }
            }
            return completions;
        }



        // 第4引数: プレイヤー名
        if (args.length == 4) {
            if (sub.equals("join")) {
                for (Player p : Bukkit.getOnlinePlayers()) completions.add(p.getName());
            }
        }

        return completions;
    }
}