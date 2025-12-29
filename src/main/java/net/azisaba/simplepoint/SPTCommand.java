package net.azisaba.simplepoint;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SPTCommand implements CommandExecutor, TabCompleter {
    private final SimplePointPlugin plugin;

    public SPTCommand(SimplePointPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cこのコマンドはプレイヤーのみ実行可能です。");
            return true;
        }
        Player p = (Player) sender;

        if (args.length == 0 || args[0].equalsIgnoreCase("help")) {
            p.sendMessage("§8§m----------§r §6§lSPT HELP §8§m----------");
            p.sendMessage("§e/spt myp <ポイント名> §7- 所持・累計ポイントを確認");
            p.sendMessage("§e/spt reward <ポイント名> §7- 報酬ショップを開く");
            p.sendMessage("§8§m----------------------------");
            return true;
        }

        String sub = args[0].toLowerCase();

        if (sub.equals("myp")) {
            if (args.length < 2) {
                p.sendMessage("§c使用法: /spt myp <ポイント名>");
                return true;
            }
            String pointName = args[1];
            if (plugin.getPointManager().getPointConfig(pointName) == null) {
                p.sendMessage("§cそのポイント名は存在しません。");
                return true;
            }

            int current = plugin.getPointManager().getPoint(pointName, p.getUniqueId());
            int total = plugin.getPointManager().getTotalPoint(pointName, p.getUniqueId());

            p.sendMessage("§8§m----------§r §b§l" + pointName.toUpperCase() + " STATUS §8§m----------");
            p.sendMessage("§7現在の所持ポイント: §e" + current + " pt");
            p.sendMessage("§7これまでの累計獲得: §a" + total + " pt");
            p.sendMessage("§8§m----------------------------");
            return true;
        }

        else if (sub.equals("reward")) {
            if (args.length < 2) {
                p.sendMessage("§c使用法: /spt reward <ポイント名>");
                return true;
            }
            String pointName = args[1];
            // togglerankingで設定した「ショップ有効化」フラグをチェック
            if (!plugin.getPointManager().getPointConfig(pointName).getBoolean("_settings.ranking_enabled", true)) {
                p.sendMessage("§c現在、この報酬ショップは利用できません。");
                return true;
            }
            plugin.getGuiManager().openRewardGUI(p, pointName, false);
            return true;
        }

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        if (args.length == 1) {
            StringUtil.copyPartialMatches(args[0], Arrays.asList("myp", "reward", "help"), completions);
        } else if (args.length == 2 && (args[0].equalsIgnoreCase("myp") || args[0].equalsIgnoreCase("reward"))) {
            StringUtil.copyPartialMatches(args[1], plugin.getPointManager().getPointNames(), completions);
        }
        return completions;
    }
}