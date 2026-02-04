package net.azisaba.simplepoint;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

public class SPPTCommand implements CommandExecutor {
    private final SimplePointPlugin plugin;

    public SPPTCommand(SimplePointPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // 短縮コマンド /teaminfo または /spt teaminfo (引数なし) で実行された場合
        if (label.equalsIgnoreCase("teaminfo") || (label.equalsIgnoreCase("spt") && args.length > 0 && args[0].equalsIgnoreCase("teaminfo"))) {
            if (!(sender instanceof Player)) {
                sender.sendMessage("§cこのコマンドはプレイヤーのみ実行可能です。");
                return true;
            }
            sendModernTeamInfo((Player) sender);
            return true;
        }

        if (args.length == 0 || args[0].equalsIgnoreCase("help")) {
            sendHelp(sender);
            return true;
        }

        String sub = args[0].toLowerCase();

        switch (sub) {
            case "create":
                if (args.length < 2) return false;
                plugin.getTeamManager().createGroup(args[1]);
                sender.sendMessage("§a[SPPT] グループ §l" + args[1] + " §aを作成しました。");
                break;

            case "teamcreate":
                if (args.length < 4) return false;
                plugin.getTeamManager().createTeam(args[1], args[2], args[3]);
                sender.sendMessage("§a[SPPT] チーム §l" + args[3] + " §aを作成しました。");
                break;

            case "join":
                if (args.length < 4) return false;
                Player target = Bukkit.getPlayer(args[3]);
                if (target == null) {
                    sender.sendMessage("§c[SPPT] プレイヤーが見つかりません。");
                    return true;
                }
                plugin.getTeamManager().addMember(args[1], args[2], target.getUniqueId());
                sender.sendMessage("§a[SPPT] " + target.getName() + " を §l" + args[2] + " §aに追加しました。");
                break;

            case "setpoint":
                if (args.length < 3) return false;
                plugin.getTeamManager().setGroupPoint(args[1], args[2]);
                sender.sendMessage("§a[SPPT] グループ §l" + args[1] + " §aをポイント §l" + args[2] + " §aに紐付けました。");
                break;

            case "toggle":
                if (args.length < 2) return false;
                boolean newState = plugin.getTeamManager().toggleGroupSync(args[1]);
                sender.sendMessage("§a[SPPT] グループ §l" + args[1] + " §aのポイント同期を §l" + (newState ? "§bON" : "§cOFF") + " §aにしました。");
                break;

            case "multiplier":
                if (args.length < 5) {
                    sender.sendMessage("§c使用法: /sppt multiplier <group> <倍率> <開始(yyyy/MM/dd-HH:mm)> <終了>");
                    return true;
                }
                try {
                    double mult = Double.parseDouble(args[2]);
                    plugin.getTeamManager().setGroupMultiplier(args[1], mult, args[3], args[4]);
                    sender.sendMessage("§a[SPPT] 倍率を設定しました。");
                } catch (Exception e) {
                    sender.sendMessage("§cエラー: " + e.getMessage());
                }
                break;

            case "vsteam":
                if (args.length < 4) return false;
                plugin.getTeamManager().startBattle(args[1], args[2], args[3]);
                sender.sendMessage("§6§l[BATTLE] §fグループ §b" + args[1] + " §fで §e" + args[2] + " vs " + args[3] + " §fが開始されました！");
                break;

            case "teaminfo": // /sppt teaminfo としても動くように
                if (!(sender instanceof Player)) return true;
                sendModernTeamInfo((Player) sender);
                break;

            case "setjoingui":
                if (args.length < 6) return false;
                boolean auto = Boolean.parseBoolean(args[5]);
                plugin.getTeamJoinGUIManager().setGuiSettings(args[1], args[2], args[3], args[4], auto);
                sender.sendMessage("§a[GUI] 参加GUIを設定しました。 (Mode: " + args[4] + ", Auto: " + auto + ")");
                break;

            case "teamjoingui":
                if (args.length < 2) return false;
                Player targetPlayer;
                if (args.length >= 3) {
                    targetPlayer = Bukkit.getPlayer(args[2]);
                } else {
                    if (!(sender instanceof Player)) return false;
                    targetPlayer = (Player) sender;
                }
                if (targetPlayer != null) plugin.getTeamJoinGUIManager().openJoinGUI(targetPlayer, args[1]);
                break;

            case "member":
                if (args.length < 3) return false;
                showTeamMembers(sender, args[1], args[2]);
                break;

            case "info":
                if (args.length < 3) return false;
                showAdminInfo(sender, args[1], args[2]);
                break;

            case "setpoint_value":
                if (args.length < 4) return false;
                handleSetPointValue(sender, args[1], args[2], args[3]);
                break;

            case "rewardgui":
                if (!(sender instanceof Player)) return true;
                if (args.length < 2) return false;
                plugin.getTeamAdminGUIManager().openGroupRewardEditor((Player) sender, args[1]);
                break;

            default:
                sender.sendMessage("§c不明なコマンドです。/sppt help を参照してください。");
                break;
        }
        return true;
    }

