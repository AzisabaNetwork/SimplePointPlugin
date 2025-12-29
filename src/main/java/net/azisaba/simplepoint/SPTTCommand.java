package net.azisaba.simplepoint;

import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;
import java.util.*;

public class SPTTCommand implements CommandExecutor, TabCompleter {
    private final SimplePointPlugin plugin;
    public SPTTCommand(SimplePointPlugin plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length < 2) {
            sender.sendMessage("§c使用法: /sptt <info|ranking|reward|join> <チーム名>");
            return true;
        }

        String sub = args[0].toLowerCase();
        String teamName = args[1];
        TeamManager tm = plugin.getTeamManager();

        if (!sub.equals("join") && !tm.getTeamNames().contains(teamName)) {
            sender.sendMessage("§cそのチームは存在しません。");
            return true;
        }

        switch (sub) {
            case "join":
                if (!(sender instanceof Player)) return true;
                String result = tm.joinRandomTeam(((Player) sender).getUniqueId());
                sender.sendMessage("§a結果: " + result);
                break;
            case "info":
                sender.sendMessage("§bTeam: " + teamName + " §f| 人数: " + tm.getMemberCount(teamName) + " | 総pt: " + tm.getTeamPoints(teamName));
                break;
            case "ranking":
                sender.sendMessage("§e--- " + teamName + " 貢献度ランキング ---");
                tm.getContributionRanking(teamName).entrySet().stream()
                        .sorted(Map.Entry.<String, Integer>comparingByValue().reversed()).limit(5)
                        .forEach(e -> sender.sendMessage("§7- " + e.getKey() + ": " + e.getValue() + "pt"));
                break;
            case "reward":
                if (!(sender instanceof Player)) return true;
                plugin.getGuiManager().openRewardGUI((Player) sender, "TEAMREWARD_" + teamName, false);
                break;
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) return StringUtil.copyPartialMatches(args[0], Arrays.asList("info", "ranking", "reward", "join"), new ArrayList<>());
        if (args.length == 2) return StringUtil.copyPartialMatches(args[1], new ArrayList<>(plugin.getTeamManager().getTeamNames()), new ArrayList<>());
        return new ArrayList<>();
    }
}