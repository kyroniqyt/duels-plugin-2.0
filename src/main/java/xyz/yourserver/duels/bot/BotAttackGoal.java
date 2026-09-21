package xyz.yourserver.duels.bot;

import de.eisi05.npc.api.ai.Goal;
import de.eisi05.npc.api.enums.WalkingResult;
import de.eisi05.npc.api.objects.NPC;
import de.eisi05.npc.api.pathfinding.Path;
import de.eisi05.npc.api.pathfinding.PathfindingUtils;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.function.Supplier;

/**
 * A hand-written chase-and-attack goal, plugged into NpcAPI's own goal
 * system (so it runs on the library's own schedule rather than a separate
 * BukkitRunnable racing against it — the likely cause of the earlier
 * jittery movement). Only recomputes a path when the opponent has actually
 * moved a meaningful distance, instead of restarting the walk every tick.
 */
public class BotAttackGoal extends Goal {

    private final Supplier<Player> targetSupplier;
    private final double attackRange;
    private final double attackDamage;
    private final long attackCooldownMillis;

    private long lastAttackMillis = 0L;
    private long lastPathMillis = 0L;
    private Location lastPathTarget;

    public BotAttackGoal(Supplier<Player> targetSupplier, double attackRange, double attackDamage, long attackCooldownMillis) {
        super(Priority.HIGH);
        this.targetSupplier = targetSupplier;
        this.attackRange = attackRange;
        this.attackDamage = attackDamage;
        this.attackCooldownMillis = attackCooldownMillis;
    }

    @Override
    protected boolean canUse(@NotNull NPC npc) {
        Player target = targetSupplier.get();
        return target != null && target.isOnline();
    }

    @Override
    protected void start(@NotNull NPC npc) {
        lastPathTarget = null;
        lastPathMillis = 0L;
    }

    @Override
    protected void tick(@NotNull NPC npc) {
        Player target = targetSupplier.get();
        if (target == null || !target.isOnline()) return;

        Location npcLoc = npc.getLocation();
        Location targetLoc = target.getLocation();
        double distance = npcLoc.distance(targetLoc);

        if (distance > attackRange) {
            long now = System.currentTimeMillis();
            boolean targetMovedEnough = lastPathTarget == null || lastPathTarget.distance(targetLoc) > 1.5;
            boolean cooledDown = now - lastPathMillis > 400;

            if (targetMovedEnough && cooledDown) {
                lastPathMillis = now;
                lastPathTarget = targetLoc.clone();
                try {
                    Path path = PathfindingUtils.findPath(List.of(npcLoc, targetLoc), 5_000, true, null);
                    if (path != null) {
                        npc.walkTo(path, 0.4, true, (WalkingResult result) -> {
                        });
                    }
                } catch (Exception ignored) {
                    // no clear route this attempt — will retry once the opponent moves again
                }
            }
        } else {
            long now = System.currentTimeMillis();
            if (now - lastAttackMillis >= attackCooldownMillis) {
                lastAttackMillis = now;
                target.damage(attackDamage);
            }
        }
    }

    @Override
    protected void stop(@NotNull NPC npc) {
        lastPathTarget = null;
    }

    @Override
    protected boolean canContinue(@NotNull NPC npc) {
        return canUse(npc);
    }

    @Override
    protected boolean canBeInterrupted(@NotNull NPC npc) {
        return true;
    }

    @Override
    public Goal copy() {
        return new BotAttackGoal(targetSupplier, attackRange, attackDamage, attackCooldownMillis);
    }
}
