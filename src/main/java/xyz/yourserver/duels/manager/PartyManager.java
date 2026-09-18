package xyz.yourserver.duels.manager;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import xyz.yourserver.duels.DuelsPlugin;
import xyz.yourserver.duels.model.Party;
import xyz.yourserver.duels.util.Msg;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class PartyManager {

    private final DuelsPlugin plugin;
    private final Map<UUID, Party> partiesByMember = new ConcurrentHashMap<>();

    public PartyManager(DuelsPlugin plugin) {
        this.plugin = plugin;
    }

    public Party getParty(Player player) {
        return partiesByMember.get(player.getUniqueId());
    }

    public Party createParty(Player leader) {
        if (getParty(leader) != null) {
            Msg.send(leader, "&cYou're already in a party.");
            return null;
        }
        Party party = new Party(leader.getUniqueId());
        partiesByMember.put(leader.getUniqueId(), party);
        Msg.send(leader, "&aParty created. Invite players with &f/party invite <player>&a.");
        return party;
    }

    public void invite(Player leader, Player target) {
        Party party = getParty(leader);
        if (party == null || !party.isLeader(leader.getUniqueId())) {
            Msg.send(leader, "&cYou must be a party leader to invite.");
            return;
        }
        if (getParty(target) != null) {
            Msg.send(leader, "&c" + target.getName() + " is already in a party.");
            return;
        }
        party.getPendingInvites().add(target.getUniqueId());
        Msg.send(target, "&e" + leader.getName() + " &7invited you to their party. Type &a/party accept " + leader.getName());
        Msg.send(leader, "&7Invited &e" + target.getName() + "&7.");
    }

    public void accept(Player target, Player leader) {
        Party party = getParty(leader);
        if (party == null || !party.getPendingInvites().contains(target.getUniqueId())) {
            Msg.send(target, "&cNo pending invite from that player.");
            return;
        }
        party.getPendingInvites().remove(target.getUniqueId());
        party.getMembers().add(target.getUniqueId());
        partiesByMember.put(target.getUniqueId(), party);

        for (UUID uuid : party.getMembers()) {
            Msg.send(Bukkit.getPlayer(uuid), "&e" + target.getName() + " &7joined the party.");
        }
    }

    public void leave(Player player) {
        Party party = getParty(player);
        if (party == null) {
            Msg.send(player, "&cYou're not in a party.");
            return;
        }
        party.getMembers().remove(player.getUniqueId());
        partiesByMember.remove(player.getUniqueId());
        Msg.send(player, "&7You left the party.");

        if (party.getMembers().isEmpty()) {
            return; // party dissolved naturally
        }
        if (party.isLeader(player.getUniqueId())) {
            UUID newLeader = party.getMembers().iterator().next();
            party.setLeader(newLeader);
            Msg.send(Bukkit.getPlayer(newLeader), "&aYou are now the party leader.");
        }
        for (UUID uuid : party.getMembers()) {
            Msg.send(Bukkit.getPlayer(uuid), "&e" + player.getName() + " &7left the party.");
        }
    }

    public void kick(Player leader, Player target) {
        Party party = getParty(leader);
        if (party == null || !party.isLeader(leader.getUniqueId())) {
            Msg.send(leader, "&cYou must be a party leader to kick.");
            return;
        }
        if (!party.getMembers().contains(target.getUniqueId())) {
            Msg.send(leader, "&cThat player isn't in your party.");
            return;
        }
        party.getMembers().remove(target.getUniqueId());
        partiesByMember.remove(target.getUniqueId());
        Msg.send(target, "&cYou were removed from the party.");
        for (UUID uuid : party.getMembers()) {
            Msg.send(Bukkit.getPlayer(uuid), "&e" + target.getName() + " &7was removed from the party.");
        }
    }
}
