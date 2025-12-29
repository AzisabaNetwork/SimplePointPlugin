package net.azisaba.simplepoint;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import java.util.*;

public class SPTTCommand implements CommandExecutor {
    private final SimplePointPlugin plugin;

    public SPTTCommand(SimplePointPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length < 2) return false;
        String sub = args[0].toLowerCase();
        String teamName = args[1];
        TeamManager tm = plugin.getTeamManager();

        switch (sub) {
            case "info":
                sender.sendMessage("§e=== Team Info: " + teamName + " ===");
                sender.sendMessage("§f人数: §b" + tm.getMemberCount(teamName) + "人");
                sender.sendMessage("§f総ポイント: §b" + tm.getTeamPoints(teamName) + " pt");
                // TODO: 報酬進捗の表示
                break;

            case "ranking":
                sender.sendMessage("§e--- " + teamName + " 貢献度ランキング ---");
                Map<String, Integer> contrib = tm.getContributionRanking(teamName);
                contrib.entrySet().stream()
                        .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                        .limit(5)
                        .forEach(e -> sender.sendMessage("§7" + e.getKey() + ": §f" + e.getValue() + " pt"));
                break;

            case "reward":
                if (!(sender instanceof Player)) return true;
                plugin.getGuiManager().openRewardGUI((Player)sender, "TEAM_" + teamName, false);
                break;
        }
        return true;
    }
}