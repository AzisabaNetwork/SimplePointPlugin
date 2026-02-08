package net.azisaba.simplepoint.commands;

import net.azisaba.simplepoint.SimplePointPlugin;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

public class SPTCommand implements CommandExecutor, TabCompleter {
    private final SimplePointPlugin plugin;

    public SPTCommand(SimplePointPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // 1. プレイヤーチェック (AudioPlayer等との衝突回避のためフルパス指定)
        if (!(sender instanceof org.bukkit.entity.Player)) {
            sender.sendMessage("§cプレイヤーのみ実行可能です。");
            return true;
        }
        org.bukkit.entity.Player player = (org.bukkit.entity.Player) sender;

        // 2. /teaminfo (短縮) または /spt teaminfo (サブコマンド) の判定
        boolean isShorthand = label.equalsIgnoreCase("teaminfo");
        boolean isSubCommand = label.equalsIgnoreCase("spt") && args.length > 0 && args[0].equalsIgnoreCase("teaminfo");

        if (isShorthand || isSubCommand) {
            String groupName = null;

            if (isShorthand) {
                // 短縮形: /teaminfo <group> なので args[0] がグループ名
                if (args.length > 0) groupName = args[0];
            } else {
                // サブコマンド: /spt teaminfo <group> なので args[1] がグループ名
                if (args.length > 1) groupName = args[1];
            }

            // 引数が2つの新しい sendModernTeamInfo を呼び出す
            plugin.getSPPTCommand().sendModernTeamInfo(player, groupName);
            return true;
        }

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
                // 引数(args[1])があれば取得し、なければnullとして第2引数に渡す
                String targetGroupId = (args.length > 1) ? args[1] : null;
                plugin.getSPPTCommand().sendModernTeamInfo(p, targetGroupId);
                break;
            case "teamreward":
                handleTeamReward(p, args);
                break;
            case "myinfo": {
                if (!(sender instanceof Player)) return true;
                Player senderPlayer = (Player) sender;

                String group = plugin.getTeamManager().getPlayerGroup(senderPlayer.getUniqueId());
                if (group == null) {
                    senderPlayer.sendMessage("§cあなたは現在どのチームにも参加していません。");
                    return true;
                }

                String teamId = plugin.getTeamManager().getPlayerTeamInGroup(senderPlayer.getUniqueId(), group);
                String teamName = plugin.getTeamManager().getTeamDisplayName(group, teamId);

                senderPlayer.sendMessage("§8§m----------§r §b§lMY STATUS §8§m----------");
                senderPlayer.sendMessage(" §7グループ: §f" + group);
                senderPlayer.sendMessage(" §7参加チーム: §a" + teamName + " §7(" + teamId + ")");
                senderPlayer.sendMessage("§8§m----------------------------");
                return true;
            }
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

    private void handleTeamReward(Player p, String[] args) {
        if (args.length < 2) {
            p.sendMessage("§c使用法: /spt teamreward <グループ名>");
            return;
        }
        String group = args[1];
        plugin.getTeamGUIManager().openTeamRewardGUI(p, group);
    }

    /**
     * 個人ポイントランキングの表示 (累計ポイント順)
     */
    private void showPersonalRanking(Player player, String pointId) {
        Map<UUID, Integer> allScores = plugin.getPointManager().getAllTotalPoints(pointId);
        if (allScores.isEmpty()) {
            player.sendMessage("§cデータが存在しません。");
            return;
        }

        String displayName = plugin.getPointManager().getDisplayName(pointId);
        player.sendMessage("§8§m-------§r " + displayName + " §6§lRANKING §8§m-------");

        List<Map.Entry<UUID, Integer>> sorted = allScores.entrySet().stream()
                .sorted((a, b) -> b.getValue().compareTo(a.getValue()))
                .limit(10)
                .collect(Collectors.toList());

        int rank = 1;
        for (Map.Entry<UUID, Integer> entry : sorted) {
            OfflinePlayer op = Bukkit.getOfflinePlayer(entry.getKey());
            String name = (op.getName() != null) ? op.getName() : "Unknown";
            String color = (rank <= 3) ? "§e§l" : "§f";
            player.sendMessage(color + rank + ". §r" + name + " §7- §b" + entry.getValue() + "pt");
            rank++;
        }
        player.sendMessage("§8§m------------------------------");
    }

    private void sendHelp(Player p) {
        p.sendMessage("§8§m----------§r §6§lSPT HELP §8§m----------");
        p.sendMessage("§e/spt myp <ID> §7- 個人のポイント確認");
        p.sendMessage("§e/spt reward <ID> §7- 報酬ショップ");
        p.sendMessage("§e/spt ranking <ID> §7- ランキング表示");
        p.sendMessage("");
        p.sendMessage("§6§l[TEAM]");
        p.sendMessage("§e/teaminfo <Group> §7- 現在の対戦状況・チーム詳細");
        p.sendMessage("§e/spt teamreward <Group> §7- チーム報酬");
        //p.sendMessage("§e/spt toggleteamstats §7- 貢献度の公開/非公開切替");
        p.sendMessage("§8§m----------------------------");
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
                if (teamDir.exists() && teamDir.list() != null) {
                    StringUtil.copyPartialMatches(args[1], Arrays.asList(teamDir.list()), completions);
                }
            }
        }
        return completions;
    }
}