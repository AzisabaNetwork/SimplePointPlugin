package net.azisaba.simplepoint;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.zip.GZIPOutputStream;

public class LogManager {
    private final SimplePointPlugin plugin;
    private final File logFolder;
    private String currentFileDate;

    public LogManager(SimplePointPlugin plugin) {
        this.plugin = plugin;
        this.logFolder = new File(plugin.getDataFolder(), "logs");
        if (!logFolder.exists()) {
            logFolder.mkdirs();
        }

        // 起動時の日付を保持
        this.currentFileDate = new SimpleDateFormat("yyyy-MM-dd").format(new Date());

        // 起動時チェック: 昨日以前の .log ファイルが残っていれば圧縮する
        initialCheck();
    }

    private void initialCheck() {
        File[] files = logFolder.listFiles((dir, name) -> name.endsWith(".log") && !name.equals(currentFileDate + ".log"));
        if (files != null) {
            for (File file : files) {
                compressFile(file);
            }
        }
    }

    public synchronized void logPointChange(String playerName, String pointId, int amount, String type) {
        checkRotation();
        String timeStamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
        String logMessage = String.format("[%s] %s, %s, %d, %s", timeStamp, playerName, pointId, amount, type);
        writeLog(logMessage);
    }

    public synchronized void logRewardPurchase(String playerName, String pointId, String rewardName, int price) {
        checkRotation();
        String timeStamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
        String logMessage = String.format("[%s] [PURCHASE] %s, %s, %s, %d pt",
                timeStamp, playerName, pointId, rewardName, price);
        writeLog(logMessage);
    }

    private void writeLog(String message) {
        File logFile = new File(logFolder, currentFileDate + ".log");
        try (PrintWriter pw = new PrintWriter(new BufferedWriter(
                new OutputStreamWriter(new FileOutputStream(logFile, true), StandardCharsets.UTF_8)))) {
            pw.println(message);
        } catch (IOException e) {
            plugin.getLogger().severe("ログの書き込みに失敗しました: " + e.getMessage());
        }
    }

    private void checkRotation() {
        String today = new SimpleDateFormat("yyyy-MM-dd").format(new Date());
        if (!today.equals(currentFileDate)) {
            File oldFile = new File(logFolder, currentFileDate + ".log");
            if (oldFile.exists()) {
                compressFile(oldFile);
            }
            this.currentFileDate = today;
        }
    }

    private void compressFile(File source) {
        File dest = new File(source.getAbsolutePath() + ".gz");
        if (dest.exists()) return; // 既に圧縮済みの場合はスキップ

        // メインスレッドを止めないよう、新しいスレッドで圧縮を実行
        new Thread(() -> {
            try (FileInputStream fis = new FileInputStream(source);
                 FileOutputStream fos = new FileOutputStream(dest);
                 GZIPOutputStream gzos = new GZIPOutputStream(fos)) {

                byte[] buffer = new byte[1024];
                int len;
                while ((len = fis.read(buffer)) > 0) {
                    gzos.write(buffer, 0, len);
                }
                gzos.finish();

                if (source.delete()) {
                    // サーバー起動直後などはLoggerが使えない場合があるため例外処理
                    try { plugin.getLogger().info("古いログをアーカイブしました: " + dest.getName()); } catch (Exception ignored) {}
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
    }
}