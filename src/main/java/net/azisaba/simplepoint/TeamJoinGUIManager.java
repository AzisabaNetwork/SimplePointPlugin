package net.azisaba.simplepoint;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class TeamJoinGUIManager implements org.bukkit.event.Listener {

    @org.bukkit.event.EventHandler
    public void onPlayerJoin(org.bukkit.event.player.PlayerJoinEvent event) {
    }
    private final SimplePointPlugin plugin;

    public TeamJoinGUIManager(SimplePointPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * チーム参加GUIを開く
     */
    public void openJoinGUI(Player player, String group) {
        // 既に参加しているかチェック
        if (plugin.getTeamManager().getPlayerTeam(player.getUniqueId()) != null) {
            player.sendMessage("§c既にいずれかのチームに参加しています。");
            return;
        }

        FileConfiguration cfg = YamlConfiguration.loadConfiguration(plugin.getTeamManager().getRewardFile(group));
        String mode = cfg.getString("gui.mode", "choice");
        String t1 = cfg.getString("gui.team1");
        String t2 = cfg.getString("gui.team2");

        if (t1 == null || t2 == null) {
            player.sendMessage("§cGUI設定が完了していません。");
            return;
        }

        Inventory inv = Bukkit.createInventory(null, 27, "§0Team Join: " + group);

        if (mode.equalsIgnoreCase("choice")) {
            // 選択モード: 2つのバナーを表示
            inv.setItem(11, createGuiItem(Material.BLUE_BANNER, "§b§l" + t1 + " §fに参加", "§7クリックしてこのチームに所属します"));
            inv.setItem(15, createGuiItem(Material.RED_BANNER, "§c§l" + t2 + " §fに参加", "§7クリックしてこのチームに所属します"));
        } else {
            // ランダムモード: 中央にスターを表示
            inv.setItem(13, createGuiItem(Material.NETHER_STAR, "§f§l運命に任せる (ランダム参加)"));
                    //"§7統計的に均等なチームへ割り振られます", "§8(95%信頼区間に基づく補正あり)"));
        }

        player.openInventory(inv);
    }

    /**
     * 統計学的なチーム割り振り (Z検定 95%信頼区間)
     */
    public String determineRandomTeam(String group, String t1, String t2) {
        int n1 = plugin.getTeamManager().getMemberNames(group, t1).size();
        int n2 = plugin.getTeamManager().getMemberNames(group, t2).size();
        int total = n1 + n2;

        if (total >= 10) { // サンプル数が少ないうちは単純ランダム
            double p = 0.5;
            double expected = total * p;
            double sd = Math.sqrt(total * p * (1 - p));
            // Z値の計算 (連続性補正あり)
            double z = (Math.abs(n1 - expected) - 0.5) / sd;

            // Z > 1.96 (有意水準5%) なら、人数が少ない方を強制選択
            if (z > 1.96) {
                return (n1 > n2) ? t2 : t1;
            }
        }
        // 偏りがない、またはサンプル不足なら 50:50
        return (Math.random() < 0.5) ? t1 : t2;
    }

    /**
     * GUI設定の保存
     */
    public void setGuiSettings(String group, String t1, String t2, String mode, boolean autoShow) {
        File file = plugin.getTeamManager().getRewardFile(group);
        FileConfiguration cfg = YamlConfiguration.loadConfiguration(file);
        cfg.set("gui.team1", t1);
        cfg.set("gui.team2", t2);
        cfg.set("gui.mode", mode);
        cfg.set("gui.auto_show", autoShow);
        try { cfg.save(file); } catch (IOException e) { e.printStackTrace(); }
    }

    private ItemStack createGuiItem(Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            meta.setLore(Arrays.asList(lore));
            item.setItemMeta(meta);
        }
        return item;
    }
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!event.getView().getTitle().startsWith("§0Team Join: ")) return;
        event.setCancelled(true);

        Player player = (Player) event.getWhoClicked();
        String group = event.getView().getTitle().replace("§0Team Join: ", "");

        FileConfiguration cfg = YamlConfiguration.loadConfiguration(plugin.getTeamManager().getRewardFile(group));
        String t1 = cfg.getString("gui.team1");
        String t2 = cfg.getString("gui.team2");

        String selectedTeam = null;
        int slot = event.getRawSlot();

        if (slot == 11) selectedTeam = t1; // 青バナー
        else if (slot == 15) selectedTeam = t2; // 赤バナー
        else if (slot == 13 && event.getCurrentItem().getType() == Material.NETHER_STAR) {
            // 統計的なランダム割り振り
            selectedTeam = determineRandomTeam(group, t1, t2);
        }

        if (selectedTeam != null) {
            plugin.getTeamManager().addMember(group, selectedTeam, player.getUniqueId());
            player.sendMessage("§a§l[!] §fチーム §l" + selectedTeam + " §fに参加しました！");
            player.closeInventory();
        }
    }
}