package xyz.yourserver.duels.command;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import xyz.yourserver.duels.DuelsPlugin;
import xyz.yourserver.duels.model.Kit;
import xyz.yourserver.duels.util.Msg;

public class DuelCommand implements CommandExecutor {

    private final DuelsPlugin plugin;

    public DuelCommand(DuelsPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Players only.");
            return true;
        }

        if (args.length == 0) {
            Msg.send(player, "&7Usage: /duel <player> [kit] | /duel accept | /duel deny | /duel queue <kit> | /duel leavequeue | /duel bot [kit] | /duel kits");
            return true;
        }

        String sub = args[0].toLowerCase();
        switch (sub) {
            case "accept" -> plugin.getDuelManager().acceptChallenge(player);
            case "deny" -> plugin.getDuelManager().denyChallenge(player);
            case "leavequeue" -> {
                if (plugin.getQueueManager().leave(player)) {
                    Msg.send(player, "&7You left the queue.");
                } else {
                    Msg.send(player, "&cYou're not in a queue.");
                }
            }
            case "kits" -> {
                if (plugin.getKitManager().getKits().isEmpty()) {
                    Msg.send(player, "&7No kits have been created yet.");
                } else {
                    Msg.send(player, "&7Available kits: &f" + String.join("&7, &f", plugin.getKitManager().getKits().keySet()));
                }
            }
            case "queue" -> {
                if (args.length < 2) {
                    Msg.send(player, "&cUsage: /duel queue <kit>");
                    return true;
                }
                Kit kit = plugin.getKitManager().getKit(args[1]);
                if (kit == null) {
                    Msg.send(player, "&cNo such kit. Use /duel kits to see options.");
                    return true;
                }
                plugin.getQueueManager().join(player, kit);
            }
            case "bot" -> {
                if (plugin.getDuelManager().inSession(player)) {
                    Msg.send(player, "&cYou're already in a duel.");
                    return true;
                }

                Kit kit;
                if (args.length >= 2) {
                    kit = plugin.getKitManager().getKit(args[1]);
                    if (kit == null) {
                        Msg.send(player, "&cNo such kit. Use /duel kits to see options.");
                        return true;
                    }
                } else {
                    kit = plugin.getKitManager().getKits().values().stream().findFirst().orElse(null);
                    if (kit == null) {
                        Msg.send(player, "&cNo kits exist yet — ask an admin to create one with /duelkit create <name>.");
                        return true;
                    }
                }

                var bot = plugin.getBotManager().spawnBot(xyz.yourserver.duels.bot.BotDifficulty.EASY);
                boolean started = plugin.getDuelManager().startSession(
                        java.util.List.of(player.getUniqueId()), java.util.List.of(bot.getEntityUuid()), false, kit);
                if (!started) {
                    plugin.getBotManager().removeBot(bot);
                }
            }
            default -> {
                Player target = Bukkit.getPlayer(args[0]);
                if (target == null) {
                    Msg.send(player, "&cPlayer not found.");
                    return true;
                }
                if (target.equals(player)) {
                    Msg.send(player, "&cYou can't duel yourself.");
                    return true;
                }
                if (plugin.getDuelManager().inSession(player)) {
                    Msg.send(player, "&cYou're already in a duel.");
                    return true;
                }
                if (plugin.getDuelManager().inSession(target)) {
                    Msg.send(player, "&cThat player is already in a duel.");
                    return true;
                }

                Kit kit;
                if (args.length >= 2) {
                    kit = plugin.getKitManager().getKit(args[1]);
                    if (kit == null) {
                        Msg.send(player, "&cNo such kit. Use /duel kits to see options.");
                        return true;
                    }
                } else {
                    kit = plugin.getKitManager().getKits().values().stream().findFirst().orElse(null);
                    if (kit == null) {
                        Msg.send(player, "&cNo kits exist yet — ask an admin to create one with /duelkit create <name>.");
                        return true;
                    }
                }
                plugin.getDuelManager().issueChallenge(player, target, kit);
            }
        }
        return true;
    }
}
