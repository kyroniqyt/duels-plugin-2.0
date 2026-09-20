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
     * Blocks PvP outside of active duels, and — this is the core trick —
     * intercepts damage that would be lethal to a dueling player and turns
     * it into an elimination instead of a real death.
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onDamage(EntityDamageEvent event) {
        if (plugin.getBotManager().isBotEntity(event.getEntity())) {
            handleBotDamage(event);
            return;
        }

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

        // Environmental damage (fall, fire, etc.) during an active duel also
        // counts toward elimination — only player-vs-player is gated above.
        if (session != null && session.getState() == DuelState.ACTIVE) {
            double healthAfter = player.getHealth() - event.getFinalDamage();
            if (healthAfter <= 0) {
                event.setCancelled(true);
                plugin.getDuelManager().handleElimination(player);
            }
        }
    }

    /** Bots don't have a real Bukkit health pool we trust for elimination —
     * we always cancel the real damage and track a virtual health value
     * ourselves in DuelManager, only counting hits from the bot's actual
     * opponent in an active session. */
    private void handleBotDamage(EntityDamageEvent event) {
        var bot = plugin.getBotManager().getBotByEntity(event.getEntity());
        if (bot == null) return;

        event.setCancelled(true);

        if (event instanceof EntityDamageByEntityEvent byEntity && byEntity.getDamager() instanceof Player attacker) {
            DuelSession session = bot.getSession();
            boolean valid = session != null && session.getState() == DuelState.ACTIVE
                    && session.containsPlayer(attacker.getUniqueId());
            if (valid) {
                plugin.getDuelManager().damageBot(bot, event.getFinalDamage());
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
