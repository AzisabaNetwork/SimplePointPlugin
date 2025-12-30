package net.azisaba.simplepoint;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

public class GUIManager implements Listener {
    private final SimplePointPlugin plugin;
    private final Map<UUID, SettingSession> sessions = new HashMap<>();
    private final Map<UUID, String> activeGuiId = new HashMap<>();

    // インベントリ切り替え中にCloseEventを無視するためのフラグ
    private final Set<UUID> switchingPlayers = new HashSet<>();

    public GUIManager(SimplePointPlugin plugin) {
        this.plugin = plugin;
    }

    private static class SettingSession {
        String pointName; int slot; ItemStack item; int price = 100; int stock = -1;
        boolean isPersonal = false;
    }

    public void openRewardGUI(Player player, String pointName, boolean isAdmin) {
        switchingPlayers.add(player.getUniqueId()); // 切り替えフラグON

        String displayName = plugin.getPointManager().getDisplayName(pointName);
        String title = displayName + (isAdmin ? "§r:編集" : "§r:受け取り");
        Inventory gui = Bukkit.createInventory(null, 54, title);

        activeGuiId.put(player.getUniqueId(), pointName);

        FileConfiguration config = plugin.getRewardManager().getRewardConfig(pointName);
        if (config != null) {
            for (String key : config.getKeys(false)) {
                try {
                    int slot = Integer.parseInt(key);
                    ItemStack item = config.getItemStack(slot + ".item").clone();
                    int price = config.getInt(slot + ".price");
                    int stock = config.getInt(slot + ".stock", -1);
                    boolean isPersonal = config.getBoolean(slot + ".is_personal", false);
                    int req = config.getInt(slot + ".requirement", 0);

                    boolean isSoldOut = false;
                    if (price != -1) {
                        if (isPersonal) {
                            int bought = getPersonalBoughtCount(player, pointName, slot);
                            if (stock != -1 && bought >= stock) isSoldOut = true;
                        } else {
                            if (stock == 0) isSoldOut = true;
                        }
                    }
                    if (isAdmin) isSoldOut = false;

                    ItemMeta meta = item.getItemMeta();
                    if (price != -1 && meta != null) {
                        List<String> lore = meta.hasLore() ? meta.getLore() : new ArrayList<>();
                        lore.add("§8----------");
                        if (isSoldOut) {
                            item.setType(Material.BARRIER);
                            lore.add("§c§l在庫切れ / 購入不可");
                        } else {
                            lore.add("§e価格: §f" + price + " pt");
                        }
                        if (isPersonal) {
                            int bought = getPersonalBoughtCount(player, pointName, slot);
                            lore.add("§a個人在庫: §f" + (stock == -1 ? "なし" : bought + " / " + stock));
                        } else {
                            lore.add("§b共有在庫: §f" + (stock == -1 ? "無限" : stock));
                        }
                        if (req > 0) lore.add("§6必要解放pt: §f" + req);
                        meta.setLore(lore);
                    }
                    item.setItemMeta(meta);
                    gui.setItem(slot, item);
                } catch (Exception ignored) {}
            }
        }
        player.openInventory(gui);
        switchingPlayers.remove(player.getUniqueId()); // 切り替えフラグOFF
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();
        String title = event.getView().getTitle();
        String pId = activeGuiId.get(player.getUniqueId());

        if (title.contains(":受け取り")) {
            event.setCancelled(true);
            if (pId != null) handlePurchase(player, pId, event.getRawSlot());
        }
        else if (title.contains(":編集")) {
            if (event.getRawSlot() < 0 || event.getRawSlot() >= 54) return;
            event.setCancelled(true);

            // pIdがnullの場合、タイトルから復元を試みる（最終防衛ライン）
            if (pId == null) {
                pId = title.split(":")[0].replace("§r", "");
                activeGuiId.put(player.getUniqueId(), pId);
            }

            ItemStack cursor = event.getCursor();
            ItemStack clicked = event.getCurrentItem();

            if (cursor != null && cursor.getType() != Material.AIR) {
                startSetting(player, pId, event.getRawSlot(), cursor.clone());
                player.setItemOnCursor(null);
            }
            else if (clicked != null && clicked.getType() != Material.AIR) {
                startSetting(player, pId, event.getRawSlot(), clicked.clone());
            }
        }
        else if (title.startsWith("報酬設定:")) {
            event.setCancelled(true);
            handleSetting(player, event.getRawSlot(), event.getClick());
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onInventoryClose(InventoryCloseEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        // 画面切り替え中でなければ、データを削除する
        if (!switchingPlayers.contains(uuid)) {
            activeGuiId.remove(uuid);
        }
    }

    private void startSetting(Player player, String pId, int slot, ItemStack item) {
        SettingSession s = new SettingSession();
        s.pointName = pId; s.slot = slot; s.item = item;
        FileConfiguration config = plugin.getRewardManager().getRewardConfig(pId);
        if (config != null && config.contains(String.valueOf(slot))) {
            s.price = config.getInt(slot + ".price");
            s.stock = config.getInt(slot + ".stock", -1);
            s.isPersonal = config.getBoolean(slot + ".is_personal", false);
        }
        sessions.put(player.getUniqueId(), s);
        openSettingGUI(player);
    }

    public void openSettingGUI(Player player) {
        SettingSession s = sessions.get(player.getUniqueId());
        if (s == null) return;

        switchingPlayers.add(player.getUniqueId());
        String displayName = plugin.getPointManager().getDisplayName(s.pointName);
        Inventory inv = Bukkit.createInventory(null, 27, "報酬設定: " + displayName);
        inv.setItem(4, s.item);

        inv.setItem(10, createGuiItem(Material.RED_STAINED_GLASS_PANE, "§c価格 -100", "§7現在: " + (s.price == -1 ? "装飾中" : s.price)));
        inv.setItem(16, createGuiItem(Material.LIME_STAINED_GLASS_PANE, "§a価格 +100", "§7現在: " + (s.price == -1 ? "装飾中" : s.price)));

        String stockDisplay = (s.stock == -1) ? "§b無限" : "§f" + s.stock;
        inv.setItem(11, createGuiItem(Material.PINK_STAINED_GLASS_PANE, "§c在庫 -1", "§7現在: " + stockDisplay));
        inv.setItem(15, createGuiItem(Material.LIGHT_BLUE_STAINED_GLASS_PANE, "§b在庫 +1", "§7現在: " + stockDisplay));

        inv.setItem(12, createGuiItem(Material.COMPARATOR, "§f在庫設定切替", "§7クリックで §b無限 §7と §e数値 §7を切り替え"));
        inv.setItem(18, createGuiItem(Material.PAINTING, "§d§l装飾モード切替", "§7現在: " + (s.price == -1 ? "§aON" : "§cOFF")));

        inv.setItem(19, createGuiItem(Material.PLAYER_HEAD, "§6§l在庫モード切替",
                "§7現在: " + (s.isPersonal ? "§a個人在庫" : "§e共有在庫") + "\n§8クリックで切り替え"));

        inv.setItem(13, createGuiItem(Material.GOLD_BLOCK, "§e§l保存", "§7スロット: " + s.slot));
        inv.setItem(22, createGuiItem(Material.BARRIER, "§4§l削除", ""));
        player.openInventory(inv);
        switchingPlayers.remove(player.getUniqueId());
    }

    private void handleSetting(Player player, int slot, ClickType click) {
        SettingSession s = sessions.get(player.getUniqueId());
        if (s == null) return;

        if (slot == 10) {
            if (s.price == -1) s.price = 0;
            s.price = Math.max(0, s.price - (click.isRightClick() ? 10 : 100));
        }
        else if (slot == 16) {
            if (s.price == -1) s.price = 0;
            s.price += (click.isRightClick() ? 10 : 100);
        }
        else if (slot == 11) {
            if (s.stock != -1) s.stock = Math.max(0, s.stock - 1);
        }
        else if (slot == 15) {
            if (s.stock == -1) s.stock = 1;
            else s.stock += (click.isRightClick() ? 10 : 1);
        }
        else if (slot == 12) s.stock = (s.stock == -1) ? 1 : -1;
        else if (slot == 18) s.price = (s.price == -1) ? 100 : -1;
        else if (slot == 19) s.isPersonal = !s.isPersonal;
        else if (slot == 22) {
            String pId = s.pointName;
            plugin.getRewardManager().deleteReward(pId, s.slot);
            sessions.remove(player.getUniqueId());
            openRewardGUI(player, pId, true);
            return;
        }
        else if (slot == 13) {
            String pId = s.pointName;
            plugin.getRewardManager().saveReward(pId, s.slot, s.item, s.price, s.stock, s.isPersonal);
            player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
            sessions.remove(player.getUniqueId());
            openRewardGUI(player, pId, true);
            return;
        }
        openSettingGUI(player);
    }

    private void handlePurchase(Player player, String pointName, int slot) {
        FileConfiguration config = plugin.getRewardManager().getRewardConfig(pointName);
        if (config == null || !config.contains(String.valueOf(slot))) return;

        int price = config.getInt(slot + ".price");
        if (price == -1) return;

        int stock = config.getInt(slot + ".stock", -1);
        boolean isPersonal = config.getBoolean(slot + ".is_personal", false);
        int req = config.getInt(slot + ".requirement", 0);

        int total = plugin.getPointManager().getTotalPoint(pointName, player.getUniqueId());
        if (total < req) { player.sendMessage("§c解放条件未達成 (" + total + "/" + req + ")"); return; }

        if (isPersonal) {
            if (stock != -1 && getPersonalBoughtCount(player, pointName, slot) >= stock) {
                player.sendMessage("§cこれ以上購入できません");
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                return;
            }
        } else {
            if (stock == 0) {
                player.sendMessage("§c在庫切れ");
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                return;
            }
        }

        int balance = plugin.getPointManager().getPoint(pointName, player.getUniqueId());
        if (balance >= price) {
            ItemStack rewardItem = config.getItemStack(slot + ".item");
            String itemName = (rewardItem != null && rewardItem.hasItemMeta() && rewardItem.getItemMeta().hasDisplayName())
                    ? rewardItem.getItemMeta().getDisplayName()
                    : (rewardItem != null ? rewardItem.getType().toString() : "Unknown Item");

            if (isPersonal) {
                addPersonalBoughtCount(player, pointName, slot);
            } else if (stock > 0) {
                plugin.getRewardManager().updateStock(pointName, slot, stock - 1);
            }

            plugin.getPointManager().addPoint(pointName, player.getUniqueId(), -price);
            if (rewardItem != null) player.getInventory().addItem(rewardItem.clone());
            plugin.getLogManager().logRewardPurchase(player.getName(), pointName, itemName, price);

            player.sendMessage("§a購入完了！");
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.2f);
            openRewardGUI(player, pointName, false);
        } else {
            player.sendMessage("§cポイント不足");
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
        }
    }

    private int getPersonalBoughtCount(Player p, String pointName, int slot) {
        FileConfiguration cfg = plugin.getPointManager().getPointConfig(pointName);
        return cfg != null ? cfg.getInt(p.getUniqueId() + ".purchased." + slot, 0) : 0;
    }

    private void addPersonalBoughtCount(Player p, String pointName, int slot) {
        FileConfiguration cfg = plugin.getPointManager().getPointConfig(pointName);
        if (cfg == null) return;
        int current = cfg.getInt(p.getUniqueId() + ".purchased." + slot, 0);
        cfg.set(p.getUniqueId() + ".purchased." + slot, current + 1);
        plugin.getPointManager().savePointConfig(pointName);
    }

    private ItemStack createGuiItem(Material m, String name, String lore) {
        ItemStack item = new ItemStack(m); ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            meta.setLore(Collections.singletonList(lore));
            item.setItemMeta(meta);
        }
        return item;
    }
}