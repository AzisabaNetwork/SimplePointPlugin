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

import static sun.audio.AudioPlayer.player;

public class SPPTCommand implements CommandExecutor {
    private final SimplePointPlugin plugin;

    public SPPTCommand(SimplePointPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // 1. コマンドの判定 (/teaminfo または /spt teaminfo)
        boolean isShorthand = label.equalsIgnoreCase("teaminfo");
        boolean isSubCommand = label.equalsIgnoreCase("spt") && args.length > 0 && args[0].equalsIgnoreCase("teaminfo");

        if (isShorthand || isSubCommand) {
            // 2. プレイヤーチェック (AudioPlayerとの衝突を防ぐためフルパス指定)
            if (!(sender instanceof org.bukkit.entity.Player)) {
                sender.sendMessage("§cプレイヤーのみ実行可能です。");
                return true;
            }
            org.bukkit.entity.Player player = (org.bukkit.entity.Player) sender;

            // 3. 引数からグループ名を抽出する (エイリアスによって位置が変わるのを修正)
            String groupName = null;
            if (isShorthand) {
                // /teaminfo <group> の場合、args[0] がグループ名
                if (args.length > 0) groupName = args[0];
            } else {
                // /spt teaminfo <group> の場合、args[1] がグループ名
                if (args.length > 1) groupName = args[1];
            }

            // 4. 2つの引数を渡して実行！
            sendModernTeamInfo(player, groupName);
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


            case "moderninfo":
            case "teaminfo":
                // 1. senderがプレイヤーであることを確認
                if (!(sender instanceof org.bukkit.entity.Player)) {
                    sender.sendMessage("§cこのコマンドはプレイヤーのみ実行可能です。");
                    return true;
                }

                // 変数名を target から targetGroupId に変更して衝突を回避
                org.bukkit.entity.Player p = (org.bukkit.entity.Player) sender;
                String targetGroupId = (args.length > 1) ? args[1] : null;

                // 2. 引数（グループ名）を渡して呼び出し
                sendModernTeamInfo(p, targetGroupId);
                return true;

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
//    public void sendModernTeamInfo(Player player) {
//        UUID uuid = player.getUniqueId();
//        // 1. 自分が実際に所属している「グループ名」と「チームID」を物理ファイルから特定
//        String group = plugin.getTeamManager().getPlayerGroup(uuid);
//        String teamId = plugin.getTeamManager().getPlayerTeam(uuid);
//
//        if (group == null || teamId == null) {
//            player.sendMessage("§c§l[!] §7所属データが見つかりません。");
//            return;
//        }
//
//        FileConfiguration gCfg = YamlConfiguration.loadConfiguration(plugin.getTeamManager().getRewardFile(group));
//        String groupDisplay = getGroupDisplayName(group); // 下記メソッド参照
//        String teamDisplay = plugin.getTeamManager().getTeamDisplayName(group, teamId);
//
//        // 2. 所属チームの統計取得
//        int myTeamPoints = plugin.getTeamManager().getTeamPoints(group, teamId);
//        int teamMemberCount = plugin.getTeamManager().getMemberNames(group, teamId).size();
//        double multiplier = plugin.getTeamManager().getTeamActiveMultiplier(group, teamId);
//
//        // --- 表示開始 ---
//        if (gCfg.getBoolean("battle.active", false)) {
//            // VSモード表示
//            String t1 = gCfg.getString("battle.team1");
//            String t2 = gCfg.getString("battle.team2");
//            String enemyId = teamId.equals(t1) ? t2 : t1;
//            int enemyPoints = plugin.getTeamManager().getTeamPoints(group, enemyId);
//            String enemyDisplay = plugin.getTeamManager().getTeamDisplayName(group, enemyId);
//
//            player.sendMessage("§8§m      §r " + groupDisplay + " §b§lVS STATUS §r §8§m      ");
//            player.sendMessage("");
//            player.sendMessage(" §f" + teamDisplay + " §b§l" + myTeamPoints + " pt §7(" + teamMemberCount + "人)");
//            player.sendMessage(" " + buildVSBar(myTeamPoints, enemyPoints));
//            player.sendMessage(" §f" + enemyDisplay + " §e§l" + enemyPoints + " pt");
//        } else {
//            // 通常モード表示
//            player.sendMessage("§8§m      §r " + groupDisplay + " §f§lTEAM INFO §r §8§m      ");
//            player.sendMessage("");
//            player.sendMessage(" §7所属チーム: " + teamDisplay);
//            player.sendMessage(" §7チーム人数: §f" + teamMemberCount + " 名");
//            player.sendMessage(" §7チーム総計: §e§l" + myTeamPoints + " pt");
//        }
//
//        // --- TOP3 取得ロジック (保存場所: teams/member/グループ/チーム.yml) ---
//        player.sendMessage("");
//        player.sendMessage(" §e§l▶ §f§lTEAM TOP CONTRIBUTORS");
//
//        // 修正: 正しいファイルパスから読み込み
//        File memberFile = new File(plugin.getDataFolder(), "teams/member/" + group + "/" + teamId + ".yml");
//        FileConfiguration mCfg = YamlConfiguration.loadConfiguration(memberFile);
//        java.util.Map<String, Double> scores = new java.util.HashMap<>();
//
//        if (mCfg.contains("contributions")) {
//            for (String key : mCfg.getConfigurationSection("contributions").getKeys(false)) {
//                scores.put(key, mCfg.getDouble("contributions." + key));
//            }
//        }
//
//        if (scores.isEmpty()) {
//            player.sendMessage(" §7(まだデータがありません)");
//        } else {
//            scores.entrySet().stream()
//                    .sorted((a, b) -> b.getValue().compareTo(a.getValue()))
//                    .limit(3)
//                    .forEach(e -> {
//                        org.bukkit.OfflinePlayer op = org.bukkit.Bukkit.getOfflinePlayer(UUID.fromString(e.getKey()));
//                        String name = (op.getName() != null) ? op.getName() : "Unknown";
//                        player.sendMessage(" §7- §f" + name + ": §e" + e.getValue().intValue() + "pt");
//                    });
//        }
//
//        // --- 個人統計 ---
//        player.sendMessage("");
//        player.sendMessage(" §e§l▶ §f§lYOUR STATS");
//        player.sendMessage("  §7現在の保持: §f" + mCfg.getDouble("contributions." + uuid.toString(), 0) + " pt");
//        player.sendMessage("  §7貢献ランク: §6" + plugin.getTeamManager().getMemberRank(group, teamId, uuid) + "位 §8| §7倍率: §d" + multiplier + "x");
//        player.sendMessage("§8§m                                     ");
//    }

    public void sendModernTeamInfo(Player player, String targetGroup) {
        UUID uuid = player.getUniqueId();
        String group = targetGroup;

        // 引数がない場合は、現在所属しているグループを探す
        if (group == null) {
            group = plugin.getTeamManager().getPlayerGroup(uuid);
        }

        if (group == null) {
            player.sendMessage("§c§l[!] §7表示するグループを指定するか、どこかのチームに参加してください。");
            player.sendMessage("§7例: /spt teaminfo neko");
            return;
        }

        // 指定されたグループでのチームIDを取得
        String teamId = plugin.getTeamManager().getPlayerTeamInGroup(uuid, group);

        if (teamId == null) {
            player.sendMessage("§c§l[!] §7指定されたグループ §b" + group + " §7には参加していません。");
            return;
        }

        // 設定ファイル: teams/reward/<グループ名>.yml
        File rewardFile = new File(plugin.getDataFolder(), "teams/reward/" + group + ".yml");
        FileConfiguration gCfg = YamlConfiguration.loadConfiguration(rewardFile);

        // メンバーファイル: teams/member/<グループ名>/<チーム名>.yml
        File memberFile = new File(plugin.getDataFolder(), "teams/member/" + group + "/" + teamId + ".yml");
        FileConfiguration mCfg = YamlConfiguration.loadConfiguration(memberFile);

        String groupDisplay = ChatColor.translateAlternateColorCodes('&', gCfg.getString("display_name", group));
        String teamDisplay = plugin.getTeamManager().getTeamDisplayName(group, teamId);

        // チーム合計点 (scoresセクションを合計)
        int myTeamTotal = 0;
        java.util.Map<String, Integer> scoreMap = new java.util.HashMap<>();
        if (mCfg.contains("scores")) {
            for (String key : mCfg.getConfigurationSection("scores").getKeys(false)) {
                int val = mCfg.getInt("scores." + key);
                scoreMap.put(key, val);
                myTeamTotal += val;
            }
        }

        int memberCount = mCfg.getStringList("members").size();

        // --- メイン表示 ---
        if (gCfg.getBoolean("battle.active", false)) {
            // 【VSモード中】
            String t1 = gCfg.getString("battle.team1");
            String t2 = gCfg.getString("battle.team2");
            String enemyId = teamId.equals(t1) ? t2 : t1;

            // 敵チームのスコアを計算
            int enemyTotal = 0;
            File enemyFile = new File(plugin.getDataFolder(), "teams/member/" + group + "/" + enemyId + ".yml");
            if (enemyFile.exists()) {
                FileConfiguration eCfg = YamlConfiguration.loadConfiguration(enemyFile);
                if (eCfg.contains("scores")) {
                    for (String key : eCfg.getConfigurationSection("scores").getKeys(false)) {
                        enemyTotal += eCfg.getInt("scores." + key);
                    }
                }
            }

            player.sendMessage("§8§m      §r " + groupDisplay + " §b§lVS STATUS §r §8§m      ");
            player.sendMessage("");
            player.sendMessage(" §f" + teamDisplay + " §b§l" + myTeamTotal + " pt §7(" + memberCount + "人)");
            player.sendMessage(" " + buildVSBar(myTeamTotal, enemyTotal));
            player.sendMessage(" §f" + plugin.getTeamManager().getTeamDisplayName(group, enemyId) + " §e§l" + enemyTotal + " pt");
        } else {
            // 【通常モード】
            player.sendMessage("§8§m      §r " + groupDisplay + " §f§lTEAM INFO §r §8§m      ");
            player.sendMessage("");
            player.sendMessage(" §7所属チーム: " + teamDisplay);
            player.sendMessage(" §7チーム人数: §b" + memberCount + " 名");
            player.sendMessage(" §7チーム総計: §e§l" + myTeamTotal + " pt");
        }

        // --- TOP3 CONTRIBUTORS ---
        player.sendMessage("");
        player.sendMessage(" §e§l▶ §f§lTEAM TOP CONTRIBUTORS");
        if (scoreMap.isEmpty()) {
            player.sendMessage(" §7(まだデータがありません)");
        } else {
            scoreMap.entrySet().stream()
                    .sorted((a, b) -> b.getValue().compareTo(a.getValue()))
                    .limit(3)
                    .forEach(e -> {
                        String name = Bukkit.getOfflinePlayer(UUID.fromString(e.getKey())).getName();
                        player.sendMessage(" §7- §f" + (name != null ? name : "Unknown") + ": §e" + e.getValue() + "pt");
                    });
        }

        // --- 個人統計 ---
        player.sendMessage("");
        player.sendMessage(" §e§l▶ §f§lYOUR STATS");
        player.sendMessage("  §7あなたのスコア: §f" + scoreMap.getOrDefault(uuid.toString(), 0) + " pt");
        player.sendMessage("  §7貢献ランク: §6" + plugin.getTeamManager().getMemberRank(group, teamId, uuid) + "位");
        player.sendMessage("§8§m                                     ");
    }

    private String getGroupDisplayName(String group) {
        File file = plugin.getTeamManager().getRewardFile(group);
        if (!file.exists()) return "§f" + group;
        FileConfiguration cfg = YamlConfiguration.loadConfiguration(file);
        String name = cfg.getString("display_name", group);
        return ChatColor.translateAlternateColorCodes('&', name);
    }

    private String buildVSBar(int p1, int p2) {
        int total = p1 + p2;
        if (total == 0) return "§7[ §8--- DRAW --- §7]";

        double pct = ((double) p1 / total) * 100;
        int segments = 20;
        int filled = (int) (pct / (100.0 / segments));

        StringBuilder bar = new StringBuilder("§7[");
        for (int i = 0; i < segments; i++) {
            if (i == segments / 2) bar.append("§f┃"); // センターライン
            if (i < filled) bar.append("§b■"); // 自チーム
            else bar.append("§e■"); // 敵チーム
        }
        bar.append("§7]");

        // 状況に応じたメッセージ
        String status;
        if (pct > 70) status = "§b§l§nCRUSHING!!";
        else if (pct > 55) status = "§3§lDOMINATING";
        else if (pct > 45) status = "§f§lDEAD HEAT";
        else if (pct > 30) status = "§6§lLOSING...";
        else status = "§c§l§nCRITICAL!!";

        return bar.toString() + " " + status + " §f(" + (int)pct + "% vs " + (100 - (int)pct) + "%)";
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