package net.azisaba.simplepoint.listeners;

import net.azisaba.simplepoint.SimplePointPlugin;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import java.io.File;

public class TeamAdminGUIListener implements Listener {
    private final SimplePointPlugin plugin;

    public TeamAdminGUIListener(SimplePointPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onAdminClick(InventoryClickEvent event) {
        String title = event.getView().getTitle();
        if (event.getClickedInventory() == null) return;
        Player player = (Player) event.getWhoClicked();

        // --- 1. チーム報酬一覧 (54スロット) ---
        if (title.startsWith("§0TeamReward Edit: ")) {
            String group = title.replace("§0TeamReward Edit: ", "");
            int slot = event.getRawSlot();
            if (slot < 0 || slot >= 54) return;

            ItemStack cursor = event.getCursor();
            ItemStack clicked = event.getCurrentItem();

            // アイテムを置いた
            if (cursor != null && cursor.getType() != Material.AIR) {
                event.setCancelled(true);
                // 初期値で保存 (価格100, 目標0, 貢献0, 在庫-1)
                plugin.getTeamManager().saveTeamReward(group, slot, cursor.clone(), 100, 0, 0, -1, -1, false);
                player.setItemOnCursor(null);
                player.playSound(player.getLocation(), Sound.ENTITY_ITEM_PICKUP, 1, 1);
                plugin.getTeamAdminGUIManager().openRewardDetailEditor(player, group, slot);
                return;
            }

            // 既存アイテムをクリック
            if (clicked != null && clicked.getType() != Material.AIR) {
                event.setCancelled(true);
                plugin.getTeamAdminGUIManager().openRewardDetailEditor(player, group, slot);
            }
        }

        // --- 2. チーム報酬詳細設定 (36スロット) ---
        else if (title.startsWith("§0報酬設定: ")) {
            event.setCancelled(true);
            String[] parts = title.replace("§0報酬設定: ", "").split(":");
            String group = parts[0];
            int slot = Integer.parseInt(parts[1]);

            File file = plugin.getTeamManager().getRewardFile(group);
            FileConfiguration cfg = YamlConfiguration.loadConfiguration(file);

            int price = cfg.getInt(slot + ".price", 100);
            int teamReq = cfg.getInt(slot + ".team_requirement", 0);
            int contReq = cfg.getInt(slot + ".contribution_requirement", 0);
            int stock = cfg.getInt(slot + ".stock", -1);
            int teamStock = cfg.getInt(slot + ".team_stock", -1);
            boolean isGlobal = cfg.getBoolean(slot + ".is_global_stock", false);
            ItemStack item = cfg.getItemStack(slot + ".item");

            boolean changed = true;
            switch (event.getRawSlot()) {
                case 10: price = Math.max(0, price - 100); break;
                case 16: price += 100; break;
                case 11: if (stock > 0) stock--; break;
                case 15: if (stock == -1) stock = 1; else stock++; break;
                case 12: stock = (stock == -1) ? 0 : -1; break;
                case 13: // 保存（一覧に戻る）
                    player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1, 1);
                    plugin.getTeamAdminGUIManager().openGroupRewardEditor(player, group);
                    return;
                case 18: contReq = Math.max(0, contReq - 100); break;
                case 26: contReq += 100; break;
                case 19: teamReq = Math.max(0, teamReq - 100); break;
                case 25: teamReq += 100; break;
                case 20: if (teamStock > 0) teamStock--; break;
                case 24: if (teamStock == -1) teamStock = 1; else teamStock++; break;
                case 21: teamStock = (teamStock == -1) ? 0 : -1; break;
                case 28: isGlobal = !isGlobal; break;
                case 31: // 削除
                    plugin.getTeamManager().deleteTeamReward(group, slot);
                    player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_BREAK, 1, 1);
                    plugin.getTeamAdminGUIManager().openGroupRewardEditor(player, group);
                    return;
                default: changed = false; break;
            }

            if (changed) {
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.5f);
                plugin.getTeamManager().saveTeamReward(group, slot, item, price, teamReq, contReq, stock, teamStock, isGlobal);
                plugin.getTeamAdminGUIManager().openRewardDetailEditor(player, group, slot);
            }
        }
    }
}