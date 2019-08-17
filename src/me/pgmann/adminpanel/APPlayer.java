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

    protected void updateScoreboard() {
        if (player.hasPermission("adminpanel.sidebar")) {
            Objective objective = board.getObjective("adminpanel");
            objective.setDisplayName(player.getDisplayName());
            Score onlinePlayers = objective.getScore(ChatColor.GREEN + "Online Players");
            onlinePlayers.setScore(Bukkit.getOnlinePlayers().size());
            Score onlineAdmins = objective.getScore(ChatColor.RED + "Online Admins");
            int adminsCount = 0;
            for (Player player : Bukkit.getOnlinePlayers()) if (player.hasPermission("adminpanel.admin")) adminsCount++;
            onlineAdmins.setScore(adminsCount);
            player.setScoreboard(board);
        } else {
            player.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
        }
    }
}
