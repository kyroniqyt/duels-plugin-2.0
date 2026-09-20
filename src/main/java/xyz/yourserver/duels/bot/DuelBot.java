package xyz.yourserver.duels.bot;

import net.citizensnpcs.api.npc.NPC;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;
import xyz.yourserver.duels.model.DuelSession;
import xyz.yourserver.duels.model.DuelState;

import java.util.Set;
import java.util.UUID;

/**
 * A bot opponent backed by a real Citizens NPC entity. Because Citizens
 * spawns an actual server-side entity (unlike packet-only NPC plugins),
 * ordinary combat — health, damage events — works the normal Bukkit way;
 * only movement and the decision to attack are custom AI here.
 */
public class DuelBot {

    private final NPC npc;
    private final UUID entityUuid;
    private final BotDifficulty difficulty;
    private final double maxHealth = 20.0;

    private double health = maxHealth;
    private DuelSession session;
    private BukkitTask aiTask;
    private long lastAttackMillis = 0L;

    public DuelBot(NPC npc, BotDifficulty difficulty) {
        this.npc = npc;
        this.difficulty = difficulty;
        this.entityUuid = npc.getEntity().getUniqueId();
    }

    public NPC getNpc() {
        return npc;
    }

    public UUID getEntityUuid() {
        return entityUuid;
    }

    public String getDisplayName() {
        return npc.getName();
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

    /** Teleports the NPC and begins its combat AI loop for the given session. */
    public void startAi(Plugin plugin, DuelSession session, Location spawnAt) {
        this.session = session;
        setHealth(maxHealth);
        stopAi();

        if (npc.isSpawned()) {
            npc.teleport(spawnAt, org.bukkit.event.player.PlayerTeleportEvent.TeleportCause.PLUGIN);
        } else {
            npc.spawn(spawnAt);
        }

        aiTask = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 20L, 10L);
    }

    public void stopAi() {
        if (aiTask != null) {
            aiTask.cancel();
            aiTask = null;
        }
        if (npc.isSpawned() && npc.getNavigator().isNavigating()) {
            npc.getNavigator().cancelNavigation();
        }
    }

    private void tick() {
        if (session == null || session.getState() != DuelState.ACTIVE || health <= 0) {
            return;
        }
        if (!npc.isSpawned()) {
            return;
        }

        Player target = findTarget();
        if (target == null || !target.isOnline()) {
            return;
        }

        Location npcLoc = npc.getEntity().getLocation();
        double distance = npcLoc.distance(target.getLocation());

        if (distance > difficulty.getAttackRange()) {
            npc.getNavigator().setTarget(target, false);
        } else {
            if (npc.getNavigator().isNavigating()) {
                npc.getNavigator().cancelNavigation();
            }
            npc.faceLocation(target.getLocation());

            long now = System.currentTimeMillis();
            if (now - lastAttackMillis >= difficulty.getAttackCooldownMillis()) {
                lastAttackMillis = now;
                target.damage(difficulty.getAttackDamage());
            }
        }
    }

    private Player findTarget() {
        int mySide = session.sideOf(entityUuid);
        Set<UUID> opponents = mySide == 1 ? session.getAliveB() : session.getAliveA();
        for (UUID uuid : opponents) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null && p.isOnline()) {
                return p;
            }
        }
        return null;
    }

    public void equipSword() {
        if (npc.getEntity() instanceof LivingEntity living && living.getEquipment() != null) {
            living.getEquipment().setItemInMainHand(new org.bukkit.inventory.ItemStack(org.bukkit.Material.IRON_SWORD));
        }
    }
}
