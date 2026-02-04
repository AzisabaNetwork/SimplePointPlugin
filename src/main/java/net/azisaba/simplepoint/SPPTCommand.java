package net.azisaba.simplepoint;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class SPPTCommand implements CommandExecutor {
    private final SimplePointPlugin plugin;

    public SPPTCommand(SimplePointPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // 短縮コマンド /teaminfo への対応
        if (label.equalsIgnoreCase("teaminfo") || (label.equalsIgnoreCase("spt") && args.length > 0 && args[0].equalsIgnoreCase("teaminfo"))) {
            if (!(sender instanceof Player)) {
                sender.sendMessage("§cプレイヤーのみ実行可能です。");
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
                if (args.length < 3) {
                    sender.sendMessage("§c使用法: /sppt create <ID> <表示名(カラーコード可)>");
                    return true;
                }
                // args[2] を表示名として渡す
                plugin.getTeamManager().createGroup(args[1], args[2]);
                sender.sendMessage("§aグループ §l" + args[1] + " §aを名称 §r" +
                        ChatColor.translateAlternateColorCodes('&', args[2]) + " §aで作成しました。");
                break;

            case "teamcreate":
                if (args.length < 4) {
                    sender.sendMessage("§c使用法: /sppt teamcreate <グループ名> <チームID> <表示名>");
                    return true;
                }
                plugin.getTeamManager().createTeam(args[1], args[2], args[3]);
                sender.sendMessage("§aチーム §l" + args[3] + " §aを作成しました。");
                break;

            case "join":
                if (args.length < 4) {
                    sender.sendMessage("§c使用法: /sppt join <グループ名> <チームID> <プレイヤー名>");
                    return true;
                }
                Player target = Bukkit.getPlayer(args[3]);
                if (target == null) {
                    sender.sendMessage("§cプレイヤーが見つかりません。");
                    return true;
                }
                plugin.getTeamManager().addMember(args[1], args[2], target.getUniqueId());
                sender.sendMessage("§a" + target.getName() + " を §l" + args[2] + " §aに追加しました。");
                break;

            case "setpoint":
                if (args.length < 3) {
                    sender.sendMessage("§c使用法: /sppt setpoint <グループ名> <ポイントID>");
                    return true;
                }
                plugin.getTeamManager().setGroupPoint(args[1], args[2]);
                sender.sendMessage("§aグループ §l" + args[1] + " §aをポイント §l" + args[2] + " §aに紐付けました。");
                break;

            case "toggle":
                if (args.length < 2) {
                    sender.sendMessage("§c使用法: /sppt toggle <グループ名>");
                    return true;
                }
                boolean newState = plugin.getTeamManager().toggleGroupSync(args[1]);
                sender.sendMessage("§aグループ §l" + args[1] + " §aのポイント同期を §l" + (newState ? "§bON" : "§cOFF") + " §aにしました。");
                break;

            case "multiplier":
                if (args.length < 5) {
                    sender.sendMessage("§c使用法: /sppt multiplier <group> <倍率> <開始(yyyy/MM/dd-HH:mm)> <終了>");
                    return true;
                }
                try {
                    double mult = Double.parseDouble(args[2]);
                    plugin.getTeamManager().setGroupMultiplier(args[1], mult, args[3], args[4]);
                    sender.sendMessage("§a倍率を設定しました。");
                } catch (Exception e) {
                    sender.sendMessage("§cエラー: " + e.getMessage());
                }
                break;

            case "vsteam":
                if (args.length < 4) {
                    sender.sendMessage("§c使用法: /sppt vsteam <グループ名> <チームID1> <チームID2>");
                    return true;
                }
                File rewardFile = plugin.getTeamManager().getRewardFile(args[1]);
                if (rewardFile.exists() && YamlConfiguration.loadConfiguration(rewardFile).getBoolean("battle.active")) {
                    sender.sendMessage("§c[!] このグループは既に対戦モードが開始されています。");
                    return true;
                }
                plugin.getTeamManager().startBattle(args[1], args[2], args[3]);
                sender.sendMessage("§6§l[BATTLE] §fグループ §b" + args[1] + " §fで §e" + args[2] + " vs " + args[3] + " §fが開始されました！");
                break;

            case "teaminfo":
            case "moderninfo":
                if (!(sender instanceof Player)) return true;
                sendModernTeamInfo((Player) sender);
                break;

            case "setjoingui":
                if (args.length < 6) {
                    sender.sendMessage("§c使用法: /sppt setjoingui <グループ> <T1> <T2> <Mode: choice/random> <Auto: true/false>");
                    return true;
                }
                boolean auto = Boolean.parseBoolean(args[5]);
                plugin.getTeamJoinGUIManager().setGuiSettings(args[1], args[2], args[3], args[4], auto);
                sender.sendMessage("§a参加GUIを設定しました。 (Mode: " + args[4] + ", Auto: " + auto + ")");
                break;

            case "teamjoingui":
                if (args.length < 2) {
                    sender.sendMessage("§c使用法: /sppt teamjoingui <グループ名> [プレイヤー名]");
                    return true;
                }
                Player targetPlayer;
                if (args.length >= 3) {
                    targetPlayer = Bukkit.getPlayer(args[2]);
                } else {
                    if (!(sender instanceof Player)) return true;
                    targetPlayer = (Player) sender;
                }
                if (targetPlayer != null) plugin.getTeamJoinGUIManager().openJoinGUI(targetPlayer, args[1]);
                break;

            case "member":
                if (args.length < 3) {
                    sender.sendMessage("§c使用法: /sppt member <グループ名> <チームID>");
                    return true;
                }
                showTeamMembers(sender, args[1], args[2]);
                break;

            case "info":
                if (args.length < 3) {
                    sender.sendMessage("§c使用法: /sppt info <グループID> <チームID>");
                    return true;
                }
                String gId = args[1];
                String tId = args[2];

                String gDisp = getGroupDisplayName(gId);
                String tDisp = plugin.getTeamManager().getTeamDisplayName(gId, tId);
                double total = plugin.getTeamManager().getTeamTotalPoints(gId, tId);
                List<String> members = plugin.getTeamManager().getMemberNames(gId, tId); // 前に作ったポイント付きリスト

                sender.sendMessage("§6§l[TEAM INFO] §r" + gDisp + " §7/ " + tDisp);
                sender.sendMessage("§f総ポイント: §e§l" + total + " pt");
                sender.sendMessage("§fメンバー数: §b" + members.size() + "名");
                sender.sendMessage("§7メンバー一覧:");
                for (String m : members) {
                    sender.sendMessage(" " + m);
                }
                break;

            case "setpoint_value":
                if (args.length < 4) {
                    sender.sendMessage("§c使用法: /sppt setpoint_value <グループ名> <チームID> <数値>");
                    return true;
                }
                handleSetPointValue(sender, args[1], args[2], args[3]);
                break;

            case "leave":
                if (args.length < 4) {
                    sender.sendMessage("§c使用法: /sppt leave <グループ> <チーム> <名前>");
                    return true;
                }

                // 予測変換のゴミを掃除
                String inputName = ChatColor.stripColor(args[3].split(" ")[0]);
                org.bukkit.OfflinePlayer offTarget = Bukkit.getOfflinePlayer(inputName);

                if (offTarget == null || offTarget.getUniqueId() == null) {
                    sender.sendMessage("§cプレイヤーが見つかりません。");
                    return true;
                }

                // 正しいパスで削除を実行
                plugin.getTeamManager().removeMember(args[1], args[2], offTarget.getUniqueId());
                sender.sendMessage("§a" + inputName + " を脱退させました。");
                break;

            case "finishvsmode":
                if (args.length < 2) {
                    sender.sendMessage("§c使用法: /sppt finishvsmode <グループ名>");
                    return true;
                }
                File vFile = plugin.getTeamManager().getRewardFile(args[1]);
                FileConfiguration vCfg = YamlConfiguration.loadConfiguration(vFile);
                vCfg.set("battle.active", false);
                try {
                    vCfg.save(vFile);
                    sender.sendMessage("§aグループ §l" + args[1] + " §aの対戦モードを終了しました。");
                } catch (IOException e) {
                    sender.sendMessage("§cエラーが発生しました。");
                }
                break;

            case "rewardgui":
                if (args.length < 2) {
                    sender.sendMessage("§c使用法: /sppt rewardgui <グループ名>");
                    return true;
                }
                if (!(sender instanceof Player)) return true;
                plugin.getTeamAdminGUIManager().openGroupRewardEditor((Player) sender, args[1]);
                break;

            default:
                sender.sendMessage("§c不明なコマンドです。 /sppt help を参照してください。");
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
            // setpoint_valueの際は、意図的な修正なのでtotalも合わせる
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
        String groupDisplay = getGroupDisplayName(group);
        String teamDisplay = plugin.getTeamManager().getTeamDisplayName(group, teamId);
        int myTeamTotalPoints = plugin.getTeamManager().getTeamPoints(group, teamId);
        double multiplier = plugin.getTeamManager().getTeamActiveMultiplier(group, teamId);

        // --- VSモード判定による表示の切り替え ---
        if (gCfg.getBoolean("battle.active", false)) {
            // 【VSモード中の表示】
            String t1 = gCfg.getString("battle.team1");
            String t2 = gCfg.getString("battle.team2");
            String enemyId = teamId.equals(t1) ? t2 : t1;
            int enemyPoints = plugin.getTeamManager().getTeamPoints(group, enemyId);
            String enemyDisplay = plugin.getTeamManager().getTeamDisplayName(group, enemyId);
            String progressBar = buildVSBar(myTeamTotalPoints, enemyPoints);

            player.sendMessage("§8§m      §r " + groupDisplay + " §b§lVS STATUS §r §8§m      ");
            player.sendMessage("");
            player.sendMessage(" §f" + teamDisplay + " §b§l" + myTeamTotalPoints + " pt");
            player.sendMessage(" " + progressBar);
            player.sendMessage(" §f" + enemyDisplay + " §e§l" + enemyPoints + " pt");
        } else {
            // 【通常時（非戦闘時）の表示】
            player.sendMessage("§8§m      §r " + groupDisplay + " §f§lTEAM INFO §r §8§m      ");
            player.sendMessage("");
            player.sendMessage(" §7所属チーム: " + teamDisplay);
            player.sendMessage(" §7チーム総計: §e§l" + myTeamTotalPoints + " pt");
        }

        // --- TOP3 貢献者セクション (共通) ---
        player.sendMessage("");
        player.sendMessage(" §e§l▶ §f§lTEAM TOP CONTRIBUTORS");

        File memberFile = plugin.getTeamManager().getMemberFile(group, teamId);
        FileConfiguration mCfg = YamlConfiguration.loadConfiguration(memberFile);
        Map<String, Integer> scores = new HashMap<>();
        if (mCfg.getConfigurationSection("contributions") != null) {
            for (String key : mCfg.getConfigurationSection("contributions").getKeys(false)) {
                scores.put(key, mCfg.getInt("contributions." + key));
            }
        }

        if (scores.isEmpty()) {
            player.sendMessage(" §7(まだデータがありません)");
        } else {
            scores.entrySet().stream()
                    .sorted((a, b) -> b.getValue().compareTo(a.getValue()))
                    .limit(3)
                    .forEach(e -> {
                        OfflinePlayer op = Bukkit.getOfflinePlayer(UUID.fromString(e.getKey()));
                        player.sendMessage(" §7- §f" + (op.getName() != null ? op.getName() : "Unknown") + ": §e" + e.getValue() + "pt");
                    });
        }

        // --- 個人統計セクション (共通) ---
        player.sendMessage("");
        player.sendMessage(" §e§l▶ §f§lYOUR STATS");
        player.sendMessage("  §7現在の保持: §f" + myTeamTotalPoints + " pt");
        player.sendMessage("  §7総貢献量:   §a" + plugin.getTeamManager().getMemberTotal(group, teamId, uuid) + " pt");
        player.sendMessage("  §7貢献ランク: §6" + plugin.getTeamManager().getMemberRank(group, teamId, uuid) + "位 §8| §7倍率: §d" + multiplier + "x");
        player.sendMessage("§8§m                                     ");
    }

    /**
     * グループの表示名を安全に取得するメソッド
     */
    private String getGroupDisplayName(String group) {
        File file = plugin.getTeamManager().getRewardFile(group);
        if (!file.exists()) return "§f" + group;
        FileConfiguration cfg = YamlConfiguration.loadConfiguration(file);
        String name = cfg.getString("display_name", group);
        return ChatColor.translateAlternateColorCodes('&', name);
    }

    private String buildVSBar(int p1, int p2) {
        int total = p1 + p2;
        if (total == 0) return "§7[§8----------§f|§8----------§7]";
        // p1(自分のチーム)が左側、p2(敵チーム)が右側
        int ratio = (int) (((double) p1 / total) * 20);
        StringBuilder sb = new StringBuilder("§b"); // 自分のチームの色
        for (int i = 0; i < 20; i++) {
            if (i == 10) sb.append("§f|§e"); // 中央のセパレータと敵チームの色
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

        // 対戦相手の情報取得
        File rewardFile = plugin.getTeamManager().getRewardFile(group);
        FileConfiguration gCfg = YamlConfiguration.loadConfiguration(rewardFile);

        sender.sendMessage("§8§m----------§r §6Team Admin Info §8§m----------");
        sender.sendMessage("§7表示名: " + dName);
        sender.sendMessage("§7メンバー数: §f" + memberCount + " 名");
        sender.sendMessage("§7現在の保持ポイント: §e" + teamCfg.getInt("points.current") + " pt");
        sender.sendMessage("§7累計ポイント: §a" + teamCfg.getInt("points.total") + " pt");
        sender.sendMessage("§7現在の倍率: " + plugin.getTeamManager().getTeamMultiplierStatus(group, teamId));

        if (gCfg.getBoolean("battle.active")) {
            String t1 = gCfg.getString("battle.team1");
            String t2 = gCfg.getString("battle.team2");
            String enemyId = teamId.equals(t1) ? t2 : t1;
            sender.sendMessage("§c§lVS §7対戦相手: §e" + enemyId + " §8(§f" + plugin.getTeamManager().getTeamPoints(group, enemyId) + " pt§8)");
        }

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