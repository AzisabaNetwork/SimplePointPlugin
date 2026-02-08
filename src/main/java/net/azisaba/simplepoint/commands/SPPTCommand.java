package net.azisaba.simplepoint.commands;

import net.azisaba.simplepoint.SimplePointPlugin;
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
            case "create": {
                if (args.length < 3) {
                    sender.sendMessage("§c使用法: /sppt create <ID> <表示名(カラーコード可)>");
                    return true;
                }
                String groupId = args[1];
                String displayName = args[2];

                // 1. まずはグループのディレクトリや基本的な管理構造を作成
                plugin.getTeamManager().createGroup(groupId, displayName);

                // 2. デフォルト設定を yml に書き込む (TeamManager.java側で実装しても良いですが、ここで完結させます)
                File rewardFile = new File(plugin.getDataFolder(), "teams/reward/" + groupId + ".yml");
                FileConfiguration gCfg = YamlConfiguration.loadConfiguration(rewardFile);

                // デフォルトの設定値をセット (ファイルが存在しない場合のみ、あるいは上書き)
                gCfg.set("display_name", displayName);
                gCfg.set("battle.active", false); // 最初はVSモードをオフにしておく
                gCfg.set("battle.team1", "teamA"); // 対戦予定スロット1
                gCfg.set("battle.team2", "teamB"); // 対戦予定スロット2
                gCfg.set("battle.color_self", "&b"); // デフォルトの自分色
                gCfg.set("battle.color_enemy", "&e"); // デフォルトの相手色

                try {
                    gCfg.save(rewardFile);
                } catch (java.io.IOException e) {
                    e.printStackTrace();
                    sender.sendMessage("§c設定ファイルの保存中にエラーが発生しました。");
                }

                sender.sendMessage("§aグループ §l" + groupId + " §aを名称 §r" +
                        ChatColor.translateAlternateColorCodes('&', displayName) + " §aで作成しました。");
                sender.sendMessage("§7(デフォルトの対戦設定を reward/" + groupId + ".yml に生成しました)");
                break;
            }

            case "teamcreate":
                if (args.length < 4) {
                    sender.sendMessage("§c使用法: /sppt teamcreate <グループ名> <チームID> <表示名>");
                    return true;
                }
                plugin.getTeamManager().createTeam(args[1], args[2], args[3]);
                sender.sendMessage("§aチーム §l" + args[3] + " §aを作成しました。");
                break;

            case "join": {
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
            }

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

            case "multiplier": {
                if (args.length < 6) { // 引数が1つ増えて 6 になります
                    sender.sendMessage("§c使用法: /sppt multiplier <group> <teamId> <倍率> <開始> <終了>");
                    return true;
                }
                try {
                    String group = args[1];
                    String teamId = args[2];
                    double mult = Double.parseDouble(args[3]);
                    String start = args[4];
                    String end = args[5];

                    plugin.getTeamManager().setTeamMultiplier(group, teamId, mult, start, end);
                    sender.sendMessage("§aチーム " + teamId + " (" + group + ") に " + mult + "倍 を設定しました。");
                } catch (Exception e) {
                    sender.sendMessage("§cエラー: " + e.getMessage());
                }
                return true;
            }

            case "vsteam": {
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
            }


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

            case "userinfo": {
                if (args.length < 2) {
                    sender.sendMessage("§c使用法: /sppt userinfo <プレイヤー名>");
                    return true;
                }
                org.bukkit.OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
                UUID uuid = target.getUniqueId();

                sender.sendMessage("§8§m----------§r §6§lUSER INFO: §e" + target.getName() + " §8§m----------");
                sender.sendMessage(" §7所属グループ        §7チーム");

                File teamDir = new File(plugin.getDataFolder(), "teams/team");
                boolean found = false;
                if (teamDir.exists() && teamDir.list() != null) {
                    for (String groupId : teamDir.list()) {
                        String teamId = plugin.getTeamManager().getPlayerTeamInGroup(uuid, groupId);
                        if (teamId != null) {
                            // ここで共通メソッドを呼び出す！
                            String groupDisplay = plugin.getTeamManager().getGroupDisplayName(groupId);
                            String teamDisplay = plugin.getTeamManager().getTeamDisplayName(groupId, teamId);

                            // 表示（フォーマットを整える）
                            sender.sendMessage(" §f" + groupDisplay + " §8| §a" + teamDisplay);
                            found = true;
                        }
                    }
                }

                if (!found) sender.sendMessage(" §7所属状態: §c未所属");
                sender.sendMessage("§8§m--------------------------------------");
                return true;
            }

            case "reload": {
                if (!sender.hasPermission("simplepoint.admin")) {
                    sender.sendMessage("§c権限がありません。");
                    return true;
                }

                // 全データのリロード実行
                plugin.getTeamManager().reloadAll();

                // --- ここから追加 ---
                // 溜まっていた個人ポイントを各チームのスコアに一括同期（振り分け）
                // 引数1: ポイントID("BetaVS"), 引数2: チームグループ名("BetaVS")
                plugin.getTeamManager().migrateAllPointsToTeam("BetaVS", "BetaVS");
                // --- ここまで追加 ---

                sender.sendMessage("§a§l[!] §fSimplePoint の全てのコンフィグをリロードし、チームスコアを同期しました。");
                return true;
            }

            case "fixdata": {
                if (!sender.hasPermission("simplepoint.admin")) return true;

                sender.sendMessage("§a[!] データ構造の修復を開始します...");
                File memberDir = new File(plugin.getDataFolder(), "teams/member");
                if (!memberDir.exists()) return true;

                int fixedCount = 0;
                for (File groupFolder : memberDir.listFiles()) {
                    if (!groupFolder.isDirectory()) continue;
                    for (File teamFile : groupFolder.listFiles()) {
                        if (!teamFile.getName().endsWith(".yml")) continue;

                        FileConfiguration cfg = YamlConfiguration.loadConfiguration(teamFile);
                        if (!cfg.contains("scores")) continue;

                        for (String uuidKey : cfg.getConfigurationSection("scores").getKeys(false)) {
                            // 古い形式（値がセクションではなく数値）かチェック
                            if (!cfg.isConfigurationSection("scores." + uuidKey)) {
                                int oldScore = cfg.getInt("scores." + uuidKey);
                                cfg.set("scores." + uuidKey, null); // 一旦消す
                                cfg.set("scores." + uuidKey + ".total", oldScore); // 階層化して再設定
                                fixedCount++;
                            }
                        }
                        try { cfg.save(teamFile); } catch (IOException e) { e.printStackTrace(); }
                    }
                }
                sender.sendMessage("§a[!] 修復完了: §e" + fixedCount + " §a件のデータを正常化しました。");
                return true;
            }

            case "deletegroup": {
                if (args.length < 2) {
                    sender.sendMessage("§c使用法: /sppt deletegroup <グループID>");
                    return true;
                }
                String gid = args[1];
                String timestamp = new java.text.SimpleDateFormat("yyyyMMdd_HHmm").format(new java.util.Date());

                // 1. 報酬設定ファイルをリネーム (.yml -> .yml.log)
                File rewardFile = new File(plugin.getDataFolder(), "teams/reward/" + gid + ".yml");
                if (rewardFile.exists()) {
                    File backupReward = new File(plugin.getDataFolder(), "teams/reward/" + gid + ".yml.deleted_" + timestamp + ".log");
                    rewardFile.renameTo(backupReward);
                }

                // 2. メンバーフォルダをリネーム (フォルダごと名前を変えて読み込まれなくする)
                File memberDir = new File(plugin.getDataFolder(), "teams/member/" + gid);
                if (memberDir.exists()) {
                    File backupDir = new File(plugin.getDataFolder(), "teams/member/" + gid + "_deleted_" + timestamp);
                    memberDir.renameTo(backupDir);
                }

                sender.sendMessage("§e§l[INFO] §fグループ 「" + gid + "」 は無効化され、ログファイルに変換されました。");
                sender.sendMessage("§8(復元が必要な場合はファイル名を戻してください)");
                return true;

            }

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

            case "manual":
                sendManual(sender);
                return true;

            case "rewardgui": {
                if (args.length < 2) {
                    sender.sendMessage("§c使用法: /sppt rewardgui <グループ名>");
                    return true;
                }
                if (!(sender instanceof Player)) return true;
                plugin.getTeamAdminGUIManager().openGroupRewardEditor((Player) sender, args[1]);
                break;
            }

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

    public void showTeamMembers(CommandSender sender, String groupId, String teamId) {
        File file = new File(plugin.getDataFolder(), "teams/member/" + groupId + "/" + teamId + ".yml");
        if (!file.exists()) {
            sender.sendMessage("§c指定されたチームのデータが見つかりません。");
            return;
        }

        FileConfiguration cfg = YamlConfiguration.loadConfiguration(file);
        List<String> members = cfg.getStringList("members");
        String teamDisplay = plugin.getTeamManager().getTeamDisplayName(groupId, teamId);

        sender.sendMessage("§8§m----------§r §6" + teamDisplay + " §eMEMBERS §8§m----------");

        if (members.isEmpty()) {
            sender.sendMessage(" §7(メンバーはいません)");
        } else {
            for (String uuidStr : members) {
                // 重要： .total まで指定して取得する
                int score = cfg.getInt("scores." + uuidStr + ".total", 0);
                String name = Bukkit.getOfflinePlayer(UUID.fromString(uuidStr)).getName();
                sender.sendMessage(" §7- §f" + (name != null ? name : "Unknown") + " §8| §e" + score + "pt");
            }
        }
        sender.sendMessage("§8§m--------------------------------------");
    }



    public void sendModernTeamInfo(org.bukkit.entity.Player player, String targetGroup) {
        UUID uuid = player.getUniqueId();
        String group = targetGroup;

        // --- 修正ポイント1: グループ指定がない場合の自動取得 ---
        if (group == null) {
            List<String> joinedGroups = plugin.getTeamManager().getAllJoinedGroups(uuid);
            if (joinedGroups.isEmpty()) {
                player.sendMessage("§c§l[!] §7あなたは現在、どのグループのチームにも所属していません。");
                return;
            }
            // 複数学所属している場合は、リストの最初（またはアクティブなもの）を表示
            group = joinedGroups.get(0);
        }

        // --- 修正ポイント2: 所属判定 ---
        String teamId = plugin.getTeamManager().getPlayerTeamInGroup(uuid, group);
        if (teamId == null) {
            player.sendMessage("§c§l[!] §7あなたはグループ §b" + group + " §7内のチームには所属していません。");
            return;
        }

        // ファイルパスの構成
        File rewardFile = new File(plugin.getDataFolder(), "teams/reward/" + group + ".yml");
        FileConfiguration gCfg = YamlConfiguration.loadConfiguration(rewardFile);

        // メンバー情報の読み込み
        File memberFile = new File(plugin.getDataFolder(), "teams/member/" + group + "/" + teamId + ".yml");
        FileConfiguration mCfg = YamlConfiguration.loadConfiguration(memberFile);

        String groupDisplay = org.bukkit.ChatColor.translateAlternateColorCodes('&', gCfg.getString("display_name", group));
        String teamDisplay = plugin.getTeamManager().getTeamDisplayName(group, teamId);
        double multiplier = plugin.getTeamManager().getTeamActiveMultiplier(group, teamId);

        // --- 修正ポイント3: スコア計算の正確化 ---
        // total_score(チーム累計)を取得
        int myTeamTotal = plugin.getTeamManager().getTeamTotalScore(group, teamId);

//        java.util.TreeMap<String, Integer> scoreMap = new java.util.TreeMap<>();
//        if (mCfg.contains("scores")) {
//            for (String key : mCfg.getConfigurationSection("scores").getKeys(false)) {
//                // ここを .total まで指定するように修正！
//                int val = mCfg.getInt("scores." + key + ".total", 0);
//                scoreMap.put(key, val);
//            }
//        }
        // --- scoreMap を正しく作成するコード ---
        Map<String, Integer> scoreMap = new HashMap<>();
        if (mCfg.contains("scores")) {
            for (String uuidKey : mCfg.getConfigurationSection("scores").getKeys(false)) {
                // scores.UUID.total を取得する。階層化されているため getInt(path + ".total") が必須
                int score = mCfg.getInt("scores." + uuidKey + ".total", 0);
                scoreMap.put(uuidKey, score);
            }
        }

        // メンバー人数の取得 (リストがない場合は0)
        List<String> members = mCfg.getStringList("members");
        int memberCount = (members != null) ? members.size() : 0;

        // --- メイン表示 (VS or NORMAL) ---
        if (gCfg.getBoolean("battle.active", false)) {
            String t1 = gCfg.getString("battle.team1");
            String t2 = gCfg.getString("battle.team2");
            String enemyId = teamId.equals(t1) ? t2 : t1;

            int enemyTotal = plugin.getTeamManager().getTeamTotalScore(group, enemyId);

            // 敵チームの人数取得
            File enemyMemberFile = new File(plugin.getDataFolder(), "teams/member/" + group + "/" + enemyId + ".yml");
            int enemyCount = YamlConfiguration.loadConfiguration(enemyMemberFile).getStringList("members").size();

            player.sendMessage("§8§m      §r " + groupDisplay + " §4§lV§6§lS §c§lSTATUS §r §8§m      ");
            player.sendMessage("");
            player.sendMessage(" §e所属チーム:" + teamDisplay + " §7合計§b§l" + myTeamTotal + "§7pt");
            player.sendMessage("");
            player.sendMessage(" §f" + teamDisplay + " §b§l" + myTeamTotal + " pt §7(" + memberCount + "人) " );
            player.sendMessage(" " + plugin.getTeamManager().buildVSBar(myTeamTotal, enemyTotal, "§b", "§e"));
            player.sendMessage(" §f" + plugin.getTeamManager().getTeamDisplayName(group, enemyId) + " §e§l" + enemyTotal + " pt §7(" + enemyCount + "人)");
        } else {
            player.sendMessage("§8§m      §r " + groupDisplay + " §f§lTEAM INFO §r §8§m      ");
            player.sendMessage("");
            player.sendMessage(" §7所属チーム: " + teamDisplay + " §8| §7人数: §f" + memberCount + "名");
            player.sendMessage(" §7チーム総計: §e§l" + myTeamTotal + " pt");
        }

        // --- TOP3 表示 ---
        player.sendMessage("");
        player.sendMessage(" §e§l▶ §f§lTEAM TOP CONTRIBUTORS");
        if (scoreMap.isEmpty()) {
            player.sendMessage(" §7(データがありません)");
        } else {
            scoreMap.entrySet().stream()
                    .sorted((a, b) -> b.getValue().compareTo(a.getValue())) // 降順ソート
                    .limit(3)
                    .forEach(e -> {
                        // OfflinePlayerの取得はメインスレッドで動作しますが、人数が多いと一瞬重くなるため
                        // 本来は名前もキャッシュするのが理想ですが、TOP3程度なら問題ありません。
                        String name = org.bukkit.Bukkit.getOfflinePlayer(UUID.fromString(e.getKey())).getName();
                        player.sendMessage(" §7- §f" + (name != null ? name : "Unknown") + " §e" + e.getValue() + "§7pt");
                    });
        }

        // --- YOUR STATS ---
        int myScore = mCfg.getInt("scores." + uuid.toString() + ".total", 0);
        int myRank = plugin.getTeamManager().getMemberRank(group, teamId, uuid);

        player.sendMessage("");
        player.sendMessage(" §e§l▶ §f§lYOUR STATS");
        player.sendMessage("  §7個人貢献: §f" + myScore + " pt §8| §7倍率: §d" + multiplier + "x");


        if (myRank > 1) {
            // 次のランクへの差
            Object[] sortedScores = scoreMap.values().stream().sorted(java.util.Comparator.reverseOrder()).toArray();
            int nextScore = (int) sortedScores[myRank - 2];
            player.sendMessage("  §7貢献ランク: §6" + myRank + "位 §8(§7あと §e" + (nextScore - myScore) + "pt §7でランクアップ!§8)");
        } else if (myRank == 1) {
            player.sendMessage("  §7貢献ランク: §6§l1位");
        }
        player.sendMessage("§8§m                                     ");
    }

    private String getGroupDisplayName(String group) {
        File file = plugin.getTeamManager().getRewardFile(group);
        if (!file.exists()) return "§f" + group;
        FileConfiguration cfg = YamlConfiguration.loadConfiguration(file);
        String name = cfg.getString("display_name", group);
        return ChatColor.translateAlternateColorCodes('&', name);
    }

