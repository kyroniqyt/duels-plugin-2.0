package xyz.yourserver.duels.gui;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.jetbrains.annotations.NotNull;

/**
 * Tags an open Inventory as one of our menu screens, so the click listener
 * can route clicks reliably (checking the holder type) instead of matching
 * on the inventory's title text, which breaks easily.
 */
public class DuelMenuHolder implements InventoryHolder {

    public enum Type {
        MAIN, CHALLENGE, QUEUE_KIT, PARTY, PARTY_INVITE
    }

    private final Type type;
    private Inventory inventory;

    public DuelMenuHolder(Type type) {
        this.type = type;
    }

    public Type getType() {
        return type;
    }

    public void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }

    @Override
    public @NotNull Inventory getInventory() {
        return inventory;
    }
}
