package xyz.yourserver.duels.manager;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import xyz.yourserver.duels.DuelsPlugin;
import xyz.yourserver.duels.model.Kit;

import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

public class KitManager {

    private final DuelsPlugin plugin;
    private final File file;
    private final Map<String, Kit> kits = new LinkedHashMap<>();

    public KitManager(DuelsPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "kits.yml");
        load();
    }

    public void load() {
        kits.clear();
        if (!file.exists()) return;

        YamlConfiguration yml = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection kitsSection = yml.getConfigurationSection("kits");
        if (kitsSection == null) return;

        for (String name : kitsSection.getKeys(false)) {
            ConfigurationSection kitSection = kitsSection.getConfigurationSection(name);
            if (kitSection == null) continue;

            Kit kit = new Kit(name);
            kit.setContents(readItemArray(kitSection, "contents", 36));
            kit.setArmor(readItemArray(kitSection, "armor", 4));
            kit.setOffHand(kitSection.getItemStack("offhand"));
            kits.put(name.toLowerCase(), kit);
        }
    }

    private ItemStack[] readItemArray(ConfigurationSection section, String key, int size) {
        ItemStack[] arr = new ItemStack[size];
        ConfigurationSection sub = section.getConfigurationSection(key);
        if (sub == null) return arr;
        for (String indexStr : sub.getKeys(false)) {
            try {
                int index = Integer.parseInt(indexStr);
                if (index >= 0 && index < size) {
                    arr[index] = sub.getItemStack(indexStr);
                }
            } catch (NumberFormatException ignored) {
            }
        }
        return arr;
    }

    public void save() {
        YamlConfiguration yml = new YamlConfiguration();
        for (Kit kit : kits.values()) {
            String base = "kits." + kit.getName();
            writeItemArray(yml, base + ".contents", kit.getContents());
            writeItemArray(yml, base + ".armor", kit.getArmor());
            if (kit.getOffHand() != null) {
                yml.set(base + ".offhand", kit.getOffHand());
            }
        }
        try {
            yml.save(file);
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to save kits.yml: " + e.getMessage());
        }
    }

    private void writeItemArray(YamlConfiguration yml, String path, ItemStack[] items) {
        for (int i = 0; i < items.length; i++) {
            if (items[i] != null) {
                yml.set(path + "." + i, items[i]);
            }
        }
    }

    public void addKit(Kit kit) {
        kits.put(kit.getName().toLowerCase(), kit);
        save();
    }

    public boolean removeKit(String name) {
        boolean removed = kits.remove(name.toLowerCase()) != null;
        if (removed) save();
        return removed;
    }

    public Kit getKit(String name) {
        return kits.get(name.toLowerCase());
    }

    public Map<String, Kit> getKits() {
        return kits;
    }
}
