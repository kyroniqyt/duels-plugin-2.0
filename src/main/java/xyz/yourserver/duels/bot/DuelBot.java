package xyz.yourserver.duels.bot;

import de.eisi05.npc.api.ai.goals.AttackEntityGoal;
import de.eisi05.npc.api.enums.ClickActionType;
import de.eisi05.npc.api.objects.NPC;
import de.eisi05.npc.api.objects.NpcName;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import xyz.yourserver.duels.DuelsPlugin;
import xyz.yourserver.duels.model.DuelSession;
import xyz.yourserver.duels.model.DuelState;

import java.util.Set;
import java.util.UUID;

/**
 * A bot opponent backed by an NpcAPI NPC, using the library's own
 * AttackEntityGoal for movement + attacking (a built-in "chase and attack"
 * behavior) rather than a hand-rolled tick loop — that hand-rolled version
 * caused jittery movement and out-of-range hits. Player-hits-bot detection
 * still goes through the NPC's own click event, since NpcAPI is
 * packet-based and has no real server-side entity to fire damage events
 * for; bot health is tracked virtually here.
 */
public class DuelBot {

    private final UUID id = UUID.randomUUID();
    private final BotDifficulty difficulty;
    private final double maxHealth = 20.0;

    private NPC npc;
    private double health = maxHealth;
    private DuelSession session;
    private DuelsPlugin plugin;
    private AttackEntityGoal attackGoal;

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

    /**
     * Spawns (first time) or repositions the NPC for a new match, and
     * resets its health. Does NOT start attacking yet — call engage()
     * once the countdown ends, so the bot doesn't hit the player before
     * "Fight!".
     */
    public void prepareForMatch(DuelsPlugin plugin, DuelSession session, Location spawnAt) {
        this.plugin = plugin;
        this.session = session;
        setHealth(maxHealth);
        disengage();

        if (npc == null) {
            npc = new NPC(spawnAt, NpcName.empty());
            npc.setEnabled(true);
            npc.showNpcToAllPlayers();
            npc.setClickEvent(event -> {
                if (event.getAction() != ClickActionType.LEFT) return;
                Player attacker = event.getPlayer();
                if (this.session == null || this.session.getState() != DuelState.ACTIVE) return;
                if (!this.session.containsPlayer(attacker.getUniqueId())) return;
                this.plugin.getDuelManager().damageBot(this, 2.0);
            });
        } else {
            npc.setLocation(spawnAt);
            npc.reload();
        }
    }

    /** Starts the bot actually chasing and attacking its opponent. Call once the countdown ends. */
    public void engage() {
        if (npc == null || session == null) return;
        Player opponent = findOpponent();
        if (opponent == null) return;

        UUID opponentUuid = opponent.getUniqueId();
        attackGoal = new AttackEntityGoal(entity -> entity instanceof Player p && p.getUniqueId().equals(opponentUuid));
        npc.addGoal(attackGoal);
        npc.getGoalSelector().start();
    }

    /** Stops the bot from acting (match over, or about to be repositioned for a new one). */
    public void disengage() {
        if (npc == null) return;
        npc.getGoalSelector().stop();
        if (attackGoal != null) {
            npc.removeGoal(attackGoal);
            attackGoal = null;
        }
    }

    /** Fully removes the NPC (called once its final duel has ended). */
    public void remove() {
        disengage();
        if (npc != null) {
            try {
                npc.delete();
            } catch (Exception e) {
                if (plugin != null) {
                    plugin.getLogger().warning("Failed to delete duel bot NPC: " + e.getMessage());
                }
            }
            npc = null;
        }
    }

    private Player findOpponent() {
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
