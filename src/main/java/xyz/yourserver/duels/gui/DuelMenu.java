package xyz.yourserver.duels.gui;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import xyz.yourserver.duels.DuelsPlugin;
import xyz.yourserver.duels.model.Kit;
import xyz.yourserver.duels.model.Party;

import java.util.ArrayList;
import java.util.List;

/**
 * Builds every GUI screen for the player-facing duel menu. Plain Bukkit
 * chest inventories — these render as a normal container screen for
 * Bedrock players through Geyser automatically, no special handling needed.
 */
public class DuelMenu {

    private final DuelsPlugin plugin;

    public DuelMenu(DuelsPlugin plugin) {
        this.plugin = plugin;
    }

    /** The item given to players that opens the menu when right-clicked. */
    public ItemStack menuItem() {
        return icon(Material.COMPASS, "Duel Menu", "Right-click to open");
    }

    private ItemStack icon(Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(name).color(NamedTextColor.AQUA).decoration(TextDecoration.ITALIC, false));
        if (lore.length > 0) {
            List<Component> loreLines = new ArrayList<>();
            for (String line : lore) {
                loreLines.add(Component.text(line).color(NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
            }
            meta.lore(loreLines);
        }
        item.setItemMeta(meta);
        return item;
    }

    public void openMain(Player player) {
        DuelMenuHolder holder = new DuelMenuHolder(DuelMenuHolder.Type.MAIN);
        Inventory inv = Bukkit.createInventory(holder, 27, Component.text("Duel Menu"));
        holder.setInventory(inv);

        inv.setItem(11, icon(Material.DIAMOND_SWORD, "Challenge a Player", "Pick someone to duel"));
        inv.setItem(13, icon(Material.CLOCK, "Join Queue", "Get matched with a random opponent"));
        inv.setItem(15, icon(Material.NETHER_STAR, "Party", "Create or manage a party"));
        inv.setItem(22, icon(Material.BARRIER, "Close"));

        player.openInventory(inv);
    }

    public void openChallenge(Player player) {
        DuelMenuHolder holder = new DuelMenuHolder(DuelMenuHolder.Type.CHALLENGE);
        List<Player> others = onlineOthersThan(player);
        int size = gridSize(others.size());
        Inventory inv = Bukkit.createInventory(holder, size, Component.text("Challenge a Player"));
        holder.setInventory(inv);

        fillPlayerHeads(inv, others, size);
        inv.setItem(size - 1, icon(Material.ARROW, "Back"));

        player.openInventory(inv);
    }

    public void openQueueKits(Player player) {
        DuelMenuHolder holder = new DuelMenuHolder(DuelMenuHolder.Type.QUEUE_KIT);
        var kits = plugin.getKitManager().getKits();
        int size = gridSize(kits.size());
        Inventory inv = Bukkit.createInventory(holder, size, Component.text("Queue - Choose Kit"));
        holder.setInventory(inv);

        int slot = 0;
        for (Kit kit : kits.values()) {
            if (slot >= size - 9) break;
            inv.setItem(slot++, icon(Material.IRON_SWORD, kit.getName(), "Click to join the queue"));
        }
        inv.setItem(size - 1, icon(Material.ARROW, "Back"));

        player.openInventory(inv);
    }

    public void openParty(Player player) {
        DuelMenuHolder holder = new DuelMenuHolder(DuelMenuHolder.Type.PARTY);
        Inventory inv = Bukkit.createInventory(holder, 27, Component.text("Party Menu"));
        holder.setInventory(inv);

        Party party = plugin.getPartyManager().getParty(player);
        if (party == null) {
            inv.setItem(13, icon(Material.NETHER_STAR, "Create Party"));
        } else {
            inv.setItem(11, icon(Material.PLAYER_HEAD, "Invite Player"));
            inv.setItem(13, icon(Material.EMERALD, "Start FFA (first kit)"));
            inv.setItem(15, icon(Material.REDSTONE, "Leave Party"));
        }
        inv.setItem(22, icon(Material.ARROW, "Back"));

        player.openInventory(inv);
    }

    public void openPartyInvite(Player player) {
        DuelMenuHolder holder = new DuelMenuHolder(DuelMenuHolder.Type.PARTY_INVITE);
        List<Player> others = onlineOthersThan(player);
        int size = gridSize(others.size());
        Inventory inv = Bukkit.createInventory(holder, size, Component.text("Invite to Party"));
        holder.setInventory(inv);

        fillPlayerHeads(inv, others, size);
        inv.setItem(size - 1, icon(Material.ARROW, "Back"));

        player.openInventory(inv);
    }

    private List<Player> onlineOthersThan(Player player) {
        List<Player> others = new ArrayList<>();
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (!p.equals(player)) others.add(p);
        }
        return others;
    }

    private int gridSize(int itemCount) {
        int rows = Math.max(1, (itemCount / 9) + 1);
        return Math.min(rows * 9 + 9, 54); // +9 reserves a row for the back/close button
    }

    private void fillPlayerHeads(Inventory inv, List<Player> players, int size) {
        int slot = 0;
        for (Player p : players) {
            if (slot >= size - 9) break;
            ItemStack skull = new ItemStack(Material.PLAYER_HEAD);
            SkullMeta meta = (SkullMeta) skull.getItemMeta();
            meta.setOwningPlayer(p);
            meta.displayName(Component.text(p.getName()).color(NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false));
            skull.setItemMeta(meta);
            inv.setItem(slot++, skull);
        }
    }
}
