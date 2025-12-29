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
                if (args.length < 3) {
                    sender.sendMessage("§c使用法: /spp create <ID> <表示名>");
                    return true;
                }
                String id = args[1];


                StringBuilder dispNameBuilder = new StringBuilder();
                for (int i = 2; i < args.length; i++) {
                    if (i > 2) dispNameBuilder.append(" "); // 引数間にスペースを戻す
                    dispNameBuilder.append(args[i]);
                }
                String displayName = dispNameBuilder.toString();

                if (plugin.getPointManager().createPointType(id, displayName)) {
                    sender.sendMessage("§aポイントを作成しました！");
                    sender.sendMessage("§7内部ID: §f" + id);
                    sender.sendMessage("§7表示名: §f" + plugin.getPointManager().getDisplayName(id));
                } else {
                    sender.sendMessage("§cそのIDは既に存在するか、作成に失敗しました。");
                }
                break;

            case "add":
            case "remove":
            case "set":
                if (args.length < 4) return false;

                OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
                String pointName = args[2];
                int amount;

                try {
                    amount = Integer.parseInt(args[3]);
                } catch (NumberFormatException e) {
                    sender.sendMessage("§c数値は整数で入力してください。");
                    return true;
                }

                if (sub.equals("add")) {
                    plugin.getPointManager().addPoint(pointName, target.getUniqueId(), amount);
                    sender.sendMessage("§a" + target.getName() + " に " + amount + " pt 追加しました。");
                    plugin.getLogManager().logPointChange(target.getName(), pointName, amount, "ADD");
                }
                else if (sub.equals("remove")) {
                    // 数値をマイナスにして加算処理
                    plugin.getPointManager().addPoint(pointName, target.getUniqueId(), -amount);
                    sender.sendMessage("§a" + target.getName() + " から " + amount + " pt 差し引きました。");
                    plugin.getLogManager().logPointChange(target.getName(), pointName, -amount, "REMOVE");
                }
                else if (sub.equals("set")) {
                    plugin.getPointManager().setPoint(pointName, target.getUniqueId(), amount);
                    sender.sendMessage("§a" + target.getName() + " のポイントを " + amount + " pt に設定しました。");
                    plugin.getLogManager().logPointChange(target.getName(), pointName, amount, "SET");
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
                // ... 前後の引数チェックなどは既存通り ...
                String pName = args[1];
                int slot = Integer.parseInt(args[2]);
                int req = Integer.parseInt(args[3]);

                FileConfiguration config = plugin.getRewardManager().getRewardConfig(pName);
                if (config.contains(String.valueOf(slot))) {
                    // 解放条件(requirement)だけをセット
                    config.set(slot + ".requirement", req);

                    // ✨ saveReward の引数に config から読み取った is_personal を追加して呼び出す
                    plugin.getRewardManager().saveReward(
                            pName,
                            slot,
                            config.getItemStack(slot + ".item"),
                            config.getInt(slot + ".price", 100),
                            config.getInt(slot + ".stock", -1),
                            config.getBoolean(slot + ".is_personal", false) // ← ここを追加！
                    );

                    sender.sendMessage("§a" + pName + " " + slot + "番に解放条件 " + req + " pt を設定しました。");
                } else {
                    sender.sendMessage("§c指定されたスロットに報酬が存在しません。");
                }
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

        // --- 全体放送用のメッセージ構築 ---
        String header = "§8§l[§6§lRanking§8§l] §e§l" + pointName.toUpperCase();
        Bukkit.broadcastMessage("§7§m--------------------------------------");
        Bukkit.broadcastMessage(header);

        for (int i = 0; i < Math.min(list.size(), 7); i++) {
            String color = (i == 0) ? "§e§l" : (i == 1) ? "§f§l" : (i == 2) ? "§6§l" : "§7";
            String name = list.get(i).getKey();
            int score = list.get(i).getValue();

            Bukkit.broadcastMessage(color + (i + 1) + ". §r" + name + " §7- §b" + score + " pt");
        }
        Bukkit.broadcastMessage("§7§m--------------------------------------");

    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage("§8§m-----------------------------------------");
        sender.sendMessage("   §6§lSimplePoint §e§l管理マネージャー");
        sender.sendMessage("");

        // 基本操作 (強調)
        sender.sendMessage(" §e§l▶ §a§lポイント基本操作");
        sender.sendMessage("  §f/spp §a§lcreate §b<ID> <表示名...> §7- 新規作成");
        sender.sendMessage("  §f/spp §a§ladd §b<ID> <プレイヤー> <数> §7- 付与");
        sender.sendMessage("  §f/spp §a§lremove §b<ID> <プレイヤー> <数> §7- 剥奪");
        sender.sendMessage("  §f/spp §a§lset §b<ID> <プレイヤー> <数> §7- 上書き");
        sender.sendMessage("");

        // 確認・報酬 (強調)
        sender.sendMessage(" §e§l▶ §a§lデータ確認・報酬設定");
        sender.sendMessage("  §f/spp §a§lscore §b<ID> <プレイヤー> §7- 所持状況確認");
        sender.sendMessage("  §f/spp §a§lrewardgui §b<ID> §7- 報酬スロット編集");
        sender.sendMessage("  §f/spp §a§lranking §b<ID> §7- ランキング表示");
        sender.sendMessage("");

        // システム設定
        sender.sendMessage(" §e§l▶ §fシステム設定");
        sender.sendMessage("  §f/spp §fsetreq §7<ID> <Slot> <pt> - 解放条件設定");
        sender.sendMessage("  §f/spp §ftoggleranking §7<ID> - ランキング有効化切替");
        sender.sendMessage("  §f/spp §fcreateteam §7<チーム名> - チームデータ作成");
        sender.sendMessage("  §f/spp §freload §7- コンフィグリロード");

        sender.sendMessage("");
        sender.sendMessage(" §7※ §b<ID>§7は内部用英数字、§b<表示名>§7は日本語/色可");
        sender.sendMessage("§8§m-----------------------------------------");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        if (args.length == 1) {
            StringUtil.copyPartialMatches(args[0], Arrays.asList("help","create", "add", "set","remove", "rewardgui", "teamrewardgui", "createteam", "setreq", "ranking", "reload", "score", "toggleranking"), completions);
        } else if (args.length == 2) {
            if (Arrays.asList("add", "set","remove").contains(args[0].toLowerCase())) {
                List<String> names = Bukkit.getOnlinePlayers().stream().map(Player::getName).collect(Collectors.toList());
                StringUtil.copyPartialMatches(args[1], names, completions);
            } else if (Arrays.asList("rewardgui", "ranking", "toggleranking", "score", "setreq").contains(args[0].toLowerCase())) {
                StringUtil.copyPartialMatches(args[1], plugin.getPointManager().getPointNames(), completions);
            } else if (args[0].equalsIgnoreCase("teamrewardgui")) {
                StringUtil.copyPartialMatches(args[1], new ArrayList<>(plugin.getTeamManager().getTeamNames()), completions);
            }
        } else if (args.length == 3 && Arrays.asList("add", "set","remove").contains(args[0].toLowerCase())) {
            StringUtil.copyPartialMatches(args[2], plugin.getPointManager().getPointNames(), completions);
        }
        return completions;
    }
}