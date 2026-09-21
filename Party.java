package xyz.yourserver.duels.model;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

public class Party {

    private UUID leader;
    private final Set<UUID> members = new LinkedHashSet<>();
    private final Set<UUID> pendingInvites = new LinkedHashSet<>();

    public Party(UUID leader) {
        this.leader = leader;
        this.members.add(leader);
    }

    public UUID getLeader() {
        return leader;
    }

    public void setLeader(UUID leader) {
        this.leader = leader;
    }

    public Set<UUID> getMembers() {
        return members;
    }

    public Set<UUID> getPendingInvites() {
        return pendingInvites;
    }

    public boolean isLeader(UUID uuid) {
        return leader.equals(uuid);
    }
}
