package net.azisaba.simplepoint;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.util.StringUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class SPPTabCompleter implements TabCompleter {
    private final SimplePointPlugin plugin;

    public SPPTabCompleter(SimplePointPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        String sub = args[0].toLowerCase();

        // 1. サブコマンドの補完: /spp <ここ>
        if (args.length == 1) {
            List<String> subCommands = Arrays.asList("togglereward","create", "add", "remove", "set", "score", "rewardgui", "setreq", "ranking", "toggleranking", "reload", "help");
            StringUtil.copyPartialMatches(args[0], subCommands, completions);
        }

        // 2. 第2引数の補完: /spp <sub> <ここ>
        else if (args.length == 2) {
            // IDが必要なコマンド群
            List<String> needsIdAsSecond = Arrays.asList("add", "remove", "set", "score", "rewardgui", "setreq", "ranking", "togglereward","toggleranking");

            if (needsIdAsSecond.contains(sub)) {
                List<String> pointIds = plugin.getPointManager().getPointNames();
                StringUtil.copyPartialMatches(args[1], pointIds, completions);
            }
        }

        // 3. 第3引数の補完: /spp <sub> <id> <ここ>
        else if (args.length == 3) {
            // プレイヤー名が来るべきコマンド: add, remove, set, score
            if (Arrays.asList("add", "remove", "set", "score").contains(sub)) {
                return null; // プレイヤー一覧を表示
            }

            // setreq の場合はスロット番号のヒント
            if (sub.equals("setreq")) {
                completions.add("[slot_number]");
            }
        }

        // 4. 第4引数の補完: /spp <sub> <id> <player> <ここ>
        else if (args.length == 4) {
            if (Arrays.asList("add", "remove", "set","setreq").contains(sub)) {
                completions.add("[amount]"); // 数値入力のヒント
            }
        }

        Collections.sort(completions);
        return completions;
    }
}