package xyz.yourserver.duels.listener;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import xyz.yourserver.duels.DuelsPlugin;

public class ConnectionListener implements Listener {

    private final DuelsPlugin plugin;

    public ConnectionListener(DuelsPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        plugin.getQueueManager().leaveAll(event.getPlayer());
        plugin.getDuelManager().forfeit(event.getPlayer());
    }
}
