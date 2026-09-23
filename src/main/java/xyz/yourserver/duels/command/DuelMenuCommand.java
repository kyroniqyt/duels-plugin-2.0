package xyz.yourserver.duels.command;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import xyz.yourserver.duels.DuelsPlugin;

public class DuelMenuCommand implements CommandExecutor {

    private final DuelsPlugin plugin;

    public DuelMenuCommand(DuelsPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Players only.");
            return true;
        }
        plugin.getDuelMenu().openMain(player);
        return true;
    }
}
