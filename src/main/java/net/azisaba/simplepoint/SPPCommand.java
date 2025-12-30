package net.azisaba.simplepoint;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.util.*;

public class SPPCommand implements CommandExecutor {
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
                    if (i > 2) dispNameBuilder.append(" ");
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
                // 順序修正: <ID> <Player> <Amount>
                if (args.length < 4) {
                    sender.sendMessage("§c使用法: /spp " + sub + " <ID> <プレイヤー> <数>");
                    return true;
                }

                String pointId = args[1];
                OfflinePlayer target = Bukkit.getOfflinePlayer(args[2]);
                int amount;

                try {
                    amount = Integer.parseInt(args[3]);
                } catch (NumberFormatException e) {
                    sender.sendMessage("§c数値は整数で入力してください。");
                    return true;
                }

                if (sub.equals("add")) {
                    plugin.getPointManager().addPoint(pointId, target.getUniqueId(), amount);
                    sender.sendMessage("§a" + target.getName() + " に " + amount + " pt 追加しました。");
                    plugin.getLogManager().logPointChange(target.getName(), pointId, amount, "ADD");
                } else if (sub.equals("remove")) {
                    plugin.getPointManager().addPoint(pointId, target.getUniqueId(), -amount);
                    sender.sendMessage("§a" + target.getName() + " から " + amount + " pt 差し引きました。");
                    plugin.getLogManager().logPointChange(target.getName(), pointId, -amount, "REMOVE");
                } else if (sub.equals("set")) {
                    plugin.getPointManager().setPoint(pointId, target.getUniqueId(), amount);
                    sender.sendMessage("§a" + target.getName() + " のポイントを " + amount + " pt に設定しました。");
                    plugin.getLogManager().logPointChange(target.getName(), pointId, amount, "SET");
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
                if (args.length < 4) return false;
                String pName = args[1];
                int slot = Integer.parseInt(args[2]);
                int req = Integer.parseInt(args[3]);

                FileConfiguration config = plugin.getRewardManager().getRewardConfig(pName);
                if (config != null && config.contains(String.valueOf(slot))) {
                    config.set(slot + ".requirement", req);
                    try {
                        config.save(new File(plugin.getDataFolder(), "rewards/" + pName + ".yml"));
                        sender.sendMessage("§a" + pName + " " + slot + "番に解放条件 " + req + " pt を設定しました。");
                    } catch (Exception e) {
                        sender.sendMessage("§c保存に失敗しました。");
                    }
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
                String ptId = args[1];
                OfflinePlayer tPlayer = Bukkit.getOfflinePlayer(args[2]);
                int score = plugin.getPointManager().getPoint(ptId, tPlayer.getUniqueId());
                sender.sendMessage("§e" + tPlayer.getName() + " の " + ptId + ": §f" + score + "pt");
                break;

            case "toggleranking":
                if (args.length < 2) {
                    sender.sendMessage("§c使用法: /spp toggleranking <ID>");
                    return true;
                }
                String rankId = args[1];
                FileConfiguration rankCfg = plugin.getPointManager().getPointConfig(rankId);
                if (rankCfg == null) {
                    sender.sendMessage("§cそのIDは存在しません。");
                    return true;
                }
                // ランキングのみ反転
                boolean currentRankState = rankCfg.getBoolean("_settings.ranking_enabled", true);
                rankCfg.set("_settings.ranking_enabled", !currentRankState);
                plugin.getPointManager().savePointConfig(rankId);
                sender.sendMessage("§a" + rankId + " §fのランキング表示を " + (!currentRankState ? "§2有効" : "§4無効") + " §fにしました。");
                break;

            case "togglefunction":
                if (args.length < 2) {
                    sender.sendMessage("§c使用法: /spp togglefunction <ID>");
                    return true;
                }
                String funcId = args[1];
                FileConfiguration funcCfg = plugin.getPointManager().getPointConfig(funcId);
                if (funcCfg == null) {
                    sender.sendMessage("§cそのIDは存在しません。");
                    return true;
                }
                // 両方を一括設定（現在の状態を見て、両方OFFまたは両方ONに切り替え）
                boolean currentState = funcCfg.getBoolean("_settings.function_enabled", true);
                boolean nextState = !currentState;

                funcCfg.set("_settings.function_enabled", nextState);
                // functionがOFFなら、個別の設定に関わらず両方止めるためのフラグとして機能させます
                plugin.getPointManager().savePointConfig(funcId);

                sender.sendMessage("§a" + funcId + " §fの全機能(ランキング・報酬)を " + (nextState ? "§2有効" : "§4無効") + " §fにしました。");
                break;

            case "reload":
                plugin.reloadConfig();
                plugin.getRewardManager().reload();
                plugin.getPointManager().reload();
                sender.sendMessage("§a[SimplePoint] 設定と報酬データをリロードしました。");
                break;

            case "togglereward":
                if (args.length < 2) {
                    sender.sendMessage("§c使用法: /spp togglereward <ID>");
                    return true;
                }
                String targetId = args[1];
                FileConfiguration rCfg = plugin.getPointManager().getPointConfig(targetId);
                if (rCfg == null) {
                    sender.sendMessage("§cそのIDは存在しません。");
                    return true;
                }

                // 現在の状態を反転 (デフォルトは true)
                boolean currentRewardState = rCfg.getBoolean("_settings.reward_enabled", true);
                boolean nextRewardState = !currentRewardState;

                rCfg.set("_settings.reward_enabled", nextRewardState);
                plugin.getPointManager().savePointConfig(targetId);

                String rDisp = plugin.getPointManager().getDisplayName(targetId);
                sender.sendMessage("§a" + rDisp + " §fの報酬画面を " + (nextRewardState ? "§2有効" : "§4無効") + " §fにしました。");
                break;

            default:
                sendHelp(sender);
                break;
        }
        return true;
    }

