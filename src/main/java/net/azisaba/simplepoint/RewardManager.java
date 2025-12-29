package net.azisaba.simplepoint;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class RewardManager {
    private final SimplePointPlugin plugin;
    private final File rewardFolder;
    private final Map<String, FileConfiguration> configs = new HashMap<>();

    public RewardManager(SimplePointPlugin plugin) {
        this.plugin = plugin;
        // rewards フォルダを作成
        this.rewardFolder = new File(plugin.getDataFolder(), "rewards");
        if (!rewardFolder.exists()) {
            rewardFolder.mkdirs();
        }
    }

    /**
     * ポイント名に対応する報酬設定ファイルを取得またはロードします
     */
    public FileConfiguration getRewardConfig(String pointName) {
        if (configs.containsKey(pointName)) {
            return configs.get(pointName);
        }

        File file = new File(rewardFolder, pointName + ".yml");
        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        FileConfiguration config = YamlConfiguration.loadConfiguration(file);
        configs.put(pointName, config);
        return config;
    }

    /**
     * 報酬アイテムを保存します
     */
    public void saveReward(String pointName, int slot, ItemStack item, int price, int stock) {
        FileConfiguration config = getRewardConfig(pointName);
        String path = String.valueOf(slot);

        config.set(path + ".item", item);
        config.set(path + ".price", price);
        config.set(path + ".stock", stock);

        saveFile(pointName, config);
    }

    /**
     * 在庫数のみを更新します
     */
    public void updateStock(String pointName, int slot, int newStock) {
        FileConfiguration config = getRewardConfig(pointName);
        config.set(slot + ".stock", newStock);
        saveFile(pointName, config);
    }

    /**
     * 指定スロットの報酬を削除します
     */
    public void deleteReward(String pointName, int slot) {
        FileConfiguration config = getRewardConfig(pointName);
        config.set(String.valueOf(slot), null);
        saveFile(pointName, config);
    }

    /**
     * ファイルに物理保存します
     */
    private void saveFile(String pointName, FileConfiguration config) {
        try {
            config.save(new File(rewardFolder, pointName + ".yml"));
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save reward config for " + pointName);
            e.printStackTrace();
        }
    }

    /**
     * メモリ上のキャッシュをクリア（リロード用）
     */
    public void reload() {
        configs.clear();
    }
}