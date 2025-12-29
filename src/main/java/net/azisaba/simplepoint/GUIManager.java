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

    public GUIManager(SimplePointPlugin plugin) {
        this.plugin = plugin;
    }

    private static class SettingSession {
        String pointName;
        int slot;
        ItemStack item;
        int price = 100;
        int stock = -1; // -1は無限
        boolean repeatable = true;
    }

    public void openRewardGUI(Player player, String pointName, boolean isAdmin) {
        String title = pointName + (isAdmin ? ":編集" : ":受け取り");
        Inventory gui = Bukkit.createInventory(null, 54, title);

        if (plugin.getRewardManager().getConfig().contains(pointName)) {
            for (String slotStr : plugin.getRewardManager().getConfig().getConfigurationSection(pointName).getKeys(false)) {
                int slot = Integer.parseInt(slotStr);
                ItemStack item = plugin.getRewardManager().getConfig().getItemStack(pointName + "." + slot + ".item").clone();
                int price = plugin.getRewardManager().getConfig().getInt(pointName + "." + slot + ".price");
                int stock = plugin.getRewardManager().getConfig().getInt(pointName + "." + slot + ".stock", -1);

                ItemMeta meta = item.getItemMeta();
                List<String> lore = meta.hasLore() ? meta.getLore() : new ArrayList<>();
                lore.add("§8----------");
                lore.add("§e価格: §f" + price + " pt");
                lore.add("§e在庫: §f" + (stock == -1 ? "無限" : stock));
                meta.setLore(lore);
                item.setItemMeta(meta);
                gui.setItem(slot, item);
            }
        }
        player.openInventory(gui);
    }

    public void openSettingGUI(Player player) {
        SettingSession s = sessions.get(player.getUniqueId());
        Inventory inv = Bukkit.createInventory(null, 27, "報酬設定: " + s.pointName);

        inv.setItem(4, s.item);
        inv.setItem(10, createGuiItem(Material.RED_TERRACOTTA, "§c価格 -100", "§7右クリックで-10"));
        inv.setItem(16, createGuiItem(Material.GREEN_TERRACOTTA, "§a価格 +100", "§7右クリックで+10"));
        inv.setItem(19, createGuiItem(Material.CHEST, "§6在庫: " + (s.stock < 0 ? "無限" : s.stock), "§7左: +5 / 右: -5 (0未満で無限)"));
        inv.setItem(22, createGuiItem(Material.LAVA_BUCKET, "§4§l設定を削除", "§7スロットを空にします"));
        inv.setItem(13, createGuiItem(Material.GOLD_INGOT, "§e§l保存する", "§7現在の価格: " + s.price));

        player.openInventory(inv);
    }

    private ItemStack createGuiItem(Material m, String name, String lore) {
        ItemStack item = new ItemStack(m);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        meta.setLore(Collections.singletonList(lore));
        item.setItemMeta(meta);
        return item;
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
            if (event.getRawSlot() < 54 && event.getCursor().getType() != Material.AIR) {
                SettingSession s = new SettingSession();
                s.pointName = title.split(":")[0];
                s.slot = event.getRawSlot();
                s.item = event.getCursor().clone();
                sessions.put(player.getUniqueId(), s);
                Bukkit.getScheduler().runTask(plugin, () -> openSettingGUI(player));
            }
        } else if (title.startsWith("報酬設定:")) {
            event.setCancelled(true);
            handleSetting(player, event.getRawSlot(), event.getClick());
        }
    }

    private void handleSetting(Player player, int slot, ClickType click) {
        SettingSession s = sessions.get(player.getUniqueId());
        if (s == null) return;

        switch (slot) {
            case 10: s.price = Math.max(0, s.price - (click.isRightClick() ? 10 : 100)); break;
            case 16: s.price += (click.isRightClick() ? 10 : 100); break;
            case 19: s.stock = (click.isRightClick() ? s.stock - 5 : s.stock + 5); if(s.stock < -1) s.stock = -1; break;
            case 22: // 削除
                plugin.getRewardManager().getConfig().set(s.pointName + "." + s.slot, null);
                plugin.getRewardManager().save();
                player.sendMessage(plugin.getConfig().getString("messages.delete-success"));
                player.closeInventory();
                return;
            case 13: // 保存
                plugin.getRewardManager().saveReward(s.pointName, s.slot, s.item, s.price, s.stock, s.repeatable);
                player.closeInventory();
                return;
        }
        openSettingGUI(player);
    }

    private void handlePurchase(Player player, String pointName, int slot) {
        // (前回の購入ロジックにログ出力を追加)
        String path = pointName + "." + slot;
        if (!plugin.getRewardManager().getConfig().contains(path)) return;

        int price = plugin.getRewardManager().getConfig().getInt(path + ".price");
        int current = plugin.getPointManager().getPoint(pointName, player.getUniqueId());

        if (current >= price) {
            plugin.getPointManager().addPoint(pointName, player.getUniqueId(), -price);
            player.getInventory().addItem(plugin.getRewardManager().getConfig().getItemStack(path + ".item"));
            plugin.getLogManager().log(player.getName() + " purchased " + pointName + " slot " + slot);
            player.sendMessage("§a購入しました！");
        }
    }
}