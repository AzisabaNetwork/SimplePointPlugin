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
        // 1. settings.yml の書き出し
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

        // --- SPP Command ---
        if (getCommand("spp") != null) {
            SPPCommand sppExecutor = new SPPCommand(this);
            getCommand("spp").setExecutor(sppExecutor);
            // ✨ 修正: SPPCommand自身ではなく、専用の補完クラスのみをセット
            getCommand("spp").setTabCompleter(new SPPTabCompleter(this));
        }

        // --- SPT Command ---
        if (getCommand("spt") != null) {
            getCommand("spt").setExecutor(new SPTCommand(this));
            getCommand("spt").setTabCompleter(new SPTTabCompleter(this));
        }

        // --- SPTT Command ---
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
        reloadConfig();
        if (rewardManager != null) rewardManager.reload();
        if (pointManager != null) pointManager.reload();
        if (teamManager != null) teamManager.loadTeams();
    }
}