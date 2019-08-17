package me.pgmann.adminpanel;

import com.google.common.collect.ImmutableList;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

public class AdminPanel extends JavaPlugin {
    private APCommand commands;
    static String rawPrefix = ChatColor.GOLD + "AdminPanel" + ChatColor.WHITE;
    static String prefix = ChatColor.WHITE + "[" + rawPrefix + ChatColor.WHITE + "] ";
    static final List<Material> UNSAFE_BLOCK_MATERIALS = ImmutableList.of(Material.AIR, Material.WATER, Material.LAVA, Material.FIRE, Material.CAMPFIRE, Material.CACTUS);
    static HashMap<UUID, APPlayer> players = new HashMap<>();

    @Override
    public void onEnable() {
        getServer().getConsoleSender().sendMessage(rawPrefix + " by " + ChatColor.GOLD + "pgmann" + ChatColor.WHITE + " is enabled!");

        // Register the command listener
        commands = new APCommand();
        getCommand("panel").setExecutor(commands);
        getCommand("ap").setExecutor(commands);

        // Register the event listener
        getServer().getPluginManager().registerEvents(new APListener(), this);

        // Handle already logged-in players
        for (Player player : Bukkit.getOnlinePlayers()) {
            players.put(player.getUniqueId(), new APPlayer(player));
        }
        updateScoreboards();

        // Set the AFK timer scheduled task
        new BukkitRunnable() {
            @Override
            public void run() {
                for(APPlayer player : players.values()) {
                    if(player.afk) {
                        // TODO log to DB
                        player.player.kickPlayer("Kicked for being AFK");
                    }
                    player.afk = true; // player is afk until proven otherwise
                }
            }
        }.runTaskTimer(this, 0, 2400); // 120s * 20tps = 2400ticks
    }

    @Override
    public void onDisable() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.hasPermission("adminpanel.sidebar"))
                player.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
        }
        getServer().getConsoleSender().sendMessage(rawPrefix + " is now disabled.");
    }

    public static void updateScoreboards() {
        for (APPlayer player : players.values()) player.updateScoreboard();
    }

    public static void showTargetSelectorInventory(Player player) {
        // show the player a double chest full of player heads from online players
        Inventory inv = Bukkit.createInventory(null, 54, AdminPanel.rawPrefix + ChatColor.DARK_GRAY + " - Target Selector");
        Player[] players = Bukkit.getOnlinePlayers().toArray(new Player[0]);
        for (int i = 0; i < 45 && i < players.length; i++) {
            Player target = players[i];
            ItemStack skull = new ItemStack(Material.PLAYER_HEAD, 1);
            SkullMeta meta = (SkullMeta) skull.getItemMeta();
            meta.setOwningPlayer(target);
            meta.setDisplayName(ChatColor.GRAY + "Manage " + ChatColor.WHITE + target.getDisplayName());
            meta.setLore(Collections.singletonList(target.getName()));
            skull.setItemMeta(meta);
            inv.addItem(skull);
        }
        player.openInventory(inv);
    }

    private static Player getTargetFromSkull(ItemStack targetSkull) {
        // TODO: ensure skull without lore isn't in bottom inv
        String targetName = targetSkull.getItemMeta().getLore().get(0);
        return Bukkit.getPlayerExact(targetName);
    }

    static ItemStack makeCustomItem(Material material, String displayName) {
        return makeCustomItem(material, displayName, (String) null);
    }

    static ItemStack makeCustomItem(Material material, String displayName, String lore) {
        return makeCustomItem(material, displayName, new String[]{lore});
    }

    static ItemStack makeCustomItem(Material material, String displayName, String[] lore) {
        ItemStack item = new ItemStack(material, 1);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(displayName);
        if (lore != null) meta.setLore(Arrays.asList(lore));
        item.setItemMeta(meta);
        return item;
    }

    public static void showTargetActionsInventory(Player player, ItemStack targetSkull) {
        Player target = getTargetFromSkull(targetSkull);
        if (target == null) {
            // target has left the game - refresh main GUI instead
            AdminPanel.showTargetSelectorInventory(player);
            return;
        }

        // Show GUI Actions screen: click an item to perform an action on the target
        Inventory inv = Bukkit.createInventory(null, 9, AdminPanel.rawPrefix + ChatColor.DARK_GRAY + " - Manage " + target.getName());
        // Teleport target to a random location
        inv.addItem(AdminPanel.makeCustomItem(
                Material.LIGHT_BLUE_WOOL,
                ChatColor.BLUE + "Teleport " + ChatColor.WHITE + target.getDisplayName(),
                ChatColor.GRAY + "Target is moved to a safe random location"
        ));
        // Set target to survival/adventure mode
        if (target.getGameMode() == GameMode.ADVENTURE) {
            inv.addItem(AdminPanel.makeCustomItem(
                    Material.LIME_WOOL,
                    ChatColor.GREEN + "Read-write mode " + ChatColor.WHITE + target.getDisplayName(),
                    ChatColor.GRAY + "Update target's gamemode to survival mode"
            ));
        } else {
            inv.addItem(AdminPanel.makeCustomItem(
                    Material.YELLOW_WOOL,
                    ChatColor.YELLOW + "Read-only mode " + ChatColor.WHITE + target.getDisplayName(),
                    ChatColor.GRAY + "Update target's gamemode to adventure mode"
            ));
        }
        // Kick target
        inv.addItem(AdminPanel.makeCustomItem(
                Material.ORANGE_WOOL,
                ChatColor.GOLD + "Kick " + ChatColor.WHITE + target.getDisplayName(),
                ChatColor.GRAY + "Disconnect the target from the server"
        ));
        // Kill target
        inv.addItem(AdminPanel.makeCustomItem(
                Material.RED_WOOL,
                ChatColor.RED + "Kill " + ChatColor.WHITE + target.getDisplayName(),
                ChatColor.GRAY + "Cause the target to die"
        ));
        // Ban target
        inv.addItem(AdminPanel.makeCustomItem(
                Material.BLACK_WOOL,
                ChatColor.DARK_RED + "Ban " + ChatColor.WHITE + target.getDisplayName(),
                ChatColor.GRAY + "Block the target from the server"
        ));
        player.openInventory(inv);
    }
}
