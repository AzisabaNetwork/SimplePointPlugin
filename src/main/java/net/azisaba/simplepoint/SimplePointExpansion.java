package net.azisaba.simplepoint;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.UUID;

public class SimplePointExpansion extends PlaceholderExpansion {
    private final SimplePointPlugin plugin;

    public SimplePointExpansion(SimplePointPlugin plugin) {
        this.plugin = plugin;
    }

    @Override public @NotNull String getIdentifier() { return "simplepoint"; }
    @Override public @NotNull String getAuthor() { return "pino223"; }
    @Override public @NotNull String getVersion() { return "1.0.0"; }
    @Override public boolean persist() { return true; }

    @Override
    public String onRequest(OfflinePlayer player, @NotNull String params) {
        // 全体的なNullチェック
        if (params == null) return "";

        try {
            // --- プレイヤーに依存しないプレースホルダー ---

            // %simplepoint_group_name_<group>%
            if (params.startsWith("group_name_")) {
                String group = params.replace("group_name_", "");
                File rewardFile = new File(plugin.getDataFolder(), "teams/reward/" + group + ".yml");
                if (!rewardFile.exists()) return group;

                FileConfiguration rCfg = YamlConfiguration.loadConfiguration(rewardFile);
                String displayName = rCfg.getString("display_name", group);
                return displayName.replace("&", "§");
            }

            // --- プレイヤーに依存するプレースホルダー ---
            if (player == null) return "";
            UUID uuid = player.getUniqueId();

            // %simplepoint_point_<ポイントID>%
            if (params.startsWith("point_")) {
                String pointId = params.replace("point_", "");
                return String.valueOf(plugin.getPointManager().getPoint(pointId, uuid));
            }

            // %simplepoint_total_point_<ポイントID>%
            if (params.startsWith("total_point_")) {
                String pointId = params.replace("total_point_", "");
                return String.valueOf(plugin.getPointManager().getTotalPoint(pointId, uuid));
            }

            // %simplepoint_total_score_<グループID>%
            if (params.startsWith("total_score_")) {
                String groupId = params.replace("total_score_", "");
                String teamId = plugin.getTeamManager().getPlayerTeamInGroup(uuid, groupId);
                if (teamId == null) return "0";
                return String.valueOf(plugin.getTeamManager().getTeamTotalScore(groupId, teamId));
            }

            // %simplepoint_vs_bar_<グループID>%
            if (params.startsWith("vs_bar_")) {
                String groupId = params.replace("vs_bar_", "");
                // 以前作成した自チーム・敵チームの色を考慮したバーを生成
                return plugin.getTeamManager().getVSBarPlaceholder(uuid, groupId);
            }

            // %simplepoint_team_name_<グループID>%
            if (params.startsWith("team_name_")) {
                String groupId = params.replace("team_name_", "");
                String teamId = plugin.getTeamManager().getPlayerTeamInGroup(uuid, groupId);
                if (teamId == null) return "§7未所属";
                return plugin.getTeamManager().getTeamDisplayName(groupId, teamId);
            }

        } catch (Exception e) {
            // TABなどの頻繁なリクエストによるエラーを握りつぶして警告を止める
            return "";
        }

        return null;
    }
}