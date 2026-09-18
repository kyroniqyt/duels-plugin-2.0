package xyz.yourserver.duels.command;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import xyz.yourserver.duels.DuelsPlugin;
import xyz.yourserver.duels.model.Kit;
import xyz.yourserver.duels.util.Msg;

public class DuelKitCommand implements CommandExecutor {

    private final DuelsPlugin plugin;

    public DuelKitCommand(DuelsPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Players only.");
            return true;
        }
        if (!player.hasPermission("duels.admin")) {
            Msg.send(player, "&cYou don't have permission.");
            return true;
        }
        if (args.length == 0) {
            Msg.send(player, "&7Usage: /duelkit <create|delete|list> [name]");
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "create" -> {
                if (args.length < 2) {
                    Msg.send(player, "&cUsage: /duelkit create <name>  (your current inventory becomes the kit)");
                    return true;
                }
                Kit kit = Kit.captureFrom(args[1], player);
                plugin.getKitManager().addKit(kit);
                Msg.send(player, "&aSaved kit &f" + args[1] + " &afrom your current inventory.");
            }
            case "delete" -> {
                if (args.length < 2) {
                    Msg.send(player, "&cUsage: /duelkit delete <name>");
                    return true;
                }
                boolean removed = plugin.getKitManager().removeKit(args[1]);
                Msg.send(player, removed ? "&aKit deleted." : "&cNo such kit.");
            }
            case "list" -> {
                if (plugin.getKitManager().getKits().isEmpty()) {
                    Msg.send(player, "&7No kits yet.");
                } else {
                    Msg.send(player, "&7Kits: &f" + String.join("&7, &f", plugin.getKitManager().getKits().keySet()));
                }
            }
            default -> Msg.send(player, "&7Usage: /duelkit <create|delete|list> [name]");
        }
        return true;
    }
}
