package net.azisaba.simplepoint.managers;

import net.azisaba.simplepoint.SimplePointPlugin;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.io.File;
import java.util.*;

public class TeamAdminGUIManager implements Listener {
    private final SimplePointPlugin plugin;
    // 編集中の数値を一時的に保持するマップ
    private final Map<UUID, RewardEditSession> sessions = new HashMap<>();

    public TeamAdminGUIManager(SimplePointPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * 1. チーム報酬一覧 (54スロット) - アイテムを配置する画面
     */
    public void openGroupRewardEditor(Player player, String group) {
        Inventory inv = Bukkit.createInventory(null, 54, "§0TeamReward Edit: " + group);
        File file = plugin.getTeamManager().getRewardFile(group);
        if (file.exists()) {
            FileConfiguration cfg = YamlConfiguration.loadConfiguration(file);
            for (int i = 0; i < 54; i++) {
                if (cfg.contains(String.valueOf(i))) {
                    inv.setItem(i, cfg.getItemStack(i + ".item"));
                }
            }
        }
        player.openInventory(inv);
    }

    /**
     * 2. 報酬の詳細編集GUI (36スロット)
     * 今回の修正: 引数を3つにし、内部でアイテムを読み込むようにしました
     */
    public void openRewardDetailEditor(Player player, String group, int slot) {
        // 保存されているファイルからアイテムを取得
        File file = plugin.getTeamManager().getRewardFile(group);
        FileConfiguration cfg = YamlConfiguration.loadConfiguration(file);
        ItemStack targetItem = cfg.getItemStack(slot + ".item");

        if (targetItem == null) {
            player.sendMessage("§cエラー: アイテムデータが見つかりません。");
            return;
        }

        // 既存の設定を読み込んでセッションを作成
        RewardEditSession session = new RewardEditSession(group, slot, targetItem);
        session.price = plugin.getTeamManager().getPrice(group, slot);
        session.stock = plugin.getTeamManager().getStock(group, slot);
        session.teamStock = plugin.getTeamManager().getTeamStock(group, slot);
        session.teamReq = plugin.getTeamManager().getTeamReq(group, slot);
        session.contReq = plugin.getTeamManager().getContReq(group, slot);
        session.isGlobalStock = cfg.getBoolean(slot + ".is_global_stock", false);

        sessions.put(player.getUniqueId(), session);
        refreshGUI(player);
    }

    /**
     * GUIの表示内容を最新の状態に更新する
     */
    private void refreshGUI(Player player) {
        RewardEditSession session = sessions.get(player.getUniqueId());
        if (session == null) return;

        // タイトルに group:slot を含めてリスナーで識別できるようにする
        Inventory inv = Bukkit.createInventory(null, 36, "§0報酬設定: " + session.group + ":" + session.slot);

        // 数値の表示用文字列作成
        String priceStr = "§e" + session.price + "pt";
        String stockStr = (session.stock < 0) ? "§b無限" : "§f" + session.stock + " 個";
        String tStockStr = (session.teamStock < 0) ? "§b無限" : "§f" + session.teamStock + " 個";

        // --- レイアウト配置 (4x9) ---
        inv.setItem(4, session.item); // 販売アイテム表示

        // 1段目: 基本設定
        inv.setItem(10, createGuiItem(Material.RED_STAINED_GLASS_PANE, "§c価格 -100", "§7現在の価格: " + priceStr));
        inv.setItem(11, createGuiItem(Material.PINK_STAINED_GLASS_PANE, "§d在庫 -1", "§7現在の在庫: " + stockStr));
        inv.setItem(12, createGuiItem(Material.REPEATER, "§f在庫設定切り替え", "§7現在の状態: " + stockStr, "§8クリックで無限/有限をリセット切替"));
        inv.setItem(13, createGuiItem(Material.GOLD_BLOCK, "§6§l設定を保存", "§7クリックしてファイルに保存し", "§7一覧画面に戻ります"));
        inv.setItem(15, createGuiItem(Material.BLUE_STAINED_GLASS_PANE, "§b在庫 +1", "§7現在の在庫: " + stockStr));
        inv.setItem(16, createGuiItem(Material.LIME_STAINED_GLASS_PANE, "§a価格 +100", "§7現在の価格: " + priceStr));

        // 2段目: チーム設定
        inv.setItem(18, createGuiItem(Material.PAINTING, "§3必要貢献ポイント -100", "§7現在の必要ポイント: §f" + session.contReq));
        inv.setItem(19, createGuiItem(Material.PLAYER_HEAD, "§6チーム目標総ポイント -100", "§7現在の目標: §f" + session.teamReq));
        inv.setItem(20, createGuiItem(Material.CHEST, "§eチーム内在庫 -1", "§7現在のチーム在庫: " + tStockStr));
        inv.setItem(21, createGuiItem(Material.REPEATER, "§fチーム在庫設定切り替え", "§7現在の状態: " + tStockStr, "§8クリックで無限/有限をリセット切替"));
        inv.setItem(24, createGuiItem(Material.CHEST, "§eチーム内在庫 +1", "§7現在のチーム在庫: " + tStockStr));
        inv.setItem(25, createGuiItem(Material.PLAYER_HEAD, "§6チーム内目標総ポイント +100", "§7現在の目標: §f" + session.teamReq));
        inv.setItem(26, createGuiItem(Material.PAINTING, "§3必要貢献ポイント +100", "§7現在の必要ポイント: §f" + session.contReq));

        // 3段目: モード設定
        inv.setItem(27, createGuiItem(Material.FLOWER_POT, "§7装飾モード切替", "§8現在は使用できません"));
        inv.setItem(28, createGuiItem(Material.COMPARATOR, "§f在庫モード切替", "§7現在の設定: " + (session.isGlobalStock ? "§6サーバー全体在庫" : "§aプレイヤーごとの在庫")));
        inv.setItem(31, createGuiItem(Material.BARRIER, "§c§lこの報酬を削除", "§7クリックでこのスロットの設定を完全に消去します"));

        player.openInventory(inv);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();
        RewardEditSession session = sessions.get(player.getUniqueId());

        // タイトル判定を修正（情報の分離）
        String title = event.getView().getTitle();
        if (session == null || !title.startsWith("§0報酬設定: ")) return;
        event.setCancelled(true);

        int slot = event.getRawSlot();
        boolean changed = true;

        switch (slot) {
            // 数値増減 (エラー回避付き)
            case 10: session.price = Math.max(0, session.price - 100); break;
            case 16: session.price += 100; break;
            case 11: if (session.stock > 0) session.stock--; break;
            case 15: if (session.stock == -1) session.stock = 1; else session.stock++; break;
            case 12: session.stock = (session.stock == -1) ? 0 : -1; break;

            case 18: session.contReq = Math.max(0, session.contReq - 100); break;
            case 26: session.contReq += 100; break;
            case 19: session.teamReq = Math.max(0, session.teamReq - 100); break;
            case 25: session.teamReq += 100; break;
            case 20: if (session.teamStock > 0) session.teamStock--; break;
            case 24: if (session.teamStock == -1) session.teamStock = 1; else session.teamStock++; break;
            case 21: session.teamStock = (session.teamStock == -1) ? 0 : -1; break;

            case 28: session.isGlobalStock = !session.isGlobalStock; break;

            case 13: // 保存
                plugin.getTeamManager().saveTeamReward(
                        session.group, session.slot, session.item, session.price,
                        session.teamReq, session.contReq, session.stock, session.teamStock, session.isGlobalStock
                );
                player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1, 1);
                player.sendMessage("§a§l[!] 報酬設定を保存しました。");
                openGroupRewardEditor(player, session.group); // 一覧に戻る
                return;

            case 31: // 削除
                plugin.getTeamManager().deleteTeamReward(session.group, session.slot);
                player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_BREAK, 1, 1);
                player.sendMessage("§c§l[!] 報酬設定を削除しました。");
                openGroupRewardEditor(player, session.group); // 一覧に戻る
                return;

            default: changed = false; break;
        }

        if (changed) {
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.5f);
            refreshGUI(player);
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        sessions.remove(event.getPlayer().getUniqueId());
    }

    private ItemStack createGuiItem(Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            meta.setLore(Arrays.asList(lore));
            item.setItemMeta(meta);
        }
        return item;
    }

    // 内部クラス: 編集セッション
    private static class RewardEditSession {
        final String group;
        final int slot;
        final ItemStack item;
        int price, stock, teamStock, teamReq, contReq;
        boolean isGlobalStock = false;

        RewardEditSession(String group, int slot, ItemStack item) {
            this.group = group;
            this.slot = slot;
            this.item = item;
        }
    }
}