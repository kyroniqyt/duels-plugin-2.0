package xyz.yourserver.duels.command;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import xyz.yourserver.duels.DuelsPlugin;
import xyz.yourserver.duels.model.Kit;
import xyz.yourserver.duels.model.Party;
import xyz.yourserver.duels.util.Msg;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class PartyCommand implements CommandExecutor {

    private final DuelsPlugin plugin;

    public PartyCommand(DuelsPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Players only.");
            return true;
        }
        if (args.length == 0) {
            Msg.send(player, "&7Usage: /party <create|invite|accept|leave|kick|ffa|duel> [target/kit]");
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "create" -> plugin.getPartyManager().createParty(player);

            case "invite" -> {
                if (args.length < 2) { Msg.send(player, "&cUsage: /party invite <player>"); return true; }
                Player target = Bukkit.getPlayer(args[1]);
                if (target == null) { Msg.send(player, "&cPlayer not found."); return true; }
                plugin.getPartyManager().invite(player, target);
            }

            case "accept" -> {
                if (args.length < 2) { Msg.send(player, "&cUsage: /party accept <leader>"); return true; }
                Player leader = Bukkit.getPlayer(args[1]);
                if (leader == null) { Msg.send(player, "&cPlayer not found."); return true; }
                plugin.getPartyManager().accept(player, leader);
            }

            case "leave" -> plugin.getPartyManager().leave(player);

            case "kick" -> {
                if (args.length < 2) { Msg.send(player, "&cUsage: /party kick <player>"); return true; }
                Player target = Bukkit.getPlayer(args[1]);
                if (target == null) { Msg.send(player, "&cPlayer not found."); return true; }
                plugin.getPartyManager().kick(player, target);
            }

            case "ffa" -> {
                if (args.length < 2) { Msg.send(player, "&cUsage: /party ffa <kit>"); return true; }
                Party party = plugin.getPartyManager().getParty(player);
                if (party == null || !party.isLeader(player.getUniqueId())) {
                    Msg.send(player, "&cOnly the party leader can start a host battle.");
                    return true;
                }
                if (party.getMembers().size() < 2) {
                    Msg.send(player, "&cNeed at least 2 party members for an FFA.");
                    return true;
                }
                Kit kit = plugin.getKitManager().getKit(args[1]);
                if (kit == null) { Msg.send(player, "&cNo such kit."); return true; }

                List<UUID> everyone = new ArrayList<>(party.getMembers());
                plugin.getDuelManager().startSession(everyone, List.of(), true, kit);
            }

            case "duel" -> {
                if (args.length < 2) { Msg.send(player, "&cUsage: /party duel <other party leader>"); return true; }
                Party mine = plugin.getPartyManager().getParty(player);
                if (mine == null || !mine.isLeader(player.getUniqueId())) {
                    Msg.send(player, "&cOnly the party leader can challenge another party.");
                    return true;
                }
                Player otherLeader = Bukkit.getPlayer(args[1]);
                if (otherLeader == null) { Msg.send(player, "&cPlayer not found."); return true; }
                Party theirs = plugin.getPartyManager().getParty(otherLeader);
                if (theirs == null || !theirs.isLeader(otherLeader.getUniqueId())) {
                    Msg.send(player, "&cThat player isn't leading a party.");
                    return true;
                }
                if (mine.getMembers().size() != theirs.getMembers().size()) {
                    Msg.send(player, "&cParty sizes must match for a team duel.");
                    return true;
                }

                Kit kit = plugin.getKitManager().getKits().values().stream().findFirst().orElse(null);
                if (kit == null) { Msg.send(player, "&cNo kits exist yet."); return true; }

                plugin.getDuelManager().startSession(
                        new ArrayList<>(mine.getMembers()),
                        new ArrayList<>(theirs.getMembers()),
                        false, kit);
            }

            default -> Msg.send(player, "&7Usage: /party <create|invite|accept|leave|kick|ffa|duel> [target/kit]");
        }
        return true;
    }
}