//    private String buildVSBar(int p1, int p2) {
//        int total = p1 + p2;
//        if (total == 0) return "§7[ §8---------- §fVS §8---------- §7]";
//
//        double pct = ((double) p1 / total) * 100;
//        int segments = 20;
//        int filled = (int) (pct / (100.0 / segments));
//
//        StringBuilder bar = new StringBuilder("§7[");
//        for (int i = 0; i < segments; i++) {
//            if (i == 10) bar.append("§f┃"); // センター
//            if (i < filled) bar.append("§b■");
//            else bar.append("§e■");
//        }
//        bar.append("§7]");
//
//        String status;
//        if (pct > 80) status = "§b§lDOMINATING!!";
//        else if (pct > 60) status = "§3§lADVANTAGE";
//        else if (pct > 40) status = "§f§lEVEN";
//        else if (pct > 20) status = "§6§lPUSHING...";
//        else status = "§c§lCRITICAL!!";
//
//        return bar.toString() + " " + status + " §8(§b" + (int)pct + "% §7vs §e" + (100 - (int)pct) + "%§8)";
//    }

    private void sendManual(CommandSender sender) {
        sender.sendMessage("§8§m-----------------------------------------");
        sender.sendMessage("   §6§lSimplePoint §b§lTeamSystem Manual");
        sender.sendMessage("");

        sender.sendMessage(" §e①.§f§lチームの作成");
        sender.sendMessage("  §f-1. /sppt createで基盤となるグループを作成しましょう。(豪華な表示名を推奨します!!)");
        sender.sendMessage("  §f-2. /sppt teamcreateでグループの中にチームを作ります。2つ以上作りましょう。");
        sender.sendMessage("  §f-3. /spp createで個人ポイントを作成します。これがグループ全体で使われます。");
        sender.sendMessage("  §7-   このときspp togglefunctionをして個人ポイントのランキング機能などを無効化することをお勧めします！");
        sender.sendMessage("  §f-4. /sppt setpointで先ほど作成した個人ポイントをグループに連携させます。これにより個人ポイント獲得時に所属するチームにも加算されます。");
        sender.sendMessage("  §f-5. /sppt setjoinguiでチーム参加GUIを設定します。");
        sender.sendMessage("  §f-   これでチームの作成は完了です！プレイヤーがチームに参加できます！");
        sender.sendMessage("");

        sender.sendMessage(" §e②. §f§lVSモード (BATTLE)");
        sender.sendMessage("  §f- /sppt vsteamでグループ内のチームで対戦を開始します。");
        sender.sendMessage("  §f- /teaminfoで戦況を確認し、優勢を保ちましょう。");
        sender.sendMessage("  §f- バーが §c§lCRITICAL §fの時は、逆転のチャンス！");
        sender.sendMessage("  §f- /sppt finishvsmodeで対戦を終了できます。");
        sender.sendMessage("");

        sender.sendMessage(" §e③. §f§lポイント貢献とランク");
        sender.sendMessage("  §f- ポイントを獲得すると自動で §aチームスコア §fに加算。");
        sender.sendMessage("  §f- 個人貢献度が高いほど、§6貢献ランク §fが上昇します。");
        sender.sendMessage("  §f- /sppt multiperで期限付きのポイント獲得倍率を設定できます。");
        sender.sendMessage("");

        sender.sendMessage(" §e④. §f§l報酬");
        sender.sendMessage("  §f- /sppt rewardguiでチーム報酬編集GUIを開けます。");
        sender.sendMessage("  §f- チーム総ポイントで取引を解禁することができます。");
        sender.sendMessage("");

        sender.sendMessage(" §e⑤. §f§lその他");
        sender.sendMessage("  §f- /sppt teamjoinguiでチーム参加GUIを開けます。");
        sender.sendMessage("  §f- sbpなどでブロックに埋め込んだりnpcに設定したりすることをおすすめします!");
        sender.sendMessage("");
        sender.sendMessage(" §8§m-----------------------------------------");
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
        sender.sendMessage(" §e§l▶ §b§lチーム・グループ基本操作");
        sender.sendMessage("  §f/sppt §bcreate §3<ID> <Name> §7- グループ作成");
        sender.sendMessage("  §f/sppt §bteamcreate §3<G> <ID> <Name> §7- チーム作成");
        sender.sendMessage("  §f/sppt §bjoin §3<G> <ID> <Player> §7- メンバー追加");
        sender.sendMessage("  §f/sppt §bleave §3<G> <ID> <Player> §7- メンバー脱退");
        sender.sendMessage("");
        sender.sendMessage(" §e§l▶ §a§l対戦・ポイント倍率設定");
        sender.sendMessage("  §f/sppt §avsteam §3<G> <T1> <T2> §7- VSモード(対戦)開始");
        sender.sendMessage("  §f/sppt §afinishvsmode §3<G> §7- VSモード(対戦)終了");
        sender.sendMessage("  §f/sppt §amultiplier §3<G> <倍率> <開始時間> <終了時間> §7- 期限付き倍率設定");
        sender.sendMessage("  §f/sppt §asetpoint §3<G> <PointID> §7- sppポイント連携");
        sender.sendMessage("  §f/sppt §ateaminfo §3<G> §7- 戦況・貢献度表示");
        sender.sendMessage("  §f/sppt §atoggle §3<G> §7- グループ全体のポイント受け取り設定");
        sender.sendMessage("  §f/sppt §asetpoint_value §3<G> <T> <Amout> §7- チームのポイントを設定(非推奨)");
        sender.sendMessage("");
        sender.sendMessage(" §e§l▶ §d§lGUI・システム管理");
        sender.sendMessage("  §f/sppt §dsetjoingui §3<G> <T1> <T2> <Mode> <Auto> §7- 参加GUI設定");
        sender.sendMessage("  §f/sppt §dteamjoingui §3<G> §7- 参加GUIを表示");
        sender.sendMessage("  §f/sppt §drewardgui §3<G> §7- 報酬スロット編集");
        sender.sendMessage("  §f/sppt §dmember §3<G> <T> §7- チームメンバー一覧");
        sender.sendMessage("  §f/sppt §dinfo §3<G> <T> §7- 運営用簡易チーム情報");
        sender.sendMessage("  §f/sppt §duserinfo §3<Player> §7- プレイヤー情報");
        sender.sendMessage("  §f/sppt §ddeletegroup §3<G> §7- 指定したグループを削除");
        sender.sendMessage("  §f/sppt §freload §7- コンフィグリロード");
        sender.sendMessage("");
        sender.sendMessage(" §7※ §3<G>§7=グループID, §3<T>§7=チームID, §3<Mode>§7=choice/random");
        sender.sendMessage(" §7※ §3<Auto>§7=true/false (ログイン時に表示するか)");
        sender.sendMessage("§8§m-----------------------------------------");
    }
}