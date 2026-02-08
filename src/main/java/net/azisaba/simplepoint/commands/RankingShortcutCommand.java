package net.azisaba.simplepoint.commands;

import net.azisaba.simplepoint.SimplePointPlugin;
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

public class RankingShortcutCommand implements CommandExecutor, TabCompleter {
    private final SimplePointPlugin plugin;

    public RankingShortcutCommand(SimplePointPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) return true;
        Player player = (Player) sender;

        if (args.length < 1) {
            player.sendMessage("§c使用法: /ranking <ポイント名>");
            return true;
        }

        String pointId = args[0];
        FileConfiguration config = plugin.getPointManager().getPointConfig(pointId);

        if (config == null) {
            player.sendMessage("§cそのポイント名は存在しません。");
            return true;
        }

        String displayName = plugin.getPointManager().getDisplayName(pointId);

        // 機能有効化チェック
        if (!config.getBoolean("_settings.ranking_enabled", true)) {
            player.sendMessage("§c" + displayName + " §cのランキングは現在非公開です。");
            return true;
        }

        // --- ランキング集計 ---
        Map<UUID, Integer> allScores = new HashMap<>();
        for (String key : config.getKeys(false)) {
            if (key.startsWith("_")) continue;
            try {
                UUID uuid = UUID.fromString(key);
                int total = config.getInt(key + ".total", 0);
                allScores.put(uuid, total);
            } catch (IllegalArgumentException ignored) {}
        }

        if (allScores.isEmpty()) {
            player.sendMessage("§cランキングデータが存在しません。");
            return true;
        }

        // ソート
        List<Map.Entry<UUID, Integer>> sortedList = new ArrayList<>(allScores.entrySet());
        sortedList.sort((a, b) -> b.getValue().compareTo(a.getValue()));

        // 自分の順位特定
        int myRank = -1;
        int myScore = 0;
        for (int i = 0; i < sortedList.size(); i++) {
            if (sortedList.get(i).getKey().equals(player.getUniqueId())) {
                myRank = i + 1;
                myScore = sortedList.get(i).getValue();
                break;
            }
        }

        // 表示
        player.sendMessage("§8§m----------§r " + displayName + " §6§lRANKING §8§m----------");
        for (int i = 0; i < Math.min(sortedList.size(), 7); i++) {
            UUID uuid = sortedList.get(i).getKey();
            OfflinePlayer op = Bukkit.getOfflinePlayer(uuid);
            int score = sortedList.get(i).getValue();
            String color = (i == 0) ? "§e" : (i == 1) ? "§f" : (i == 2) ? "§6" : "§7";
            player.sendMessage(color + (i + 1) + ". §r" + (op.getName() != null ? op.getName() : "Unknown") + " §7: §b" + score + " pt");
        }

        player.sendMessage("§8§m--------------------------------------");
        if (myRank != -1) {
            player.sendMessage("§fあなたの順位: §a" + myRank + "位 §7(累計: §e" + myScore + " pt§7)");
        } else {
            player.sendMessage("§fあなたの順位: §7圏外");
        }
        player.sendMessage("§8§m--------------------------------------");

        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 1.0f, 1.0f);
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> completions = new ArrayList<>();
            StringUtil.copyPartialMatches(args[0], plugin.getPointManager().getPointNames(), completions);
            Collections.sort(completions);
            return completions;
        }
        return new ArrayList<>();
    }
}