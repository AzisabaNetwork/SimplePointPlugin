//package net.azisaba.simplepoint.listeners;
//
//import net.azisaba.simplepoint.PointAddEvent;
//import net.azisaba.simplepoint.SimplePointPlugin;
//import org.bukkit.event.EventHandler;
//import org.bukkit.event.Listener;
//
//public class PointSyncListener implements Listener {
//    private final SimplePointPlugin plugin;
//
//    public PointSyncListener(SimplePointPlugin plugin) {
//        this.plugin = plugin;
//    }
//
//    @EventHandler
//    public void onPointAdd(PointAddEvent event) {
//        // ここで TeamManager の syncPoint を呼び出す
//        // これにより、Multiplier(倍率) や ON/OFF設定が適用される
//        if (plugin.getTeamManager() != null) {
//            plugin.getTeamManager().syncPoint(
//                    event.getUuid(),
//                    event.getId(),
//                    event.getAmount()
//            );
//        }
//    }
//}
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
        plugin.getTeamManager().syncAllTeamsTotalScore(event.getUuid(), event.getPointId(), event.getAmount());
    }
}