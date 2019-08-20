package me.pgmann.adminpanel;

import com.google.common.collect.ImmutableList;
import me.pgmann.adminpanel.db.APDatabase;
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
    private AdminPanel inst;
    private APCommand commands;
    private APDatabase db;
    public static String rawPrefix = ChatColor.GOLD + "AdminPanel" + ChatColor.WHITE;
    public static String prefix = ChatColor.WHITE + "[" + rawPrefix + ChatColor.WHITE + "] ";
    static final List<Material> UNSAFE_BLOCK_MATERIALS = ImmutableList.of(Material.AIR, Material.WATER, Material.LAVA, Material.FIRE, Material.CAMPFIRE, Material.CACTUS);
    static HashMap<UUID, APPlayer> players = new HashMap<>();

    @Override
    public void onEnable() {
        inst = this;
        getServer().getConsoleSender().sendMessage(rawPrefix + " by " + ChatColor.GOLD + "pgmann" + ChatColor.WHITE + " is enabled!");

        // Copy/load default config
        saveDefaultConfig();

        // Register the DB manager
        db = new APDatabase(getConfig().getConfigurationSection("db"));

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
                // Check if each player has moved since last time the task ran
                // (array conversion prevents ConcurrentModificationException as the array is not modified)
                for (APPlayer player : players.values().toArray(new APPlayer[0])) {
                    if (player.afk) {
                        // Log event to DB
                        db.insertAfkEvent(inst, player.player.getUniqueId(), player.player.getName());
                        // Kick player from server
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
            player.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
        }
        getServer().getConsoleSender().sendMessage(rawPrefix + " is now disabled.");
    }

    /**
     * Manages the sidebar scoreboard for all online players,
     * making sure it is always showing the correct count
     */
    public static void updateScoreboards() {
        updateScoreboards(null);
    }

    /**
     * Manages the sidebar scoreboard for all online players,
     * making sure it is always showing the correct count
     *
     * @param leavingPlayer when not null the specified player will be removed from the online counts
     */
    public static void updateScoreboards(Player leavingPlayer) {
        for (APPlayer player : players.values()) player.updateScoreboard(leavingPlayer);
    }

    /**
     * Creates and displays a GUI to allow the player to pick a target to execute commands on.
     * A target is selected by the user clicking on a head in the inventory.
     *
     * @param player the user to show the inventory GUI to
     */
    public static void showTargetSelectorInventory(Player player) {
        showTargetSelectorInventory(player, 1);
    }

    /**
     * Creates and displays a GUI to allow the player to pick a target to execute commands on.
     * A target is selected by the user clicking on a head in the inventory.
     *
     * @param player the user to show the inventory GUI to
     * @param page the page number for the inventory
     */
    public static void showTargetSelectorInventory(Player player, int page) {
        // show the player a double chest full of player heads from online players
        Inventory inv = Bukkit.createInventory(null, 54, rawPrefix + ChatColor.DARK_GRAY + " - Target Selector");
        Player[] players = Bukkit.getOnlinePlayers().toArray(new Player[0]);
        // to test with 100 players: players = new Player[100]; Arrays.fill(players, player);
        int perPage = inv.getSize() - 9;
        int startI = (page - 1) * perPage;
        // display a page-ful of possible targets
        for (int i = startI; i < startI + perPage && i < players.length; i++) {
            Player target = players[i];
            ItemStack skull = new ItemStack(Material.PLAYER_HEAD, 1);
            SkullMeta meta = (SkullMeta) skull.getItemMeta();
            meta.setOwningPlayer(target);
            meta.setDisplayName(ChatColor.GRAY + "Manage " + ChatColor.WHITE + target.getDisplayName());
            meta.setLore(Collections.singletonList(target.getName()));
            skull.setItemMeta(meta);
            inv.addItem(skull);
        }
        // set pagination controls in bottom row of inventory
        boolean hasPrev = startI > 0;
        boolean hasNext = startI + perPage < players.length;
        for (int i = perPage; i < inv.getSize(); i++) {
            if (i == perPage && hasPrev)
                // Prev page
                inv.setItem(i, makeCustomItem(
                        Material.GREEN_STAINED_GLASS,
                        ChatColor.GREEN + "Previous Page",
                        ChatColor.GRAY + "Go to page " + (page - 1)
                ));
            else if (i == inv.getSize() - 1 && hasNext)
                // Next page
                inv.setItem(i, makeCustomItem(
                        Material.GREEN_STAINED_GLASS,
                        ChatColor.GREEN + "Next Page",
                        ChatColor.GRAY + "Go to page " + (page + 1)
                ));
            else
                inv.setItem(i, makeCustomItem(Material.WHITE_STAINED_GLASS, " "));
        }
        player.openInventory(inv);
    }

    /**
     * Converts an item stack into a player object assuming:
     * - the name of the target player is set as the lore text
     * - the target player is currently online
     *
     * @param targetSkull the item that has a player name as the lore text
     * @return a player if the target could be found, or else null
     */
    public static Player getTargetFromSkull(ItemStack targetSkull) {
        String targetName = targetSkull.getItemMeta().getLore().get(0);
        return Bukkit.getPlayerExact(targetName);
    }

    /**
     * A convenience helper for making an item for use in a GUI.
     *
     * @param material    the type of item to use
     * @param displayName the name of the item
     * @return the custom-made ItemStack ready for adding to an inventory
     */
    static ItemStack makeCustomItem(Material material, String displayName) {
        return makeCustomItem(material, displayName, (String) null);
    }

    /**
     * A convenience helper for making an item for use in a GUI.
     *
     * @param material    the type of item to use
     * @param displayName the name of the item
     * @param lore        the more detailed description text to display below the name
     * @return the custom-made ItemStack ready for adding to an inventory
     */
    static ItemStack makeCustomItem(Material material, String displayName, String lore) {
        return makeCustomItem(material, displayName, new String[]{lore});
    }

    /**
     * A convenience helper for making an item for use in a GUI.
     *
     * @param material    the type of item to use
     * @param displayName the name of the item
     * @param lore        the more detailed description text to display below the name
     * @return the custom-made ItemStack ready for adding to an inventory
     */
    static ItemStack makeCustomItem(Material material, String displayName, String[] lore) {
        ItemStack item = new ItemStack(material, 1);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(displayName);
        if (lore.length > 0 && lore[0] != null) meta.setLore(Arrays.asList(lore));
        item.setItemMeta(meta);
        return item;
    }

    /**
     * Creates and displays an inventory GUI to the player containing a list of admin actions,
     * represented as different wool colors based on the severity of the actions they take when clicked.
     *
     * @param player the player to show the inventory GUI to
     * @param target the player the action will be performed on
     */
    public static void showTargetActionsInventory(Player player, Player target) {
        if (target == null) {
            // target has left the game - refresh main GUI instead
            showTargetSelectorInventory(player);
            return;
        }

        // Show GUI Actions screen: click an item to perform an action on the target
        Inventory inv = Bukkit.createInventory(null, 9, rawPrefix + ChatColor.DARK_GRAY + " - Manage " + target.getName());
        // Teleport target to a random location
        inv.addItem(makeCustomItem(
                Material.LIGHT_BLUE_WOOL,
                ChatColor.BLUE + "Teleport " + ChatColor.WHITE + target.getDisplayName(),
                ChatColor.GRAY + "Target is moved to a safe random location"
        ));
        // Set target to survival/adventure mode
        if (target.getGameMode() == GameMode.ADVENTURE) {
            inv.addItem(makeCustomItem(
                    Material.LIME_WOOL,
                    ChatColor.GREEN + "Read-write mode " + ChatColor.WHITE + target.getDisplayName(),
                    ChatColor.GRAY + "Update target's gamemode to survival mode"
            ));
        } else {
            inv.addItem(makeCustomItem(
                    Material.YELLOW_WOOL,
                    ChatColor.YELLOW + "Read-only mode " + ChatColor.WHITE + target.getDisplayName(),
                    ChatColor.GRAY + "Update target's gamemode to adventure mode"
            ));
        }
        // Kick target
        inv.addItem(makeCustomItem(
                Material.ORANGE_WOOL,
                ChatColor.GOLD + "Kick " + ChatColor.WHITE + target.getDisplayName(),
                ChatColor.GRAY + "Disconnect the target from the server"
        ));
        // Kill target
        inv.addItem(makeCustomItem(
                Material.RED_WOOL,
                ChatColor.RED + "Kill " + ChatColor.WHITE + target.getDisplayName(),
                ChatColor.GRAY + "Cause the target to die"
        ));
        // Ban target
        inv.addItem(makeCustomItem(
                Material.BLACK_WOOL,
                ChatColor.DARK_RED + "Ban " + ChatColor.WHITE + target.getDisplayName(),
                ChatColor.GRAY + "Block the target from the server"
        ));
        player.openInventory(inv);
    }
}
