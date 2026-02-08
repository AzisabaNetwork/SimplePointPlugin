package net.azisaba.simplepoint.listeners;

import net.azisaba.simplepoint.PointAddEvent;
import net.azisaba.simplepoint.SimplePointPlugin;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class PointSyncListener implements Listener {
    private final SimplePointPlugin plugin;

    public PointSyncListener(SimplePointPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPointAdd(PointAddEvent event) {
        // 個人ポイントが入ったタイミングで、全所属チームの累計・消費用スコアを同期
        //plugin.getTeamManager().syncAllTeamsTotalScore(event.getUuid(), event.getAmount());
        //plugin.getLogger().info("[Debug] PointAddEvent キャッチ: " + event.getUuid() + " が " + event.getPointId() + " を " + event.getAmount() + "pt 獲得");
        plugin.getTeamManager().syncAllTeamsTotalScore(event.getUuid(), event.getPointId(), event.getAmount());
        //plugin.getLogger().info("§a[Debug] 同期処理メソッドを呼び出しました。");
    }
}