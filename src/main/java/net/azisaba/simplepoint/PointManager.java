package net.azisaba.simplepoint;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

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

    // ポイントファイルの新規作成
    public boolean createPointType(String name) {
        File file = new File(dataFolder, name + ".yml");
        if (file.exists()) return false;
        try {
            file.createNewFile();
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    // ポイントの加算
    public void addPoint(String pointName, UUID uuid, int amount) {
        File file = new File(dataFolder, pointName + ".yml");
        if (!file.exists()) return;

        FileConfiguration config = YamlConfiguration.loadConfiguration(file);
        int current = config.getInt(uuid.toString(), 0);
        config.set(uuid.toString(), current + amount);
        try {
            config.save(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // 存在するポイント名の一覧を取得（補完用）
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