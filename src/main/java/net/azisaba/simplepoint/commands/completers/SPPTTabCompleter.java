package net.azisaba.simplepoint.commands.completers;

import net.azisaba.simplepoint.SimplePointPlugin;
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

        // 1. 第1引数: サブコマンド
        if (args.length == 1) {
            List<String> subs = Arrays.asList(
                    "create", "teamcreate", "join", "leave", "vsteam", "finishvsmode",
                    "multiplier", "setpoint", "teaminfo", "toggle", "setpoint_value",
                    "setjoingui", "teamjoingui", "rewardgui", "member", "info", "help"
            );
            return StringUtil.copyPartialMatches(args[0], subs, completions);
        }

        String sub = args[0].toLowerCase();

        // 2. 第2引数: ほとんどが「グループID」
        if (args.length == 2) {
            // 例外的なサブコマンド
            if (sub.equals("help") || sub.equals("create")) return completions;

            // グループディレクトリからIDを補完
            File dir = new File(plugin.getDataFolder(), "teams/team");
            if (dir.exists() && dir.list() != null) {
                List<String> groups = Arrays.asList(dir.list());
                return StringUtil.copyPartialMatches(args[1], groups, completions);
            }
        }

        // 3. 第3引数: チーム名 / ポイント名 / 倍率
        if (args.length == 3) {
            switch (sub) {
                case "setpoint":
                    // ポイントIDの補完 (SimplePoint本体から取得)
                    return StringUtil.copyPartialMatches(args[2], plugin.getPointManager().getPointNames(), completions);
                case "teamcreate":
                    return completions; // ID入力なので補完なし
                case "multiplier":
                    return StringUtil.copyPartialMatches(args[2], Collections.singletonList("1.5"), completions);
                case "setpoint_value":
                case "vsteam":
                case "join":
                case "member":
                case "info":
                case "leave":
                    // チーム名の補完
                    return StringUtil.copyPartialMatches(args[2], getTeams(args[1]), completions);
                case "setjoingui":
                    // 第3引数(T1)
                    return StringUtil.copyPartialMatches(args[2], getTeams(args[1]), completions);
            }
        }

        // 4. 第4引数: プレイヤー名 / チーム2 / 期間
        if (args.length == 4) {
            switch (sub) {
                case "join":
                    List<String> players = Bukkit.getOnlinePlayers().stream()
                            .map(Player::getName)
                            .collect(Collectors.toList());
                    return StringUtil.copyPartialMatches(args[3], players, completions);
                case "vsteam":
                case "setjoingui":
                    // 第4引数(T2)
                    List<String> teams = getTeams(args[1]);
                    teams.remove(args[2]); // T1を除外
                    return StringUtil.copyPartialMatches(args[3], teams, completions);
                case "leave":
                    // 純粋なメンバー名のリスト
                    List<String> memberNames = new ArrayList<>();
                    for (String name : plugin.getTeamManager().getMemberNames(args[1], args[2])) {
                        memberNames.add(ChatColor.stripColor(name).split(" ")[0]);
                    }
                    return StringUtil.copyPartialMatches(args[3], memberNames, completions);
                case "multiplier":
                    return StringUtil.copyPartialMatches(args[3], Collections.singletonList("60"), completions); // 分
            }
        }

        // 5. 第5引数以降
        if (sub.equals("setjoingui")) {
            if (args.length == 5) {
                return StringUtil.copyPartialMatches(args[4], Arrays.asList("choice", "random"), completions);
            }
            if (args.length == 6) {
                return StringUtil.copyPartialMatches(args[5], Arrays.asList("true", "false"), completions);
            }
        }

        return completions;
    }

    /**
     * 指定されたグループ内のチームリストを取得
     */
    private List<String> getTeams(String group) {
        File dir = new File(plugin.getDataFolder(), "teams/team/" + group);
        if (!dir.exists() || dir.list() == null) return new ArrayList<>();
        return Arrays.stream(dir.list())
                .filter(s -> s.endsWith(".yml"))
                .map(s -> s.replace(".yml", ""))
                .collect(Collectors.toList());
    }
}