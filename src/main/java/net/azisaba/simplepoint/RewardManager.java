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
     * 報酬アイテムを保存します (isPersonal 引数を追加)
     */
    public void saveReward(String pointName, int slot, ItemStack item, int price, int stock, boolean isPersonal) {
        FileConfiguration config = getRewardConfig(pointName);
        String path = String.valueOf(slot);

        config.set(path + ".item", item);
        config.set(path + ".price", price);
        config.set(path + ".stock", stock);
        config.set(path + ".is_personal", isPersonal); // ✨ ここで保存

        saveFile(pointName, config);
    }

    public void updateStock(String pointName, int slot, int newStock) {
        FileConfiguration config = getRewardConfig(pointName);
        config.set(slot + ".stock", newStock);
        saveFile(pointName, config);
    }

    public void deleteReward(String pointName, int slot) {
        FileConfiguration config = getRewardConfig(pointName);
        config.set(String.valueOf(slot), null);
        saveFile(pointName, config);
    }

    private void saveFile(String pointName, FileConfiguration config) {
        try {
            config.save(new File(rewardFolder, pointName + ".yml"));
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save reward config for " + pointName);
            e.printStackTrace();
        }
    }

    public void reload() {
        configs.clear();
    }
}