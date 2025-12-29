package net.azisaba.simplepoint;

import org.bukkit.plugin.java.JavaPlugin;
import java.io.File;

public class SimplePointPlugin extends JavaPlugin {
    private PointManager pointManager;
    private RewardManager rewardManager;
    private GUIManager guiManager;
    private LogManager logManager;
    private TeamManager teamManager;

    @Override
    public void onEnable() {
        // settings.ymlがない場合は作成する
        saveResource("settings.yml", false);

        this.pointManager = new PointManager(this);
        this.rewardManager = new RewardManager(this);
        this.logManager = new LogManager(this);
        this.teamManager = new TeamManager(this);
        this.guiManager = new GUIManager(this);

        getServer().getPluginManager().registerEvents(guiManager, this);

        getCommand("spp").setExecutor(new SPPCommand(this));
        getCommand("spt").setExecutor(new SPTCommand(this));

        getLogger().info("SimplePointPlugin has been enabled! ✨");
    }

    public PointManager getPointManager() { return pointManager; }
    public RewardManager getRewardManager() { return rewardManager; }
    public GUIManager getGuiManager() { return guiManager; }
    public LogManager getLogManager() { return logManager; }
    public TeamManager getTeamManager() { return teamManager; }
}