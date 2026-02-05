package net.azisaba.simplepoint;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
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
import java.util.UUID;

public class TeamJoinGUIManager implements Listener {
    private final SimplePointPlugin plugin;

    public TeamJoinGUIManager(SimplePointPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        // 所属チェック（全走査）
        String currentTeam = plugin.getTeamManager().getPlayerTeam(player.getUniqueId());
        if (currentTeam != null) return;

        File rewardDir = new File(plugin.getDataFolder(), "teams/reward");
        File[] files = rewardDir.listFiles();
        if (files == null) return;

        for (File f : files) {
            FileConfiguration cfg = YamlConfiguration.loadConfiguration(f);
            if (cfg.getBoolean("gui.auto_show", false)) {
                String group = f.getName().replace(".yml", "");
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    if (plugin.getTeamManager().getPlayerTeam(player.getUniqueId()) == null) {
                        openJoinGUI(player, group);
                    }
                }, 20L);
                break;
            }
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        String title = event.getView().getTitle();
        if (!title.contains("Team Join:")) return;

        event.setCancelled(true);
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) return;

        Player player = (Player) event.getWhoClicked();
        String groupId = null;
        File rewardDir = new File(plugin.getDataFolder(), "teams/reward");
        File[] files = rewardDir.listFiles();
        if (files != null) {
            for (File f : files) {
                String id = f.getName().replace(".yml", "");
                String dName = ChatColor.stripColor(getGroupDisplayName(id));
                if (ChatColor.stripColor(title).contains(dName)) {
                    groupId = id;
                    break;
                }
            }
        }

        if (groupId == null) return;

        FileConfiguration cfg = YamlConfiguration.loadConfiguration(plugin.getTeamManager().getRewardFile(groupId));
        String t1 = cfg.getString("gui.team1");
        String t2 = cfg.getString("gui.team2");

        String selectedTeam = null;
        int slot = event.getRawSlot();

        if (slot == 11) selectedTeam = t1;
        else if (slot == 15) selectedTeam = t2;
        else if (slot == 13 && clicked.getType() == Material.NETHER_STAR) {
            selectedTeam = determineRandomTeam(groupId, t1, t2);
        }

        if (selectedTeam != null) {
            handleGuiClick(player, groupId, selectedTeam);
        }
    }

    public void handleGuiClick(Player player, String group, String teamId) {
        UUID uuid = player.getUniqueId();
        if (plugin.getTeamManager().getPlayerTeam(uuid) != null) {
            player.sendMessage("§c§l[!] §7エラー：既にチームに参加しています。");
            player.closeInventory();
            return;
        }

        plugin.getTeamManager().addMember(group, teamId, uuid);
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.2f);
        player.sendMessage("§a§l[!] §fチーム §b" + teamId + " §fに参加しました！");
        player.closeInventory();
    }

    public void openJoinGUI(Player player, String group) {
        if (plugin.getTeamManager().getPlayerTeam(player.getUniqueId()) != null) {
            player.sendMessage("§c既にチームに参加しているため、参加GUIは開けません。");
            return;
        }

        FileConfiguration cfg = YamlConfiguration.loadConfiguration(plugin.getTeamManager().getRewardFile(group));
        String mode = cfg.getString("gui.mode", "choice");
        String t1 = cfg.getString("gui.team1");
        String t2 = cfg.getString("gui.team2");
        String groupDisplayName = getGroupDisplayName(group);

        if (t1 == null || t2 == null) return;

        Inventory inv = Bukkit.createInventory(null, 27, "§0Team Join: " + groupDisplayName);
        if (mode.equalsIgnoreCase("choice")) {
            inv.setItem(11, createGuiItem(Material.BLUE_BANNER, "§b§l" + t1 + " §fに参加", "§7クリックして加入"));
            inv.setItem(15, createGuiItem(Material.RED_BANNER, "§c§l" + t2 + " §fに参加", "§7クリックして加入"));
        } else {
            inv.setItem(13, createGuiItem(Material.NETHER_STAR, "§f§lランダム参加", "§7均等なチームへ自動で割り振られます"));
        }
        player.openInventory(inv);
    }

    private String getGroupDisplayName(String group) {
        File file = plugin.getTeamManager().getRewardFile(group);
        if (!file.exists()) return group;
        FileConfiguration cfg = YamlConfiguration.loadConfiguration(file);
        String name = cfg.getString("display_name", group);
        return ChatColor.translateAlternateColorCodes('&', name);
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

    public String determineRandomTeam(String group, String t1, String t2) {
        int n1 = plugin.getTeamManager().getMemberNames(group, t1).size();
        int n2 = plugin.getTeamManager().getMemberNames(group, t2).size();
        if (n1 == n2) return Math.random() < 0.5 ? t1 : t2;
        return (n1 < n2) ? t1 : t2;
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