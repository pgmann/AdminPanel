package me.pgmann.adminpanel;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;

public class APCommand implements CommandExecutor {
    APCommand() { }

    /**
     * This command is used to open the admin GUI window.<br>
     * Usage: /panel
     *
     * @param sender  The command sender
     * @param command The executed command
     * @param label   The alias used for this command
     * @param args    The arguments given to the command
     */
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (sender instanceof Player) {
            Player player = (Player) sender;
            if(player.hasPermission("adminpanel.gui")) {
                AdminPanel.showTargetSelectorInventory(player);
            } else {
                player.sendMessage(AdminPanel.prefix + ChatColor.RED + "You don't have permission.");
            }
        } else {
            sender.sendMessage(AdminPanel.prefix + ChatColor.RED + "You must be a player to execute this command.");
        }
        return true;
    }
}
