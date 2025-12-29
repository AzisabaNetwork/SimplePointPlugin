package net.azisaba.simplepoint;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.util.StringUtil;

import java.util.*;
import java.util.stream.Collectors;

public class SPPCommand implements CommandExecutor, TabCompleter {

    private final SimplePointPlugin plugin;

    public SPPCommand(SimplePointPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0 || args[0].equalsIgnoreCase("help")) {
            sendHelp(sender);
            return true;
        }

        String sub = args[0].toLowerCase();
        PointManager pm = plugin.getPointManager();

        switch (sub) {
            case "reload":
                plugin.reloadConfig();
                sender.sendMessage("§aプラグインの設定をリロードしました。");
                return true;

            case "create":
                if (args.length < 2) return false;
                if (pm.createPointType(args[1])) sender.sendMessage("§aポイント「" + args[1] + "」を作成しました。");
                else sender.sendMessage("§c作成に失敗しました。");
                return true;

            case "add":
            case "remove":
            case "set":
                if (args.length < 4) return false;
                handlePointUpdate(sender, sub, args[1], args[2], args[3]);
                return true;

            case "score":
                if (args.length < 3) return false;
                OfflinePlayer scoreTarget = Bukkit.getOfflinePlayer(args[2]);
                int score = pm.getPoint(args[1], scoreTarget.getUniqueId());
                sender.sendMessage("§b" + scoreTarget.getName() + " の " + args[1] + ": §f" + score);
                return true;

            case "toggle":
            case "toggleranking":
                if (args.length < 2) return false;
                handleToggle(sender, args[1], sub.equals("toggle") ? "_settings.enabled" : "_settings.ranking_enabled");
                return true;

            case "ranking":
                if (args.length < 2) return false;
                showRanking(sender, args[1]);
                return true;
        }
        return true;
    }

    private void handlePointUpdate(CommandSender sender, String action, String point, String player, String val) {
        OfflinePlayer target = Bukkit.getOfflinePlayer(player);
        int amount;
        try { amount = Integer.parseInt(val); } catch (NumberFormatException e) { return; }

        if (action.equals("add")) plugin.getPointManager().addPoint(point, target.getUniqueId(), amount);
        else if (action.equals("remove")) plugin.getPointManager().addPoint(point, target.getUniqueId(), -amount);
        else plugin.getPointManager().setPoint(point, target.getUniqueId(), amount);

        sender.sendMessage("§a" + target.getName() + " の " + point + " を更新しました。");
    }

    private void handleToggle(CommandSender sender, String point, String path) {
        FileConfiguration config = plugin.getPointManager().getPointConfig(point);
        if (config == null) return;
        boolean current = config.getBoolean(path, true);
        config.set(path, !current);
        plugin.getPointManager().saveConfig(point, config);
        sender.sendMessage("§a" + point + " の " + path + " を " + (!current) + " に変更しました。");
    }

    private void showRanking(CommandSender sender, String point) {
        FileConfiguration config = plugin.getPointManager().getPointConfig(point);
        if (config == null) return;

        Map<String, Integer> scores = new HashMap<>();
        for (String key : config.getKeys(false)) {
            if (key.startsWith("_")) continue;
            scores.put(Bukkit.getOfflinePlayer(UUID.fromString(key)).getName(), config.getInt(key));
        }

        List<Map.Entry<String, Integer>> list = new ArrayList<>(scores.entrySet());
        list.sort((a, b) -> b.getValue().compareTo(a.getValue()));

        sender.sendMessage("§e--- " + point + " ランキング ---");
        for (int i = 0; i < Math.min(list.size(), 10); i++) {
            sender.sendMessage("§7" + (i + 1) + ". §f" + list.get(i).getKey() + ": §b" + list.get(i).getValue());
        }
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage("§e=== SimplePointPlugin Help ===");
        sender.sendMessage("§f/spp create <name> - ポイント作成");
        sender.sendMessage("§f/spp add/remove/set <name> <player> <num>");
        sender.sendMessage("§f/spp score <name> <player> - 確認");
        sender.sendMessage("§f/spp ranking <name> - ランキング表示");
        sender.sendMessage("§f/spp toggle <name> - 受取可否");
        sender.sendMessage("§f/spp reload - リロード");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        if (args.length == 1) {
            StringUtil.copyPartialMatches(args[0], Arrays.asList("add", "remove", "set", "score", "ranking", "toggle", "toggleranking", "rewardgui", "help", "reload", "create"), completions);
        } else if (args.length == 2) {
            StringUtil.copyPartialMatches(args[1], plugin.getPointManager().getPointNames(), completions);
        } else if (args.length == 3) {
            return null; // プレイヤー名自動補完
        }
        return completions;
    }
}