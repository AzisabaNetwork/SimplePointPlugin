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

            case "rewardgui":
                if (!(sender instanceof Player)) return true;
                if (args.length < 2) return false;
                plugin.getGuiManager().openRewardGUI((Player) sender, args[1], true);
                break;

            case "teamrewardgui":
                if (!(sender instanceof Player)) return true;
                if (args.length < 2) return false;
                // チーム報酬の内部名は "TEAMREWARD_チーム名"
                plugin.getGuiManager().openRewardGUI((Player) sender, "TEAMREWARD_" + args[1], true);
                break;

            case "createteam":
                if (args.length < 2) return false;
                plugin.getTeamManager().createTeam(args[1]);
                sender.sendMessage("§aチーム「" + args[1] + "」を作成しました。");
                break;

            case "setreq": // ✨ 新機能: 必要ポイント(進捗)の設定
                if (args.length < 4) {
                    sender.sendMessage("§c使用法: /spp setreq <ポイント名> <スロット番号> <必要総ポイント>");
                    return true;
                }
                String pName = args[1];
                int slot = Integer.parseInt(args[2]);
                int req = Integer.parseInt(args[3]);
                plugin.getRewardManager().getConfig().set(pName + "." + slot + ".requirement", req);
                plugin.getRewardManager().save();
                sender.sendMessage("§a" + pName + " の " + slot + " 番スロットに必要ポイント " + req + " pt を設定しました。");
                break;

            case "ranking":
                if (args.length < 2) return false;
                showRanking(sender, args[1]);
                break;

            case "reload":
                plugin.reloadConfig();
                sender.sendMessage("§a設定をリロードしました。");
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
            sender.sendMessage("§cランキングを表示できません。");
            return;
        }

        Map<String, Integer> scores = new HashMap<>();
        for (String key : config.getKeys(false)) {
            if (key.startsWith("_")) continue;
            // 累計(total)でランキングを表示するように修正 📊
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
        sender.sendMessage("§f/spp create <名> §7- ポイント作成");
        sender.sendMessage("§f/spp add <人> <名> <数> §7- ポイント付与");
        sender.sendMessage("§f/spp rewardgui <名> §7- 報酬編集");
        sender.sendMessage("§f/spp teamrewardgui <チーム> §7- チーム報酬編集");
        sender.sendMessage("§f/spp setreq <名> <スロット> <pt> §7- 解放条件設定");
        sender.sendMessage("§f/spp createteam <名> §7- チーム作成");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        if (args.length == 1) {
            StringUtil.copyPartialMatches(args[0], Arrays.asList("create", "add", "set", "rewardgui", "teamrewardgui", "createteam", "setreq", "ranking", "reload"), completions);
        } else if (args.length == 3 && (args[0].equals("add") || args[0].equals("set") || args[0].equals("rewardgui") || args[0].equals("setreq"))) {
            StringUtil.copyPartialMatches(args[2], plugin.getPointManager().getPointNames(), completions);
        }
        return completions;
    }
}