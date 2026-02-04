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
import java.io.IOException;
import java.util.List;
import java.util.UUID;

public class SPPTCommand implements CommandExecutor {
    private final SimplePointPlugin plugin;

    public SPPTCommand(SimplePointPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
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

            case "toggle": // /sppt toggle <group>
                if (args.length < 2) return false;
                boolean newState = plugin.getTeamManager().toggleGroupSync(args[1]);
                sender.sendMessage("§a[SPPT] グループ §l" + args[1] + " §aのポイント同期を §l" + (newState ? "§bON" : "§cOFF") + " §aにしました。");
                break;

            case "multiplier": // /sppt multiplier <group> <倍率>
                if (args.length < 3) return false;
                try {
                    double mult = Double.parseDouble(args[2]);
                    plugin.getTeamManager().setGroupMultiplier(args[1], mult);
                    sender.sendMessage("§a[SPPT] グループ §l" + args[1] + " §aのポイント倍率を §e" + mult + "倍 §aに設定しました。");
                } catch (NumberFormatException e) {
                    sender.sendMessage("§c倍率は数値で指定してください。");
                }
                break;

            case "member": // /sppt member <group> <id>
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

    private void showAdminInfo(CommandSender sender, String group, String teamId) {
        File teamFile = plugin.getTeamManager().getTeamFile(group, teamId);
        if (!teamFile.exists()) {
            sender.sendMessage("§cチームが見 -つかりません。");
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
        sender.sendMessage("§8§m------------------------------------");
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage("§8§m----------§r §d§lSPPT ADMIN HELP §8§m----------");
        sender.sendMessage("§e/sppt create <group> §7- グループ作成");
        sender.sendMessage("§e/sppt teamcreate <group> <id> <name> §7- チーム作成");
        sender.sendMessage("§e/sppt join <group> <id> <player> §7- メンバー追加");
        sender.sendMessage("§e/sppt setpoint <group> <pointId> §7- ポイント連携");
        sender.sendMessage("§b/sppt toggle <group> §7- ポイント同期のON/OFF");
        sender.sendMessage("§e/sppt multiplier <group> <倍率> §7- 獲得ポイントの倍率設定");
        sender.sendMessage("§e/sppt info <group> <id> §7- チーム詳細表示");
        sender.sendMessage("§e/sppt rewardgui <group> §7- 報酬設定GUI");
        sender.sendMessage("§e/sppt setpoint_value <group> <id> <value> §7- ポイント操作");
        sender.sendMessage("§8§m------------------------------------");
    }
}