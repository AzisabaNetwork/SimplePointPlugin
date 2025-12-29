package net.azisaba.simplepoint;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;

import java.util.*;

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

        Player player = (Player) sender;
        if (args.length < 2) {
            player.sendMessage("§c使用法: /spt <myp|ranking|reward> <ポイント名>");
            return true;
        }

        String sub = args[0].toLowerCase();
        String pointName = args[1];
        PointManager pm = plugin.getPointManager();
        FileConfiguration config = pm.getPointConfig(pointName);

        if (config == null) {
            player.sendMessage("§c指定されたポイントが存在しません。");
            return true;
        }

        switch (sub) {
            case "myp":
                int amount = pm.getPoint(pointName, player.getUniqueId());
                player.sendMessage("§bあなたの " + pointName + " 保有数: §f" + amount);
                break;

            case "ranking":
                // 鯖民用ランキング（実行者にのみ見える）
                showPrivateRanking(player, pointName, config);
                break;

            case "reward":
                // 報酬GUIが有効かチェック（管理者のtoggle設定）
                if (!config.getBoolean("_settings.ranking_enabled", true)) {
                    player.sendMessage("§c現在、この報酬GUIは利用できません。");
                    return true;
                }
                // TODO: GUIを開く処理 (次のステップで実装)
                player.sendMessage("§a" + pointName + " の報酬GUIを開きます...（実装中）");
                break;

            default:
                player.sendMessage("§c不明なサブコマンドです。");
                break;
        }

        return true;
    }

    private void showPrivateRanking(Player player, String point, FileConfiguration config) {
        Map<String, Integer> scores = new HashMap<>();
        for (String key : config.getKeys(false)) {
            if (key.startsWith("_")) continue;
            try {
                String name = Bukkit.getOfflinePlayer(UUID.fromString(key)).getName();
                if (name != null) scores.put(name, config.getInt(key));
            } catch (IllegalArgumentException e) {
                // UUIDではないキーをスキップ
            }
        }

        List<Map.Entry<String, Integer>> list = new ArrayList<>(scores.entrySet());
        list.sort((a, b) -> b.getValue().compareTo(a.getValue()));

        player.sendMessage("§e--- " + point + " ランキング (個人表示) ---");
        for (int i = 0; i < Math.min(list.size(), 10); i++) {
            player.sendMessage("§7" + (i + 1) + ". §f" + list.get(i).getKey() + ": §b" + list.get(i).getValue());
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        if (args.length == 1) {
            StringUtil.copyPartialMatches(args[0], Arrays.asList("myp", "ranking", "reward"), completions);
        } else if (args.length == 2) {
            StringUtil.copyPartialMatches(args[1], plugin.getPointManager().getPointNames(), completions);
        }
        return completions;
    }
}