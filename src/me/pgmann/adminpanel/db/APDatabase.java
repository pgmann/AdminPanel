package me.pgmann.adminpanel.db;

import com.mysql.jdbc.exceptions.jdbc4.CommunicationsException;
import me.pgmann.adminpanel.AdminPanel;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.UUID;

public class APDatabase {
    private Connection connection;
    private String dbHost, dbName, dbUser, dbPass, dbAfkTable;
    private int dbPort;
    private boolean dbSSL, enabled;

    public APDatabase(ConfigurationSection dbConfig) {
        enabled = dbConfig.getBoolean("enabled");
        if(enabled) {
            dbHost = dbConfig.getString("host");
            dbPort = dbConfig.getInt("port");
            dbSSL = dbConfig.getBoolean("ssl");
            dbName = dbConfig.getString("name");
            dbUser = dbConfig.getString("user");
            dbPass = dbConfig.getString("pass");
            dbAfkTable = dbConfig.getString("afk_table");
        } else {
            Bukkit.getConsoleSender().sendMessage(AdminPanel.prefix + ChatColor.DARK_RED + "Database connection is disabled");
        }
    }

    private boolean openConnection() throws SQLException, ClassNotFoundException {
        if (!enabled) return false;
        if (connection != null && !connection.isClosed()) return true;

        try {
            Class.forName("com.mysql.jdbc.Driver");
            connection = DriverManager.getConnection("jdbc:mysql://" + dbHost + ":" + dbPort + "/" + dbName + "?useSSL=" + dbSSL, dbUser, dbPass);
            return true;
        } catch (CommunicationsException ex) {
            Bukkit.getConsoleSender().sendMessage(AdminPanel.prefix + ChatColor.DARK_RED + "Connection to database failed: " + ChatColor.RED + ex.getCause().getMessage());
            return false;
        }
    }

    public void insertAfkEvent(Plugin plugin, UUID uuid, String name) {
        new BukkitRunnable() {
            @Override
            public void run() {
                try {
                    if(!openConnection()) return;
                    PreparedStatement statement = connection.prepareStatement("INSERT INTO " + dbAfkTable + " VALUES(?, ?, ?)");
                    statement.setString(1, uuid.toString());
                    statement.setString(2, name);
                    statement.setTimestamp(3, Timestamp.valueOf(LocalDateTime.now()));
                    statement.executeUpdate();
                } catch (ClassNotFoundException | SQLException e) {
                    e.printStackTrace();
                }
            }
        }.runTaskAsynchronously(plugin);
    }
}
