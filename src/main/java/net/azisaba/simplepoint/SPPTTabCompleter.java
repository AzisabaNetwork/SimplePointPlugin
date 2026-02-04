package net.azisaba.simplepoint;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;
import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

public class SPPTTabCompleter implements TabCompleter {
    private final SimplePointPlugin plugin;

    public SPPTTabCompleter(SimplePointPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        // 1. サブコマンドの補完
        if (args.length == 1) {
            List<String> subs = Arrays.asList("leave", "finishvsmode", "create", "teamcreate", "join", "setpoint", "info", "rewardgui", "multiplier", "vsteam", "moderninfo", "teamjoingui", "setjoingui", "member", "toggle", "setpoint_value", "help");
            return StringUtil.copyPartialMatches(args[0], subs, completions);
        }

        String sub = args[0].toLowerCase();

        // 2. 第2引数: グループ名
        if (args.length == 2) {
            File dir = new File(plugin.getDataFolder(), "teams/team");
            if (dir.exists() && dir.list() != null) {
                List<String> groups = Arrays.asList(dir.list());
                return StringUtil.copyPartialMatches(args[1], groups, completions);
            }
        }

        // 3. 第3引数: チーム名 または ポイント名
        if (args.length == 3) {
            if (sub.equals("setpoint")) {
                return StringUtil.copyPartialMatches(args[2], plugin.getPointManager().getPointNames(), completions);
            }
            // leave, vsteam, join 等のチームリスト取得
            return StringUtil.copyPartialMatches(args[2], getTeams(args[1]), completions);
        }

        // 4. 第4引数: プレイヤー名 (join / leave) または チーム2 (vsteam / setjoingui)
        if (args.length == 4) {
            if (sub.equals("vsteam") || sub.equals("setjoingui")) {
                List<String> teams = getTeams(args[1]);
                teams.remove(args[2]); // チーム1を除外
                return StringUtil.copyPartialMatches(args[3], teams, completions);
            }

            if (sub.equals("join")) {
                List<String> players = Bukkit.getOnlinePlayers().stream()
                        .map(Player::getName)
                        .collect(Collectors.toList());
                return StringUtil.copyPartialMatches(args[3], players, completions);
            }

            if (sub.equals("leave")) {
                if (args.length == 3) {
                    return StringUtil.copyPartialMatches(args[2], getTeams(args[1]), completions);
                }
                if (args.length == 4) {
                    // 修正：装飾やポイントを含まない「純粋な名前」のリストを取得
                    List<String> memberNames = new ArrayList<>();
                    for (String name : plugin.getTeamManager().getMemberNames(args[1], args[2])) {
                        memberNames.add(ChatColor.stripColor(name).split(" ")[0]);
                    }
                    return StringUtil.copyPartialMatches(args[3], memberNames, completions);
                }
            }
        }

        // 5. 第5引数以降 (setjoingui)
        if (args.length == 5 && sub.equals("setjoingui")) {
            return StringUtil.copyPartialMatches(args[4], Arrays.asList("choice", "random"), completions);
        }
        if (args.length == 6 && sub.equals("setjoingui")) {
            return StringUtil.copyPartialMatches(args[5], Arrays.asList("true", "false"), completions);
        }

        return completions;
    }

    private List<String> getTeams(String group) {
        File dir = new File(plugin.getDataFolder(), "teams/team/" + group);
        if (!dir.exists() || dir.list() == null) return new ArrayList<>();
        return Arrays.stream(dir.list())
                .map(s -> s.replace(".yml", ""))
                .collect(Collectors.toList());
    }
}