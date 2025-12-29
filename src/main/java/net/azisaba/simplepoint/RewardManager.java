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
        this.rewardFolder = new File(plugin.getDataFolder(), "rewards");
        if (!rewardFolder.exists()) {
            rewardFolder.mkdirs();
        }
    }

    public FileConfiguration getRewardConfig(String pointId) {
        if (configs.containsKey(pointId)) {
            return configs.get(pointId);
        }

        File file = new File(rewardFolder, pointId + ".yml");
        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        FileConfiguration config = YamlConfiguration.loadConfiguration(file);
        configs.put(pointId, config);
        return config;
    }

    /**
     * 報酬アイテムを保存します
     * @param pointId ポイントの内部ID
     * @param isPersonal 個人制限モードなら true, 共有在庫モードなら false
     */
    public void saveReward(String pointId, int slot, ItemStack item, int price, int stock, boolean isPersonal) {
        FileConfiguration config = getRewardConfig(pointId);
        String path = String.valueOf(slot);

        config.set(path + ".item", item);
        config.set(path + ".price", price);
        config.set(path + ".stock", stock);
        config.set(path + ".is_personal", isPersonal);

        saveFile(pointId, config);
    }

    public void updateStock(String pointId, int slot, int newStock) {
        FileConfiguration config = getRewardConfig(pointId);
        config.set(slot + ".stock", newStock);
        saveFile(pointId, config);
    }

    public void deleteReward(String pointId, int slot) {
        FileConfiguration config = getRewardConfig(pointId);
        config.set(String.valueOf(slot), null);
        saveFile(pointId, config);
    }

    private void saveFile(String pointId, FileConfiguration config) {
        try {
            config.save(new File(rewardFolder, pointId + ".yml"));
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save reward config for " + pointId);
            e.printStackTrace();
        }
    }

    public void reload() {
        configs.clear();
    }
}