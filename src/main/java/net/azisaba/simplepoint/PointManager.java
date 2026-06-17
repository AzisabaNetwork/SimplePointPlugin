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
    // 名前を configs に統一しました
    private final Map<String, FileConfiguration> configs = new HashMap<>();
    private final File pointFolder;

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
        configs.clear(); // configCache -> configs
        namesConfig = YamlConfiguration.loadConfiguration(namesFile);
    }

    /**
     * チームのメンバー・スコア保存用ファイルを取得します
     * パス: plugins/SimplePoint/teams/member/グループ名/チーム名.yml
     */
    public File getMemberFile(String group, String teamId) {
        File dir = new File(plugin.getDataFolder(), "teams/member/" + group);
        if (!dir.exists()) dir.mkdirs();
        return new File(dir, teamId + ".yml");
    }

    /**
     * ポイントを付与する（心臓部）
     * フォルダ構造: plugins/SimplePoint/points/ポイントID/UUID.yml
     */
    public void addPoint(String id, UUID uuid, int amount) {
        FileConfiguration cfg = getPointConfig(id);
        if (cfg == null) return;

        String path = uuid.toString();
        int current = cfg.getInt(path + ".current", 0);
        int total = cfg.getInt(path + ".total", 0);

        cfg.set(path + ".current", current + amount);
        cfg.set("total_score_sum", cfg.getInt("total_score_sum", 0) + amount); // 任意：ポイントIDごとの総発行量
        cfg.set(path + ".total", total + amount);

        savePointConfig(id);

        // イベント
        PointAddEvent event = new PointAddEvent(uuid, id, amount);
        Bukkit.getPluginManager().callEvent(event);
    }

    // --- ここが倍率についての回答です ---
    // ここでは倍率をかけず、そのままの値を保存します。
    // 倍率は Event -> Listener -> TeamManager.syncAllTeamsTotalScore の中で計算されます。
    public void addMember(String group, String teamId, UUID uuid) {
        // 1. 他のチームに所属していないかチェックし、所属していれば削除（二重所属防止）
        File dir = new File(plugin.getDataFolder(), "teams/member/" + group);
        if (dir.exists() && dir.listFiles() != null) {
            for (File f : dir.listFiles()) {
                if (!f.getName().endsWith(".yml")) continue;

                FileConfiguration otherCfg = YamlConfiguration.loadConfiguration(f);
                List<String> members = otherCfg.getStringList("members");

                if (members.contains(uuid.toString())) {
                    members.remove(uuid.toString());
                    otherCfg.set("members", members);
                    // 貢献度データ(scores.UUID)を消すかどうかは運用次第ですが、
                    // チーム移動でスコアをリセットするならここでもcfg.set("scores." + uuid, null)をします。
                    try { otherCfg.save(f); } catch (IOException e) { e.printStackTrace(); }
                }
            }
        }

        // 2. 新しいチームへの追加
        File f = getMemberFile(group, teamId); // teams/member/group/teamId.yml
        if (!f.getParentFile().exists()) f.getParentFile().mkdirs();

        FileConfiguration cfg = YamlConfiguration.loadConfiguration(f);
        List<String> members = cfg.getStringList("members");

        if (!members.contains(uuid.toString())) {
            members.add(uuid.toString());
            cfg.set("members", members);

            // 所属判定およびランキング用の初期値設定
            if (!cfg.contains("scores." + uuid.toString())) {
                cfg.set("scores." + uuid.toString() + ".total", 0);
            }

            try {
                cfg.save(f);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    // --- ユーティリティ ---

    public FileConfiguration getPointConfig(String id) {
        if (configs.containsKey(id)) return configs.get(id);

        File file = new File(pointFolder, id + ".yml");
        if (!file.exists()) return null;

        FileConfiguration cfg = YamlConfiguration.loadConfiguration(file);
        configs.put(id, cfg);
        return cfg;
    }

    public void savePointConfig(String id) {
        FileConfiguration cfg = configs.get(id);
        if (cfg == null) return;
        try {
            cfg.save(new File(pointFolder, id + ".yml"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public int getPoint(String id, UUID uuid) {
        FileConfiguration cfg = getPointConfig(id);
        if (cfg == null) return 0;
        return cfg.getInt(uuid + ".current", 0);
    }

    public int getTotalPoint(String id, UUID uuid) {
        FileConfiguration cfg = getPointConfig(id);
        return cfg.getInt(uuid.toString() + ".total", 0);
    }

    public void setPoint(String id, UUID uuid, int amount) {
        FileConfiguration cfg = getPointConfig(id);
        if (cfg == null) return;
        cfg.set(uuid.toString() + ".current", amount);
        savePointConfig(id);
    }

    public Map<UUID, Integer> getAllTotalPoints(String pointId) {
        Map<UUID, Integer> map = new HashMap<>();
        FileConfiguration cfg = getPointConfig(pointId);

        // ファイル内の全キーを走査
        for (String key : cfg.getKeys(false)) {
            try {
                UUID uuid = UUID.fromString(key);
                int total = cfg.getInt(key + ".total", 0);
                map.put(uuid, total);
            } catch (IllegalArgumentException ignored) {} // UUID以外のキー(linked_group等)は無視
        }
        return map;
    }

    public List<String> getPointNames() {
        return new ArrayList<>(namesConfig.getKeys(false));
    }


    public boolean takePoint(String id, UUID uuid, int amount) {
        if (amount < 0) return false;
        FileConfiguration cfg = getPointConfig(id);
        if (cfg == null) return false;
        String path = uuid.toString() + ".current";
        int current = cfg.getInt(path, 0);
        if (current < amount) return false;
        cfg.set(path, current - amount);
        savePointConfig(id);
        return true;
    }

    // 表示名管理用
    public boolean createPointType(String id, String displayName) {
        File file = new File(pointFolder, id + ".yml");
        if (file.exists()) return false;
        try {
            file.createNewFile();
            namesConfig.set(id, displayName);
            namesConfig.save(namesFile);
            return true;
        } catch (IOException e) { return false; }
    }



    public String getDisplayName(String id) {
        return ChatColor.translateAlternateColorCodes('&', namesConfig.getString(id, id));
    }
}