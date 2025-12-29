package net.azisaba.simplepoint;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;

import java.util.*;
import java.util.stream.Collectors;

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
            p.sendMessage("§e/spt ranking <ポイント名> §7- 上位7名のランキングを確認");
            p.sendMessage("§e/spt help §7- このヘルプを表示");
            p.sendMessage("§8§m----------------------------");
            return true;
        }

        String sub = args[0].toLowerCase();

        if (sub.equals("myp")) {
            if (args.length < 2) {
                p.sendMessage("§c使用法: /spt myp <ポイント名>");
                return true;
            }
            String pointId = args[1];
            if (plugin.getPointManager().getPointConfig(pointId) == null) {
                p.sendMessage("§cそのポイント名は存在しません。");
                return true;
            }

            // ✨ 表示名を取得
            String displayName = plugin.getPointManager().getDisplayName(pointId);
            int current = plugin.getPointManager().getPoint(pointId, p.getUniqueId());
            int total = plugin.getPointManager().getTotalPoint(pointId, p.getUniqueId());

            p.sendMessage("§8§m-------§r " + displayName + " §b§lSTATUS §8§m-------");
            p.sendMessage("§7現在の所持ポイント: §e" + current + " pt");
            p.sendMessage("§7これまでの累計獲得: §a" + total + " pt");
            p.sendMessage("§8§m---------------------");
            return true;
        }

        else if (sub.equals("reward")) {
            if (args.length < 2) {
                p.sendMessage("§c使用法: /spt reward <ポイント名>");
                return true;
            }
            String pointId = args[1];
            FileConfiguration cfg = plugin.getPointManager().getPointConfig(pointId);
            String displayName = plugin.getPointManager().getDisplayName(pointId);

            if (cfg == null) {
                p.sendMessage("§cそのポイント名は存在しません。");
                return true;
            }
            if (!cfg.getBoolean("_settings.ranking_enabled", true)) {
                p.sendMessage("§c現在、" + displayName + " §cの報酬ショップは利用できません。");
                return true;
            }
            if (!cfg.getBoolean("_settings.reward_enabled", true)) {
                p.sendMessage("§c現在、" + displayName + " §cの報酬ショップは閉鎖されています。");
                return true;
            }
            // 内部IDを使ってGUIを開く
            plugin.getGuiManager().openRewardGUI(p, pointId, false);
            return true;
        }

        else if (sub.equals("ranking")) {
            if (args.length < 2) {
                p.sendMessage("§c使用法: /spt ranking <ポイント名>");
                return true;
            }
            showPersonalRanking(p, args[1]);
            return true;
        }

        return true;
    }

    private void showPersonalRanking(Player player, String pointId) {
        FileConfiguration config = plugin.getPointManager().getPointConfig(pointId);
        String displayName = plugin.getPointManager().getDisplayName(pointId);

        if (config == null || !config.getBoolean("_settings.ranking_enabled", true)) {
            player.sendMessage("§c" + displayName + " §cのランキングは非公開です。");
            return;
        }

        Map<UUID, Integer> allScores = new HashMap<>();
        for (String key : config.getKeys(false)) {
            if (key.startsWith("_")) continue;
            try {
                UUID uuid = UUID.fromString(key);
                int total = config.getInt(key + ".total", 0);
                allScores.put(uuid, total);
            } catch (IllegalArgumentException ignored) {}
        }

        List<Map.Entry<UUID, Integer>> sortedList = new ArrayList<>(allScores.entrySet());
        sortedList.sort((a, b) -> b.getValue().compareTo(a.getValue()));

        int myRank = -1;
        int myScore = 0;
        for (int i = 0; i < sortedList.size(); i++) {
            if (sortedList.get(i).getKey().equals(player.getUniqueId())) {
                myRank = i + 1;
                myScore = sortedList.get(i).getValue();
                break;
            }
        }

        player.sendMessage("§8§m----------§r " + displayName + " §6§lRANKING §8§m----------");

        for (int i = 0; i < Math.min(sortedList.size(), 7); i++) {
            UUID uuid = sortedList.get(i).getKey();
            OfflinePlayer op = Bukkit.getOfflinePlayer(uuid);
            String name = op.getName();
            int score = sortedList.get(i).getValue();

            String color = (i == 0) ? "§e" : (i == 1) ? "§f" : (i == 2) ? "§6" : "§7";
            player.sendMessage(color + (i + 1) + ". §r" + (name != null ? name : "Unknown") + " §7: §b" + score + " pt");
        }

        player.sendMessage("§8§m--------------------------------------");

        if (myRank != -1) {
            player.sendMessage("§fあなたの順位: §a" + myRank + "位 §7(累計: §e" + myScore + " pt§7)");
        } else {
            player.sendMessage("§fあなたの順位: §7圏外");
        }
        player.sendMessage("§8§m--------------------------------------");
        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 1.0f, 1.0f);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        if (args.length == 1) {
            StringUtil.copyPartialMatches(args[0], Arrays.asList("myp", "reward", "ranking", "help"), completions);
        } else if (args.length == 2 && Arrays.asList("myp", "reward", "ranking").contains(args[0].toLowerCase())) {
            // タブ補完は内部IDを出す（コマンド引数がIDであるため）
            StringUtil.copyPartialMatches(args[1], plugin.getPointManager().getPointNames(), completions);
        }
        return completions;
    }
}