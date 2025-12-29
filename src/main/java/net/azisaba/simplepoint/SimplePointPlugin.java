package net.azisaba.simplepoint;

import org.bukkit.plugin.java.JavaPlugin;

public class SimplePointPlugin extends JavaPlugin {
    private PointManager pointManager;
    private RewardManager rewardManager; // 追加
    private GUIManager guiManager;     // 追加

    @Override
    public void onEnable() {
        this.pointManager = new PointManager(this);
        this.rewardManager = new RewardManager(this); // 初期化
        this.guiManager = new GUIManager(this);      // 初期化

        getServer().getPluginManager().registerEvents(guiManager, this); // イベント登録

        getCommand("spp").setExecutor(new SPPCommand(this));
        getCommand("spt").setExecutor(new SPTCommand(this));

        saveDefaultConfig();
        getLogger().info("SimplePointPlugin has been enabled! ✨");
    }

    public PointManager getPointManager() { return pointManager; }
    public RewardManager getRewardManager() { return rewardManager; } // 追加
    public GUIManager getGuiManager() { return guiManager; }         // 追加
}