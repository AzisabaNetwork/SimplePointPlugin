package net.azisaba.simplepoint.managers;

import net.azisaba.simplepoint.SimplePointPlugin;
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

public class TeamJoinGUIManager implements Listener {
    private final SimplePointPlugin plugin;

    public TeamJoinGUIManager(SimplePointPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        // ログイン1秒後に、auto_showが有効なグループをチェック
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            File rewardDir = new File(plugin.getDataFolder(), "teams/reward");
            if (!rewardDir.exists()) return;

            for (File f : rewardDir.listFiles(f -> f.getName().endsWith(".yml"))) {
                String groupId = f.getName().replace(".yml", "");
                FileConfiguration cfg = YamlConfiguration.loadConfiguration(f);

                // 【修正】「その特定のグループ」に未参加の場合のみGUIを表示
                if (cfg.getBoolean("gui.auto_show", false)) {
                    if (plugin.getTeamManager().getPlayerTeamInGroup(player.getUniqueId(), groupId) == null) {
                        openJoinGUI(player, groupId);
                        break; // 1つ表示したら終了
                    }
                }
            }
        }, 20L);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        String title = event.getView().getTitle();
        if (!ChatColor.stripColor(title).contains("Team Join:")) return;

        event.setCancelled(true);
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) return;

        Player player = (Player) event.getWhoClicked();

        // GUIタイトルからグループIDを特定
        String groupId = null;
        File rewardDir = new File(plugin.getDataFolder(), "teams/reward");
        for (File f : rewardDir.listFiles(f -> f.getName().endsWith(".yml"))) {
            String id = f.getName().replace(".yml", "");
            if (ChatColor.stripColor(title).contains(ChatColor.stripColor(getGroupDisplayName(id)))) {
                groupId = id;
                break;
            }
        }
        if (groupId == null) return;

        FileConfiguration cfg = YamlConfiguration.loadConfiguration(new File(rewardDir, groupId + ".yml"));
        String t1 = cfg.getString("gui.team1");
        String t2 = cfg.getString("gui.team2");

        int slot = event.getRawSlot();
        if (slot == 11) handleGuiClick(player, groupId, t1);
        else if (slot == 15) handleGuiClick(player, groupId, t2);
        else if (slot == 13 && clicked.getType() == Material.NETHER_STAR) {
            handleGuiClick(player, groupId, determineRandomTeam(groupId, t1, t2));
        }
    }

    public void handleGuiClick(Player player, String group, String teamId) {
        // 【重要】そのグループに既に所属していないかチェック
        if (plugin.getTeamManager().getPlayerTeamInGroup(player.getUniqueId(), group) != null) {
            player.sendMessage("§c§l[!] §7あなたは既にこのグループのチームに参加しています。");
            player.closeInventory();
            return;
        }

        plugin.getTeamManager().addMember(group, teamId, player.getUniqueId());
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.2f);
        player.sendMessage("§a§l[!] §fグループ §b" + group + " §fのチーム §l" + teamId + " §fに参加しました！");
        player.closeInventory();
    }

    public void openJoinGUI(Player player, String group) {
        // すでにそのグループに入っているなら開かない
        if (plugin.getTeamManager().getPlayerTeamInGroup(player.getUniqueId(), group) != null) {
            player.sendMessage("§c既にこのグループには参加済みです。");
            return;
        }

        File file = new File(plugin.getDataFolder(), "teams/reward/" + group + ".yml");
        if (!file.exists()) return;
        FileConfiguration cfg = YamlConfiguration.loadConfiguration(file);

        String t1 = cfg.getString("gui.team1");
        String t2 = cfg.getString("gui.team2");
        if (t1 == null || t2 == null) return;

        Inventory inv = Bukkit.createInventory(null, 27, "§0Team Join: " + getGroupDisplayName(group));
        if (cfg.getString("gui.mode", "choice").equalsIgnoreCase("choice")) {
            inv.setItem(11, createGuiItem(Material.BLUE_BANNER, "§b§l" + t1 + " §fに参加"));
            inv.setItem(15, createGuiItem(Material.RED_BANNER, "§c§l" + t2 + " §fに参加"));
        } else {
            inv.setItem(13, createGuiItem(Material.NETHER_STAR, "§f§lランダム参加", "§7どちらかのチームへ自動で割り振られます"));
        }
        player.openInventory(inv);
    }

    public void setGuiSettings(String group, String t1, String t2, String mode, boolean autoShow) {
        File file = new File(plugin.getDataFolder(), "teams/reward/" + group + ".yml");
        FileConfiguration cfg = YamlConfiguration.loadConfiguration(file);
        cfg.set("gui.team1", t1);
        cfg.set("gui.team2", t2);
        cfg.set("gui.mode", mode);
        cfg.set("gui.auto_show", autoShow);
        try { cfg.save(file); } catch (IOException e) { e.printStackTrace(); }
    }

    private String getGroupDisplayName(String group) {
        File file = new File(plugin.getDataFolder(), "teams/reward/" + group + ".yml");
        if (!file.exists()) return group;
        return ChatColor.translateAlternateColorCodes('&', YamlConfiguration.loadConfiguration(file).getString("display_name", group));
    }

    public String determineRandomTeam(String group, String t1, String t2) {
        int n1 = plugin.getTeamManager().getMemberNames(group, t1).size();
        int n2 = plugin.getTeamManager().getMemberNames(group, t2).size();
        return (n1 <= n2) ? t1 : t2;
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