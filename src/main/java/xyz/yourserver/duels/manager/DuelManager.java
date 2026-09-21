package xyz.yourserver.duels.manager;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
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
     * Starts a match. Works identically for 1v1 (singleton lists), party
     * duels (multi-member lists), and FFA (sideB empty, ffa=true).
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

        // teleport + equip immediately so players can see each other during the countdown
        teleportAndEquip(session);

        org.bukkit.scheduler.BukkitTask task = new BukkitRunnable() {
            int remaining = seconds;

            @Override
            public void run() {
                if (remaining <= 0) {
                    session.setState(DuelState.ACTIVE);
                    broadcast(session, "&aFight!");
                    engageBots(session);
                    cancel();
                    return;
                }
                broadcast(session, "&7Fight starts in &f" + remaining + "&7...");
                remaining--;
            }
        }.runTaskTimer(plugin, 0L, 20L);

        session.setActiveTask(task);
    }

    /** Bots don't start chasing/attacking until the countdown actually ends. */
    private void engageBots(DuelSession session) {
        for (UUID uuid : session.allParticipants()) {
            var bot = plugin.getBotManager().getBotByUuid(uuid);
            if (bot != null) {
                bot.engage();
            }
        }
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

    private void setUpParticipant(UUID uuid, DuelSession session, org.bukkit.Location spawn) {
        var bot = plugin.getBotManager().getBotByUuid(uuid);
        if (bot != null) {
            bot.prepareForMatch(plugin, session, spawn);
            return;
        }
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

        int side = session.sideOf(player.getUniqueId());
        if (side == 1) session.getAliveA().remove(player.getUniqueId());
        if (side == 2) session.getAliveB().remove(player.getUniqueId());

        player.setHealth(player.getMaxHealth());
        player.setGameMode(GameMode.SPECTATOR);
        broadcast(session, "&c" + player.getName() + " &7has been eliminated.");

        if (session.isMatchOver()) {
            finishSession(session);
        }
    }

    /**
     * Called by the combat listener when a bot would otherwise die. Bot
     * health is tracked here rather than relying on the NPC entity's real
     * health, so elimination behaves identically to a real player's.
     */
    public void damageBot(xyz.yourserver.duels.bot.DuelBot bot, double amount) {
        DuelSession session = bot.getSession();
        if (session == null || session.getState() != DuelState.ACTIVE) return;

        bot.setHealth(bot.getHealth() - amount);
        if (bot.getHealth() <= 0) {
            handleBotElimination(bot);
        }
    }

    public void handleBotElimination(xyz.yourserver.duels.bot.DuelBot bot) {
        DuelSession session = bot.getSession();
        if (session == null || session.getState() != DuelState.ACTIVE) return;

        UUID botUuid = bot.getEntityUuid();
        int side = session.sideOf(botUuid);
        if (side == 1) session.getAliveA().remove(botUuid);
        if (side == 2) session.getAliveB().remove(botUuid);

        bot.disengage();
        broadcast(session, "&c" + bot.getDisplayName() + " &7has been eliminated.");

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

        // give everyone a moment to see the result, then clean up + restore arena
        Bukkit.getScheduler().runTaskLater(plugin, () -> cleanupSession(session), 60L);
    }

    private void cleanupSession(DuelSession session) {
        Arena arena = session.getArena();
        for (UUID uuid : session.allParticipants()) {
            playerSessions.remove(uuid);
            var bot = plugin.getBotManager().getBotByUuid(uuid);
            if (bot != null) {
                plugin.getBotManager().removeBot(bot);
                continue;
            }
            Player p = Bukkit.getPlayer(uuid);
            if (p != null) {
                p.setGameMode(GameMode.SURVIVAL);
                p.getInventory().clear();
                p.teleport(plugin.getServer().getWorlds().get(0).getSpawnLocation());
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

    public Map<UUID, Challenge> getPendingChallenges() {
        return pendingChallenges;
    }
}
