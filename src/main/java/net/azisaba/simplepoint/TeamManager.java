package net.azisaba.simplepoint;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.Date;

public class TeamManager {
    private final SimplePointPlugin plugin;
    private final Set<UUID> hiddenStats = new HashSet<>(); // toggleStatsで使用

    public TeamManager(SimplePointPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * RGB (&#FFFFFF) とカラーコードを適用
     */
    public String formatName(String name) {
        if (name == null) return "";
        Pattern pattern = Pattern.compile("&#([A-Fa-f0-9]{6})");
        Matcher matcher = pattern.matcher(name);
        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            String color = matcher.group(1);
            StringBuilder replacement = new StringBuilder("§x");
            for (char c : color.toCharArray()) replacement.append('§').append(c);
            matcher.appendReplacement(sb, replacement.toString());
        }
        matcher.appendTail(sb);
        return ChatColor.translateAlternateColorCodes('&', sb.toString());
    }

    // --- グループ・チーム作成 ---
    public void createGroup(String group, String displayName) {
        File file = getRewardFile(group);
        FileConfiguration cfg = YamlConfiguration.loadConfiguration(file);
        cfg.set("display_name", displayName); // 表示名を保存
        try {
            cfg.save(file);
            // フォルダ作成も念のため
            new File(plugin.getDataFolder(), "teams/team/" + group).mkdirs();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void createTeam(String group, String teamId, String displayName) {
        File teamFile = getTeamFile(group, teamId);
        FileConfiguration cfg = YamlConfiguration.loadConfiguration(teamFile);
        cfg.set("display_name", displayName);
        cfg.set("points.current", 0);
        cfg.set("points.total", 0);
        try { cfg.save(teamFile); } catch (IOException e) { e.printStackTrace(); }
    }

    // --- メンバー管理 ---
    public void addMember(String group, String teamId, UUID uuid) {
        File f = getMemberFile(group, teamId);
        if (!f.getParentFile().exists()) f.getParentFile().mkdirs();
        FileConfiguration cfg = YamlConfiguration.loadConfiguration(f);
        List<String> members = cfg.getStringList("members");
        if (!members.contains(uuid.toString())) {
            members.add(uuid.toString());
            cfg.set("members", members);
            try { cfg.save(f); } catch (IOException e) { e.printStackTrace(); }
        }
    }

    public List<String> getMemberNames(String group, String teamId) {
        FileConfiguration cfg = YamlConfiguration.loadConfiguration(getMemberFile(group, teamId));
        List<String> uuids = cfg.getStringList("members");
        List<String> result = new ArrayList<>();
        for (String s : uuids) {
            OfflinePlayer op = Bukkit.getOfflinePlayer(UUID.fromString(s));
            int score = cfg.getInt("scores." + s, 0);
            result.add("§f" + (op.getName() != null ? op.getName() : "Unknown") + " §7- §e" + score + "pt");
        }
        return result;
    }


    public List<String> getMemberNamesOnly(String group, String teamId) {
        File memberFile = getMemberFile(group, teamId);
        FileConfiguration cfg = YamlConfiguration.loadConfiguration(memberFile);
        List<String> uuids = cfg.getStringList("members");
        List<String> names = new ArrayList<>();
        for (String s : uuids) {
            // UUIDから純粋な名前だけを取得（装飾を入れない！）
            String name = Bukkit.getOfflinePlayer(UUID.fromString(s)).getName();
            if (name != null) names.add(name);
        }
        return names;
    }

    // --- ポイント同期 (倍率適用) ---
    public void syncPoint(UUID uuid, String pointId, int amount) {
        File rewardDir = new File(plugin.getDataFolder(), "teams/reward");
        File[] files = rewardDir.listFiles();
        if (files == null) return;

        for (File f : files) {
            FileConfiguration groupCfg = YamlConfiguration.loadConfiguration(f);
            if (!groupCfg.getBoolean("enabled", true)) continue;

            // 連携ポイントの確認
            String linked = groupCfg.getString("linked_point", "none");
            if (linked.equalsIgnoreCase(pointId)) {
                String group = f.getName().replace(".yml", "");
                String teamId = findTeamIdInGroup(group, uuid);

                if (teamId != null) {
                    double multiplier = groupCfg.getDouble("multiplier", 1.0);
                    long expiry = groupCfg.getLong("multiplier_expiry", 0);

                    // 期限が設定されており、かつ現在時刻が期限を過ぎている場合は倍率を1.0にする
                    if (expiry > 0 && System.currentTimeMillis() > expiry) {
                        multiplier = 1.0;
                    }

                    int finalAmount = (int) (amount * multiplier);
                    addTeamPoints(group, teamId, uuid, finalAmount);
                }
            }
        }
    }

    public void addTeamPoints(String group, String teamId, UUID uuid, int finalAmount) {
        File teamFile = getTeamFile(group, teamId);
        FileConfiguration teamCfg = YamlConfiguration.loadConfiguration(teamFile);
        teamCfg.set("points.current", teamCfg.getInt("points.current", 0) + finalAmount);
        teamCfg.set("points.total", teamCfg.getInt("points.total", 0) + finalAmount);

        File memberFile = getMemberFile(group, teamId);
        FileConfiguration memberCfg = YamlConfiguration.loadConfiguration(memberFile);
        int currentContrib = memberCfg.getInt("scores." + uuid.toString(), 0);
        memberCfg.set("scores." + uuid.toString(), currentContrib + finalAmount);

        try {
            teamCfg.save(teamFile);
            memberCfg.save(memberFile);
        } catch (IOException e) { e.printStackTrace(); }
    }

    // --- ゲッター系 ---
    public int getPrice(String group, int slot) { return YamlConfiguration.loadConfiguration(getRewardFile(group)).getInt(slot + ".price", 0); }
    public int getStock(String group, int slot) { return YamlConfiguration.loadConfiguration(getRewardFile(group)).getInt(slot + ".stock", -1); }
    public int getTeamReq(String group, int slot) { return YamlConfiguration.loadConfiguration(getRewardFile(group)).getInt(slot + ".team_requirement", 0); }
    public int getContReq(String group, int slot) { return YamlConfiguration.loadConfiguration(getRewardFile(group)).getInt(slot + ".contribution_requirement", 0); }
    public int getTeamTotalPoint(String group, String teamId) { return YamlConfiguration.loadConfiguration(getTeamFile(group, teamId)).getInt("points.total", 0); }
    public int getTeamCurrentPoint(String group, String teamId) { return YamlConfiguration.loadConfiguration(getTeamFile(group, teamId)).getInt("points.current", 0); }
    public int getContribution(String group, String teamId, UUID uuid) { return YamlConfiguration.loadConfiguration(getMemberFile(group, teamId)).getInt("scores." + uuid.toString(), 0); }

    public String getTeamDisplayName(String group, String teamId) {
        File file = new File(plugin.getDataFolder(), "teams/team/" + group + "/" + teamId + ".yml");
        if (!file.exists()) return teamId;
        FileConfiguration cfg = YamlConfiguration.loadConfiguration(file);
        String name = cfg.getString("display_name", teamId);
        return ChatColor.translateAlternateColorCodes('&', name);
    }

    public double getTeamTotalPoints(String group, String teamId) {
        File memberFile = new File(plugin.getDataFolder(), "teams/member/" + group + "/" + teamId + ".yml");
        if (!memberFile.exists()) return 0.0;
        FileConfiguration cfg = YamlConfiguration.loadConfiguration(memberFile);

        double total = 0;
        if (cfg.contains("contributions")) {
            for (String key : cfg.getConfigurationSection("contributions").getKeys(false)) {
                total += cfg.getDouble("contributions." + key);
            }
        }
        return total;
    }

    // --- 公開設定系 (再追加) ---
    public void toggleStats(UUID uuid) {
        if (hiddenStats.contains(uuid)) hiddenStats.remove(uuid);
        else hiddenStats.add(uuid);
    }

    public boolean canShowStats(UUID uuid) {
        return !hiddenStats.contains(uuid);
    }

    // --- 設定操作 ---
    public void setGroupPoint(String group, String pointId) {
        File file = getRewardFile(group);
        FileConfiguration cfg = YamlConfiguration.loadConfiguration(file);
        cfg.set("linked_point", pointId);
        try { cfg.save(file); } catch (IOException e) { e.printStackTrace(); }
    }

    public boolean toggleGroupSync(String group) {
        File file = getRewardFile(group);
        FileConfiguration cfg = YamlConfiguration.loadConfiguration(file);
        boolean current = cfg.getBoolean("enabled", true);
        cfg.set("enabled", !current);
        try { cfg.save(file); } catch (IOException e) { e.printStackTrace(); }
        return !current;
    }

    public void setGroupMultiplier(String group, double multiplier, String startStr, String endStr) {
        File file = getRewardFile(group);
        FileConfiguration cfg = YamlConfiguration.loadConfiguration(file);

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd-HH:mm");
        try {
            long startMillis = sdf.parse(startStr).getTime();
            long endMillis = sdf.parse(endStr).getTime();

            cfg.set("multiplier", multiplier);
            cfg.set("multiplier_start", startMillis);
            cfg.set("multiplier_end", endMillis);
            cfg.save(file);
        } catch (Exception e) {
            throw new IllegalArgumentException("日付形式が正しくありません (yyyy/MM/dd-HH:mm)");
        }
    }
    public double getActiveMultiplier(String group) {
        FileConfiguration cfg = YamlConfiguration.loadConfiguration(getRewardFile(group));
        long now = System.currentTimeMillis();
        long start = cfg.getLong("multiplier_start", 0);
        long end = cfg.getLong("multiplier_end", 0);

        if (now >= start && now <= end) {
            return cfg.getDouble("multiplier", 1.0);
        }
        return 1.0;
    }
    // --- 対戦(Battle)管理 ---
    public void startBattle(String group, String t1, String t2) {
        File file = getRewardFile(group);
        FileConfiguration cfg = YamlConfiguration.loadConfiguration(file);
        cfg.set("battle.active", true);
        cfg.set("battle.team1", t1);
        cfg.set("battle.team2", t2);
        try { cfg.save(file); } catch (IOException e) { e.printStackTrace(); }
    }



    // --- 統計学的なチーム割り振りロジック ---
    public String getRandomTeam(String group, String t1, String t2) {
        int n1 = getMemberNames(group, t1).size();
        int n2 = getMemberNames(group, t2).size();
        int total = n1 + n2;

        if (total > 10) { // サンプル数がある程度ある場合のみ検定
            double p = 0.5;
            double z = (Math.abs(n1 - total * p) - 0.5) / Math.sqrt(total * p * (1 - p));
            // z > 1.96 なら 95%の確率で「偶然とは言えない偏り」がある
            if (z > 1.96) return (n1 > n2) ? t2 : t1;
        }
        return Math.random() < 0.5 ? t1 : t2;
    }
//    public String getMultiplierStatus(String group) {
//        FileConfiguration cfg = YamlConfiguration.loadConfiguration(getRewardFile(group));
//        long now = System.currentTimeMillis();
//        long start = cfg.getLong("multiplier_start", 0);
//        long end = cfg.getLong("multiplier_end", 0);
//        double val = cfg.getDouble("multiplier", 1.0);
//
//        if (now < start) {
//            return "§7[待機中] §f開始まで: §e" + formatTime(start - now);
//        } else if (now <= end) {
//            return "§a[発動中] §b" + val + "倍 §f(終了まで: §e" + formatTime(end - now) + "§f)";
//        } else {
//            return "§c[終了済]";
//        }
//    }

    /**
     * プレイヤーが現在所属しているグループ名を取得します。
     */
    public String getPlayerGroup(UUID uuid) {
        File memberBaseDir = new File(plugin.getDataFolder(), "teams/member");
        if (!memberBaseDir.exists()) return null;

        File[] groupDirs = memberBaseDir.listFiles(File::isDirectory);
        if (groupDirs == null) return null;

        for (File gDir : groupDirs) {
            File[] teamFiles = gDir.listFiles((dir, name) -> name.endsWith(".yml"));
            if (teamFiles == null) continue;

            for (File tFile : teamFiles) {
                FileConfiguration cfg = YamlConfiguration.loadConfiguration(tFile);
                if (cfg.getStringList("members").contains(uuid.toString())) {
                    return gDir.getName(); // フォルダ名（グループ名）を返す
                }
            }
        }
        return null;
    }


    // 内部用：特定のグループ内でチームを探す
    /**
     * プレイヤーが所属しているチームIDを、/teams/member/ 内のファイルから走査します。
     */
    public String getPlayerTeam(UUID uuid) {
        // パスを /teams/member/グループ名/チーム名.yml に合わせる
        File memberBaseDir = new File(plugin.getDataFolder(), "teams/member");
        if (!memberBaseDir.exists()) return null;

        File[] groupDirs = memberBaseDir.listFiles(File::isDirectory);
        if (groupDirs == null) return null;

        for (File gDir : groupDirs) {
            File[] teamFiles = gDir.listFiles((dir, name) -> name.endsWith(".yml"));
            if (teamFiles == null) continue;

            for (File tFile : teamFiles) {
                FileConfiguration cfg = YamlConfiguration.loadConfiguration(tFile);
                List<String> members = cfg.getStringList("members");
                if (members.contains(uuid.toString())) {
                    return tFile.getName().replace(".yml", "");
                }
            }
        }
        return null;
    }

    /**
     * 脱退処理：指定した場所のファイルからUUIDを削除します。
     */
    public void removeMember(String group, String teamId, UUID uuid) {
        // 修正されたパス: teams/member/グループ名/チーム名.yml
        File groupDir = new File(plugin.getDataFolder(), "teams/member/" + group);
        File file = new File(groupDir, teamId + ".yml");

        if (!file.exists()) return;

        FileConfiguration cfg = YamlConfiguration.loadConfiguration(file);
        List<String> members = cfg.getStringList("members");

        // UUIDをリストから削除
        members.remove(uuid.toString());
        cfg.set("members", members);

        // 貢献度データ(contributions)もリセットする場合
        if (cfg.contains("contributions." + uuid.toString())) {
            cfg.set("contributions." + uuid.toString(), null);
        }

        try {
            cfg.save(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    /**
     * チームの現在のポイントを取得する際のガード
     */
    public int getTeamPoints(String group, String teamId) {
        String pointId = getLinkedPointId(group);
        if (pointId == null) {
            // ここで画像のエラーメッセージを送信している可能性があります
            return 0;
        }

        File teamFile = getTeamFile(group, teamId);
        FileConfiguration cfg = YamlConfiguration.loadConfiguration(teamFile);
        return cfg.getInt("points.current", 0);
    }

    /**
     * 個人の累計貢献ポイントを取得
     */
    public int getMemberTotal(String group, String teamId, UUID uuid) {
        File file = getMemberFile(group, teamId);
        if (!file.exists()) return 0;
        return YamlConfiguration.loadConfiguration(file).getInt("contributions." + uuid.toString(), 0);
    }

    /**
     * チーム内での貢献ランキング順位を取得
     */
    public int getMemberRank(String group, String teamId, UUID uuid) {
        File file = getMemberFile(group, teamId);
        if (!file.exists()) return 1;
        FileConfiguration cfg = YamlConfiguration.loadConfiguration(file);

        // 全メンバーのスコアをリスト化してソート
        Map<String, Integer> scores = new HashMap<>();
        if (cfg.getConfigurationSection("contributions") == null) return 1;

        for (String key : cfg.getConfigurationSection("contributions").getKeys(false)) {
            scores.put(key, cfg.getInt("contributions." + key));
        }

        List<Map.Entry<String, Integer>> list = new ArrayList<>(scores.entrySet());
        list.sort((a, b) -> b.getValue().compareTo(a.getValue()));

        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).getKey().equals(uuid.toString())) return i + 1;
        }
        return list.size();
    }

    /**
     * ポイント同期が有効かどうかを返す (RewardManagerで使用)
     */
    public boolean isSyncEnabled(String group) {
        File rewardFile = getRewardFile(group);
        if (!rewardFile.exists()) return false;
        FileConfiguration cfg = YamlConfiguration.loadConfiguration(rewardFile);
        return cfg.getBoolean("sync_enabled", false);
    }

    /**
     * チームポイントを増減させる (購入時などに使用)
     * プレイヤーを指定しない単純な加減算用
     */
    public void addTeamPoints(String group, String teamId, int amount) {
        File file = getTeamFile(group, teamId);
        if (!file.exists()) return;
        FileConfiguration cfg = YamlConfiguration.loadConfiguration(file);
        int current = cfg.getInt("points.current", 0);
        cfg.set("points.current", current + amount);
        try { cfg.save(file); } catch (Exception e) { e.printStackTrace(); }
    }

    public double getTeamActiveMultiplier(String group, String teamId) {
        File file = getTeamFile(group, teamId);
        if (!file.exists()) return 1.0;
        FileConfiguration cfg = YamlConfiguration.loadConfiguration(file);

        double mult = cfg.getDouble("multiplier.value", 1.0);
        String startStr = cfg.getString("multiplier.start");
        String endStr = cfg.getString("multiplier.end");

        if (startStr == null || endStr == null) return 1.0;

        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy/MM/dd-HH:mm");
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime start = LocalDateTime.parse(startStr, formatter);
            LocalDateTime end = LocalDateTime.parse(endStr, formatter);

            if (now.isAfter(start) && now.isBefore(end)) {
                return mult;
            }
        } catch (Exception e) {
            return 1.0;
        }
        return 1.0;
    }

    /**
     * 管理画面等で表示するための倍率ステータス文字列を取得
     */
    public String getTeamMultiplierStatus(String group, String teamId) {
        double activeMult = getTeamActiveMultiplier(group, teamId);
        if (activeMult <= 1.0) return "§7なし (1.0x)";

        File file = getTeamFile(group, teamId);
        FileConfiguration cfg = YamlConfiguration.loadConfiguration(file);
        return "§d§l" + activeMult + "x §7(" + cfg.getString("multiplier.end") + " まで)";
    }

    /**
     * 以前の形式との互換用 (グループ全体の倍率)
     */
    public String getMultiplierStatus(String group) {
        return "§7チーム別設定を参照";
    }

    /**
     * チームごとの倍率設定メソッド (倍率をチームごとに変更するため)
     */
    public void setTeamMultiplier(String group, String teamId, double multiplier, String start, String end) {
        File file = getTeamFile(group, teamId);
        if (!file.exists()) return;
        FileConfiguration cfg = YamlConfiguration.loadConfiguration(file);
        cfg.set("multiplier.value", multiplier);
        cfg.set("multiplier.start", start);
        cfg.set("multiplier.end", end);
        try { cfg.save(file); } catch (Exception e) { e.printStackTrace(); }
    }

    /**
     * 対戦（バトル）を開始状態にする
     */
//    public void startBattle(String group, String t1, String t2) {
//        File file = getRewardFile(group);
//        FileConfiguration cfg = YamlConfiguration.loadConfiguration(file);
//        cfg.set("battle.active", true);
//        cfg.set("battle.team1", t1);
//        cfg.set("battle.team2", t2);
//        try { cfg.save(file); } catch (IOException e) { e.printStackTrace(); }
//    }
    private String formatTime(long millis) {
        long minutes = (millis / 1000) / 60;
        long hours = minutes / 60;
        long days = hours / 24;
        if (days > 0) return days + "日" + (hours % 24) + "時間";
        if (hours > 0) return hours + "時間" + (minutes % 60) + "分";
        return (minutes % 60) + "分";
    }

    // --- ユーティリティ ---
    public String findTeamIdInGroup(String group, UUID uuid) {
        File dir = new File(plugin.getDataFolder(), "teams/member/" + group);
        if (!dir.exists() || !dir.isDirectory()) return null;
        for (File f : dir.listFiles()) {
            if (YamlConfiguration.loadConfiguration(f).getStringList("members").contains(uuid.toString())) {
                return f.getName().replace(".yml", "");
            }
        }
        return null;
    }

    public String getLinkedPointId(String group) {
        // グループ設定ファイル (teams/reward/グループ名.yml) から連携ポイントIDを読み込む
        File file = getRewardFile(group);
        if (!file.exists()) return null;

        FileConfiguration cfg = YamlConfiguration.loadConfiguration(file);
        // 修正: ここが null だと画像のエラーになります
        return cfg.getString("linked_point");
    }

    public File getTeamFile(String group, String teamId) { return new File(plugin.getDataFolder(), "teams/team/" + group + "/" + teamId + ".yml"); }
    public File getMemberFile(String group, String teamId) { return new File(plugin.getDataFolder(), "teams/member/" + group + "/" + teamId + ".yml"); }
    public File getRewardFile(String group) { return new File(plugin.getDataFolder(), "teams/reward/" + group + ".yml"); }

    public void saveTeamReward(String group, int slot, ItemStack item, int price, int teamReq, int contReq, int stock, int teamStock, boolean isGlobalStock) {
        File file = getRewardFile(group);
        FileConfiguration cfg = YamlConfiguration.loadConfiguration(file);

        cfg.set(slot + ".item", item);
        cfg.set(slot + ".price", price);
        cfg.set(slot + ".team_requirement", teamReq);
        cfg.set(slot + ".contribution_requirement", contReq);
        cfg.set(slot + ".stock", stock);           // slot 11/15用 (個人/サーバー)
        cfg.set(slot + ".team_stock", teamStock); // slot 20/24用 (チーム内)
        cfg.set(slot + ".is_global_stock", isGlobalStock);

        try { cfg.save(file); } catch (IOException e) { e.printStackTrace(); }
    }

    public int getTeamStock(String group, int slot) {
        return YamlConfiguration.loadConfiguration(getRewardFile(group)).getInt(slot + ".team_stock", -1);
    }


    public void deleteTeamReward(String group, int slot) {
        File file = getRewardFile(group);
        FileConfiguration cfg = YamlConfiguration.loadConfiguration(file);
        cfg.set(String.valueOf(slot), null);
        try { cfg.save(file); } catch (IOException e) { e.printStackTrace(); }
    }
}