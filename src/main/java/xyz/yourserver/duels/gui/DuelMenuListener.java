package xyz.yourserver.duels.gui;

import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import xyz.yourserver.duels.DuelsPlugin;
import xyz.yourserver.duels.model.Kit;
import xyz.yourserver.duels.model.Party;
import xyz.yourserver.duels.util.Msg;

import java.util.ArrayList;
import java.util.List;

public class DuelMenuListener implements Listener {

    private final DuelsPlugin plugin;
    private final DuelMenu menu;

    public DuelMenuListener(DuelsPlugin plugin, DuelMenu menu) {
        this.plugin = plugin;
        this.menu = menu;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (!hasMenuItem(player)) {
            player.getInventory().setItem(8, menu.menuItem());
        }
    }

    private boolean hasMenuItem(Player player) {
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.isSimilar(menu.menuItem())) {
                return true;
            }
        }
        return false;
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        ItemStack item = event.getItem();
        if (item == null || !item.isSimilar(menu.menuItem())) return;
        event.setCancelled(true);
        menu.openMain(event.getPlayer());
    }

    @EventHandler
    public void onDrop(PlayerDropItemEvent event) {
        if (event.getItemDrop().getItemStack().isSimilar(menu.menuItem())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof DuelMenuHolder holder)) return;
        event.setCancelled(true);

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) return;
        Player player = (Player) event.getWhoClicked();

        switch (holder.getType()) {
            case MAIN -> handleMainClick(player, clicked);
            case CHALLENGE -> handleChallengeClick(player, clicked);
            case QUEUE_KIT -> handleQueueKitClick(player, clicked);
            case PARTY -> handlePartyClick(player, clicked);
            case PARTY_INVITE -> handlePartyInviteClick(player, clicked);
        }
    }

    private String plainName(ItemStack item) {
        if (item.getItemMeta() == null || item.getItemMeta().displayName() == null) return "";
        return PlainTextComponentSerializer.plainText().serialize(item.getItemMeta().displayName());
    }

    private void handleMainClick(Player player, ItemStack clicked) {
        switch (plainName(clicked)) {
            case "Challenge a Player" -> menu.openChallenge(player);
            case "Join Queue" -> menu.openQueueKits(player);
            case "Party" -> menu.openParty(player);
            case "Close" -> player.closeInventory();
        }
    }

    private void handleChallengeClick(Player player, ItemStack clicked) {
        String name = plainName(clicked);
        if (name.equals("Back")) {
            menu.openMain(player);
            return;
        }
        if (clicked.getType() != Material.PLAYER_HEAD) return;

        SkullMeta meta = (SkullMeta) clicked.getItemMeta();
        if (meta == null || meta.getOwningPlayer() == null) return;
        Player target = Bukkit.getPlayer(meta.getOwningPlayer().getUniqueId());
        if (target == null) {
            Msg.send(player, "&cThat player just left.");
            return;
        }
        if (plugin.getDuelManager().inSession(player) || plugin.getDuelManager().inSession(target)) {
            Msg.send(player, "&cOne of you is already in a duel.");
            player.closeInventory();
            return;
        }
        Kit kit = plugin.getKitManager().getKits().values().stream().findFirst().orElse(null);
        if (kit == null) {
            Msg.send(player, "&cNo kits exist yet.");
            player.closeInventory();
            return;
        }
        player.closeInventory();
        plugin.getDuelManager().issueChallenge(player, target, kit);
    }

    private void handleQueueKitClick(Player player, ItemStack clicked) {
        String name = plainName(clicked);
        if (name.equals("Back")) {
            menu.openMain(player);
            return;
        }
        Kit kit = plugin.getKitManager().getKit(name);
        if (kit == null) return;
        player.closeInventory();
        plugin.getQueueManager().join(player, kit);
    }

    private void handlePartyClick(Player player, ItemStack clicked) {
        switch (plainName(clicked)) {
            case "Back" -> menu.openMain(player);
            case "Create Party" -> {
                plugin.getPartyManager().createParty(player);
                menu.openParty(player);
            }
            case "Invite Player" -> menu.openPartyInvite(player);
            case "Leave Party" -> {
                plugin.getPartyManager().leave(player);
                player.closeInventory();
            }
            case "Start FFA (first kit)" -> {
                Party party = plugin.getPartyManager().getParty(player);
                Kit kit = plugin.getKitManager().getKits().values().stream().findFirst().orElse(null);
                if (party == null || kit == null || party.getMembers().size() < 2) {
                    Msg.send(player, "&cNeed a party of 2+ and at least one kit to start an FFA.");
                    player.closeInventory();
                    return;
                }
                player.closeInventory();
                plugin.getDuelManager().startSession(new ArrayList<>(party.getMembers()), List.of(), true, kit);
            }
        }
    }

    private void handlePartyInviteClick(Player player, ItemStack clicked) {
        String name = plainName(clicked);
        if (name.equals("Back")) {
            menu.openParty(player);
            return;
        }
        if (clicked.getType() != Material.PLAYER_HEAD) return;

        SkullMeta meta = (SkullMeta) clicked.getItemMeta();
        if (meta == null || meta.getOwningPlayer() == null) return;
        Player target = Bukkit.getPlayer(meta.getOwningPlayer().getUniqueId());
        if (target == null) return;
        plugin.getPartyManager().invite(player, target);
        player.closeInventory();
    }
}
