package net.azisaba.simplepoint;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import java.io.File;
import java.io.IOException;
import java.util.*;

public class TeamManager {
    private final SimplePointPlugin plugin;
    private final File file;
    private FileConfiguration config;

    public TeamManager(SimplePointPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "teams.yml");
        if (!file.exists()) {
            try { file.createNewFile(); } catch (IOException e) { e.printStackTrace(); }
        }
        this.config = YamlConfiguration.loadConfiguration(file);
    }

    // 追加: チーム名のリストを取得
    public Set<String> getTeamNames() {
        if (!config.contains("teams")) return new HashSet<>();
        return config.getConfigurationSection("teams").getKeys(false);
    }

    public void createTeam(String teamName) {
        config.set("teams." + teamName + ".points", 0);
        config.set("teams." + teamName + ".members", new ArrayList<String>());
        save();
    }

    public String joinRandomTeam(UUID uuid) {
        if (getPlayerTeam(uuid) != null) return "already_joined";
        if (!config.contains("teams")) return "no_teams";
        String bestTeam = null;
        int minMembers = Integer.MAX_VALUE;
        for (String teamName : getTeamNames()) {
            int count = getMemberCount(teamName);
            if (count < minMembers) {
                minMembers = count;
                bestTeam = teamName;
            }
        }
        if (bestTeam != null) {
            List<String> members = config.getStringList("teams." + bestTeam + ".members");
            members.add(uuid.toString());
            config.set("teams." + bestTeam + ".members", members);
            save();
        }
        return bestTeam;
    }

    public String getPlayerTeam(UUID uuid) {
        for (String teamName : getTeamNames()) {
            if (config.getStringList("teams." + teamName + ".members").contains(uuid.toString())) return teamName;
        }
        return null;
    }

    public int getMemberCount(String teamName) {
        return config.getStringList("teams." + teamName + ".members").size();
    }

    public int getTeamPoints(String teamName) {
        return config.getInt("teams." + teamName + ".points", 0);
    }

    public void addTeamPoints(String teamName, int amount) {
        config.set("teams." + teamName + ".points", getTeamPoints(teamName) + amount);
        save();
    }

    public void addContribution(String teamName, UUID uuid, int amount) {
        int current = config.getInt("teams." + teamName + ".contrib." + uuid.toString(), 0);
        config.set("teams." + teamName + ".contrib." + uuid.toString(), current + amount);
        addTeamPoints(teamName, amount);
        save();
    }

    public Map<String, Integer> getContributionRanking(String teamName) {
        Map<String, Integer> ranking = new HashMap<>();
        String path = "teams." + teamName + ".contrib";
        if (config.contains(path)) {
            for (String uuidStr : config.getConfigurationSection(path).getKeys(false)) {
                String name = Bukkit.getOfflinePlayer(UUID.fromString(uuidStr)).getName();
                ranking.put(name != null ? name : "Unknown", config.getInt(path + "." + uuidStr));
            }
        }
        return ranking;
    }

    public void save() {
        try { config.save(file); } catch (IOException e) { e.printStackTrace(); }
    }
}