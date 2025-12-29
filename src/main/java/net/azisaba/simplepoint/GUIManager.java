package net.azisaba.simplepoint;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

public class GUIManager implements Listener {
    private final SimplePointPlugin plugin;
    // 設定中のデータを一時保存するためのMap
    private final Map<UUID, SettingSession> sessions = new HashMap<>();

    public GUIManager(SimplePointPlugin plugin) {
        this.plugin = plugin;
    }

    // セッション保持用クラス
    private static class SettingSession {
        String pointName;
        int slot;
        ItemStack item;
        int price = 100;
        boolean repeatable = true;
    }

    public void openRewardGUI(Player player, String pointName, boolean isAdmin) {
        String title = pointName + (isAdmin ? ":編集" : ":受け取り");
        Inventory gui = Bukkit.createInventory(null, 54, title);

        if (plugin.getRewardManager().getConfig().contains(pointName)) {
            for (String slotStr : plugin.getRewardManager().getConfig().getConfigurationSection(pointName).getKeys(false)) {
                int slot = Integer.parseInt(slotStr);
                ItemStack item = plugin.getRewardManager().getConfig().getItemStack(pointName + "." + slot + ".item");
                int price = plugin.getRewardManager().getConfig().getInt(pointName + "." + slot + ".price");
                boolean rep = plugin.getRewardManager().getConfig().getBoolean(pointName + "." + slot + ".repeatable");

                item = item.clone();
                ItemMeta meta = item.getItemMeta();
                List<String> lore = meta.hasLore() ? meta.getLore() : new ArrayList<>();
                lore.add("§8----------");
                lore.add("§e価格: §f" + price + " pt");
                lore.add("§e再購入: §f" + (rep ? "可能" : "一度きり"));
                if (!isAdmin) {
                    lore.add("§b現在の保有: " + plugin.getPointManager().getPoint(pointName, player.getUniqueId()) + " pt");
                }
                meta.setLore(lore);
                item.setItemMeta(meta);
                gui.setItem(slot, item);
            }
        }
        player.openInventory(gui);
    }

    // 設定専用GUI 🛠️
    public void openSettingGUI(Player player) {
        SettingSession s = sessions.get(player.getUniqueId());
        Inventory inv = Bukkit.createInventory(null, 27, "報酬設定: " + s.pointName);

        inv.setItem(4, s.item); // 売りたいアイテム
        inv.setItem(10, createGuiItem(Material.RED_TERRACOTTA, "§c-100", "§7現在の価格: " + s.price));
        inv.setItem(11, createGuiItem(Material.PINK_TERRACOTTA, "§c-10", ""));
        inv.setItem(13, createGuiItem(Material.GOLD_INGOT, "§e価格: " + s.price, "§7ここをクリックして保存"));
        inv.setItem(15, createGuiItem(Material.LIME_TERRACOTTA, "§a+10", ""));
        inv.setItem(16, createGuiItem(Material.GREEN_TERRACOTTA, "§a+100", ""));
        inv.setItem(22, createGuiItem(s.repeatable ? Material.REPEATER : Material.BARRIER,
                "§f再購入設定: " + (s.repeatable ? "§a可能" : "§c一度のみ"), "§7クリックで切替"));

        player.openInventory(inv);
    }

    private ItemStack createGuiItem(Material m, String name, String lore) {
        ItemStack item = new ItemStack(m);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        if(!lore.isEmpty()) meta.setLore(Collections.singletonList(lore));
        item.setItemMeta(meta);
        return item;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();
        String title = event.getView().getTitle();
        ItemStack current = event.getCurrentItem();
        if (current == null || current.getType() == Material.AIR) return;

        // --- 購入処理 ---
        if (title.contains(":受け取り")) {
            event.setCancelled(true);
            String pointName = title.split(":")[0];
            int slot = event.getRawSlot();
            handlePurchase(player, pointName, slot);
        }

        // --- 編集モード（アイテム設置） ---
        else if (title.contains(":編集")) {
            if (event.getRawSlot() < 54) event.setCancelled(true);
            if (event.getRawSlot() < 54 && event.getCursor().getType() != Material.AIR) {
                SettingSession s = new SettingSession();
                s.pointName = title.split(":")[0];
                s.slot = event.getRawSlot();
                s.item = event.getCursor().clone();
                sessions.put(player.getUniqueId(), s);
                Bukkit.getScheduler().runTask(plugin, () -> openSettingGUI(player));
            }
        }

        // --- 設定GUIの操作 ---
        else if (title.startsWith("報酬設定:")) {
            event.setCancelled(true);
            SettingSession s = sessions.get(player.getUniqueId());
            if (s == null) return;

            switch (event.getRawSlot()) {
                case 10: s.price = Math.max(0, s.price - 100); break;
                case 11: s.price = Math.max(0, s.price - 10); break;
                case 15: s.price += 10; break;
                case 16: s.price += 100; break;
                case 22: s.repeatable = !s.repeatable; break;
                case 13: // 保存
                    plugin.getRewardManager().saveReward(s.pointName, s.slot, s.item, s.price, s.repeatable);
                    player.sendMessage("§a報酬を保存しました！");
                    player.closeInventory();
                    return;
            }
            openSettingGUI(player);
        }
    }

    private void handlePurchase(Player player, String pointName, int slot) {
        String path = pointName + "." + slot;
        int price = plugin.getRewardManager().getConfig().getInt(path + ".price");
        boolean rep = plugin.getRewardManager().getConfig().getBoolean(path + ".repeatable");
        int currentPoint = plugin.getPointManager().getPoint(pointName, player.getUniqueId());

        // 一度きりチェック
        if (!rep && plugin.getRewardManager().getConfig().getBoolean("history." + pointName + "." + slot + "." + player.getUniqueId())) {
            player.sendMessage("§cこの報酬は一度しか受け取れません！");
            return;
        }

        if (currentPoint < price) {
            player.sendMessage("§cポイントが足りません！");
            return;
        }

        // ポイント減算とアイテム付与
        plugin.getPointManager().addPoint(pointName, player.getUniqueId(), -price);
        ItemStack item = plugin.getRewardManager().getConfig().getItemStack(path + ".item").clone();
        player.getInventory().addItem(item);

        if (!rep) {
            plugin.getRewardManager().getConfig().set("history." + pointName + "." + slot + "." + player.getUniqueId(), true);
            plugin.getRewardManager().save();
        }
        player.sendMessage("§aアイテムを購入しました！");
    }
}