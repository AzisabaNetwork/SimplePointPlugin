package net.azisaba.simplepoint;

import org.bukkit.Bukkit;
import org.bukkit.Material;
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

    public GUIManager(SimplePointPlugin plugin) { this.plugin = plugin; }

    private static class SettingSession {
        String pointName; int slot; ItemStack item; int price = 100; int stock = -1;
    }

    public void openRewardGUI(Player player, String pointName, boolean isAdmin) {
        String title = pointName + (isAdmin ? ":編集" : ":受け取り");
        Inventory gui = Bukkit.createInventory(null, 54, title);

        if (plugin.getRewardManager().getConfig().contains(pointName)) {
            for (String slotStr : plugin.getRewardManager().getConfig().getConfigurationSection(pointName).getKeys(false)) {
                try {
                    int slot = Integer.parseInt(slotStr);
                    ItemStack item = plugin.getRewardManager().getConfig().getItemStack(pointName + "." + slot + ".item").clone();
                    int price = plugin.getRewardManager().getConfig().getInt(pointName + "." + slot + ".price");
                    int stock = plugin.getRewardManager().getConfig().getInt(pointName + "." + slot + ".stock", -1);
                    int req = plugin.getRewardManager().getConfig().getInt(pointName + "." + slot + ".requirement", 0);

                    ItemMeta meta = item.getItemMeta();
                    List<String> lore = meta.hasLore() ? meta.getLore() : new ArrayList<>();
                    lore.add("§8----------");
                    lore.add("§e価格: §f" + price + " pt");
                    if (stock != -1) lore.add("§b在庫: §f残り " + stock + " 個");
                    if (req > 0) lore.add("§6必要解放pt: §f" + req + " pt");
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
        } else if (title.contains(":編集")) {
            event.setCancelled(true);
            handleEditClick(player, title, event);
        } else if (title.startsWith("報酬設定:")) {
            event.setCancelled(true);
            handleSettingUpdate(player, event.getRawSlot(), event.getClick());
        }
    }

    private void handlePurchase(Player player, String pointName, int slot) {
        String path = pointName + "." + slot;
        if (!plugin.getRewardManager().getConfig().contains(path)) return;

        int price = plugin.getRewardManager().getConfig().getInt(path + ".price");
        int stock = plugin.getRewardManager().getConfig().getInt(path + ".stock", -1);

        if (stock == 0) { player.sendMessage("§c在庫切れです。"); return; }

        String pKey = pointName.startsWith("TEAMREWARD_") ? pointName.replace("TEAMREWARD_", "") : pointName;
        int balance = plugin.getPointManager().getPoint(pKey, player.getUniqueId());

        if (balance >= price) {
            if (stock > 0) {
                plugin.getRewardManager().getConfig().set(path + ".stock", stock - 1);
                plugin.getRewardManager().save();
            }
            plugin.getPointManager().addPoint(pKey, player.getUniqueId(), -price);
            player.getInventory().addItem(plugin.getRewardManager().getConfig().getItemStack(path + ".item").clone());
            player.sendMessage("§a購入しました！");
            if (!player.getOpenInventory().getTitle().equals("Inventory")) openRewardGUI(player, pointName, false);
        } else {
            player.sendMessage("§cポイントが足りません。");
        }
    }

    private void handleEditClick(Player player, String title, InventoryClickEvent event) {
        ItemStack item = (event.getCursor() != null && event.getCursor().getType() != Material.AIR) ?
                event.getCursor().clone() : (event.getCurrentItem() != null ? event.getCurrentItem().clone() : null);
        if (item == null) return;
        if (event.getCursor() != null) player.setItemOnCursor(null);

        SettingSession s = new SettingSession();
        s.pointName = title.split(":")[0];
        s.slot = event.getRawSlot();
        s.item = item;
        sessions.put(player.getUniqueId(), s);
        openSettingGUI(player);
    }

    public void openSettingGUI(Player player) {
        SettingSession s = sessions.get(player.getUniqueId());
        Inventory inv = Bukkit.createInventory(null, 27, "報酬設定: " + s.pointName);
        inv.setItem(4, s.item);
        inv.setItem(13, createGuiItem(Material.GOLD_INGOT, "§e保存: 価格 " + s.price + "pt", "§7在庫: " + (s.stock == -1 ? "無限" : s.stock)));
        inv.setItem(22, createGuiItem(Material.LAVA_BUCKET, "§c削除", ""));
        player.openInventory(inv);
    }

    private void handleSettingUpdate(Player player, int slot, ClickType click) {
        SettingSession s = sessions.get(player.getUniqueId());
        if (slot == 13) {
            plugin.getRewardManager().saveReward(s.pointName, s.slot, s.item, s.price, s.stock, true);
            player.closeInventory();
        } else if (slot == 22) {
            plugin.getRewardManager().getConfig().set(s.pointName + "." + s.slot, null);
            plugin.getRewardManager().save();
            player.closeInventory();
        }
    }

    private ItemStack createGuiItem(Material m, String name, String lore) {
        ItemStack item = new ItemStack(m); ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name); meta.setLore(Collections.singletonList(lore));
        item.setItemMeta(meta); return item;
    }
}