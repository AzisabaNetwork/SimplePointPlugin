package net.azisaba.simplepoint;

import org.bukkit.plugin.java.JavaPlugin;
import java.io.File;

public class SimplePointPlugin extends JavaPlugin {
    private PointManager pointManager;
    private RewardManager rewardManager;
    private GUIManager guiManager;
    private LogManager logManager;
    private TeamManager teamManager;
    private TeamAdminGUIManager teamAdminGUIManager;
    private TeamGUIManager teamGUIManager;

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
        this.teamManager = new TeamManager(this);
        this.teamGUIManager = new TeamGUIManager(this);
        this.pointManager = new PointManager(this);
        this.rewardManager = new RewardManager(this);
        this.logManager = new LogManager(this);
        this.teamAdminGUIManager = new TeamAdminGUIManager(this);
        this.guiManager = new GUIManager(this);

        // 3. イベントの登録
        getServer().getPluginManager().registerEvents(guiManager, this);
        getServer().getPluginManager().registerEvents(new TeamAdminGUIListener(this), this);
        getServer().getPluginManager().registerEvents(teamGUIManager, this);

        // ★ 追加: 個人ポイント獲得をチームに同期させるリスナー
        getServer().getPluginManager().registerEvents(new PointSyncListener(this), this);

        // --- コマンド登録 ---
        registerCommands();

        getLogger().info("SimplePointPlugin v1.3 Enabled! 🚀");
    }

    private void registerCommands() {
        if (getCommand("spp") != null) {
            getCommand("spp").setExecutor(new SPPCommand(this));
            getCommand("spp").setTabCompleter(new SPPTabCompleter(this));
        }
        if (getCommand("spt") != null) {
            getCommand("spt").setExecutor(new SPTCommand(this));
            getCommand("spt").setTabCompleter(new SPTTabCompleter(this));
        }
        if (getCommand("myp") != null) {
            MYPCommand myp = new MYPCommand(this);
            getCommand("myp").setExecutor(myp);
            getCommand("myp").setTabCompleter(myp);
        }
        if (getCommand("ranking") != null) {
            RankingShortcutCommand rk = new RankingShortcutCommand(this);
            getCommand("ranking").setExecutor(rk);
            getCommand("ranking").setTabCompleter(rk);
        }
        if (getCommand("sppt") != null) {
            getCommand("sppt").setExecutor(new SPPTCommand(this));
            getCommand("sppt").setTabCompleter(new SPPTTabCompleter(this));
        }
    }

    // --- Getters ---
    public PointManager getPointManager() { return pointManager; }
    public RewardManager getRewardManager() { return rewardManager; }
    public GUIManager getGuiManager() { return guiManager; }
    public LogManager getLogManager() { return logManager; }
    public TeamManager getTeamManager() { return teamManager; }
    public TeamAdminGUIManager getTeamAdminGUIManager() { return teamAdminGUIManager; }
    public TeamGUIManager getTeamGUIManager() { return teamGUIManager; }

    public void reloadAllConfig() {
        reloadConfig();
        if (rewardManager != null) rewardManager.reload();
        if (pointManager != null) pointManager.reload();
    }
}