    private void showRanking(CommandSender sender, String pointName) {
        FileConfiguration config = plugin.getPointManager().getPointConfig(pointName);
        // ✨ IDから表示名（色付き）を取得
        String displayName = plugin.getPointManager().getDisplayName(pointName);

        if (config == null || !config.getBoolean("_settings.ranking_enabled", true)) {
            sender.sendMessage("§c" + displayName + " §cのランキングは現在無効です。");
            return;
        }

        Map<String, Integer> scores = new HashMap<>();
        for (String key : config.getKeys(false)) {
            if (key.startsWith("_")) continue;
            int total = config.getInt(key + ".total", 0);
            try {
                OfflinePlayer op = Bukkit.getOfflinePlayer(UUID.fromString(key));
                if (op.getName() != null) scores.put(op.getName(), total);
            } catch (IllegalArgumentException ignored) {}
        }

        List<Map.Entry<String, Integer>> list = new ArrayList<>(scores.entrySet());
        list.sort((a, b) -> b.getValue().compareTo(a.getValue()));

        // --- 全体放送用のメッセージ構築 ---
        Bukkit.broadcastMessage("§7§m--------------------------------------");
        // ✨ IDではなく表示名を使用
        Bukkit.broadcastMessage("§8§l[§6§lRanking§8§l] §e§l" + displayName);

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
        sender.sendMessage(" §e§l▶ §a§lポイント基本操作");
        sender.sendMessage("  §f/spp §a§lcreate §b<ID> <表示名...> §7- 新規作成");
        sender.sendMessage("  §f/spp §a§ladd §b<ID> <プレイヤー> <数> §7- 付与");
        sender.sendMessage("  §f/spp §a§lremove §b<ID> <プレイヤー> <数> §7- 剥奪");
        sender.sendMessage("  §f/spp §a§lset §b<ID> <プレイヤー> <数> §7- 上書き");
        sender.sendMessage("");
        sender.sendMessage(" §e§l▶ §a§lデータ確認・報酬設定");
        sender.sendMessage("  §f/spp §a§lscore §b<ID> <プレイヤー> §7- 所持状況確認");
        sender.sendMessage("  §f/spp §a§lrewardgui §b<ID> §7- 報酬スロット編集");
        sender.sendMessage("  §f/spp §a§lranking §b<ID> §7- ランキング表示");
        sender.sendMessage("");
        sender.sendMessage(" §e§l▶ §fシステム設定");
        sender.sendMessage("  §f/spp §fsetreq §7<ID> <Slot> <pt> - 解放条件設定");
        sender.sendMessage("  §f/spp §ftoggleranking §7<ID> - ランキング有効化切替");
        sender.sendMessage("  §f/spp §ftoggleranking §7<ID> - 報酬受け取り有効化切替");
        sender.sendMessage("  §f/spp §ftogglefunction §7<ID> - 報酬受け取り、ランキング有効化切替");
        sender.sendMessage("  §f/spp §fcreateteam §7<チーム名> - チームデータ作成");
        sender.sendMessage("  §f/spp §freload §7- コンフィグリロード");
        sender.sendMessage("");
        sender.sendMessage(" §7※ §b<ID>§7は内部用英数字、§b<表示名>§7は日本語/色可");
        sender.sendMessage("§8§m-----------------------------------------");
    }
}