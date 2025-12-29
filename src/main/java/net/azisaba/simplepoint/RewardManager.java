package net.azisaba.simplepoint;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import java.io.File;
import java.io.IOException;

public class RewardManager {
    private final SimplePointPlugin plugin;
    private final File file;
    private FileConfiguration config;

    public RewardManager(SimplePointPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "reward.yml");
        if (!file.exists()) {
            try { file.createNewFile(); } catch (IOException e) { e.printStackTrace(); }
        }
        this.config = YamlConfiguration.loadConfiguration(file);
    }

    // 引数にstockを追加してエラーを解消！ ✨
    public void saveReward(String pointName, int slot, ItemStack item, int price, int stock, boolean repeatable) {
        String path = pointName + "." + slot;
        config.set(path + ".item", item);
        config.set(path + ".price", price);
        config.set(path + ".stock", stock);
        config.set(path + ".repeatable", repeatable);
        save();
    }

    public FileConfiguration getConfig() {
        return config;
    }

    public void save() {
        try { config.save(file); } catch (IOException e) { e.printStackTrace(); }
    }
}