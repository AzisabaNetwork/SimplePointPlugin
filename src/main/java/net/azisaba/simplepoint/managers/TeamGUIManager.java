package net.azisaba.simplepoint.managers;

import net.azisaba.simplepoint.SimplePointPlugin;
import net.azisaba.simplepoint.TeamManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class TeamGUIManager implements Listener {
    private final SimplePointPlugin plugin;
    private static final String TITLE_PREFIX = "§0Team Reward: ";

    public TeamGUIManager(SimplePointPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * プレイヤーにチーム報酬GUIを開く
     */
    public void openTeamRewardGUI(Player player, String group) {
        // プレイヤーがそのグループでどのチームに所属しているか特定
        String teamId = plugin.getTeamManager().findTeamIdInGroup(group, player.getUniqueId());
        if (teamId == null) {
            player.sendMessage("§cあなたは現在、このグループのチームに所属していません。");
            return;
        }

        Inventory inv = Bukkit.createInventory(null, 54, TITLE_PREFIX + group);
        File rewardFile = plugin.getTeamManager().getRewardFile(group);
        FileConfiguration rewardCfg = YamlConfiguration.loadConfiguration(rewardFile);

        TeamManager tm = plugin.getTeamManager();
        int teamTotal = tm.getTeamTotalPoint(group, teamId);
        int playerCont = tm.getContribution(group, teamId, player.getUniqueId());

        for (String key : rewardCfg.getKeys(false)) {
            try {
                int slot = Integer.parseInt(key);
                inv.setItem(slot, createDisplayItem(rewardCfg, slot, teamTotal, playerCont));
            } catch (NumberFormatException ignored) {}
        }

        player.openInventory(inv);
    }

    /**
     * 購入条件に基づいたアイテム表示の生成
     */
    private ItemStack createDisplayItem(FileConfiguration cfg, int slot, int teamTotal, int playerCont) {
        ItemStack item = cfg.getItemStack(slot + ".item").clone();
        int teamReq = cfg.getInt(slot + ".team_requirement", 0);
        int contReq = cfg.getInt(slot + ".contribution_requirement", 0);
        int stock = cfg.getInt(slot + ".team_stock", -1);
        int price = cfg.getInt(slot + ".price", 0);

        // --- 1. ロック判定 (チーム全体の累計ポイントが不足している場合) ---
        if (teamTotal < teamReq) {
            ItemStack lock = new ItemStack(Material.BARRIER);
            ItemMeta meta = lock.getItemMeta();
            meta.setDisplayName("§c§l報酬ロック中");
            meta.addEnchant(Enchantment.LUCK, 1, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            List<String> lore = new ArrayList<>();
            lore.add("§8チームの目標スコアに達していないため封印されています。");
            lore.add("");
            lore.add("§7必要チームスコア: §e" + teamReq + " pt");
            lore.add("§7現在のチームスコア: §f" + teamTotal + " pt");
            meta.setLore(lore);
            lock.setItemMeta(meta);
            return lock;
        }

        // --- 2. 通常表示 (条件情報の追記) ---
        ItemMeta meta = item.getItemMeta();
        List<String> lore = meta.hasLore() ? meta.getLore() : new ArrayList<>();
        lore.add("§8§m-----------------------");
        lore.add("§e価格: §f" + price + " pt");

        // 個人貢献度条件の表示
        if (contReq > 0) {
            String color = (playerCont >= contReq) ? "§a" : "§c";
            lore.add("§6必要貢献度: " + color + playerCont + " / " + contReq + " pt");
        }

        // チーム共有在庫の表示
        if (stock != -1) {
            String color = (stock > 0) ? "§b" : "§4";
            lore.add("§d共有在庫: " + color + (stock > 0 ? stock + " 個" : "売り切れ"));
            if (stock == 0) {
                item.setType(Material.COAL); // 売り切れ感を出すために石炭などに変更可能
            }
        }

        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        String title = event.getView().getTitle();
        if (!title.startsWith(TITLE_PREFIX)) return;
        event.setCancelled(true);

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) return;
        if (clicked.getType() == Material.BARRIER) return; // ロック中

        Player player = (Player) event.getWhoClicked();
        String group = title.replace(TITLE_PREFIX, "");
        int slot = event.getSlot();

        // 所属チームを再取得
        String teamId = plugin.getTeamManager().findTeamIdInGroup(group, player.getUniqueId());
        if (teamId == null) return;

        processPurchase(player, group, teamId, slot);
    }

    private void processPurchase(Player player, String group, String teamId, int slot) {
        File rewardFile = plugin.getTeamManager().getRewardFile(group);
        FileConfiguration rewardCfg = YamlConfiguration.loadConfiguration(rewardFile);

        if (!rewardCfg.contains(String.valueOf(slot))) return;

        // 1. 連携ポイントIDの取得先を rewardCfg (グループ設定) に変更
        // これにより「連携ポイントが設定されていません」のエラーを解消します
        String linkedPointId = rewardCfg.getString("linked_point", "none");

        if (linkedPointId.equals("none")) {
            player.sendMessage("§cこのグループには連携ポイントが設定されていません。管理者に /sppt setpoint を実行するよう伝えてください。");
            return;
        }

        // 設定値の取得
        int price = rewardCfg.getInt(slot + ".price", 0);
        int teamReq = rewardCfg.getInt(slot + ".team_requirement", 0);
        int contReq = rewardCfg.getInt(slot + ".contribution_requirement", 0);
        int stock = rewardCfg.getInt(slot + ".team_stock", -1);

        // --- バリデーション ---

        // チーム累計チェック
        if (plugin.getTeamManager().getTeamTotalPoint(group, teamId) < teamReq) {
            player.sendMessage("§cチームの累計獲得スコアが目標(" + teamReq + "pt)に達していないため解放されていません。");
            return;
        }

        // 個人貢献度チェック (累計貢献度で判定)
        if (plugin.getTeamManager().getContribution(group, teamId, player.getUniqueId()) < contReq) {
            player.sendMessage("§cあなたの累計貢献度が " + contReq + "pt に達していないため購入できません。");
            return;
        }

        // 在庫チェック
        if (stock == 0) {
            player.sendMessage("§cこの報酬はチーム内で売り切れました。");
            return;
        }

        // 所持ポイントチェック
        int playerBalance = plugin.getPointManager().getPoint(linkedPointId, player.getUniqueId());
        if (playerBalance < price) {
            player.sendMessage("§c所持ポイントが足りません！ (所持: " + playerBalance + " / 必要: " + price + ")");
            return;
        }

        // --- 購入処理の実行 ---

        // 重要: 総ポイント(total)を減らさず、現在の所持ポイント(current)のみを減らすメソッドを使用
        // PointManager側に消費専用のロジック（addPoint内でtotalを更新しないフラグ等）があることを前提としています
        plugin.getPointManager().takePoint(linkedPointId, player.getUniqueId(), price);

        // 在庫の減算
        if (stock > 0) {
            rewardCfg.set(slot + ".team_stock", stock - 1);
            try { rewardCfg.save(rewardFile); } catch (IOException e) { e.printStackTrace(); }
        }

        // アイテム付与
        ItemStack rewardItem = rewardCfg.getItemStack(slot + ".item").clone();
        player.getInventory().addItem(rewardItem);

        // 演出
        player.sendMessage("§a§l購入完了！ §f" + rewardItem.getItemMeta().getDisplayName() + " §fを受け取りました。");
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1.2f);

        // GUIをリフレッシュ
        openTeamRewardGUI(player, group);
    }
}