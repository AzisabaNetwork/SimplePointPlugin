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
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }
    }

    public boolean createPointType(String name) {
        File file = new File(dataFolder, name + ".yml");
        if (file.exists()) return false;
        try {
            file.createNewFile();
            // 初期設定を書き込む
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
        try {
            config.save(new File(dataFolder, pointName + ".yml"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public int getPoint(String pointName, UUID uuid) {
        FileConfiguration config = getPointConfig(pointName);
        return (config != null) ? config.getInt(uuid.toString(), 0) : 0;
    }

    public void setPoint(String pointName, UUID uuid, int amount) {
        FileConfiguration config = getPointConfig(pointName);
        if (config == null) return;
        config.set(uuid.toString(), Math.max(0, amount));
        saveConfig(pointName, config);
    }

    public void addPoint(String pointName, UUID uuid, int amount) {
        setPoint(pointName, uuid, getPoint(pointName, uuid) + amount);
    }

    public List<String> getPointNames() {
        List<String> names = new ArrayList<>();
        File[] files = dataFolder.listFiles();
        if (files != null) {
            for (File f : files) {
                if (f.getName().endsWith(".yml")) {
                    names.add(f.getName().replace(".yml", ""));
                }
            }
        }
        return names;
    }
}