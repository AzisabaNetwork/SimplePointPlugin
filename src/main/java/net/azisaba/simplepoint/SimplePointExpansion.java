package net.azisaba.simplepoint;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;

public class SimplePointExpansion extends PlaceholderExpansion {
    private final SimplePointPlugin plugin;

    public SimplePointExpansion(SimplePointPlugin plugin) {
        this.plugin = plugin;
    }

    @Override public @NotNull String getIdentifier() { return "simplepoint"; }
    @Override public @NotNull String getAuthor() { return "pino223"; }
    @Override public @NotNull String getVersion() { return "1.0.0"; }
    @Override public boolean persist() { return true; } // リロードしても登録解除しない

    @Override
    public String onRequest(OfflinePlayer player, @NotNull String params) {
        if (player == null) return "";

        // %simplepoint_point_<ポイントID>% (現在の所持ポイント：使うと減る)
        if (params.startsWith("point_")) {
            String pointId = params.replace("point_", "");
            // PointManagerから現在のポイントを取得
            return String.valueOf(plugin.getPointManager().getPoint(pointId, player.getUniqueId()));
        }

        // %simplepoint_total_point_<ポイントID>% (累計ポイント：実績)
        if (params.startsWith("total_point_")) {
            String pointId = params.replace("total_point_", "");
            return String.valueOf(plugin.getPointManager().getTotalPoint(pointId, player.getUniqueId()));
        }

        // %simplepoint_vs_bar_<グループID>%
        if (params.startsWith("vs_bar_")) {
            String groupId = params.replace("vs_bar_", "");
            // TeamManagerに移動したメソッドを呼び出す
            return plugin.getTeamManager().getVSBarPlaceholder(player.getUniqueId(), groupId);
        }

        // %simplepoint_team_name_<グループID>%
        if (params.startsWith("team_name_")) {
            String groupId = params.replace("team_name_", "");
            String teamId = plugin.getTeamManager().getPlayerTeamInGroup(player.getUniqueId(), groupId);
            if (teamId == null) return "§7未所属";
            return plugin.getTeamManager().getTeamDisplayName(groupId, teamId);
        }



        return null;
    }
}