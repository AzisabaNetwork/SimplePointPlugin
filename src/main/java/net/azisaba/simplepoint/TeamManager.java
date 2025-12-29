package net.azisaba.simplepoint;

import org.bukkit.configuration.file.YamlConfiguration;
import java.io.File;
import java.io.IOException;
import java.util.*;

public class TeamManager {
    private final SimplePointPlugin plugin;
    private final File folder;

    public TeamManager(SimplePointPlugin plugin) {
        this.plugin = plugin;
        this.folder = new File(plugin.getDataFolder(), "teams");
        if (!folder.exists()) folder.mkdirs();
    }

    public void createTeam(String name, UUID owner) {
        File file = new File(folder, name + ".yml");
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        config.set("owner", owner.toString());
        config.set("points", 0);
        config.set("level", 1);
        config.set("members", Collections.singletonList(owner.toString()));
        save(name, config);
    }

    public void addTeamPoint(String teamName, int amount) {
        File file = new File(folder, teamName + ".yml");
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        config.set("points", config.getInt("points") + amount);
        save(teamName, config);
    }

    private void save(String name, YamlConfiguration config) {
        try { config.save(new File(folder, name + ".yml")); } catch (IOException e) { e.printStackTrace(); }
    }
}