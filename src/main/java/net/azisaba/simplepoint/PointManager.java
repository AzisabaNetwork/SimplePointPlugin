package net.azisaba.simplepoint;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class PointManager {
    private final SimplePointPlugin plugin;
    private final File pointFolder;
    private final Map<String, FileConfiguration> configCache = new HashMap<>();

    // IDと表示名を紐付けるためのファイル
    private final File namesFile;
    private FileConfiguration namesConfig;

    public PointManager(SimplePointPlugin plugin) {
        this.plugin = plugin;
        this.pointFolder = new File(plugin.getDataFolder(), "points");
        if (!pointFolder.exists()) pointFolder.mkdirs();

        // point_names.yml の初期化
        this.namesFile = new File(plugin.getDataFolder(), "point_names.yml");
        this.namesConfig = YamlConfiguration.loadConfiguration(namesFile);
    }

    public void reload() {
        configCache.clear();
        namesConfig = YamlConfiguration.loadConfiguration(namesFile);
    }

    /**
     * ポイントの作成 (IDと表示名を分ける)
     */
    public boolean createPointType(String id, String displayName) {
        File file = new File(pointFolder, id + ".yml");
        if (file.exists()) return false;
        try {
            file.createNewFile();
            FileConfiguration cfg = YamlConfiguration.loadConfiguration(file);
            cfg.set("_settings.ranking_enabled", true);
            cfg.set("_settings.function_enabled", true);
            cfg.save(file);

            // 表示名を保存
            namesConfig.set(id, displayName);
            saveNamesFile();
            return true;
        } catch (IOException e) { return false; }
    }

    /**
     * 累計(total)を減らさずに、現在の所持ポイント(points)のみを減算します。
     */
    public void takePoint(String pointId, UUID uuid, int amount) {
        File file = getPointFile(pointId, uuid);
        FileConfiguration cfg = YamlConfiguration.loadConfiguration(file);

        // 現在のポイントを取得 (キー名は既存の保存形式に合わせて 'points' または 'current' に修正してください)
        int current = cfg.getInt("points", 0);

        // 新しい値を設定 (マイナスにならないよう Math.max を使用)
        cfg.set("points", Math.max(0, current - amount));

        try {
            cfg.save(file);
        } catch (IOException e) {
            plugin.getLogger().severe("ポイントの保存中にエラーが発生しました: " + file.getName());
            e.printStackTrace();
        }
    }

    /**
     * 指定されたポイントIDとUUIDに対応するファイルオブジェクトを取得します。
     * @param pointId ポイントの種類 (例: event_point)
     * @param uuid プレイヤーのUUID
     * @return ファイルオブジェクト
     */
    public File getPointFile(String pointId, UUID uuid) {
        // フォルダ構成: plugins/SimplePoint/points/pointId/uuid.yml
        File dir = new File(plugin.getDataFolder(), "points/" + pointId);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        return new File(dir, uuid.toString() + ".yml");
    }

    /**
     * 指定されたポイントIDにおける全プレイヤーの累計スコアをMapで取得します。
     * @param pointId ポイントの識別ID
     * @return UUIDと累計ポイントのマップ
     */
    public Map<UUID, Integer> getAllTotalPoints(String pointId) {
        Map<UUID, Integer> map = new HashMap<>();
        File pointDir = new File(plugin.getDataFolder(), "points/" + pointId);

        if (!pointDir.exists() || !pointDir.isDirectory()) return map;

        File[] files = pointDir.listFiles((dir, name) -> name.endsWith(".yml"));
        if (files == null) return map;

        for (File file : files) {
            try {
                String uuidStr = file.getName().replace(".yml", "");
                UUID uuid = UUID.fromString(uuidStr);
                FileConfiguration cfg = YamlConfiguration.loadConfiguration(file);
                // 累計ポイント(total)を取得
                int total = cfg.getInt("total", 0);
                map.put(uuid, total);
            } catch (Exception ignored) {
                // 不正なファイル名はスキップ
            }
        }
        return map;
    }

    /**
     * 表示名を取得 (&を§に変換) - RGB対応が必要な場合は TeamManager.formatName と同様の処理を推奨
     */
    public String getDisplayName(String id) {
        String name = namesConfig.getString(id, id);
        return ChatColor.translateAlternateColorCodes('&', name);
    }

    private void saveNamesFile() {
        try { namesConfig.save(namesFile); } catch (IOException e) { e.printStackTrace(); }
    }

    public FileConfiguration getPointConfig(String id) {
        if (configCache.containsKey(id)) return configCache.get(id);
        File file = new File(pointFolder, id + ".yml");
        if (!file.exists()) return null;
        FileConfiguration cfg = YamlConfiguration.loadConfiguration(file);
        configCache.put(id, cfg);
        return cfg;
    }

    /**
     * 設定の保存
     */
    public void savePointConfig(String id) {
        FileConfiguration cfg = getPointConfig(id);
        if (cfg == null) return;
        try {
            cfg.save(new File(pointFolder, id + ".yml"));
        } catch (IOException e) { e.printStackTrace(); }
    }

    public int getPoint(String id, UUID uuid) {
        FileConfiguration cfg = getPointConfig(id);
        return cfg != null ? cfg.getInt(uuid.toString() + ".current", 0) : 0;
    }

    public int getTotalPoint(String id, UUID uuid) {
        FileConfiguration cfg = getPointConfig(id);
        return cfg != null ? cfg.getInt(uuid.toString() + ".total", 0) : 0;
    }

    /**
     * ポイントの加算
     */
    public void addPoint(String id, UUID uuid, int amount) {
        FileConfiguration cfg = getPointConfig(id);
        if (cfg == null) return;

        // --- 倍率の取得 ---
        String groupId = plugin.getTeamManager().getPlayerGroup(uuid);
        String teamId = (groupId != null) ? plugin.getTeamManager().getPlayerTeamInGroup(uuid, groupId) : null;
        double multiplier = (groupId != null && teamId != null) ?
                plugin.getTeamManager().getTeamActiveMultiplier(groupId, teamId) : 1.0;

        int finalAmount = (int) (amount * multiplier);

        // --- 1. 個人ポイントの保存 (current: 使うと減る / total: 実績) ---
        int current = cfg.getInt(uuid.toString() + ".current", 0);
        int total = cfg.getInt(uuid.toString() + ".total", 0);

        cfg.set(uuid.toString() + ".current", current + finalAmount);
        if (finalAmount > 0) {
            cfg.set(uuid.toString() + ".total", total + finalAmount);
        }
        savePointConfig(id);

        // --- 2. チーム累計ポイントの更新 (syncPoint内で処理) ---
        // ここで syncPoint が呼ばれることで、チームファイル側の「減らない合計」も更新されます
        PointAddEvent event = new PointAddEvent(uuid, id, finalAmount);
        Bukkit.getPluginManager().callEvent(event);

        // --- 3. メッセージ表示 (装飾名を使用) ---
        Player p = Bukkit.getPlayer(uuid);
        if (p != null && groupId != null && teamId != null) {
            // 装飾付きの表示名を取得
            String groupDisplay = plugin.getTeamManager().getGroupDisplayName(groupId);
            String teamDisplay = plugin.getTeamManager().getTeamDisplayName(groupId, teamId);

            p.sendMessage("§a§l[+] §f" + teamDisplay + "§7(§f" + groupDisplay + "§7) に §e" + finalAmount + "pt §f貢献しました！");
        }
    }

    public void setPoint(String id, UUID uuid, int amount) {
        FileConfiguration cfg = getPointConfig(id);
        if (cfg == null) return;
        cfg.set(uuid.toString() + ".current", amount);
        savePointConfig(id);
    }

    /**
     * 存在するすべてのポイントIDを取得
     */
    public List<String> getPointNames() {
        return new ArrayList<>(namesConfig.getKeys(false));
    }
}