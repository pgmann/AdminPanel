package me.pgmann.adminpanel;

import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Random;

public class APListener implements Listener {
    @EventHandler(priority = EventPriority.HIGHEST)
    protected void onPlayerJoin(final PlayerJoinEvent e) {
        AdminPanel.players.put(e.getPlayer().getUniqueId(), new APPlayer(e.getPlayer()));
        AdminPanel.updateScoreboards();
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    protected void onPlayerQuit(final PlayerQuitEvent e) {
        AdminPanel.updateScoreboards(e.getPlayer());
        AdminPanel.players.remove(e.getPlayer().getUniqueId());
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    protected void onPlayerMove(final PlayerMoveEvent e) {
        AdminPanel.players.get(e.getPlayer().getUniqueId()).afk = false;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    protected void onInventoryClick(final InventoryClickEvent e) {
        // Ensure the event is intended for this plugin
        if (!(e.getWhoClicked() instanceof Player)) return;
        Player player = (Player) e.getWhoClicked();
        if (!e.getView().getTitle().startsWith(AdminPanel.rawPrefix)) return;
        if (e.getCurrentItem() == null) return;
        if (!e.getClickedInventory().equals(e.getView().getTopInventory())) {
            // bottom inventory has been clicked - cancel and ignore
            e.setCancelled(true);
            return;
        }

        // Determine the action to perform based on the item type
        ItemStack itemStack = e.getCurrentItem();
        if (itemStack.getType() == Material.GREEN_STAINED_GLASS) {
            // Target selector pagination
            String[] lore = itemStack.getItemMeta().getLore().get(0).split(" ");
            int page = Integer.parseInt(lore[lore.length - 1]);
            AdminPanel.showTargetSelectorInventory(player, page);
        } else if (itemStack.getItemMeta().getDisplayName().equals(" ")) {
            // Target selector divider, ignore
        } else if (itemStack.getType() == Material.PLAYER_HEAD) {
            // Target selector GUI
            AdminPanel.showTargetActionsInventory(player, AdminPanel.getTargetFromSkull(itemStack));
        } else {
            // Target actions GUI
            // get target name from end of inventory title (4th word to end of string)
            String[] title = e.getView().getTitle().split(" ", 4);
            String targetName = title[title.length - 1];
            Player target = Bukkit.getPlayerExact(targetName);
            if (target == null) {
                player.sendMessage(AdminPanel.prefix + ChatColor.WHITE + targetName + ChatColor.RED + " is no longer online!");
            } else {
                switch (itemStack.getType()) {
                    case LIGHT_BLUE_WOOL:
                        // random tp within 10,000 blocks of the spawn point
                        World world = target.getWorld();
                        Location spawn = world.getSpawnLocation();
                        int minX = spawn.getBlockX() - 10000, maxX = spawn.getBlockX() + 10000;
                        int minZ = spawn.getBlockZ() - 10000, maxZ = spawn.getBlockZ() + 10000;
                        Location loc;
                        Random random = new Random();
                        do {
                            // Find a solid block for the player to land on - liquids are not very safe (especially lava)
                            int x = random.nextInt(maxX - minX) + minX + 1;
                            int z = random.nextInt(maxZ - minZ) + minZ + 1;
                            loc = new Location(world, x - 0.5, world.getHighestBlockYAt(x, z), z - 0.5); // gets block above the highest block
                        } while (AdminPanel.UNSAFE_BLOCK_MATERIALS.contains(loc.clone().add(0, -1, 0).getBlock().getType()));
                        target.teleport(loc);
                        player.sendMessage(AdminPanel.prefix + ChatColor.WHITE + target.getDisplayName() + ChatColor.BLUE + " has been teleported to " + loc.getX() + ", " + loc.getY() + ", " + loc.getZ());
                        break;
                    case LIME_WOOL:
                        // disable read-only mode (set to survival mode)
                        target.setGameMode(GameMode.SURVIVAL);
                        player.sendMessage(AdminPanel.prefix + ChatColor.WHITE + target.getDisplayName() + ChatColor.GREEN + " is now in read-write mode");
                        break;
                    case YELLOW_WOOL:
                        // enable read-only mode (set to adventure mode)
                        target.setGameMode(GameMode.ADVENTURE);
                        player.sendMessage(AdminPanel.prefix + ChatColor.WHITE + target.getDisplayName() + ChatColor.YELLOW + " is now in read-only mode");
                        break;
                    case ORANGE_WOOL:
                        target.kickPlayer("Kicked by an admin");
                        player.sendMessage(AdminPanel.prefix + ChatColor.WHITE + target.getDisplayName() + ChatColor.GOLD + " has been kicked from the server");
                        break;
                    case RED_WOOL:
                        target.setHealth(0);
                        player.sendMessage(AdminPanel.prefix + ChatColor.WHITE + target.getDisplayName() + ChatColor.RED + " has been killed");
                        break;
                    case BLACK_WOOL:
                        Bukkit.getBanList(BanList.Type.NAME).addBan(target.getName(), null, null, player.getName());
                        target.kickPlayer("Banned from server");
                        player.sendMessage(AdminPanel.prefix + ChatColor.WHITE + target.getDisplayName() + ChatColor.DARK_RED + " has been banned");
                        break;
                }
                player.closeInventory();
            }
        }
        e.setCancelled(true); // prevent the item being removed from the inventory
    }
}
