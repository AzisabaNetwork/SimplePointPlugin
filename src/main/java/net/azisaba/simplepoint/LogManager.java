package net.azisaba.simplepoint;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;

public class LogManager {
    private final SimplePointPlugin plugin;

    public LogManager(SimplePointPlugin plugin) {
        this.plugin = plugin;
    }

    public void log(String message) {
        String fileName = new SimpleDateFormat("yyyy-MM-dd").format(new Date());
        String timeStamp = new SimpleDateFormat("HH:mm:ss").format(new Date());
        File logFolder = new File(plugin.getDataFolder(), "logs");
        if (!logFolder.exists()) logFolder.mkdirs();

        File logFile = new File(logFolder, fileName + ".log");
        try (PrintWriter pw = new PrintWriter(new FileWriter(logFile, true))) {
            pw.println("[" + timeStamp + "] " + message);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}