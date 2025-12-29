package net.azisaba.simplepoint;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;

import java.util.ArrayList;
import java.util.List;

public class RankingShortcutCommand implements CommandExecutor, TabCompleter {
    private final SimplePointPlugin plugin;

    public RankingShortcutCommand(SimplePointPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length < 1) {
            sender.sendMessage("§c使用法: /ranking <ポイント名>");
            return true;
        }

        // SPPCommand の showRanking と同じロジックを実行する。
        // ※ SPTCommand 側の showPersonalRanking を使いたい場合はそちらに合わせます。
        // ここでは全員に放送される形式ではなく、実行者にだけ見せる「個人用ランキング表示」にします。
        if (!(sender instanceof Player)) return true;

        // 既存の SPTCommand 内にある showPersonalRanking と同じロジックをここに書くか、
        // メソッドを共通化して呼び出します。

        return true; // ここには SPTCommand の showPersonalRanking の内容を移植してください
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