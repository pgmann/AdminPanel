# AdminPanel

An admin GUI panel plugin with lots of useful commands to help you manage your Minecraft server.

## Features

![Admin Panel Command GUI](https://raw.githubusercontent.com/pgmann/AdminPanel/master/screenshots/2.png)
* Manage your players using the handy GUI interface - open it with the `/panel` command
    * Requires `adminpanel.command` permission to use (default: op)
    * Actions are color coded from least severe to most severe:
        * Blue: Teleport a player to a safe random location (within 10,000 blocks of the spawn point)
        * Yellow: Control whether a player can break/place blocks by setting them to adventure mode
        * Orange: Kick a player
        * Red: Kill a player
        * Black: Ban a player

![Online stats sidebar](https://raw.githubusercontent.com/pgmann/AdminPanel/master/screenshots/3.png)
* Get a side panel that displays current online player and online admin count
    * Requires `adminpanel.sidebar` permission to see sidebar (default: op)
    * Requires `adminpanel.admin` to be counted in the 'online admins' count (default: op)
* Save server resources by kicking inactive players. Players who don't move for 2 minutes will be kicked from the server.
    * This can be logged to a MySQL DB for auditing purposes.

## Screenshots

There are more screenshots available in the [screenshots folder](https://github.com/pgmann/AdminPanel/tree/master/screenshots).

## Installing the plugin

Requires Spigot v1.14 or above. Older versions are not supported.

Basic installation involves dragging the plugin `AdminPanel.jar` into your server's `plugins` folder and restarting/reloading.

All permissions are granted to server operators by default.
See the previous section for a list of all individual permission nodes.

### DB Setup

The database connection is used to log AFK kick events for auditing purposes.
If you are interested in enabling this feature, follow the steps below:

1. Set up a MySQL database.
2. Create the tables in your database:
    ```mysql
    CREATE TABLE afk_kick_log (
    uuid VARCHAR(36),
       name VARCHAR(20),
       time DATETIME
    );
    ```
3. Populate `config.yml` with your host, port, table name, username and password.
4. If you wish to enable SSL make sure your SQL server's certificate is trusted in your keystore.
