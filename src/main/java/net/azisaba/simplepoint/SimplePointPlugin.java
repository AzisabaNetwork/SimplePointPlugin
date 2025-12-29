package net.azisaba.simplepoint;

import net.azisaba.SPPCommand;
import net.azisaba.simplepoint.PointManager;
import org.bukkit.plugin.java.JavaPlugin;

public class SimplePointPlugin extends JavaPlugin {

    private PointManager pointManager;

    @Override
    public void onEnable() {
        // マネージャーの初期化
        this.pointManager = new PointManager(this);

        // コマンドの登録
        SPPCommand sppCommand = new SPPCommand(this);
        getCommand("spp").setExecutor(sppCommand);
        getCommand("spp").setTabCompleter(sppCommand);

        getLogger().info("SimplePointPlugin has been enabled!");
    }

    public PointManager getPointManager() {
        return pointManager;
    }
}