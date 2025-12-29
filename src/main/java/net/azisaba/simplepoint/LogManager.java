package net.azisaba.simplepoint;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

public class LogManager {
    private final SimplePointPlugin plugin;
    private final File logFile;

    public LogManager(SimplePointPlugin plugin) {
        this.plugin = plugin;
        this.logFile = new File(plugin.getDataFolder(), "logs.csv");

        // ファイルが新規作成された場合、ヘッダーを書き込む
        if (!logFile.exists()) {
            try {
                logFile.createNewFile();
                write("timestamp_ms,type,player,target_point,amount_or_slot,info");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 購入ログを記録
     */
    public void logPurchase(String playerName, String pointName, int price, int slot) {
        // 形式: タイムスタンプ,種別,プレイヤー,ポイント名,スロット,価格
        String message = String.format("%d,PURCHASE,%s,%s,%d,%d",
                System.currentTimeMillis(), playerName, pointName, slot, price);
        write(message);
    }

    /**
     * ポイント変動ログを記録
     */
    public void logPointChange(String playerName, String pointName, int amount, String type) {
        // 形式: タイムスタンプ,種別,プレイヤー,ポイント名,変動量,備考
        String message = String.format("%d,POINT_%s,%s,%s,%d,",
                System.currentTimeMillis(), type.toUpperCase(), playerName, pointName, amount);
        write(message);
    }

    private void write(String message) {
        try (FileWriter fw = new FileWriter(logFile, true);
             PrintWriter pw = new PrintWriter(fw)) {
            pw.println(message);
        } catch (IOException e) {
            plugin.getLogger().warning("Could not write to CSV log file: " + e.getMessage());
        }
    }
}