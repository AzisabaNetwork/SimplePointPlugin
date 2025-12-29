package net.azisaba.simplepoint;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;

import java.util.ArrayList;
import java.util.List;

public class MYPCommand implements CommandExecutor, TabCompleter {
    private final SimplePointPlugin plugin;

    public MYPCommand(SimplePointPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) return true;

        // 引数がない場合はヘルプを表示するか、デフォルトの動作をさせる
        if (args.length < 1) {
            sender.sendMessage("§c使用法: /myp <ポイント名>");
            return true;
        }

        // 既存の /spt myp <id> のロジックを呼び出すために、argsをそのまま渡す
        // ここでは SPTCommand を直接実行するのではなく、同じロジックを実行します
        Player p = (Player) sender;
        String pointId = args[0];

        if (plugin.getPointManager().getPointConfig(pointId) == null) {
            p.sendMessage("§cそのポイント名は存在しません。");
            return true;
        }

        String displayName = plugin.getPointManager().getDisplayName(pointId);
        int current = plugin.getPointManager().getPoint(pointId, p.getUniqueId());
        int total = plugin.getPointManager().getTotalPoint(pointId, p.getUniqueId());

        p.sendMessage("§8§m----------§r " + displayName + " §b§lSTATUS §8§m----------");
        p.sendMessage("§7現在の所持ポイント: §e" + current + " pt");
        p.sendMessage("§7これまでの累計獲得: §a" + total + " pt");
        p.sendMessage("§8§m----------------------------");

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> completions = new ArrayList<>();
            StringUtil.copyPartialMatches(args[0], plugin.getPointManager().getPointNames(), completions);
            return completions;
        }
        return new ArrayList<>();
    }
}