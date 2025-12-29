package net.azisaba.simplepoint;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import java.io.File;
import java.io.IOException;
import java.util.*;

public class PointManager {
    private final SimplePointPlugin plugin;
    private final File dataFolder;

    public PointManager(SimplePointPlugin plugin) {
        this.plugin = plugin;
        this.dataFolder = new File(plugin.getDataFolder(), "playerpointdata");
        if (!dataFolder.exists()) dataFolder.mkdirs();
    }

    public boolean createPointType(String name) {
        File file = new File(dataFolder, name + ".yml");
        if (file.exists()) return false;
        try {
            file.createNewFile();
            FileConfiguration config = YamlConfiguration.loadConfiguration(file);
            config.set("_settings.enabled", true);
            config.set("_settings.ranking_enabled", true);
            config.save(file);
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    public FileConfiguration getPointConfig(String pointName) {
        File file = new File(dataFolder, pointName + ".yml");
        if (!file.exists()) return null;
        return YamlConfiguration.loadConfiguration(file);
    }

    public void saveConfig(String pointName, FileConfiguration config) {
        try { config.save(new File(dataFolder, pointName + ".yml")); } catch (IOException e) { e.printStackTrace(); }
    }

    // 現在の所持ポイントを取得
    public int getPoint(String pointName, UUID uuid) {
        FileConfiguration config = getPointConfig(pointName);
        return (config != null) ? config.getInt(uuid.toString() + ".current", 0) : 0;
    }

    // 累計ポイントを取得
    public int getTotalPoint(String pointName, UUID uuid) {
        FileConfiguration config = getPointConfig(pointName);
        return (config != null) ? config.getInt(uuid.toString() + ".total", 0) : 0;
    }

    public void addPoint(String pointName, UUID uuid, int amount) {
        FileConfiguration config = getPointConfig(pointName);
        if (config == null) return;

        int current = config.getInt(uuid.toString() + ".current", 0);
        int total = config.getInt(uuid.toString() + ".total", 0);

        config.set(uuid.toString() + ".current", Math.max(0, current + amount));

        // ポイントが増える時だけ累計とチーム貢献に加算 📈
        if (amount > 0) {
            config.set(uuid.toString() + ".total", total + amount);
            String teamName = plugin.getTeamManager().getPlayerTeam(uuid);
            if (teamName != null) {
                plugin.getTeamManager().addContribution(teamName, uuid, amount);
            }
        }
        saveConfig(pointName, config);
    }

    public void setPoint(String pointName, UUID uuid, int amount) {
        FileConfiguration config = getPointConfig(pointName);
        if (config == null) return;
        config.set(uuid.toString() + ".current", Math.max(0, amount));
        saveConfig(pointName, config);
    }

    public List<String> getPointNames() {
        List<String> names = new ArrayList<>();
        File[] files = dataFolder.listFiles();
        if (files != null) {
            for (File f : files) {
                if (f.getName().endsWith(".yml")) names.add(f.getName().replace(".yml", ""));
            }
        }
        return names;
    }
}