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
                int amount;
                try {
                    amount = Integer.parseInt(args[3]);
                } catch (NumberFormatException e) {
                    sender.sendMessage("§c数値には整数を入力してください。");
                    return true;
                }

                if (sub.equals("add")) {
                    plugin.getPointManager().addPoint(pointName, target.getUniqueId(), amount);
                    sender.sendMessage("§a" + target.getName() + " に " + amount + " pt 追加しました。");
                } else {
                    plugin.getPointManager().setPoint(pointName, target.getUniqueId(), amount);
                    sender.sendMessage("§a" + target.getName() + " のポイントを " + amount + " pt に設定しました。");
                }
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
                if (args.length < 4) {
                    sender.sendMessage("§c使用法: /spp setreq <ポイント名> <スロット番号> <必要総ポイント>");
                    return true;
                }
                try {
                    String pName = args[1];
                    String slot = args[2];
                    int req = Integer.parseInt(args[3]);

                    // 修正点: RewardManager から特定のポイントの設定ファイルを取得
                    FileConfiguration config = plugin.getRewardManager().getRewardConfig(pName);
                    config.set(slot + ".requirement", req);

                    // 修正点: save() ではなく RewardManager の内部保存ロジック（saveReward等）に合わせるか、直接保存
                    plugin.getRewardManager().saveReward(
                            pName,
                            Integer.parseInt(slot),
                            config.getItemStack(slot + ".item"),
                            config.getInt(slot + ".price"),
                            config.getInt(slot + ".stock", -1)
                    );

                    sender.sendMessage("§a" + pName + " の " + slot + " 番スロットに解放条件 " + req + " pt を設定しました。");
                } catch (Exception e) {
                    sender.sendMessage("§c設定に失敗しました。数値を確認してください。");
                }
                break;

            case "ranking":
                if (args.length < 2) return false;
                showRanking(sender, args[1]);
                break;

            case "reload":
                plugin.reloadConfig();
                sender.sendMessage("§a設定をリロードしました。");
                break;

            case "score":
                if (args.length < 3) return false;
                String ptName = args[1];
                OfflinePlayer targetPlayer = Bukkit.getOfflinePlayer(args[2]);
                int scoreValue = plugin.getPointManager().getPoint(ptName, targetPlayer.getUniqueId());
                sender.sendMessage("§e" + targetPlayer.getName() + " の " + ptName + ": §f" + scoreValue + "pt");
                break;

            case "toggleranking":
                if (args.length < 2) return false;
                FileConfiguration cfg = plugin.getPointManager().getPointConfig(args[1]);
                if (cfg == null) {
                    sender.sendMessage("§cそのポイント名は存在しません。");
                    return true;
                }
                boolean newState = !cfg.getBoolean("_settings.ranking_enabled", true);
                cfg.set("_settings.ranking_enabled", newState);
                plugin.getPointManager().saveConfig(args[1], cfg);
                sender.sendMessage("§a" + args[1] + " の報酬ショップ/ランキングを " + (newState ? "§2有効" : "§4無効") + " §aにしました。");
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
            sender.sendMessage("§cランキングを表示できないか、無効化されています。");
            return;
        }

        Map<String, Integer> scores = new HashMap<>();
        if (config.getKeys(false) != null) {
            for (String key : config.getKeys(false)) {
                if (key.startsWith("_")) continue;
                int total = config.getInt(key + ".total", 0);
                try {
                    OfflinePlayer op = Bukkit.getOfflinePlayer(UUID.fromString(key));
                    if (op.getName() != null) scores.put(op.getName(), total);
                } catch (IllegalArgumentException e) {
                    // UUIDではないキー（設定など）はスキップ
                }
            }
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
        sender.sendMessage("§f/spp create <名> §7- ポイント作成");
        sender.sendMessage("§f/spp createteam <名> §7- チーム作成");
        sender.sendMessage("§f/spp add <人> <名> <数> §7- ポイント付与");
        sender.sendMessage("§f/spp score <名> <人> §7- 個人ポイント確認");
        sender.sendMessage("§f/spp rewardgui <名> §7- 報酬編集");
        sender.sendMessage("§f/spp teamrewardgui <チーム> §7- チーム報酬編集");
        sender.sendMessage("§f/spp setreq <名> <スロット> <pt> §7- 解放条件設定");
        sender.sendMessage("§f/spp ranking <名> §7- ランキング表示");
        sender.sendMessage("§f/spp toggleranking <名> §7- ショップ/ランキングの有効化切替");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        List<String> subCommands = Arrays.asList("create", "add", "set", "rewardgui", "teamrewardgui", "createteam", "setreq", "ranking", "reload", "score", "toggleranking");

        if (args.length == 1) {
            StringUtil.copyPartialMatches(args[0], subCommands, completions);
        } else if (args.length == 2) {
            // 2番目の引数がプレイヤー名のコマンド
            if (Arrays.asList("add", "set").contains(args[0].toLowerCase())) {
                List<String> names = Bukkit.getOnlinePlayers().stream().map(Player::getName).collect(Collectors.toList());
                StringUtil.copyPartialMatches(args[1], names, completions);
            }
            // 2番目の引数がポイント名のコマンド
            else if (Arrays.asList("rewardgui", "ranking", "toggleranking", "score", "setreq").contains(args[0].toLowerCase())) {
                StringUtil.copyPartialMatches(args[1], plugin.getPointManager().getPointNames(), completions);
            }
            // 2番目の引数がチーム名のコマンド
            else if (args[0].equalsIgnoreCase("teamrewardgui")) {
                StringUtil.copyPartialMatches(args[1], new ArrayList<>(plugin.getTeamManager().getTeamNames()), completions);
            }
        } else if (args.length == 3) {
            // 3番目の引数がポイント名のコマンド
            if (Arrays.asList("add", "set").contains(args[0].toLowerCase())) {
                StringUtil.copyPartialMatches(args[2], plugin.getPointManager().getPointNames(), completions);
            }
            // 3番目の引数がプレイヤー名のコマンド
            else if (args[0].equalsIgnoreCase("score")) {
                List<String> names = Bukkit.getOnlinePlayers().stream().map(Player::getName).collect(Collectors.toList());
                StringUtil.copyPartialMatches(args[2], names, completions);
            }
        }

        Collections.sort(completions);
        return completions;
    }
}