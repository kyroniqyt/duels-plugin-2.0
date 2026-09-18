package xyz.yourserver.duels.model;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

/**
 * A saved loadout. Bukkit's YamlConfiguration natively serializes ItemStack,
 * so KitManager stores these directly — no manual base64 encoding needed.
 */
public class Kit {

    private final String name;
    private ItemStack[] contents = new ItemStack[36]; // main inventory, includes hotbar
    private ItemStack[] armor = new ItemStack[4];      // boots, leggings, chestplate, helmet order (Bukkit convention)
    private ItemStack offHand;

    public Kit(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public ItemStack[] getContents() {
        return contents;
    }

    public void setContents(ItemStack[] contents) {
        this.contents = contents;
    }

    public ItemStack[] getArmor() {
        return armor;
    }

    public void setArmor(ItemStack[] armor) {
        this.armor = armor;
    }

    public ItemStack getOffHand() {
        return offHand;
    }

    public void setOffHand(ItemStack offHand) {
        this.offHand = offHand;
    }

    /** Captures a player's current inventory as this kit's contents. */
    public static Kit captureFrom(String name, Player player) {
        Kit kit = new Kit(name);
        PlayerInventory inv = player.getInventory();
        kit.setContents(inv.getContents().clone());
        kit.setArmor(inv.getArmorContents().clone());
        kit.setOffHand(inv.getItemInOffHand().clone());
        return kit;
    }

    /** Wipes and re-equips a player with this kit. */
    public void applyTo(Player player) {
        PlayerInventory inv = player.getInventory();
        inv.clear();
        inv.setContents(cloneArray(contents, 36));
        inv.setArmorContents(cloneArray(armor, 4));
        if (offHand != null) {
            inv.setItemInOffHand(offHand.clone());
        }
        player.updateInventory();
    }

    private ItemStack[] cloneArray(ItemStack[] src, int size) {
        ItemStack[] out = new ItemStack[size];
        for (int i = 0; i < size && i < src.length; i++) {
            out[i] = src[i] == null ? null : src[i].clone();
        }
        return out;
    }
}
