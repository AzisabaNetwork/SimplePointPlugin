package net.azisaba.simplepoint;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
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
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        String sub = args[0].toLowerCase();

        switch (sub) {
            case "create":
                if (args.length < 2) return false;
                if (plugin.getPointManager().createPointType(args[1])) {
                    sender.sendMessage("§aポイント「" + args[1] + "」を新規作成しました。");
                } else {
                    sender.sendMessage("§c既にそのポイントは存在します。");
                }
                break;

            case "add":
            case "set":
                if (args.length < 4) return false;
                OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
                String pointName = args[2];
                int amount = Integer.parseInt(args[3]);
                if (sub.equals("add")) {
                    plugin.getPointManager().addPoint(pointName, target.getUniqueId(), amount);
                    sender.sendMessage("§a" + target.getName() + " に " + amount + " pt 追加しました。");
                } else {
                    plugin.getPointManager().setPoint(pointName, target.getUniqueId(), amount);
                    sender.sendMessage("§a" + target.getName() + " のポイントを " + amount + " pt に設定しました。");
                }
                break;

            case "remove":
                if (args.length < 4) return false;
                OfflinePlayer targetRem = Bukkit.getOfflinePlayer(args[1]);
                String pNameRem = args[2];
                int amountRem;
                try {
                    amountRem = Integer.parseInt(args[3]);
                } catch (NumberFormatException e) {
                    sender.sendMessage("§c数値は整数で入力してください。");
                    return true;
                }
                // 指定したポイントを減らす
                plugin.getPointManager().addPoint(pNameRem, targetRem.getUniqueId(), -amountRem);
                sender.sendMessage("§a" + targetRem.getName() + " から " + amountRem + " pt 差し引きました。");
                break;

            case "rewardgui":
                if (!(sender instanceof Player)) return true;
                if (args.length < 2) return false;
                plugin.getGuiManager().openRewardGUI((Player) sender, args[1], true);
                break;

            case "teamrewardgui":
                if (!(sender instanceof Player)) return true;
                if (args.length < 2) return false;
                plugin.getGuiManager().openRewardGUI((Player) sender, "TEAMREWARD_" + args[1], true);
                break;

            case "createteam":
                if (args.length < 2) return false;
                plugin.getTeamManager().createTeam(args[1]);
                sender.sendMessage("§aチーム「" + args[1] + "」を作成しました。");
                break;

            case "setreq":
                if (args.length < 4) return false;
                String pName = args[1];
                int slot = Integer.parseInt(args[2]);
                int req = Integer.parseInt(args[3]);
                FileConfiguration config = plugin.getRewardManager().getRewardConfig(pName);
                config.set(slot + ".requirement", req);
                // 既存のデータを維持して保存
                plugin.getRewardManager().saveReward(pName, slot,
                        config.getItemStack(slot + ".item"),
                        config.getInt(slot + ".price", 100),
                        config.getInt(slot + ".stock", -1));
                sender.sendMessage("§a" + pName + " " + slot + "番に解放条件 " + req + " pt を設定しました。");
                break;

            case "ranking":
                if (args.length < 2) return false;
                showRanking(sender, args[1]);
                break;

            case "score":
                if (args.length < 3) return false;
                String ptName = args[1];
                OfflinePlayer targetPlayer = Bukkit.getOfflinePlayer(args[2]);
                int score = plugin.getPointManager().getPoint(ptName, targetPlayer.getUniqueId());
                sender.sendMessage("§e" + targetPlayer.getName() + " の " + ptName + ": §f" + score + "pt");
                break;

            case "toggleranking":
                if (args.length < 2) return false;
                FileConfiguration cfg = plugin.getPointManager().getPointConfig(args[1]);
                boolean newState = !cfg.getBoolean("_settings.ranking_enabled", true);
                cfg.set("_settings.ranking_enabled", newState);
                plugin.getPointManager().saveConfig(args[1], cfg);
                sender.sendMessage("§a報酬/ランキングを " + (newState ? "§2有効" : "§4無効") + " にしました。");
                break;

            case "reload": // ✨ 全体リロードに変更
                plugin.reloadConfig(); // Bukkit標準のconfig.ymlリロード
                plugin.getRewardManager().reload(); // 報酬キャッシュクリア
                sender.sendMessage("§a[SimplePoint] 設定と報酬データをリロードしました。");
                break;

            default:
                sendHelp(sender);
                break;
        }
        return true;
    }

    private void showRanking(CommandSender sender, String pointName) {
        FileConfiguration config = plugin.getPointManager().getPointConfig(pointName);
        if (config == null || !config.getBoolean("_settings.ranking_enabled", true)) {
            sender.sendMessage("§cランキングは現在無効です。");
            return;
        }
        Map<String, Integer> scores = new HashMap<>();
        for (String key : config.getKeys(false)) {
            if (key.startsWith("_")) continue;
            int total = config.getInt(key + ".total", 0);
            OfflinePlayer op = Bukkit.getOfflinePlayer(UUID.fromString(key));
            if (op.getName() != null) scores.put(op.getName(), total);
        }
        List<Map.Entry<String, Integer>> list = new ArrayList<>(scores.entrySet());
        list.sort((a, b) -> b.getValue().compareTo(a.getValue()));
        sender.sendMessage("§e--- " + pointName + " 累計ランキング ---");
        for (int i = 0; i < Math.min(list.size(), 10); i++) {
            sender.sendMessage("§7" + (i + 1) + ". §f" + list.get(i).getKey() + ": §b" + list.get(i).getValue() + " pt");
        }
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage("§6§lSimplePoint 管理ヘルプ");
        sender.sendMessage("§a/spp create <名> §7- ポイント作成");
        sender.sendMessage("§f/spp createteam <名> §7- チーム作成");
        sender.sendMessage("§a/spp add/remove/set <人> <名> <数> §7- プレイヤーのポイント操作");
        sender.sendMessage("§a/spp score <名> <人> §7- 個人ポイント確認");
        sender.sendMessage("§a/spp rewardgui <名> §7- 報酬編集");
        sender.sendMessage("§f/spp teamrewardgui <チーム> §7- チーム報酬編集");
        sender.sendMessage("§f/spp setreq <名> <スロット> <pt> §7- 解放に必要なポイント数設定");
        sender.sendMessage("§a/spp ranking <名> §7- ランキング表示");
        sender.sendMessage("§f/spp toggleranking <名> §7- ショップ/ランキングの有効化切替");
        sender.sendMessage("§f/spp help §7- このヘルプを表示");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        if (args.length == 1) {
            StringUtil.copyPartialMatches(args[0], Arrays.asList("help","create", "add", "set","remove", "rewardgui", "teamrewardgui", "createteam", "setreq", "ranking", "reload", "score", "toggleranking"), completions);
        } else if (args.length == 2) {
            if (Arrays.asList("add", "set").contains(args[0].toLowerCase())) {
                List<String> names = Bukkit.getOnlinePlayers().stream().map(Player::getName).collect(Collectors.toList());
                StringUtil.copyPartialMatches(args[1], names, completions);
            } else if (Arrays.asList("rewardgui", "ranking", "toggleranking", "score", "setreq").contains(args[0].toLowerCase())) {
                StringUtil.copyPartialMatches(args[1], plugin.getPointManager().getPointNames(), completions);
            } else if (args[0].equalsIgnoreCase("teamrewardgui")) {
                StringUtil.copyPartialMatches(args[1], new ArrayList<>(plugin.getTeamManager().getTeamNames()), completions);
            }
        } else if (args.length == 3 && Arrays.asList("add", "set").contains(args[0].toLowerCase())) {
            StringUtil.copyPartialMatches(args[2], plugin.getPointManager().getPointNames(), completions);
        }
        return completions;
    }
}