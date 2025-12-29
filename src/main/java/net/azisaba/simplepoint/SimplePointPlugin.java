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
        // 1. settings.yml の書き出し (エラー対策)
        try {
            if (!new File(getDataFolder(), "settings.yml").exists()) {
                saveResource("settings.yml", false);
            }
        } catch (Exception e) {
            getLogger().warning("settings.yml resource not found in JAR. Using defaults.");
        }

        // 2. マネージャーの初期化
        this.pointManager = new PointManager(this);
        this.rewardManager = new RewardManager(this);
        this.logManager = new LogManager(this);
        this.teamManager = new TeamManager(this);
        this.guiManager = new GUIManager(this);

        // 3. イベントとコマンドの登録
        getServer().getPluginManager().registerEvents(guiManager, this);

        if (getCommand("spp") != null) {
            SPPCommand spp = new SPPCommand(this);
            getCommand("spp").setExecutor(spp);
            getCommand("spp").setTabCompleter(spp);
        }
        if (getCommand("spt") != null) {
            getCommand("spt").setExecutor(new SPTCommand(this));
        }
        if (getCommand("sptt") != null) {
            getCommand("sptt").setExecutor(new SPTTCommand(this));
        }

        getLogger().info("SimplePointPlugin v1.2 Enabled! 🚀");
    }

    public PointManager getPointManager() { return pointManager; }
    public RewardManager getRewardManager() { return rewardManager; }
    public GUIManager getGuiManager() { return guiManager; }
    public LogManager getLogManager() { return logManager; }
    public TeamManager getTeamManager() { return teamManager; }
    public void reloadAllConfig() {
        // 1. config.yml のリロード
        reloadConfig();

        // 2. RewardManager のキャッシュリロード
        if (rewardManager != null) {
            rewardManager.reload();
        }

        // 3. TeamManager のリロード (ファイルから読み直し)
        if (teamManager != null) {
            teamManager.loadTeams(); // TeamManagerにこのメソッドがある場合
        }

        // 4. PointManager のキャッシュリロード
        if (pointManager != null) {
            pointManager.reload(); // PointManagerにキャッシュがある場合
        }

        getLogger().info("All configurations reloaded.");
    }
}