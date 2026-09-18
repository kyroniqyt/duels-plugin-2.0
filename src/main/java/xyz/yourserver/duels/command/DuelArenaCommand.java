package xyz.yourserver.duels.command;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import xyz.yourserver.duels.DuelsPlugin;
import xyz.yourserver.duels.model.Arena;
import xyz.yourserver.duels.util.Msg;

public class DuelArenaCommand implements CommandExecutor {

    private final DuelsPlugin plugin;

    public DuelArenaCommand(DuelsPlugin plugin) {
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
            Msg.send(player, "&7Usage: /duelarena <create|pos1|pos2|setspawn1|setspawn2|save|list> [name]");
            return true;
        }

        String sub = args[0].toLowerCase();

        if (sub.equals("list")) {
            if (plugin.getArenaManager().getArenas().isEmpty()) {
                Msg.send(player, "&7No arenas yet.");
            } else {
                Msg.send(player, "&7Arenas: &f" + String.join("&7, &f", plugin.getArenaManager().getArenas().keySet()));
            }
            return true;
        }

        if (args.length < 2) {
            Msg.send(player, "&cUsage: /duelarena " + sub + " <name>");
            return true;
        }

        String name = args[1];
        Arena arena;

        switch (sub) {
            case "create" -> {
                arena = plugin.getArenaManager().createArena(name);
                arena.setWorld(player.getWorld());
                Msg.send(player, "&aArena &f" + name + " &acreated. Now set pos1, pos2, setspawn1, setspawn2, then save.");
            }
            case "pos1" -> {
                arena = requireArena(player, name);
                if (arena == null) return true;
                arena.setWorld(player.getWorld());
                arena.setPos1(player.getLocation());
                plugin.getArenaManager().save();
                Msg.send(player, "&aPos1 set for &f" + name + "&a.");
            }
            case "pos2" -> {
                arena = requireArena(player, name);
                if (arena == null) return true;
                arena.setPos2(player.getLocation());
                plugin.getArenaManager().save();
                Msg.send(player, "&aPos2 set for &f" + name + "&a.");
            }
            case "setspawn1" -> {
                arena = requireArena(player, name);
                if (arena == null) return true;
                arena.setSpawn1(player.getLocation());
                plugin.getArenaManager().save();
                Msg.send(player, "&aSpawn1 set for &f" + name + "&a.");
            }
            case "setspawn2" -> {
                arena = requireArena(player, name);
                if (arena == null) return true;
                arena.setSpawn2(player.getLocation());
                plugin.getArenaManager().save();
                Msg.send(player, "&aSpawn2 set for &f" + name + "&a.");
            }
            case "save" -> {
                arena = requireArena(player, name);
                if (arena == null) return true;
                try {
                    plugin.getArenaManager().captureSchematic(arena);
                    Msg.send(player, "&aArena &f" + name + " &acaptured — it will auto-restore after every duel.");
                } catch (Exception e) {
                    Msg.send(player, "&cFailed to save schematic: " + e.getMessage());
                }
            }
            default -> Msg.send(player, "&7Usage: /duelarena <create|pos1|pos2|setspawn1|setspawn2|save|list> [name]");
        }
        return true;
    }

    private Arena requireArena(Player player, String name) {
        Arena arena = plugin.getArenaManager().getArena(name);
        if (arena == null) {
            Msg.send(player, "&cNo such arena. Create it first with /duelarena create " + name);
        }
        return arena;
    }
}