    private void handleSetPointValue(CommandSender sender, String group, String teamId, String amountStr) {
        try {
            int amount = Integer.parseInt(amountStr);
            File file = plugin.getTeamManager().getTeamFile(group, teamId);
            if (!file.exists()) {
                sender.sendMessage("§cチームが存在しません。");
                return;
            }
            FileConfiguration cfg = YamlConfiguration.loadConfiguration(file);
            cfg.set("points.current", amount);
            cfg.set("points.total", amount);
            cfg.save(file);
            sender.sendMessage("§aチームポイントを §e" + amount + " §aに設定しました。");
        } catch (Exception e) {
            sender.sendMessage("§c数値が不正です。");
        }
    }

    private void showTeamMembers(CommandSender sender, String group, String teamId) {
        List<String> members = plugin.getTeamManager().getMemberNames(group, teamId);
        String dName = plugin.getTeamManager().getTeamDisplayName(group, teamId);
        sender.sendMessage("§8§m----------§r " + dName + " §bMembers §8§m----------");
        if (members.isEmpty()) {
            sender.sendMessage("§7メンバーはいません。");
        } else {
            members.forEach(sender::sendMessage);
        }
        sender.sendMessage("§8§m------------------------------------");
    }

    /**
     * モダンなチーム対戦状況表示
     */
    public void sendModernTeamInfo(Player player) {
        UUID uuid = player.getUniqueId();
        String group = plugin.getTeamManager().getPlayerGroup(uuid);
        String teamId = plugin.getTeamManager().getPlayerTeam(uuid);

        if (group == null || teamId == null) {
            player.sendMessage("§c§l[!] §7あなたはチームに参加していません。");
            return;
        }

        FileConfiguration gCfg = YamlConfiguration.loadConfiguration(plugin.getTeamManager().getRewardFile(group));
        if (!gCfg.getBoolean("battle.active")) {
            player.sendMessage("§c現在、このグループは対戦状態ではありません。");
            return;
        }

        String t1 = gCfg.getString("battle.team1");
        String t2 = gCfg.getString("battle.team2");
        String enemyId = teamId.equals(t1) ? t2 : t1;

        int myPoints = plugin.getTeamManager().getTeamPoints(group, teamId);
        int enemyPoints = plugin.getTeamManager().getTeamPoints(group, enemyId);
        double multiplier = plugin.getTeamManager().getActiveMultiplier(group);

        String progressBar = buildVSBar(myPoints, enemyPoints);

        // --- TOP3 取得ロジックの埋め込み ---
        File memberFile = plugin.getTeamManager().getMemberFile(group, teamId);
        FileConfiguration mCfg = YamlConfiguration.loadConfiguration(memberFile);
        Map<String, Integer> scores = new HashMap<>();
        if (mCfg.getConfigurationSection("contributions") != null) {
            for (String key : mCfg.getConfigurationSection("contributions").getKeys(false)) {
                scores.put(key, mCfg.getInt("contributions." + key));
            }
        }

        List<String> top3List = scores.entrySet().stream()
                .sorted((a, b) -> b.getValue().compareTo(a.getValue()))
                .limit(3)
                .map(e -> {
                    OfflinePlayer op = Bukkit.getOfflinePlayer(UUID.fromString(e.getKey()));
                    return " §7- §f" + (op.getName() != null ? op.getName() : "Unknown") + ": §e" + e.getValue() + "pt";
                })
                .collect(Collectors.toList());

        // --- チャット表示出力 ---
        player.sendMessage("§8§m      §r §b§lVS BATTLE STATUS §r §8§m      ");
        player.sendMessage("");
        player.sendMessage(" §f" + plugin.getTeamManager().getTeamDisplayName(group, teamId) + " §b§l" + myPoints + " pt");
        player.sendMessage(" " + progressBar);
        player.sendMessage(" §f" + plugin.getTeamManager().getTeamDisplayName(group, enemyId) + " §e§l" + enemyPoints + " pt");
        player.sendMessage("");
        player.sendMessage(" §e§l▶ §f§lTEAM TOP CONTRIBUTORS");
        if (top3List.isEmpty()) {
            player.sendMessage(" §7(まだデータがありません)");
        } else {
            top3List.forEach(player::sendMessage);
        }
        player.sendMessage("");
        player.sendMessage(" §e§l▶ §f§lYOUR STATS");
        player.sendMessage("  §7現在の保持: §f" + myPoints + " pt  §7/  総貢献量: §a" + plugin.getTeamManager().getMemberTotal(group, teamId, uuid) + " pt");
        player.sendMessage("  §7貢献ランク: §6" + plugin.getTeamManager().getMemberRank(group, teamId, uuid) + "位 §8| §7倍率: §d" + multiplier + "x");
        player.sendMessage("§8§m                                     ");
    }

