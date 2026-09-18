package xyz.yourserver.duels.util;

import net.md_5.bungee.api.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

public class Msg {

    private static String prefix = "&8[&cDuels&8] &r";

    public static void init(FileConfiguration config) {
        prefix = config.getString("messages-prefix", prefix);
    }

    public static String color(String s) {
        return ChatColor.translateAlternateColorCodes('&', s);
    }

    public static void send(Player player, String message) {
        if (player == null || !player.isOnline()) return;
        player.sendMessage(color(prefix + message));
    }

    public static void broadcastTo(Iterable<Player> players, String message) {
        for (Player p : players) {
            send(p, message);
        }
    }
}
