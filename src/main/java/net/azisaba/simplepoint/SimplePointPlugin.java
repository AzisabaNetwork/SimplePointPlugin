package net.azisaba.simplepoint;

import org.bukkit.plugin.java.JavaPlugin;

public class SimplePointPlugin extends JavaPlugin {

    private PointManager pointManager;

    @Override
    public void onEnable() {
        this.pointManager = new PointManager(this);

        // 管理用コマンド登録
        SPPCommand sppCommand = new SPPCommand(this);
        getCommand("spp").setExecutor(sppCommand);
        getCommand("spp").setTabCompleter(sppCommand);

        // プレイヤー用コマンド登録
        SPTCommand sptCommand = new SPTCommand(this);
        getCommand("spt").setExecutor(sptCommand);
        getCommand("spt").setTabCompleter(sptCommand);

        saveDefaultConfig();
        getLogger().info("SimplePointPlugin has been enabled!");
    }

    public PointManager getPointManager() {
        return pointManager;
    }
}