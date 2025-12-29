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
        int stock = -1;
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
                    int req = plugin.getRewardManager().getConfig().getInt(pointName + "." + slot + ".requirement", 0);

                    ItemMeta meta = item.getItemMeta();
                    List<String> lore = meta.hasLore() ? meta.getLore() : new ArrayList<>();
                    lore.add("§8----------");
                    lore.add("§e価格: §f" + price + " pt");
                    if (req > 0) {
                        lore.add("§6必要解放ポイント: §f" + req + " pt");
                    }
                    meta.setLore(lore);
                    item.setItemMeta(meta);
                    gui.setItem(slot, item);
                } catch (Exception ignored) {}
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
        inv.setItem(13, createGuiItem(Material.GOLD_INGOT, "§e§l設定を保存する", "§7現在の価格: " + s.price + " pt"));
        inv.setItem(22, createGuiItem(Material.LAVA_BUCKET, "§4§lスロットを削除", "§7この報酬を消去します"));

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
            if (event.getRawSlot() < 54) handlePurchase(player, title.split(":")[0], event.getRawSlot());
        }
        else if (title.contains(":編集")) {
            if (event.getRawSlot() < 54) {
                event.setCancelled(true);
                ItemStack itemToSet = null;
                if (event.getCursor() != null && event.getCursor().getType() != Material.AIR) {
                    itemToSet = event.getCursor().clone();
                    player.setItemOnCursor(null);
                }
                else if (event.getCurrentItem() != null && event.getCurrentItem().getType() != Material.AIR) {
                    itemToSet = event.getCurrentItem().clone();
                }

                if (itemToSet != null) {
                    startSetting(player, title, event.getRawSlot(), itemToSet);
                }
            }
        }
        else if (title.startsWith("報酬設定:")) {
            event.setCancelled(true);
            handleSetting(player, event.getRawSlot(), event.getClick());
        }
    }

    private void startSetting(Player player, String title, int slot, ItemStack item) {
        SettingSession s = new SettingSession();
        s.pointName = title.split(":")[0];
        s.slot = slot;
        s.item = item;
        String path = s.pointName + "." + s.slot;
        if (plugin.getRewardManager().getConfig().contains(path)) {
            s.price = plugin.getRewardManager().getConfig().getInt(path + ".price");
        }
        sessions.put(player.getUniqueId(), s);
        Bukkit.getScheduler().runTask(plugin, () -> openSettingGUI(player));
    }

    private void handleSetting(Player player, int slot, ClickType click) {
        SettingSession s = sessions.get(player.getUniqueId());
        if (s == null) return;

        switch (slot) {
            case 10: s.price = Math.max(0, s.price - (click.isRightClick() ? 10 : 100)); openSettingGUI(player); break;
            case 16: s.price += (click.isRightClick() ? 10 : 100); openSettingGUI(player); break;
            case 22:
                plugin.getRewardManager().getConfig().set(s.pointName + "." + s.slot, null);
                plugin.getRewardManager().save();
                player.sendMessage("§c報酬を削除しました。");
                player.closeInventory();
                break;
            case 13:
                plugin.getRewardManager().saveReward(s.pointName, s.slot, s.item, s.price, s.stock, true);
                player.sendMessage("§a報酬を保存しました！");
                player.closeInventory();
                break;
        }
    }

    private void handlePurchase(Player player, String pointName, int slot) {
        String path = pointName + "." + slot;
        if (!plugin.getRewardManager().getConfig().contains(path)) return;

        int price = plugin.getRewardManager().getConfig().getInt(path + ".price");
        int req = plugin.getRewardManager().getConfig().getInt(path + ".requirement", 0);

        // --- 解放条件(requirement)のチェック ---
        if (req > 0) {
            int currentTotal;
            if (pointName.startsWith("TEAMREWARD_")) {
                // チーム報酬の場合：チームの総ポイントをチェック 👥
                String teamName = pointName.replace("TEAMREWARD_", "");
                currentTotal = plugin.getTeamManager().getTeamPoints(teamName);
            } else {
                // 通常報酬の場合：個人の累計ポイントをチェック 👤
                currentTotal = plugin.getPointManager().getTotalPoint(pointName, player.getUniqueId());
            }

            if (currentTotal < req) {
                player.sendMessage("§c解放条件を満たしていません！ (必要: " + req + " pt / 現在: " + currentTotal + " pt)");
                return;
            }
        }

        int currentBalance = plugin.getPointManager().getPoint(pointName.startsWith("TEAMREWARD_") ?
                pointName.replace("TEAMREWARD_", "") : pointName, player.getUniqueId());

        if (currentBalance >= price) {
            String pName = pointName.startsWith("TEAMREWARD_") ? pointName.replace("TEAMREWARD_", "") : pointName;
            plugin.getPointManager().addPoint(pName, player.getUniqueId(), -price);
            player.getInventory().addItem(plugin.getRewardManager().getConfig().getItemStack(path + ".item").clone());
            player.sendMessage("§a購入が完了しました！");
            plugin.getLogManager().log(player.getName() + " purchased slot " + slot + " from " + pointName);
        } else {
            player.sendMessage("§cポイントが足りません！");
        }
    }
}