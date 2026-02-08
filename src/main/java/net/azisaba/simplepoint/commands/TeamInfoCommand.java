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


    public void sendModernTeamInfo(Player player, String targetGroup) {
        UUID uuid = player.getUniqueId();
        String group = targetGroup;

        if (group == null) {
            group = plugin.getTeamManager().getPlayerGroup(uuid);
        }

        if (group == null) {
            player.sendMessage("§c§l[!] §7表示するグループを指定するか、どこかのチームに参加してください。");
            return;
        }

        String teamId = plugin.getTeamManager().getPlayerTeamInGroup(uuid, group);
        if (teamId == null) {
            player.sendMessage("§c§l[!] §7グループ §b" + group + " §7には参加していません。");
            return;
        }

        File rewardFile = new File(plugin.getDataFolder(), "teams/reward/" + group + ".yml");
        FileConfiguration gCfg = YamlConfiguration.loadConfiguration(rewardFile);
        File memberFile = new File(plugin.getDataFolder(), "teams/member/" + group + "/" + teamId + ".yml");
        FileConfiguration mCfg = YamlConfiguration.loadConfiguration(memberFile);

        String groupDisplay = ChatColor.translateAlternateColorCodes('&', gCfg.getString("display_name", group));
        // getTeamDisplayName などの既存メソッドを使用
        String teamDisplay = plugin.getTeamManager().getTeamDisplayName(group, teamId);
        double multiplier = plugin.getTeamManager().getTeamActiveMultiplier(group, teamId);

        // スコア計算
        int myTeamTotal = 0;
        java.util.TreeMap<String, Integer> scoreMap = new java.util.TreeMap<>();
        if (mCfg.contains("scores")) {
            for (String key : mCfg.getConfigurationSection("scores").getKeys(false)) {
                int val = mCfg.getInt("scores." + key);
                scoreMap.put(key, val);
                myTeamTotal += val;
            }
        }
        int memberCount = mCfg.getStringList("members").size();

        // --- メイン表示 (VS or NORMAL) ---
        if (gCfg.getBoolean("battle.active", false)) {
            String t1 = gCfg.getString("battle.team1");
            String t2 = gCfg.getString("battle.team2");
            String enemyId = teamId.equals(t1) ? t2 : t1;

            int enemyTotal = 0;
            int enemyCount = 0;
            File enemyFile = new File(plugin.getDataFolder(), "teams/member/" + group + "/" + enemyId + ".yml");
            if (enemyFile.exists()) {
                FileConfiguration eCfg = YamlConfiguration.loadConfiguration(enemyFile);
                enemyCount = eCfg.getStringList("members").size();
                if (eCfg.contains("scores")) {
                    for (String key : eCfg.getConfigurationSection("scores").getKeys(false)) {
                        enemyTotal += eCfg.getInt("scores." + key);
                    }
                }
            }

            player.sendMessage("§8§m      §r " + groupDisplay + " §b§lVS STATUS §r §8§m      ");
            player.sendMessage("");
            player.sendMessage(" §f" + teamDisplay + " §b§l" + myTeamTotal + " pt §7(" + memberCount + "人) " );
            player.sendMessage(" " + buildVSBar(myTeamTotal, enemyTotal));
            player.sendMessage(" §f" + plugin.getTeamManager().getTeamDisplayName(group, enemyId) + " §e§l" + enemyTotal + " pt §7(" + enemyCount + "人)");
        } else {
            player.sendMessage("§8§m      §r " + groupDisplay + " §f§lTEAM INFO §r §8§m      ");
            player.sendMessage("");
            player.sendMessage(" §7所属チーム: " + teamDisplay + " §8| §7人数: §f" + memberCount + "名");
            player.sendMessage(" §7チーム総計: §e§l" + myTeamTotal + " pt");
        }

        // --- TOP3 ---
        player.sendMessage("");
        player.sendMessage(" §e§l▶ §f§lTEAM TOP CONTRIBUTORS");
        if (scoreMap.isEmpty()) {
            player.sendMessage(" §7(データがありません)");
        } else {
            scoreMap.entrySet().stream()
                    .sorted((a, b) -> b.getValue().compareTo(a.getValue()))
                    .limit(3)
                    .forEach(e -> {
                        String name = Bukkit.getOfflinePlayer(UUID.fromString(e.getKey())).getName();
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
            int[] sortedScores = scoreMap.values().stream()
                    .sorted(java.util.Comparator.reverseOrder())
                    .mapToInt(Integer::intValue)
                    .toArray();
            int nextScore = sortedScores[myRank - 2];

            player.sendMessage("  §7貢献ランク: §6" + myRank + "位 §8(§7あと §e" + (nextScore - myScore) + "pt §7でランクアップ!§8)");
        } else {
            player.sendMessage("  §7貢献ランク: §6§l1位");
        }
        player.sendMessage("§8§m                                     ");
        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BELL, 1.0f, 1.0f);
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