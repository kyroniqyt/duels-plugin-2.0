package xyz.yourserver.duels.bot;

import de.eisi05.npc.api.objects.NPC;
import de.eisi05.npc.api.objects.NpcName;
import de.eisi05.npc.api.pathfinding.Path;
import de.eisi05.npc.api.pathfinding.PathfindingUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;
import xyz.yourserver.duels.DuelsPlugin;
import xyz.yourserver.duels.model.DuelSession;
import xyz.yourserver.duels.model.DuelState;

import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * A bot opponent backed by an NpcAPI NPC. NpcAPI is packet-based (no real
 * server-side entity), so combat can't rely on ordinary Bukkit damage
 * events — instead this listens directly to the NPC's own left-click
 * (attack) event and tracks a virtual health value here, the same way
 * DuelManager tracks it. The NPC itself is only created once we know the
 * exact arena spawn point it should appear at, avoiding any need to
 * "teleport" an already-spawned NPC.
 */
public class DuelBot {

    private final UUID id = UUID.randomUUID();
    private final BotDifficulty difficulty;
    private final double maxHealth = 20.0;

    private NPC npc;
    private double health = maxHealth;
    private DuelSession session;
    private DuelsPlugin plugin;
    private BukkitTask aiTask;
    private long lastAttackMillis = 0L;

    public DuelBot(BotDifficulty difficulty) {
        this.difficulty = difficulty;
    }

    /** Used as this bot's participant UUID in DuelSession's team lists. */
    public UUID getEntityUuid() {
        return id;
    }

    public String getDisplayName() {
        return "Bot";
    }

    public double getHealth() {
        return health;
    }

    public double getMaxHealth() {
        return maxHealth;
    }

    public void setHealth(double health) {
        this.health = Math.max(0, Math.min(maxHealth, health));
    }

    public DuelSession getSession() {
        return session;
    }

    /** Creates (first time) or is called again to restart AI for a new match at the given spawn point. */
    public void startAi(DuelsPlugin plugin, DuelSession session, Location spawnAt) {
        this.plugin = plugin;
        this.session = session;
        setHealth(maxHealth);
        stopAi();

        if (npc == null) {
            npc = new NPC(spawnAt, NpcName.empty());
            npc.setEnabled(true);
            npc.showNpcToAllPlayers();
            npc.setClickEvent(event -> {
                if (event.getAction() != de.eisi05.npc.api.enums.ClickActionType.LEFT) return;
                Player attacker = event.getPlayer();
                if (this.session == null || this.session.getState() != DuelState.ACTIVE) return;
                if (!this.session.containsPlayer(attacker.getUniqueId())) return;
                if (attacker.getLocation().distance(npc.getLocation()) > 5.0) return;
                this.plugin.getDuelManager().damageBot(this, 2.0);
            });
        }

        aiTask = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 20L, 10L);
    }

    public void stopAi() {
        if (aiTask != null) {
            aiTask.cancel();
            aiTask = null;
        }
    }

    /** Fully removes the NPC (called once its final duel has ended). */
    public void remove() {
        stopAi();
        if (npc != null) {
            npc.setEnabled(false);
        }
    }

    private void tick() {
        if (session == null || session.getState() != DuelState.ACTIVE || health <= 0 || npc == null) {
            return;
        }

        Player target = findTarget();
        if (target == null || !target.isOnline()) {
            return;
        }

        Location npcLoc = npc.getLocation();
        double distance = npcLoc.distance(target.getLocation());

        if (distance > difficulty.getAttackRange()) {
            Path path = PathfindingUtils.findPath(List.of(npcLoc, target.getLocation()), 10_000, true, null);
            if (path != null) {
                npc.walkTo(path, 0.4, true, result -> {
                });
            }
        } else {
            long now = System.currentTimeMillis();
            if (now - lastAttackMillis >= difficulty.getAttackCooldownMillis()) {
                lastAttackMillis = now;
                target.damage(difficulty.getAttackDamage());
            }
        }
    }

    private Player findTarget() {
        int mySide = session.sideOf(id);
        Set<UUID> opponents = mySide == 1 ? session.getAliveB() : session.getAliveA();
        for (UUID uuid : opponents) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null && p.isOnline()) {
                return p;
            }
        }
        return null;
    }
}
