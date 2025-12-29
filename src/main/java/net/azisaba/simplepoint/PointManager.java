package net.azisaba.simplepoint;

import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class PointManager {
    private final SimplePointPlugin plugin;
    private final File pointFolder;
    private final Map<String, FileConfiguration> configCache = new HashMap<>();

    // IDと表示名を紐付けるためのファイル
    private final File namesFile;
    private FileConfiguration namesConfig;

    public PointManager(SimplePointPlugin plugin) {
        this.plugin = plugin;
        this.pointFolder = new File(plugin.getDataFolder(), "points");
        if (!pointFolder.exists()) pointFolder.mkdirs();

        // point_names.yml の初期化
        this.namesFile = new File(plugin.getDataFolder(), "point_names.yml");
        this.namesConfig = YamlConfiguration.loadConfiguration(namesFile);
    }

    public void reload() {
        configCache.clear();
        namesConfig = YamlConfiguration.loadConfiguration(namesFile);
    }

    /**
     * ポイントの作成 (IDと表示名を分ける)
     */
    public boolean createPointType(String id, String displayName) {
        File file = new File(pointFolder, id + ".yml");
        if (file.exists()) return false;
        try {
            file.createNewFile();
            FileConfiguration cfg = YamlConfiguration.loadConfiguration(file);
            cfg.set("_settings.ranking_enabled", true);
            cfg.save(file);

            // 表示名を保存
            namesConfig.set(id, displayName);
            saveNamesFile();
            return true;
        } catch (IOException e) { return false; }
    }

    /**
     * 表示名を取得 (&を§に変換)
     */
    public String getDisplayName(String id) {
        String name = namesConfig.getString(id, id);
        return ChatColor.translateAlternateColorCodes('&', name);
    }

    private void saveNamesFile() {
        try { namesConfig.save(namesFile); } catch (IOException e) { e.printStackTrace(); }
    }

    public FileConfiguration getPointConfig(String id) {
        if (configCache.containsKey(id)) return configCache.get(id);
        File file = new File(pointFolder, id + ".yml");
        if (!file.exists()) return null;
        FileConfiguration cfg = YamlConfiguration.loadConfiguration(file);
        configCache.put(id, cfg);
        return cfg;
    }

    /**
     * 設定の保存
     */
    public void savePointConfig(String id) {
        FileConfiguration cfg = getPointConfig(id);
        if (cfg == null) return;
        try {
            cfg.save(new File(pointFolder, id + ".yml"));
        } catch (IOException e) { e.printStackTrace(); }
    }

    // 互換用メソッド
    public void saveConfig(String id, FileConfiguration cfg) {
        savePointConfig(id);
    }

    public int getPoint(String id, UUID uuid) {
        FileConfiguration cfg = getPointConfig(id);
        return cfg != null ? cfg.getInt(uuid.toString() + ".current", 0) : 0;
    }

    public int getTotalPoint(String id, UUID uuid) {
        FileConfiguration cfg = getPointConfig(id);
        return cfg != null ? cfg.getInt(uuid.toString() + ".total", 0) : 0;
    }

    public void addPoint(String id, UUID uuid, int amount) {
        FileConfiguration cfg = getPointConfig(id);
        if (cfg == null) return;

        int current = cfg.getInt(uuid.toString() + ".current", 0);
        int total = cfg.getInt(uuid.toString() + ".total", 0);

        cfg.set(uuid.toString() + ".current", current + amount);
        if (amount > 0) {
            cfg.set(uuid.toString() + ".total", total + amount);
        }
        savePointConfig(id);
    }

    public void setPoint(String id, UUID uuid, int amount) {
        FileConfiguration cfg = getPointConfig(id);
        if (cfg == null) return;
        cfg.set(uuid.toString() + ".current", amount);
        savePointConfig(id);
    }

    /**
     * 存在するすべてのポイントIDを取得
     */
    public List<String> getPointNames() {
        return new ArrayList<>(namesConfig.getKeys(false));
    }
}