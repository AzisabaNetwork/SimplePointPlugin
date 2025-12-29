package net.azisaba;

import net.azisaba.simplepoint.SimplePointPlugin;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.util.StringUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SPPCommand implements CommandExecutor, TabCompleter {

    private final SimplePointPlugin plugin;

    public SPPCommand(SimplePointPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) return false;

        String subCommand = args[0].toLowerCase();

        // /spp create <名前>
        if (subCommand.equals("create")) {
            if (args.length < 2) {
                sender.sendMessage("§c使用法: /spp create <ポイント名>");
                return true;
            }
            if (plugin.getPointManager().createPointType(args[1])) {
                sender.sendMessage("§aポイント「" + args[1] + "」を作成しました。");
            } else {
                sender.sendMessage("§cその名前は既に存在するか、作成に失敗しました。");
            }
            return true;
        }

        // /spp add <ポイント名> <プレイヤー> <量>
        if (subCommand.equals("add")) {
            if (args.length < 4) {
                sender.sendMessage("§c使用法: /spp add <ポイント名> <プレイヤー> <数値>");
                return true;
            }
            String pointName = args[1];
            OfflinePlayer target = Bukkit.getOfflinePlayer(args[2]);
            int amount;
            try {
                amount = Integer.parseInt(args[3]);
            } catch (NumberFormatException e) {
                sender.sendMessage("§c数値は整数で入力してください。");
                return true;
            }

            plugin.getPointManager().addPoint(pointName, target.getUniqueId(), amount);
            sender.sendMessage("§a" + target.getName() + "に" + pointName + "を" + amount + "付与しました。");
            return true;
        }

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        if (args.length == 1) {
            StringUtil.copyPartialMatches(args[0], Arrays.asList("create", "add"), completions);
        } else if (args.length == 2) {
            if (args[0].equalsIgnoreCase("add")) {
                StringUtil.copyPartialMatches(args[1], plugin.getPointManager().getPointNames(), completions);
            }
        }
        return completions;
    }
}