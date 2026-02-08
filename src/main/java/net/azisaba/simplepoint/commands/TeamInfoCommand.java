package net.azisaba.simplepoint.commands;

import net.azisaba.simplepoint.SimplePointPlugin;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

public class TeamInfoCommand implements CommandExecutor, TabCompleter {
    private final SimplePointPlugin plugin;

    public TeamInfoCommand(SimplePointPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cこのコマンドはプレイヤーのみ実行可能です。");
            return true;
        }

        Player p = (Player) sender;
        String targetGroupId = (args.length > 0) ? args[0] : null;

        // あなたの提示したロジックを実行
        sendModernTeamInfo(p, targetGroupId);
        return true;
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

        java.util.TreeMap<String, Integer> scoreMap = new java.util.TreeMap<>();
        if (mCfg.contains("scores")) {
            for (String key : mCfg.getConfigurationSection("scores").getKeys(false)) {
                // ここを .total まで指定するように修正！
                int val = mCfg.getInt("scores." + key + ".total", 0);
                scoreMap.put(key, val);
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
        int myScore = scoreMap.getOrDefault(uuid.toString(), 0);
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

    private String buildVSBar(int p1, int p2) {
        int total = p1 + p2;
        if (total == 0) return "§7[ §8---------- §fVS §8---------- §7]";

        double pct = ((double) p1 / total) * 100;
        int segments = 20;
        int filled = (int) (pct / (100.0 / segments));

        StringBuilder bar = new StringBuilder("§7[");
        for (int i = 0; i < segments; i++) {
            if (i == 10) bar.append("§f┃");
            if (i < filled) bar.append("§b■");
            else bar.append("§e■");
        }
        bar.append("§7]");

        String status;
        if (pct > 80) status = "§b§lDOMINATING!!";
        else if (pct > 60) status = "§3§lADVANTAGE";
        else if (pct > 40) status = "§f§lEVEN";
        else if (pct > 20) status = "§6§lPUSHING...";
        else status = "§c§lCRITICAL!!";

        return bar.toString() + " " + status + " §8(§b" + (int)pct + "% §7vs §e" + (100 - (int)pct) + "%§8)";
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> completions = new ArrayList<>();
            File dir = new File(plugin.getDataFolder(), "teams/reward"); // rewardフォルダのファイル名を補完
            if (dir.exists() && dir.list() != null) {
                List<String> groups = Arrays.stream(dir.list())
                        .filter(s -> s.endsWith(".yml"))
                        .map(s -> s.replace(".yml", ""))
                        .collect(Collectors.toList());
                StringUtil.copyPartialMatches(args[0], groups, completions);
            }
            return completions;
        }
        return new ArrayList<>();
    }
}