    private String buildVSBar(int p1, int p2) {
        int total = p1 + p2;
        if (total == 0) return "§7[§8----------§f|§8----------§7]";
        int ratio = (int) (((double) p1 / total) * 20);
        StringBuilder sb = new StringBuilder("§b");
        for (int i = 0; i < 20; i++) {
            if (i == 10) sb.append("§f|§e");
            sb.append(i < ratio ? "■" : "□");
        }
        return "§7[" + sb.toString() + "§7] " + (p1 >= p2 ? "§b§l← ADVANTAGE" : "§e§lDISADVANTAGE →");
    }

    private void showAdminInfo(CommandSender sender, String group, String teamId) {
        File teamFile = plugin.getTeamManager().getTeamFile(group, teamId);
        if (!teamFile.exists()) {
            sender.sendMessage("§cチームが見つかりません。");
            return;
        }
        FileConfiguration teamCfg = YamlConfiguration.loadConfiguration(teamFile);
        FileConfiguration memCfg = YamlConfiguration.loadConfiguration(plugin.getTeamManager().getMemberFile(group, teamId));

        String dName = plugin.getTeamManager().formatName(teamCfg.getString("display_name"));
        int memberCount = memCfg.getStringList("members").size();

        sender.sendMessage("§8§m----------§r §6Team Admin Info §8§m----------");
        sender.sendMessage("§7表示名: " + dName);
        sender.sendMessage("§7メンバー数: §f" + memberCount + " 名");
        sender.sendMessage("§7現在のポイント: §e" + teamCfg.getInt("points.current") + " pt");
        sender.sendMessage("§7累計ポイント: §a" + teamCfg.getInt("points.total") + " pt");
        sender.sendMessage("§7現在の倍率: " + plugin.getTeamManager().getMultiplierStatus(group));
        sender.sendMessage("§8§m------------------------------------");
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage("§8§m-----------------------------------------");
        sender.sendMessage("   §6§lSimplePoint §b§lTeam Manager (SPPT)");
        sender.sendMessage("");
        sender.sendMessage(" §e§l▶ §b§lチーム基本操作");
        sender.sendMessage("  §f/sppt §bcreate §3<G> §7- グループ作成");
        sender.sendMessage("  §f/sppt §bteamcreate §3<G> <ID> <Name> §7- チーム作成");
        sender.sendMessage("  §f/sppt §bjoin §3<G> <ID> <Player> §7- 手動加入");
        sender.sendMessage("");
        sender.sendMessage(" §e§l▶ §a§l対戦・倍率設定");
        sender.sendMessage("  §f/sppt §avsteam §3<G> <T1> <T2> §7- 対戦開始");
        sender.sendMessage("  §f/sppt §amultiplier §3<G> <倍率> <開始> <終了> §7- 期間指定");
        sender.sendMessage("  §f/sppt §amoderninfo §7- (短縮: /teaminfo) 戦況表示");
        sender.sendMessage("");
        sender.sendMessage(" §e§l▶ §d§lGUI・システム設定");
        sender.sendMessage("  §f/sppt §dsetjoingui §3<G> <T1> <T2> <Mode> <Auto> §7- 参加GUI");
        sender.sendMessage("  §f/sppt §dteamjoingui §3<G> §7- 参加GUIを開く");
        sender.sendMessage("  §f/sppt §drewardgui §3<G> §7- 報酬設定");
        sender.sendMessage("§8§m-----------------------------------------");
    }
}