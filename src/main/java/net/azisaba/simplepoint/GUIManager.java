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
        String pointName;
        int slot;
        ItemStack item;
        int price = 100;
        int stock = -1;
    }

    public void openRewardGUI(Player player, String pointName, boolean isAdmin) {
        String title = pointName + (isAdmin ? ":編集" : ":受け取り");
        Inventory gui = Bukkit.createInventory(null, 54, title);

        // 各ポイントごとの報酬設定ファイルを読み込む
        FileConfiguration config = plugin.getRewardManager().getRewardConfig(pointName);
        if (config != null) {
            for (String key : config.getKeys(false)) {
                try {
                    int slot = Integer.parseInt(key);
                    ItemStack item = config.getItemStack(slot + ".item").clone();
                    int price = config.getInt(slot + ".price");
                    int stock = config.getInt(slot + ".stock", -1);

                    ItemMeta meta = item.getItemMeta();
                    List<String> lore = meta.hasLore() ? meta.getLore() : new ArrayList<>();
                    lore.add("§8----------");
                    lore.add("§e価格: §f" + price + " pt");
                    if (stock != -1) lore.add("§b在庫: §f" + stock + " 個");
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
            event.setCancelled(true);
            handlePurchase(player, title.split(":")[0], event.getRawSlot());
        }
        else if (title.contains(":編集")) {
            event.setCancelled(true);
            if (event.getRawSlot() >= 54) return;

            String pName = title.split(":")[0];
            ItemStack cursor = event.getCursor();
            ItemStack current = event.getCurrentItem();

            if (cursor != null && cursor.getType() != Material.AIR) {
                ItemStack item = cursor.clone();
                player.setItemOnCursor(null);
                startSettingSession(player, pName, event.getRawSlot(), item);
            } else if (current != null && current.getType() != Material.AIR) {
                startSettingSession(player, pName, event.getRawSlot(), current.clone());
            }
        }
        else if (title.startsWith("報酬設定:")) {
            event.setCancelled(true);
            handleSettingClick(player, event.getRawSlot(), event.getClick());
        }
    }

    private void startSettingSession(Player player, String pName, int slot, ItemStack item) {
        SettingSession s = new SettingSession();
        s.pointName = pName;
        s.slot = slot;
        s.item = item;

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
        inv.setItem(10, createGuiItem(Material.RED_STAINED_GLASS_PANE, "§c価格 -100", "§7右クリックで-10"));
        inv.setItem(11, createGuiItem(Material.PINK_STAINED_GLASS_PANE, "§c在庫 -1", "§7現在: " + (s.stock == -1 ? "無限" : s.stock)));
        inv.setItem(13, createGuiItem(Material.GOLD_BLOCK, "§e§l保存", "§f価格: " + s.price + " | 在庫: " + s.stock));
        inv.setItem(15, createGuiItem(Material.LIGHT_BLUE_STAINED_GLASS_PANE, "§b在庫 +1", "§7右クリックで+10"));
        inv.setItem(16, createGuiItem(Material.LIME_STAINED_GLASS_PANE, "§a価格 +100", "§7右クリックで+10"));
        inv.setItem(22, createGuiItem(Material.BARRIER, "§4§l削除", ""));

        player.openInventory(inv);
    }

    private void handleSettingClick(Player player, int slot, ClickType click) {
        SettingSession s = sessions.get(player.getUniqueId());
        if (s == null) return;

        if (slot == 10) s.price = Math.max(0, s.price - (click.isRightClick() ? 10 : 100));
        else if (slot == 16) s.price += (click.isRightClick() ? 10 : 100);
        else if (slot == 11) s.stock = Math.max(-1, s.stock - 1);
        else if (slot == 15) s.stock = (s.stock == -1) ? 1 : s.stock + (click.isRightClick() ? 10 : 1);
        else if (slot == 22) {
            plugin.getRewardManager().deleteReward(s.pointName, s.slot);
            player.closeInventory();
            return;
        } else if (slot == 13) {
            plugin.getRewardManager().saveReward(s.pointName, s.slot, s.item, s.price, s.stock);
            player.sendMessage("§a報酬を保存しました。");
            player.closeInventory();
            return;
        }
        openSettingGUI(player);
    }

    private void handlePurchase(Player player, String pointName, int slot) {
        FileConfiguration config = plugin.getRewardManager().getRewardConfig(pointName);
        String path = String.valueOf(slot);
        if (config == null || !config.contains(path)) return;

        int price = config.getInt(path + ".price");
        int stock = config.getInt(path + ".stock", -1);

        if (stock == 0) {
            player.sendMessage("§c在庫切れです。");
            return;
        }

        // チーム報酬かどうかでポイントソースを切り替え
        String pKey = pointName.startsWith("TEAMREWARD_") ? pointName.replace("TEAMREWARD_", "") : pointName;
        int balance = plugin.getPointManager().getPoint(pKey, player.getUniqueId());

        if (balance >= price) {
            // 在庫減算
            if (stock > 0) {
                plugin.getRewardManager().updateStock(pointName, slot, stock - 1);
            }
            plugin.getPointManager().addPoint(pKey, player.getUniqueId(), -price);
            player.getInventory().addItem(config.getItemStack(path + ".item").clone());
            player.sendMessage("§a購入が完了しました！");

            // ログ記録
            plugin.getLogManager().logPurchase(player.getName(), pointName, price, slot);

            player.closeInventory();
        } else {
            player.sendMessage("§cポイントが不足しています。");
        }
    }

    private ItemStack createGuiItem(Material m, String name, String lore) {
        ItemStack item = new ItemStack(m);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        meta.setLore(Collections.singletonList(lore));
        item.setItemMeta(meta);
        return item;
    }
}