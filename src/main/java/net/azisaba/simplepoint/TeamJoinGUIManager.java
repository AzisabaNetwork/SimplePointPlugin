package net.azisaba.simplepoint;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

public class TeamJoinGUIManager implements Listener {
    private final SimplePointPlugin plugin;

    public TeamJoinGUIManager(SimplePointPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        File rewardDir = new File(plugin.getDataFolder(), "teams/reward");
        File[] files = rewardDir.listFiles();
        if (files == null) return;

        for (File f : files) {
            FileConfiguration cfg = YamlConfiguration.loadConfiguration(f);
            if (cfg.getBoolean("gui.auto_show", false)) {
                String group = f.getName().replace(".yml", "");
                if (plugin.getTeamManager().getPlayerTeam(player.getUniqueId()) == null) {
                    Bukkit.getScheduler().runTaskLater(plugin, () -> openJoinGUI(player, group), 20L);
                    break;
                }
            }
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        String title = event.getView().getTitle();
        if (!title.startsWith("§0Team Join: ")) return;

        event.setCancelled(true); // アイテム取得を防止

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) return;

        Player player = (Player) event.getWhoClicked();
        String group = title.replace("§0Team Join: ", "");

        FileConfiguration cfg = YamlConfiguration.loadConfiguration(plugin.getTeamManager().getRewardFile(group));
        String t1 = cfg.getString("gui.team1");
        String t2 = cfg.getString("gui.team2");

        String selectedTeam = null;
        int slot = event.getRawSlot();

        if (slot == 11) selectedTeam = t1;
        else if (slot == 15) selectedTeam = t2;
        else if (slot == 13 && clicked.getType() == Material.NETHER_STAR) {
            selectedTeam = determineRandomTeam(group, t1, t2);
        }

        if (selectedTeam != null) {
            plugin.getTeamManager().addMember(group, selectedTeam, player.getUniqueId());
            player.sendMessage("§a§l[!] §fチーム §l" + selectedTeam + " §fに参加しました！");
            player.closeInventory();
        }
    }

    public void handleGuiClick(Player player, String group, String teamId) {
        String currentTeam = plugin.getTeamManager().getPlayerTeam(player.getUniqueId());
        String currentGroup = plugin.getTeamManager().getPlayerGroup(player.getUniqueId());

        if (currentGroup != null && currentGroup.equals(group)) {
            if (teamId.equals(currentTeam)) {
                player.sendMessage("§c[!] 既にこのチームに参加しています。");
            } else {
                player.sendMessage("§c[!] 既にこのグループの別のチームに所属しています。");
            }
            player.closeInventory();
            return;
        }

        // 参加処理...
        plugin.getTeamManager().addMember(group, teamId, player.getUniqueId());
        player.sendMessage("§a[!] " + teamId + " に参加しました！");
        player.closeInventory();
    }

    public void openJoinGUI(Player player, String group) {
        if (plugin.getTeamManager().getPlayerTeam(player.getUniqueId()) != null) {
            player.sendMessage("§c既に参加しています。");
            return;
        }

        FileConfiguration cfg = YamlConfiguration.loadConfiguration(plugin.getTeamManager().getRewardFile(group));
        String mode = cfg.getString("gui.mode", "choice");
        String t1 = cfg.getString("gui.team1");
        String t2 = cfg.getString("gui.team2");

        if (t1 == null || t2 == null) return;

        Inventory inv = Bukkit.createInventory(null, 27, "§0Team Join: " + group);
        if (mode.equalsIgnoreCase("choice")) {
            inv.setItem(11, createGuiItem(Material.BLUE_BANNER, "§b§l" + t1 + " §fに参加", "§7クリックして加入"));
            inv.setItem(15, createGuiItem(Material.RED_BANNER, "§c§l" + t2 + " §fに参加", "§7クリックして加入"));
        } else {
            inv.setItem(13, createGuiItem(Material.NETHER_STAR, "§f§lランダム参加", "§7統計的に均等なチームへ割り振られます"));
        }
        player.openInventory(inv);
    }

    public String determineRandomTeam(String group, String t1, String t2) {
        int n1 = plugin.getTeamManager().getMemberNames(group, t1).size();
        int n2 = plugin.getTeamManager().getMemberNames(group, t2).size();
        int total = n1 + n2;

        if (total >= 10) {
            double p = 0.5, expected = total * p, sd = Math.sqrt(total * p * (1 - p));
            double z = (Math.abs(n1 - expected) - 0.5) / sd;
            if (z > 1.96) return (n1 > n2) ? t2 : t1;
        }
        return (Math.random() < 0.5) ? t1 : t2;
    }

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
}