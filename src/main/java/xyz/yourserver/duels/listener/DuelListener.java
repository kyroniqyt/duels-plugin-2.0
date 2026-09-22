package xyz.yourserver.duels.listener;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import xyz.yourserver.duels.DuelsPlugin;
import xyz.yourserver.duels.model.DuelSession;
import xyz.yourserver.duels.model.DuelState;

public class DuelListener implements Listener {

    private final DuelsPlugin plugin;

    public DuelListener(DuelsPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Blocks PvP outside of active duels, and — the core trick — intercepts
     * damage that would be lethal to a dueling player and turns it into an
     * elimination instead of a real death.
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;

        DuelSession session = plugin.getDuelManager().getSession(player);

        if (event instanceof EntityDamageByEntityEvent byEntity && byEntity.getDamager() instanceof Player attacker) {
            DuelSession attackerSession = plugin.getDuelManager().getSession(attacker);
            boolean sameActiveSession = session != null && session.equals(attackerSession)
                    && session.getState() == DuelState.ACTIVE;
            if (!sameActiveSession) {
                event.setCancelled(true);
                return;
            }
        }

        if (session != null && session.getState() == DuelState.ACTIVE) {
            double healthAfter = player.getHealth() - event.getFinalDamage();
            if (healthAfter <= 0) {
                event.setCancelled(true);
                plugin.getDuelManager().handleElimination(player);
            }
        }
    }

    @EventHandler
    public void onFoodChange(FoodLevelChangeEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (plugin.getConfig().getBoolean("disable-food-loss", false)) {
            DuelSession session = plugin.getDuelManager().getSession(player);
            if (session != null) {
                event.setCancelled(true);
            }
        }
    }
}
