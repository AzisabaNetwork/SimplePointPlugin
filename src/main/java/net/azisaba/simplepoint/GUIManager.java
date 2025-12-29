package net.azisaba.simplepoint;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
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
        boolean isPersonal = false; // ✨追加
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
                    boolean isPersonal = config.getBoolean(slot + ".is_personal", false);
                    int req = config.getInt(slot + ".requirement", 0);

                    // 在庫切れ判定
                    boolean isSoldOut = false;
                    if (price != -1) {
                        if (isPersonal) {
                            int bought = getPersonalBoughtCount(player, pointName, slot);
                            if (stock != -1 && bought >= stock) isSoldOut = true;
                        } else {
                            if (stock == 0) isSoldOut = true;
                        }
                    }

                    // --- 編集モード時のみの特別処理 ---
                    // 編集モードならバリアに変えず、かつ在庫情報をそのまま出す
                    if (isAdmin) isSoldOut = false;

                    ItemMeta meta = item.getItemMeta(); // 元の名前・説明文を保持したメタを取得

                    if (price != -1) {
                        List<String> lore = meta.hasLore() ? meta.getLore() : new ArrayList<>();
                        lore.add("§8----------");

                        if (isSoldOut) {
                            // 在庫切れ時の演出
                            item.setType(Material.BARRIER); // ✨ 見た目だけをバリアに変更
                            lore.add("§c§l在庫切れ / 購入不可");
                        } else {
                            lore.add("§e価格: §f" + price + " pt");
                        }

                        // 在庫状況の表示
                        if (isPersonal) {
                            int bought = getPersonalBoughtCount(player, pointName, slot);
                            lore.add("§b個人在庫: §f" + (stock == -1 ? "なし" : bought + " / " + stock));
                        } else {
                            lore.add("§b共有在庫: §f" + (stock == -1 ? "無限" : stock));
                        }

                        if (req > 0) lore.add("§6必要解放pt: §f" + req);
                        meta.setLore(lore); // SPPの情報を追加したLoreをセット
                    }

                    item.setItemMeta(meta); // バリアになったアイテムに元のメタを上書き
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
            if (event.getRawSlot() < 0 || event.getRawSlot() >= 54) return;
            event.setCancelled(true);
            String pName = title.split(":")[0];
            ItemStack cursor = event.getCursor();
            ItemStack clicked = event.getCurrentItem();

            if (cursor != null && cursor.getType() != Material.AIR) {
                startSetting(player, pName, event.getRawSlot(), cursor.clone());
                player.setItemOnCursor(null);
            }
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
            s.isPersonal = config.getBoolean(slot + ".is_personal", false); // ✨追加
        }
        sessions.put(player.getUniqueId(), s);
        openSettingGUI(player);
    }

    public void openSettingGUI(Player player) {
        SettingSession s = sessions.get(player.getUniqueId());
        Inventory inv = Bukkit.createInventory(null, 27, "報酬設定: " + s.pointName);
        inv.setItem(4, s.item);

        inv.setItem(10, createGuiItem(Material.RED_STAINED_GLASS_PANE, "§c価格 -100", "§7現在: " + (s.price == -1 ? "装飾中" : s.price)));
        inv.setItem(16, createGuiItem(Material.LIME_STAINED_GLASS_PANE, "§a価格 +100", "§7現在: " + (s.price == -1 ? "装飾中" : s.price)));

        String stockDisplay = (s.stock == -1) ? "§b無限" : "§f" + s.stock;
        inv.setItem(11, createGuiItem(Material.PINK_STAINED_GLASS_PANE, "§c在庫 -1", "§7現在: " + stockDisplay));
        inv.setItem(15, createGuiItem(Material.LIGHT_BLUE_STAINED_GLASS_PANE, "§b在庫 +1", "§7現在: " + stockDisplay));

        inv.setItem(12, createGuiItem(Material.COMPARATOR, "§f在庫設定切替", "§7クリックで §b無限 §7と §e数値 §7を切り替え"));
        inv.setItem(18, createGuiItem(Material.PAINTING, "§d§l装飾モード切替", "§7現在: " + (s.price == -1 ? "§aON" : "§cOFF")));

        // ✨在庫モード切替ボタン
        inv.setItem(19, createGuiItem(Material.PLAYER_HEAD, "§6§l在庫モード切替",
                "§7現在: " + (s.isPersonal ? "§a個人制限" : "§e共有在庫") + "\n§8クリックで切り替え"));

        inv.setItem(13, createGuiItem(Material.GOLD_BLOCK, "§e§l保存", "§7スロット: " + s.slot));
        inv.setItem(22, createGuiItem(Material.BARRIER, "§4§l削除", ""));
        player.openInventory(inv);
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
        else if (slot == 19) s.isPersonal = !s.isPersonal; // ✨追加
        else if (slot == 22) {
            plugin.getRewardManager().deleteReward(s.pointName, s.slot);
            player.closeInventory();
            return;
        }
        else if (slot == 13) {
            // ✨引数に s.isPersonal を追加
            plugin.getRewardManager().saveReward(s.pointName, s.slot, s.item, s.price, s.stock, s.isPersonal);
            player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
            player.closeInventory();
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
        boolean isPersonal = config.getBoolean(slot + ".is_personal", false); // ✨追加
        int req = config.getInt(slot + ".requirement", 0);

        // ✨解放条件チェック
        int total = pointName.startsWith("TEAMREWARD_") ?
                plugin.getTeamManager().getTeamPoints(pointName.replace("TEAMREWARD_", "")) :
                plugin.getPointManager().getTotalPoint(pointName, player.getUniqueId());
        if (total < req) { player.sendMessage("§c解放条件未達成 (" + total + "/" + req + ")"); return; }

        // ✨在庫チェックの分岐
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

        String pKey = pointName.startsWith("TEAMREWARD_") ? pointName.replace("TEAMREWARD_", "") : pointName;
        int balance = plugin.getPointManager().getPoint(pKey, player.getUniqueId());

        if (balance >= price) {
            // ✨減算処理の分岐
            if (isPersonal) {
                addPersonalBoughtCount(player, pointName, slot);
            } else if (stock > 0) {
                plugin.getRewardManager().updateStock(pointName, slot, stock - 1);
            }

            plugin.getPointManager().addPoint(pKey, player.getUniqueId(), -price);
            player.getInventory().addItem(config.getItemStack(slot + ".item").clone());

            List<String> commands = config.getStringList(slot + ".commands");
            for (String cmd : commands) {
                String processedCmd = cmd.replace("%player%", player.getName()).replace("%point%", pointName);
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), processedCmd);
            }

            plugin.getLogManager().logPurchase(player.getName(), pointName, price, slot);
            player.sendMessage("§a購入完了！");
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.2f);
            openRewardGUI(player, pointName, false);
        } else {
            player.sendMessage("§cポイント不足");
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
        }
    }

    // ✨個人データの管理用ヘルパー
    private int getPersonalBoughtCount(Player p, String pointName, int slot) {
        FileConfiguration cfg = plugin.getPointManager().getPointConfig(pointName);
        return cfg.getInt(p.getUniqueId() + ".purchased." + slot, 0);
    }

    private void addPersonalBoughtCount(Player p, String pointName, int slot) {
        FileConfiguration cfg = plugin.getPointManager().getPointConfig(pointName);
        int current = cfg.getInt(p.getUniqueId() + ".purchased." + slot, 0);
        cfg.set(p.getUniqueId() + ".purchased." + slot, current + 1);
        plugin.getPointManager().savePointConfig(pointName);
    }

    private ItemStack createGuiItem(Material m, String name, String lore) {
        ItemStack item = new ItemStack(m); ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name); meta.setLore(Collections.singletonList(lore));
        item.setItemMeta(meta); return item;
    }
}