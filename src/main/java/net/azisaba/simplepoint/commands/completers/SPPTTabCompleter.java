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
                    "setjoingui", "teamjoingui", "rewardgui", "member", "info", "help",
                    "userinfo", "deletegroup" // 追加
            );
            return StringUtil.copyPartialMatches(args[0], subs, completions);
        }

        String sub = args[0].toLowerCase();

        // 2. 第2引数: 基本的に「グループID」
        if (args.length == 2) {
            if (sub.equals("help") || sub.equals("create")) return completions;

            // userinfo の場合はプレイヤー名を補完
            if (sub.equals("userinfo")) {
                List<String> players = Bukkit.getOnlinePlayers().stream()
                        .map(Player::getName)
                        .collect(Collectors.toList());
                return StringUtil.copyPartialMatches(args[1], players, completions);
            }

            // それ以外はグループディレクトリからIDを補完
            File dir = new File(plugin.getDataFolder(), "teams/team");
            if (dir.exists() && dir.list() != null) {
                List<String> groups = Arrays.asList(dir.list());
                return StringUtil.copyPartialMatches(args[1], groups, completions);
            }
        }

        // 3. 第3引数
        if (args.length == 3) {
            switch (sub) {
                case "multiplier":
                    // グループ内のチームを補完 (チーム個別倍率用)
                    return StringUtil.copyPartialMatches(args[2], getTeams(args[1]), completions);
                case "setpoint":
                    return StringUtil.copyPartialMatches(args[2], plugin.getPointManager().getPointNames(), completions);
                case "teamcreate":
                    return completions;
                case "setpoint_value":
                case "vsteam":
                case "join":
                case "member":
                case "info":
                case "leave":
                case "setjoingui":
                    return StringUtil.copyPartialMatches(args[2], getTeams(args[1]), completions);
            }
        }

        // 4. 第4引数
        if (args.length == 4) {
            switch (sub) {
                case "multiplier":
                    // 倍率の例を出す
                    return StringUtil.copyPartialMatches(args[3], Arrays.asList("1.5", "2.0"), completions);
                case "join":
                    List<String> players = Bukkit.getOnlinePlayers().stream().map(Player::getName).collect(Collectors.toList());
                    return StringUtil.copyPartialMatches(args[3], players, completions);
                case "vsteam":
                case "setjoingui":
                    List<String> teams = getTeams(args[1]);
                    teams.remove(args[2]);
                    return StringUtil.copyPartialMatches(args[3], teams, completions);
                case "leave":
                    List<String> memberNames = new ArrayList<>();
                    for (String name : plugin.getTeamManager().getMemberNames(args[1], args[2])) {
                        memberNames.add(ChatColor.stripColor(name).split(" ")[0]);
                    }
                    return StringUtil.copyPartialMatches(args[3], memberNames, completions);
            }
        }

        // 5. 第5引数: 倍率開始時間
        if (args.length == 5 && sub.equals("multiplier")) {
            String now = new java.text.SimpleDateFormat("yyyy/MM/dd-HH:mm").format(new java.util.Date());
            return StringUtil.copyPartialMatches(args[4], Collections.singletonList(now), completions);
        }

        // 6. 第6引数: 倍率終了時間
        if (args.length == 6 && sub.equals("multiplier")) {
            // 現在時刻から1時間後のヒントを出す
            Calendar cal = Calendar.getInstance();
            cal.add(Calendar.HOUR, 1);
            String end = new java.text.SimpleDateFormat("yyyy/MM/dd-HH:mm").format(cal.getTime());
            return StringUtil.copyPartialMatches(args[5], Collections.singletonList(end), completions);
        }

        // 既存の setjoingui 用補完
        if (sub.equals("setjoingui")) {
            if (args.length == 5) return StringUtil.copyPartialMatches(args[4], Arrays.asList("choice", "random"), completions);
            if (args.length == 6) return StringUtil.copyPartialMatches(args[5], Arrays.asList("true", "false"), completions);
        }

        return completions;
    }

    private List<String> getTeams(String group) {
        File dir = new File(plugin.getDataFolder(), "teams/team/" + group);
        if (!dir.exists() || dir.list() == null) return new ArrayList<>();
        return Arrays.stream(dir.list())
                .filter(s -> s.endsWith(".yml"))
                .map(s -> s.replace(".yml", ""))
                .collect(Collectors.toList());
    }
}