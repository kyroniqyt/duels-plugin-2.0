package xyz.yourserver.duels.manager;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import xyz.yourserver.duels.DuelsPlugin;
import xyz.yourserver.duels.model.Arena;
import xyz.yourserver.duels.model.DuelSession;
import xyz.yourserver.duels.model.DuelState;
import xyz.yourserver.duels.model.Kit;
import xyz.yourserver.duels.util.Msg;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class DuelManager {

    /** A pending /duel challenge, expires after config challenge-expiry-seconds. */
    public static class Challenge {
        public final UUID challenger;
        public final Kit kit;
        public final long expiresAtMillis;

        public Challenge(UUID challenger, Kit kit, long expiresAtMillis) {
            this.challenger = challenger;
            this.kit = kit;
            this.expiresAtMillis = expiresAtMillis;
        }
    }

    private final DuelsPlugin plugin;
    private final Map<UUID, Challenge> pendingChallenges = new ConcurrentHashMap<>(); // target -> challenge
    private final Map<UUID, DuelSession> playerSessions = new ConcurrentHashMap<>();   // player -> session

    public DuelManager(DuelsPlugin plugin) {
        this.plugin = plugin;
    }

    // ---------- Direct challenges ----------

    public void issueChallenge(Player challenger, Player target, Kit kit) {
        long expiry = System.currentTimeMillis() + (plugin.getConfig().getInt("challenge-expiry-seconds", 30) * 1000L);
        pendingChallenges.put(target.getUniqueId(), new Challenge(challenger.getUniqueId(), kit, expiry));

        Component challengeMsg = Component.text(challenger.getName() + " has challenged you to a duel (kit: " + kit.getName() + ").")
                .color(NamedTextColor.YELLOW)
                .appendNewline()
                .append(Component.text("[Click to accept]")
                        .color(NamedTextColor.GREEN)
                        .decorate(TextDecoration.BOLD)
                        .clickEvent(ClickEvent.runCommand("/duel accept")))
                .append(Component.text("  "))
                .append(Component.text("[Click to deny]")
                        .color(NamedTextColor.RED)
                        .decorate(TextDecoration.BOLD)
                        .clickEvent(ClickEvent.runCommand("/duel deny")));
        target.sendMessage(challengeMsg);
        target.playSound(target.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);

        Msg.send(challenger, "&7Challenge sent to &e" + target.getName() + "&7.");
    }

    public void acceptChallenge(Player target) {
        Challenge challenge = pendingChallenges.remove(target.getUniqueId());
        if (challenge == null) {
            Msg.send(target, "&cYou have no pending challenge.");
            return;
        }
        if (challenge.expiresAtMillis < System.currentTimeMillis()) {
            Msg.send(target, "&cThat challenge expired.");
            return;
        }
        Player challenger = Bukkit.getPlayer(challenge.challenger);
        if (challenger == null || !challenger.isOnline()) {
            Msg.send(target, "&cThat player is no longer online.");
            return;
        }
        if (inSession(challenger) || inSession(target)) {
            Msg.send(target, "&cOne of you is already in a duel.");
            return;
        }
        target.playSound(target.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
        challenger.playSound(challenger.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
        startSession(List.of(challenger.getUniqueId()), List.of(target.getUniqueId()), false, challenge.kit);
    }

    public void denyChallenge(Player target) {
        Challenge challenge = pendingChallenges.remove(target.getUniqueId());
        if (challenge == null) {
            Msg.send(target, "&cYou have no pending challenge.");
            return;
        }
        Player challenger = Bukkit.getPlayer(challenge.challenger);
        Msg.send(target, "&7Challenge declined.");
        if (challenger != null) {
            Msg.send(challenger, "&e" + target.getName() + " &7declined your challenge.");
        }
    }

    // ---------- Session lifecycle ----------

    public boolean inSession(Player player) {
        return playerSessions.containsKey(player.getUniqueId());
    }

    public DuelSession getSession(Player player) {
        return playerSessions.get(player.getUniqueId());
    }

    /**
     * Starts a match. Works identically for 1v1 (singleton lists) and party
     * duels/FFA (multi-member lists, sideB empty for FFA).
     * Returns true if a match actually started (i.e. an arena was free).
     */
    public boolean startSession(List<UUID> sideA, List<UUID> sideB, boolean ffa, Kit kit) {
        Optional<Arena> freeArena = plugin.getArenaManager().getFreeArena();
        if (freeArena.isEmpty()) {
            for (UUID uuid : sideA) Msg.send(Bukkit.getPlayer(uuid), "&cNo free arenas right now — try again shortly.");
            for (UUID uuid : sideB) Msg.send(Bukkit.getPlayer(uuid), "&cNo free arenas right now — try again shortly.");
            return false;
        }

        Arena arena = freeArena.get();
        arena.setInUse(true);

        int rounds = plugin.getConfig().getInt("default-rounds-to-win", 1);
        DuelSession session = new DuelSession(sideA, sideB, ffa, kit, arena, rounds);

        for (UUID uuid : session.allParticipants()) {
            playerSessions.put(uuid, session);
        }

        beginCountdown(session);
        return true;
    }

    private void beginCountdown(DuelSession session) {
        session.setState(DuelState.COUNTDOWN);
        int seconds = plugin.getConfig().getInt("countdown-seconds", 5);

        teleportAndEquip(session);

        BukkitTask task = new BukkitRunnable() {
            int remaining = seconds;

            @Override
            public void run() {
                if (remaining <= 0) {
                    session.setState(DuelState.ACTIVE);
                    broadcast(session, "&aFight!");
                    playSoundToAll(session, Sound.ENTITY_PLAYER_LEVELUP);
                    cancel();
                    return;
                }
                broadcast(session, "&7Fight starts in &f" + remaining + "&7...");
                remaining--;
            }
        }.runTaskTimer(plugin, 0L, 20L);

        session.setActiveTask(task);
    }

    private void teleportAndEquip(DuelSession session) {
        Arena arena = session.getArena();
        for (UUID uuid : session.getSideA()) {
            setUpParticipant(uuid, session, arena.getSpawn1());
        }
        for (UUID uuid : session.getSideB()) {
            setUpParticipant(uuid, session, arena.getSpawn2());
        }
    }

    private void setUpParticipant(UUID uuid, DuelSession session, Location spawn) {
        Player p = Bukkit.getPlayer(uuid);
        if (p == null) return;
        p.teleport(spawn);
        resetForDuel(p, session.getKit());
    }

    private void resetForDuel(Player p, Kit kit) {
        p.setGameMode(GameMode.SURVIVAL);
        p.setHealth(p.getMaxHealth());
        p.setFoodLevel(20);
        p.setFireTicks(0);
        kit.applyTo(p);
    }

    /**
     * Called by the combat listener when a player would otherwise die.
     * Instead of a real death, we heal them, flip them to spectator, and
     * check whether the match/round is decided.
     */
    public void handleElimination(Player player) {
        DuelSession session = getSession(player);
        if (session == null || session.getState() != DuelState.ACTIVE) return;

        UUID uuid = player.getUniqueId();
        int side = session.sideOf(uuid);
        if (side == 1) session.getAliveA().remove(uuid);
        if (side == 2) session.getAliveB().remove(uuid);

        player.setHealth(player.getMaxHealth());
        player.setGameMode(GameMode.SPECTATOR);
        broadcast(session, "&c" + player.getName() + " &7has been eliminated.");
        playSoundToAll(session, Sound.ENTITY_PLAYER_DEATH);

        if (session.isMatchOver()) {
            finishSession(session);
        }
    }

    private void finishSession(DuelSession session) {
        session.setState(DuelState.ENDING);
        int winnerSide = session.roundWinnerSide();

        if (session.isFfa()) {
            String winnerName = session.getAliveA().stream()
                    .findFirst().map(Bukkit::getPlayer).map(Player::getName).orElse("nobody");
            broadcast(session, "&6" + winnerName + " &awins the FFA!");
        } else if (winnerSide == 1) {
            session.incrementWinsA();
            broadcast(session, "&aTeam A wins the duel!");
        } else if (winnerSide == 2) {
            session.incrementWinsB();
            broadcast(session, "&aTeam B wins the duel!");
        } else {
            broadcast(session, "&7Duel ended in a draw.");
        }

        Bukkit.getScheduler().runTaskLater(plugin, () -> cleanupSession(session), 60L);
    }

    private void cleanupSession(DuelSession session) {
        Arena arena = session.getArena();
        for (UUID uuid : session.allParticipants()) {
            playerSessions.remove(uuid);
            Player p = Bukkit.getPlayer(uuid);
            if (p != null) {
                p.setGameMode(GameMode.SURVIVAL);
                p.getInventory().clear();
                // Run the server's own /spawn command rather than guessing a
                // location ourselves — this respects whatever spawn plugin
                // (EssentialsSpawn, etc.) actually controls where players land,
                // instead of the raw vanilla world spawn point.
                p.performCommand("spawn");
            }
        }

        try {
            plugin.getArenaManager().restoreArena(arena);
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to restore arena '" + arena.getName() + "': " + e.getMessage());
        } finally {
            plugin.getArenaManager().clearLeftoverEntities(arena);
            arena.setInUse(false);
        }
    }

    /** Called on disconnect: the leaving player forfeits, their side loses instantly. */
    public void forfeit(Player player) {
        DuelSession session = getSession(player);
        if (session == null) return;
        broadcast(session, "&c" + player.getName() + " &7disconnected and forfeited.");
        handleElimination(player);
    }

    private void broadcast(DuelSession session, String message) {
        for (UUID uuid : session.allParticipants()) {
            Msg.send(Bukkit.getPlayer(uuid), message);
        }
    }

    private void playSoundToAll(DuelSession session, Sound sound) {
        for (UUID uuid : session.allParticipants()) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null) {
                p.playSound(p.getLocation(), sound, 1.0f, 1.0f);
            }
        }
    }

    public Map<UUID, Challenge> getPendingChallenges() {
        return pendingChallenges;
    }
}
