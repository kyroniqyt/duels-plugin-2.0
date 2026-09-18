package xyz.yourserver.duels.model;

import org.bukkit.scheduler.BukkitTask;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * One active match. Deliberately generalized to *teams* rather than two
 * players, so 1v1 direct/queue duels, party-vs-party duels, and party FFA
 * ("host battles") all run through the exact same engine:
 *
 *  - 1v1:          sideA = [A], sideB = [B]
 *  - party duel:   sideA = party1 members, sideB = party2 members
 *  - FFA/host:     sideA = all participants, sideB = empty, ffa = true
 */
public class DuelSession {

    private final UUID id = UUID.randomUUID();
    private final List<UUID> sideA;
    private final List<UUID> sideB;
    private final Set<UUID> aliveA;
    private final Set<UUID> aliveB;
    private final boolean ffa;
    private final Kit kit;
    private final Arena arena;
    private final int roundsToWin;

    private DuelState state = DuelState.COUNTDOWN;
    private int winsA = 0;
    private int winsB = 0;
    private BukkitTask activeTask;

    public DuelSession(List<UUID> sideA, List<UUID> sideB, boolean ffa, Kit kit, Arena arena, int roundsToWin) {
        this.sideA = sideA;
        this.sideB = sideB;
        this.ffa = ffa;
        this.kit = kit;
        this.arena = arena;
        this.roundsToWin = roundsToWin;
        this.aliveA = new LinkedHashSet<>(sideA);
        this.aliveB = new LinkedHashSet<>(sideB);
    }

    public UUID getId() {
        return id;
    }

    public List<UUID> getSideA() {
        return sideA;
    }

    public List<UUID> getSideB() {
        return sideB;
    }

    public Set<UUID> getAliveA() {
        return aliveA;
    }

    public Set<UUID> getAliveB() {
        return aliveB;
    }

    public boolean isFfa() {
        return ffa;
    }

    public Kit getKit() {
        return kit;
    }

    public Arena getArena() {
        return arena;
    }

    public int getRoundsToWin() {
        return roundsToWin;
    }

    public DuelState getState() {
        return state;
    }

    public void setState(DuelState state) {
        this.state = state;
    }

    public int getWinsA() {
        return winsA;
    }

    public int getWinsB() {
        return winsB;
    }

    public void incrementWinsA() {
        winsA++;
    }

    public void incrementWinsB() {
        winsB++;
    }

    public BukkitTask getActiveTask() {
        return activeTask;
    }

    public void setActiveTask(BukkitTask activeTask) {
        this.activeTask = activeTask;
    }

    public boolean containsPlayer(UUID uuid) {
        return sideA.contains(uuid) || sideB.contains(uuid);
    }

    public int sideOf(UUID uuid) {
        if (sideA.contains(uuid)) return 1;
        if (sideB.contains(uuid)) return 2;
        return 0;
    }

    public List<UUID> allParticipants() {
        List<UUID> all = new java.util.ArrayList<>(sideA);
        all.addAll(sideB);
        return all;
    }

    /**
     * For FFA: match ends when 1 or 0 players remain alive.
     * For team duels: match ends when one whole side is eliminated.
     */
    public boolean isMatchOver() {
        if (ffa) {
            return aliveA.size() <= 1;
        }
        return aliveA.isEmpty() || aliveB.isEmpty();
    }

    /** Returns 1 if side A won the round/match, 2 if side B won, 0 if undecided/draw. */
    public int roundWinnerSide() {
        if (ffa) {
            return aliveA.size() == 1 ? 1 : 0;
        }
        if (aliveB.isEmpty() && !aliveA.isEmpty()) return 1;
        if (aliveA.isEmpty() && !aliveB.isEmpty()) return 2;
        return 0;
    }
}
