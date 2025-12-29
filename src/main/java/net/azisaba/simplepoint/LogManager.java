package net.azisaba.simplepoint;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;

public class LogManager {
    private final SimplePointPlugin plugin;
    private final File logFolder;

    public LogManager(SimplePointPlugin plugin) {
        this.plugin = plugin;
        // 1. データフォルダの中に「logs」フォルダを作成
        this.logFolder = new File(plugin.getDataFolder(), "logs");
        if (!logFolder.exists()) {
            logFolder.mkdirs();
        }
    }

    /**
     * ポイント変更のログを日付別のファイルに記録する
     */
    public void logPointChange(String playerName, String pointId, int amount, String type) {
        // 2. 今日の日付でファイル名を作成 (例: 2023-10-27.log)
        String fileName = new SimpleDateFormat("yyyy-MM-dd").format(new Date()) + ".log";
        File logFile = new File(logFolder, fileName);

        // 3. ログの時刻と内容を構築
        String timeStamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
        // [時刻] プレイヤー名, ポイントID, 増減量, 操作タイプ
        String logMessage = String.format("[%s] %s, %s, %d, %s", timeStamp, playerName, pointId, amount, type);

        // 4. ファイルに書き込み (trueで追記モード)
        try (FileWriter fw = new FileWriter(logFile, true);
             PrintWriter pw = new PrintWriter(fw)) {
            pw.println(logMessage);
        } catch (IOException e) {
            plugin.getLogger().severe("ログの書き込みに失敗しました: " + e.getMessage());
        }
    }
    public void logRewardPurchase(String playerName, String pointId, String rewardName, int price) {
        String fileName = new SimpleDateFormat("yyyy-MM-dd").format(new Date()) + ".log";
        File logFile = new File(logFolder, fileName);

        String timeStamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
        // [時刻] [PURCHASE] プレイヤー名, ポイントID, 報酬名, 消費ポイント
        String logMessage = String.format("[%s] [PURCHASE] %s, %s, %s, %d pt",
                timeStamp, playerName, pointId, rewardName, price);

        try (FileWriter fw = new FileWriter(logFile, true);
             PrintWriter pw = new PrintWriter(fw)) {
            pw.println(logMessage);
        } catch (IOException e) {
            plugin.getLogger().severe("購入ログの書き込みに失敗しました: " + e.getMessage());
        }
    }
}