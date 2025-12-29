package net.azisaba.simplepoint;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import java.io.File;
import java.io.IOException;
import java.util.*;

public class PointManager {
    private final SimplePointPlugin plugin;
    private final File pointFolder;
    private final Map<String, FileConfiguration> configCache = new HashMap<>();

    public PointManager(SimplePointPlugin plugin) {
        this.plugin = plugin;
        this.pointFolder = new File(plugin.getDataFolder(), "points");
        if (!pointFolder.exists()) pointFolder.mkdirs();
    }

    public void reload() {
        configCache.clear();
    }

    public boolean createPointType(String name) {
        File file = new File(pointFolder, name + ".yml");
        if (file.exists()) return false;
        try {
            file.createNewFile();
            FileConfiguration cfg = YamlConfiguration.loadConfiguration(file);
            cfg.set("_settings.ranking_enabled", true);
            cfg.save(file);
            return true;
        } catch (IOException e) { return false; }
    }

    public FileConfiguration getPointConfig(String name) {
        if (configCache.containsKey(name)) return configCache.get(name);
        File file = new File(pointFolder, name + ".yml");
        if (!file.exists()) return null;
        FileConfiguration cfg = YamlConfiguration.loadConfiguration(file);
        configCache.put(name, cfg);
        return cfg;
    }

    public void saveConfig(String name, FileConfiguration cfg) {
        try {
            cfg.save(new File(pointFolder, name + ".yml"));
            configCache.put(name, cfg);
        } catch (IOException e) { e.printStackTrace(); }
    }

    public int getPoint(String name, UUID uuid) {
        FileConfiguration cfg = getPointConfig(name);
        return cfg != null ? cfg.getInt(uuid.toString() + ".current", 0) : 0;
    }

    public int getTotalPoint(String name, UUID uuid) {
        FileConfiguration cfg = getPointConfig(name);
        return cfg != null ? cfg.getInt(uuid.toString() + ".total", 0) : 0;
    }

    public void addPoint(String name, UUID uuid, int amount) {
        FileConfiguration cfg = getPointConfig(name);
        if (cfg == null) return;

        int current = cfg.getInt(uuid.toString() + ".current", 0);
        int total = cfg.getInt(uuid.toString() + ".total", 0);

        cfg.set(uuid.toString() + ".current", current + amount);
        if (amount > 0) {
            cfg.set(uuid.toString() + ".total", total + amount);
        }
        saveConfig(name, cfg);
    }

    public void setPoint(String name, UUID uuid, int amount) {
        FileConfiguration cfg = getPointConfig(name);
        if (cfg == null) return;
        cfg.set(uuid.toString() + ".current", amount);
        saveConfig(name, cfg);
    }

    public List<String> getPointNames() {
        File[] files = pointFolder.listFiles((dir, name) -> name.endsWith(".yml"));
        List<String> names = new ArrayList<>();
        if (files != null) {
            for (File f : files) names.add(f.getName().replace(".yml", ""));
        }
        return names;
    }

    public void savePointConfig(String pointName) {
        FileConfiguration config = getPointConfig(pointName);
        try {
            config.save(new File(pointFolder, pointName + ".yml"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}