package net.azisaba.simplepoint;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import java.util.Map;

public class SPTTCommand implements CommandExecutor {
    private final SimplePointPlugin plugin;

    public SPTTCommand(SimplePointPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length < 2) {
            sender.sendMessage("§c使用法: /sptt <info|ranking|reward> <チーム名>");
            return true;
        }

        String sub = args[0].toLowerCase();
        String teamName = args[1];
        TeamManager tm = plugin.getTeamManager();

        if (!tm.getTeamNames().contains(teamName)) {
            sender.sendMessage("§cそのチームは存在しません。");
            return true;
        }

        switch (sub) {
            case "info":
                sender.sendMessage("§8§m----------§r §b§lTEAM INFO §8§m----------");
                sender.sendMessage("§7チーム名: §f" + teamName);
                sender.sendMessage("§7人数: §f" + tm.getMemberCount(teamName) + " 人");
                sender.sendMessage("§7総獲得ポイント: §e" + tm.getTeamPoints(teamName) + " pt");
                sender.sendMessage("§8§m----------------------------");
                break;

            case "ranking":
                sender.sendMessage("§6§l" + teamName + " 貢献度ランキング (TOP5)");
                Map<String, Integer> ranking = tm.getContributionRanking(teamName);
                ranking.entrySet().stream()
                        .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                        .limit(5)
                        .forEach(e -> sender.sendMessage("§7- §f" + e.getKey() + ": §e" + e.getValue() + " pt"));
                break;

            case "reward":
                if (!(sender instanceof Player)) return true;
                // プレイヤーがそのチームに所属しているかチェック（任意）
                plugin.getGuiManager().openRewardGUI((Player) sender, "TEAMREWARD_" + teamName, false);
                break;
        }
        return true;
    }
}