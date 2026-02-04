package net.azisaba.simplepoint;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
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

public class SPTCommand implements CommandExecutor, TabCompleter {
    private final SimplePointPlugin plugin;

    public SPTCommand(SimplePointPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cこのコマンドはプレイヤーのみ実行可能です.");
            return true;
        }
        Player p = (Player) sender;

        if (args.length == 0 || args[0].equalsIgnoreCase("help")) {
            sendHelp(p);
            return true;
        }

        String sub = args[0].toLowerCase();

        switch (sub) {
            case "myp":
                handleMyp(p, args);
                break;
            case "reward":
                handleReward(p, args);
                break;
            case "ranking":
                if (args.length < 2) {
                    p.sendMessage("§c使用法: /spt ranking <ポイント名>");
                    return true;
                }
                showPersonalRanking(p, args[1]);
                break;
            case "teaminfo":
                handleTeamInfo(p, args);
                break;
            case "teamreward":
                handleTeamReward(p, args);
                break;
            case "toggleteamstats":
                plugin.getTeamManager().toggleStats(p.getUniqueId());
                boolean now = plugin.getTeamManager().canShowStats(p.getUniqueId());
                p.sendMessage("§a貢献度スコアの公開設定を §l" + (now ? "ON" : "OFF") + " §aにしました。");
                break;
            default:
                p.sendMessage("§c不明なコマンドです。/spt help を確認してください。");
                break;
        }
        return true;
    }

    private void handleMyp(Player p, String[] args) {
        if (args.length < 2) {
            p.sendMessage("§c使用法: /spt myp <ポイント名>");
            return;
        }
        String pointId = args[1];
        FileConfiguration cfg = plugin.getPointManager().getPointConfig(pointId);
        if (cfg == null) {
            p.sendMessage("§cそのポイント名は存在しません。");
            return;
        }
        String displayName = plugin.getPointManager().getDisplayName(pointId);
        int current = plugin.getPointManager().getPoint(pointId, p.getUniqueId());
        int total = plugin.getPointManager().getTotalPoint(pointId, p.getUniqueId());

        p.sendMessage("§8§m-------§r " + displayName + " §b§lSTATUS §8§m-------");
        p.sendMessage("§7現在の所持ポイント: §e" + current + " pt");
        p.sendMessage("§7これまでの累計獲得: §a" + total + " pt");
        p.sendMessage("§8§m---------------------");
    }

    private void handleReward(Player p, String[] args) {
        if (args.length < 2) {
            p.sendMessage("§c使用法: /spt reward <ポイント名>");
            return;
        }
        String pointId = args[1];
        plugin.getGuiManager().openRewardGUI(p, pointId, false);
    }

    private void handleTeamInfo(Player p, String[] args) {
        if (args.length < 2) {
            p.sendMessage("§c使用法: /spt teaminfo <グループ名>");
            return;
        }
        String group = args[1];
        String teamId = plugin.getTeamManager().findTeamIdInGroup(group, p.getUniqueId());

        if (teamId == null) {
            p.sendMessage("§cあなたはグループ " + group + " 内のどのチームにも所属していません。");
            return;
        }

        TeamManager tm = plugin.getTeamManager();
        String teamName = tm.getTeamDisplayName(group, teamId);
        int current = tm.getTeamCurrentPoint(group, teamId);
        int total = tm.getTeamTotalPoint(group, teamId);
        int contribution = tm.getContribution(group, teamId, p.getUniqueId());
        // 修正点: チームごとの倍率情報を取得
        String multStatus = tm.getTeamMultiplierStatus(group, teamId);

        p.sendMessage("§8§m-------§r " + teamName + " §6§lTEAM STATUS §8§m-------");
        p.sendMessage("§7現在のチームポイント: §e" + current + " pt");
        p.sendMessage("§7累計獲得スコア: §a" + total + " pt");
        p.sendMessage("§7適用中の倍率: " + multStatus);

        // 目標達成率の表示
        int goal = YamlConfiguration.loadConfiguration(tm.getTeamFile(group, teamId)).getInt("goal_point", 100000);
        double percent = Math.min(100.0, (double) total / (goal > 0 ? goal : 1) * 100);
        p.sendMessage("§f目標達成率: " + createProgressBar(percent) + " §e" + String.format("%.1f", percent) + "%");

        p.sendMessage("");
        p.sendMessage("§e§l▶ §fあなたの貢献データ");
        p.sendMessage("  §7貢献度スコア: §b" + contribution + " pt");
        p.sendMessage("  §7貢献ランク: §6" + tm.getMemberRank(group, teamId, p.getUniqueId()) + "位");

        p.sendMessage("");
        p.sendMessage("§f所属メンバー (貢献度):");
        List<String> memberNames = new ArrayList<>();
        FileConfiguration memberCfg = YamlConfiguration.loadConfiguration(tm.getMemberFile(group, teamId));
        for (String uuidStr : memberCfg.getStringList("members")) {
            UUID uuid = UUID.fromString(uuidStr);
            OfflinePlayer op = Bukkit.getOfflinePlayer(uuid);
            String name = (op.getName() != null) ? op.getName() : "Unknown";

            if (tm.canShowStats(uuid)) {
                int c = tm.getContribution(group, teamId, uuid);
                memberNames.add("§f" + name + " §7(§b" + c + "§7)");
            } else {
                memberNames.add("§7" + name + " §8(非公開)");
            }
        }
        p.sendMessage(" " + String.join("§r, ", memberNames));
        p.sendMessage("§8§m--------------------------------------");
        p.playSound(p.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1f);
    }

    private void handleTeamReward(Player p, String[] args) {
        if (args.length < 2) {
            p.sendMessage("§c使用法: /spt teamreward <グループ名>");
            return;
        }
        String group = args[1];
        plugin.getTeamGUIManager().openTeamRewardGUI(p, group);
    }

    private String createProgressBar(double percent) {
        int filled = (int) (percent / 10);
        StringBuilder sb = new StringBuilder("§7[");
        for (int i = 0; i < 10; i++) {
            sb.append(i < filled ? "§a■" : "§8■");
        }
        return sb.append("§7]").toString();
    }

    private void sendHelp(Player p) {
        p.sendMessage("§8§m----------§r §6§lSPT HELP §8§m----------");
        p.sendMessage("§e/spt myp <ID> §7- 個人のポイント確認");
        p.sendMessage("§e/spt reward <ID> §7- 報酬ショップ");
        p.sendMessage("§e/spt ranking <ID> §7- ランキング表示");
        p.sendMessage("");
        p.sendMessage("§6§l[TEAM]");
        p.sendMessage("§e/spt teaminfo <Group> §7- 所属チームの詳細確認");
        p.sendMessage("§e/spt teamreward <Group> §7- チーム報酬ショップ");
        p.sendMessage("§e/spt toggleteamstats §7- 貢献度の公開/非公開切替");
        p.sendMessage("§8§m----------------------------");
    }

    private void showPersonalRanking(Player player, String pointId) {
        player.sendMessage("§cランキング機能は現在メンテナンス中です。");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        if (args.length == 1) {
            StringUtil.copyPartialMatches(args[0], Arrays.asList("myp", "reward", "ranking", "teaminfo", "teamreward", "toggleteamstats", "help"), completions);
        } else if (args.length == 2) {
            String sub = args[0].toLowerCase();
            if (Arrays.asList("myp", "reward", "ranking").contains(sub)) {
                StringUtil.copyPartialMatches(args[1], plugin.getPointManager().getPointNames(), completions);
            } else if (Arrays.asList("teaminfo", "teamreward").contains(sub)) {
                File teamDir = new File(plugin.getDataFolder(), "teams/team");
                if (teamDir.exists()) {
                    String[] list = teamDir.list();
                    if (list != null) StringUtil.copyPartialMatches(args[1], Arrays.asList(list), completions);
                }
            }
        }
        return completions;
    }
}