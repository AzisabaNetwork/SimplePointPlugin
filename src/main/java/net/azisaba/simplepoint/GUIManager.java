package net.azisaba.simplepoint;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

public class GUIManager implements Listener {
    private final SimplePointPlugin plugin;
    private final Map<UUID, SettingSession> sessions = new HashMap<>();

    public GUIManager(SimplePointPlugin plugin) {
        this.plugin = plugin;
    }

    private static class SettingSession {
        String pointName; int slot; ItemStack item; int price = 100; int stock = -1;
    }

    public void openRewardGUI(Player player, String pointName, boolean isAdmin) {
        String title = pointName + (isAdmin ? ":編集" : ":受け取り");
        Inventory gui = Bukkit.createInventory(null, 54, title);
        FileConfiguration config = plugin.getRewardManager().getRewardConfig(pointName);
        if (config != null) {
            for (String key : config.getKeys(false)) {
                try {
                    int slot = Integer.parseInt(key);
                    ItemStack item = config.getItemStack(slot + ".item").clone();
                    int price = config.getInt(slot + ".price");
                    int stock = config.getInt(slot + ".stock", -1);
                    int req = config.getInt(slot + ".requirement", 0);

                    ItemMeta meta = item.getItemMeta();
                    List<String> lore = meta.hasLore() ? meta.getLore() : new ArrayList<>();
                    lore.add("§8----------");
                    lore.add("§e価格: §f" + price + " pt");
                    if (stock != -1) lore.add("§b在庫: §f" + stock);
                    if (req > 0) lore.add("§6必要解放pt: §f" + req);
                    meta.setLore(lore);
                    item.setItemMeta(meta);
                    gui.setItem(slot, item);
                } catch (Exception ignored) {}
            }
        }
        player.openInventory(gui);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();
        String title = event.getView().getTitle();

        if (title.contains(":受け取り")) {
            event.setCancelled(true); // 購入用GUIは固定
            handlePurchase(player, title.split(":")[0], event.getRawSlot());
        }
        else if (title.contains(":編集")) {
            // インベントリ外のクリックは無視
            if (event.getRawSlot() < 0) return;

            // 自分の持ち物スロットをクリックした場合はキャンセルしない（アイテムを上に持っていけるようにするため）
            if (event.getRawSlot() >= 54) return;

            // 編集用GUIのメインエリア
            event.setCancelled(true); // デフォルトではキャンセル

            String pName = title.split(":")[0];
            ItemStack cursor = event.getCursor();
            ItemStack clicked = event.getCurrentItem();

            // 1. 手にアイテムを持って空のスロット、または既存アイテムをクリックした時（設置/上書き）
            if (cursor != null && cursor.getType() != Material.AIR) {
                ItemStack itemToSave = cursor.clone();
                // 設置した瞬間に設定画面へ
                startSetting(player, pName, event.getRawSlot(), itemToSave);
                // 手元のアイテムを消す（設置した演出）
                player.setItemOnCursor(null);
            }
            // 2. 何も持たずに既存のアイテムをクリックした時（再編集）
            else if (clicked != null && clicked.getType() != Material.AIR) {
                startSetting(player, pName, event.getRawSlot(), clicked.clone());
            }
        }
        else if (title.startsWith("報酬設定:")) {
            event.setCancelled(true);
            handleSetting(player, event.getRawSlot(), event.getClick());
        }
    }

    private void startSetting(Player player, String pName, int slot, ItemStack item) {
        SettingSession s = new SettingSession();
        s.pointName = pName; s.slot = slot; s.item = item;
        FileConfiguration config = plugin.getRewardManager().getRewardConfig(pName);
        if (config.contains(String.valueOf(slot))) {
            s.price = config.getInt(slot + ".price");
            s.stock = config.getInt(slot + ".stock", -1);
        }
        sessions.put(player.getUniqueId(), s);
        openSettingGUI(player);
    }

    public void openSettingGUI(Player player) {
        SettingSession s = sessions.get(player.getUniqueId());
        Inventory inv = Bukkit.createInventory(null, 27, "報酬設定: " + s.pointName);
        inv.setItem(4, s.item);
        inv.setItem(10, createGuiItem(Material.RED_STAINED_GLASS_PANE, "§c価格 -100", "§7現在: " + s.price));
        inv.setItem(11, createGuiItem(Material.PINK_STAINED_GLASS_PANE, "§c在庫 -1", "§7現在: " + s.stock));
        inv.setItem(13, createGuiItem(Material.GOLD_BLOCK, "§e§l保存", "§f価格: " + s.price + " | 在庫: " + s.stock));
        inv.setItem(15, createGuiItem(Material.LIGHT_BLUE_STAINED_GLASS_PANE, "§b在庫 +1", ""));
        inv.setItem(16, createGuiItem(Material.LIME_STAINED_GLASS_PANE, "§a価格 +100", ""));
        inv.setItem(22, createGuiItem(Material.BARRIER, "§4§l削除", ""));
        player.openInventory(inv);
    }

    private void handleSetting(Player player, int slot, ClickType click) {
        SettingSession s = sessions.get(player.getUniqueId());
        if (s == null) return;
        if (slot == 10) s.price = Math.max(0, s.price - (click.isRightClick() ? 10 : 100));
        else if (slot == 16) s.price += (click.isRightClick() ? 10 : 100);
        else if (slot == 11) s.stock = Math.max(-1, s.stock - 1);
        else if (slot == 15) s.stock = (s.stock == -1) ? 1 : s.stock + (click.isRightClick() ? 10 : 1);
        else if (slot == 22) { plugin.getRewardManager().deleteReward(s.pointName, s.slot); player.closeInventory(); return; }
        else if (slot == 13) { plugin.getRewardManager().saveReward(s.pointName, s.slot, s.item, s.price, s.stock); player.closeInventory(); return; }
        openSettingGUI(player);
    }

    private void handlePurchase(Player player, String pointName, int slot) {
        FileConfiguration config = plugin.getRewardManager().getRewardConfig(pointName);
        if (config == null || !config.contains(String.valueOf(slot))) return;
        int price = config.getInt(slot + ".price");
        int stock = config.getInt(slot + ".stock", -1);
        int req = config.getInt(slot + ".requirement", 0);

        // 解放条件チェック
        int total = pointName.startsWith("TEAMREWARD_") ?
                plugin.getTeamManager().getTeamPoints(pointName.replace("TEAMREWARD_", "")) :
                plugin.getPointManager().getTotalPoint(pointName, player.getUniqueId());
        if (total < req) { player.sendMessage("§c解放条件未達成 (" + total + "/" + req + ")"); return; }
        if (stock == 0) { player.sendMessage("§c在庫切れ"); return; }

        String pKey = pointName.startsWith("TEAMREWARD_") ? pointName.replace("TEAMREWARD_", "") : pointName;
        int balance = plugin.getPointManager().getPoint(pKey, player.getUniqueId());
        if (balance >= price) {
            if (stock > 0) plugin.getRewardManager().updateStock(pointName, slot, stock - 1);
            plugin.getPointManager().addPoint(pKey, player.getUniqueId(), -price);
            player.getInventory().addItem(config.getItemStack(slot + ".item").clone());
            plugin.getLogManager().logPurchase(player.getName(), pointName, price, slot);
            player.sendMessage("§a購入完了！");
            openRewardGUI(player, pointName, false);
        } else player.sendMessage("§cポイント不足");
    }

    private ItemStack createGuiItem(Material m, String name, String lore) {
        ItemStack item = new ItemStack(m); ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name); meta.setLore(Collections.singletonList(lore));
        item.setItemMeta(meta); return item;
    }
}