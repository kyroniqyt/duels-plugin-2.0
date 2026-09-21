package xyz.yourserver.duels.manager;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import xyz.yourserver.duels.DuelsPlugin;
import xyz.yourserver.duels.model.Kit;
import xyz.yourserver.duels.util.Msg;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * FIFO matchmaking per kit name. First two players to queue for the same
 * kit are paired off automatically. (Swap this for an ELO-nearest match
 * later without touching DuelManager — it only needs two UUID lists.)
 */
public class QueueManager {

    private final DuelsPlugin plugin;
    private final Map<String, Set<UUID>> queues = new ConcurrentHashMap<>(); // kit name -> waiting players

    public QueueManager(DuelsPlugin plugin) {
        this.plugin = plugin;
    }

    public void join(Player player, Kit kit) {
        if (plugin.getDuelManager().inSession(player)) {
            Msg.send(player, "&cYou're already in a duel.");
            return;
        }
        leaveAll(player); // only queue for one kit at a time

        Set<UUID> queue = queues.computeIfAbsent(kit.getName().toLowerCase(), k -> new LinkedHashSet<>());
        queue.add(player.getUniqueId());
        Msg.send(player, "&7Joined the &f" + kit.getName() + " &7queue. Type &c/duel leavequeue &7to leave.");

        tryMatch(kit.getName().toLowerCase());
    }

    public void leaveAll(Player player) {
        for (Set<UUID> queue : queues.values()) {
            queue.remove(player.getUniqueId());
        }
    }

    public boolean leave(Player player) {
        boolean wasQueued = false;
        for (Set<UUID> queue : queues.values()) {
            wasQueued |= queue.remove(player.getUniqueId());
        }
        return wasQueued;
    }

    private void tryMatch(String kitKey) {
        Set<UUID> queue = queues.get(kitKey);
        if (queue == null || queue.size() < 2) return;

        var iterator = queue.iterator();
        UUID first = iterator.next();
        UUID second = iterator.next();
        queue.remove(first);
        queue.remove(second);

        Player p1 = Bukkit.getPlayer(first);
        Player p2 = Bukkit.getPlayer(second);
        Kit kit = plugin.getKitManager().getKit(kitKey);

        if (p1 == null || p2 == null || kit == null) {
            // one disconnected between queueing and matching — requeue whoever is still valid
            if (p1 != null) queue.add(first);
            if (p2 != null) queue.add(second);
            return;
        }

        Msg.send(p1, "&aMatch found against &e" + p2.getName() + "&a!");
        Msg.send(p2, "&aMatch found against &e" + p1.getName() + "&a!");
        plugin.getDuelManager().startSession(List.of(first), List.of(second), false, kit);
    }
}
