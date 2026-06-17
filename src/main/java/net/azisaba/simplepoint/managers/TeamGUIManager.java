package net.azisaba.simplepoint.managers;

import net.azisaba.simplepoint.SimplePointPlugin;
import net.azisaba.simplepoint.TeamManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
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
import org.bukkit.Registry;

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
            meta.addEnchant(Registry.ENCHANTMENT.get(NamespacedKey.minecraft("luck")), 1, true);
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

        // 1. 紐付けられたポイントIDを動的に取得
        String linkedPointId = "none";
        File pointsFolder = new File(plugin.getDataFolder(), "points");
        if (pointsFolder.exists() && pointsFolder.listFiles() != null) {
            for (File f : pointsFolder.listFiles()) {
                if (!f.getName().endsWith(".yml")) continue;
                FileConfiguration pCfg = YamlConfiguration.loadConfiguration(f);
                if (group.equals(pCfg.getString("linked_group"))) {
                    linkedPointId = f.getName().replace(".yml", "");
                    break;
                }
            }
        }

        if (linkedPointId.equals("none")) {
            player.sendMessage("§cこのグループ(§l" + group + "§c)にはポイントIDが紐付けられていません。");
            player.sendMessage("§7/sppt setpoint " + group + " <ポイントID> で紐付けてください。");
            return;
        }

        // 設定値の取得
        int price = rewardCfg.getInt(slot + ".price", 0);
        int teamReq = rewardCfg.getInt(slot + ".team_requirement", 0);
        int contReq = rewardCfg.getInt(slot + ".contribution_requirement", 0);
        int stock = rewardCfg.getInt(slot + ".team_stock", -1);

        // --- バリデーション (購入可否チェック) ---

        // チーム累計チェック (TeamManagerのメソッドを使用)
        int currentTeamTotal = plugin.getTeamManager().getTeamTotalScore(group, teamId);
        if (currentTeamTotal < teamReq) {
            player.sendMessage("§cチーム累計スコアが足りません (§f" + currentTeamTotal + "§7/§e" + teamReq + "pt§c)");
            return;
        }

        // 個人累計貢献度チェック
        int currentCont = plugin.getTeamManager().getContribution(group, teamId, player.getUniqueId());
        if (currentCont < contReq) {
            player.sendMessage("§cあなたの累計貢献度が足りません (§f" + currentCont + "§7/§e" + contReq + "pt§c)");
            return;
        }

        // 在庫チェック
        if (stock == 0) {
            player.sendMessage("§cこの報酬は売り切れです。");
            return;
        }

        // 所持ポイント(消費用)チェック
        int playerBalance = plugin.getPointManager().getPoint(linkedPointId, player.getUniqueId());
        if (playerBalance < price) {
            player.sendMessage("§c所持ポイントが足りません (§f" + playerBalance + "§7/§e" + price + "pt§c)");
            return;
        }

        // --- 購入処理の実行 ---

        // 1. ポイントを減算 (currentのみ減らす)
        plugin.getPointManager().takePoint(linkedPointId, player.getUniqueId(), price);

        // 2. 共有在庫の減算
        if (stock > 0) {
            rewardCfg.set(slot + ".team_stock", stock - 1);
            try {
                rewardCfg.save(rewardFile);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        // 3. アイテム付与
        ItemStack rewardItem = rewardCfg.getItemStack(slot + ".item");
        if (rewardItem != null) {
            player.getInventory().addItem(rewardItem.clone());
        }

        // 4. 演出とログ
        player.sendMessage("§a§l購入完了! §e" + price + "pt §f消費しました。");
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1.2f);

        // 5. GUIをリフレッシュ（最新の在庫・所持ポイントを表示）
        openTeamRewardGUI(player, group);
    }
}