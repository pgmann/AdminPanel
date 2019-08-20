package me.pgmann.adminpanel;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.*;

public class APPlayer {
    Player player;
    Scoreboard board;
    boolean afk;

    protected APPlayer(Player player) {
        this.player = player;

        // init scoreboard
        ScoreboardManager manager = Bukkit.getScoreboardManager();
        board = manager.getNewScoreboard();
        Objective objective = board.registerNewObjective("adminpanel", "dummy", ChatColor.WHITE + "" + ChatColor.BOLD + player.getDisplayName());
        objective.setDisplaySlot(DisplaySlot.SIDEBAR);
    }

    /**
     * Manages the sidebar scoreboard for the player, making sure it is
     * always showing the correct count
     */
    protected void updateScoreboard() {
        updateScoreboard(null);
    }

    /**
     * Manages the sidebar scoreboard for the player, making sure it is
     * always showing the correct count
     * @param leavingPlayer when not null the specified player will be removed from the online counts
     */
    protected void updateScoreboard(Player leavingPlayer) {
        if (!player.equals(leavingPlayer) && player.hasPermission("adminpanel.sidebar")) {
            // update 'scores' (online counts) and display name on sidebar
            Objective objective = board.getObjective("adminpanel");
            objective.setDisplayName(player.getDisplayName());
            Score onlinePlayers = objective.getScore(ChatColor.GREEN + "Online Players");
            onlinePlayers.setScore(Bukkit.getOnlinePlayers().size() - (leavingPlayer != null ? 1 : 0));
            Score onlineAdmins = objective.getScore(ChatColor.RED + "Online Admins");
            int adminsCount = 0;
            for (Player player : Bukkit.getOnlinePlayers())
                if (!player.equals(leavingPlayer) && player.hasPermission("adminpanel.admin")) adminsCount++;
            onlineAdmins.setScore(adminsCount);
            player.setScoreboard(board);
        } else {
            // remove this plugin's scoreboard - use the main vanilla one instead
            player.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
        }
    }
}
