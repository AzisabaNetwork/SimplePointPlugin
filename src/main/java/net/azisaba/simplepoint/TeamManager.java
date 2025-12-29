package net.azisaba.simplepoint;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import java.io.File;
import java.io.IOException;
import java.util.*;

public class TeamManager {
    private final SimplePointPlugin plugin;
    private final File teamFile;
    private FileConfiguration teamConfig;
    private final Map<String, Set<UUID>> teams = new HashMap<>();

    public TeamManager(SimplePointPlugin plugin) {
        this.plugin = plugin;
        this.teamFile = new File(plugin.getDataFolder(), "teams.yml");
        loadTeams();
    }

    public void loadTeams() {
        if (!teamFile.exists()) {
            try { teamFile.createNewFile(); } catch (IOException e) { e.printStackTrace(); }
        }
        teamConfig = YamlConfiguration.loadConfiguration(teamFile);
        teams.clear();
        for (String teamName : teamConfig.getKeys(false)) {
            List<String> uuids = teamConfig.getStringList(teamName + ".members");
            Set<UUID> memberSet = new HashSet<>();
            for (String s : uuids) memberSet.add(UUID.fromString(s));
            teams.put(teamName, memberSet);
        }
    }

    public void createTeam(String name) {
        if (!teams.containsKey(name)) {
            teams.put(name, new HashSet<>());
            saveTeams();
        }
    }

    public Set<String> getTeamNames() { return teams.keySet(); }

    public int getMemberCount(String teamName) {
        return teams.containsKey(teamName) ? teams.get(teamName).size() : 0;
    }

    public String joinRandomTeam(UUID uuid) {
        String bestTeam = teams.keySet().stream()
                .min(Comparator.comparingInt(this::getMemberCount))
                .orElse(null);
        if (bestTeam != null) {
            teams.get(bestTeam).add(uuid);
            saveTeams();
            return bestTeam;
        }
        return "No Teams Available";
    }

    public int getTeamPoints(String teamName) {
        if (!teams.containsKey(teamName)) return 0;
        int total = 0;
        for (UUID uuid : teams.get(teamName)) {
            // ポイント取得ロジック (PointManager経由)
            total += plugin.getPointManager().getPoint(teamName, uuid);
        }
        return total;
    }

    public Map<String, Integer> getContributionRanking(String teamName) {
        Map<String, Integer> ranking = new HashMap<>();
        if (!teams.containsKey(teamName)) return ranking;
        for (UUID uuid : teams.get(teamName)) {
            ranking.put(org.bukkit.Bukkit.getOfflinePlayer(uuid).getName(),
                    plugin.getPointManager().getPoint(teamName, uuid));
        }
        return ranking;
    }

    private void saveTeams() {
        for (Map.Entry<String, Set<UUID>> entry : teams.entrySet()) {
            List<String> list = new ArrayList<>();
            for (UUID u : entry.getValue()) list.add(u.toString());
            teamConfig.set(entry.getKey() + ".members", list);
        }
        try { teamConfig.save(teamFile); } catch (IOException e) { e.printStackTrace(); }
    }
}