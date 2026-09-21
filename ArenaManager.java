package xyz.yourserver.duels.manager;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import xyz.yourserver.duels.DuelsPlugin;
import xyz.yourserver.duels.model.Arena;
import xyz.yourserver.duels.util.SchematicUtil;

import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class ArenaManager {

    private final DuelsPlugin plugin;
    private final File file;
    private final File schematicsFolder;
    private final Map<String, Arena> arenas = new LinkedHashMap<>();

    public ArenaManager(DuelsPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "arenas.yml");
        this.schematicsFolder = new File(plugin.getDataFolder(), "schematics");
        load();
    }

    public void load() {
        arenas.clear();
        if (!file.exists()) return;

        YamlConfiguration yml = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection section = yml.getConfigurationSection("arenas");
        if (section == null) return;

        for (String name : section.getKeys(false)) {
            ConfigurationSection s = section.getConfigurationSection(name);
            if (s == null) continue;

            Arena arena = new Arena(name);
            String worldName = s.getString("world");
            if (worldName != null) {
                World world = Bukkit.getWorld(worldName);
                arena.setWorld(world);
                if (world != null) {
                    arena.setPos1(readLoc(s, "pos1", world));
                    arena.setPos2(readLoc(s, "pos2", world));
                    arena.setSpawn1(readLoc(s, "spawn1", world));
                    arena.setSpawn2(readLoc(s, "spawn2", world));
                }
            }
            arenas.put(name.toLowerCase(), arena);
        }
    }

    private Location readLoc(ConfigurationSection s, String key, World world) {
        if (!s.contains(key)) return null;
        return new Location(world,
                s.getDouble(key + ".x"),
                s.getDouble(key + ".y"),
                s.getDouble(key + ".z"),
                (float) s.getDouble(key + ".yaw"),
                (float) s.getDouble(key + ".pitch"));
    }

    private void writeLoc(YamlConfiguration yml, String base, Location loc) {
        if (loc == null) return;
        yml.set(base + ".x", loc.getX());
        yml.set(base + ".y", loc.getY());
        yml.set(base + ".z", loc.getZ());
        yml.set(base + ".yaw", loc.getYaw());
        yml.set(base + ".pitch", loc.getPitch());
    }

    public void save() {
        YamlConfiguration yml = new YamlConfiguration();
        for (Arena arena : arenas.values()) {
            String base = "arenas." + arena.getName();
            if (arena.getWorld() != null) {
                yml.set(base + ".world", arena.getWorld().getName());
            }
            writeLoc(yml, base + ".pos1", arena.getPos1());
            writeLoc(yml, base + ".pos2", arena.getPos2());
            writeLoc(yml, base + ".spawn1", arena.getSpawn1());
            writeLoc(yml, base + ".spawn2", arena.getSpawn2());
        }
        try {
            yml.save(file);
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to save arenas.yml: " + e.getMessage());
        }
    }

    public Arena createArena(String name) {
        Arena arena = new Arena(name);
        arenas.put(name.toLowerCase(), arena);
        save();
        return arena;
    }

    public Arena getArena(String name) {
        return arenas.get(name.toLowerCase());
    }

    public Map<String, Arena> getArenas() {
        return arenas;
    }

    /** First arena that is configured and not currently in use. */
    public Optional<Arena> getFreeArena() {
        return arenas.values().stream()
                .filter(Arena::isConfigured)
                .filter(a -> !a.isInUse())
                .findFirst();
    }

    /** Captures the arena's region into a schematic file. Requires WorldEdit/FAWE. */
    public void captureSchematic(Arena arena) throws IOException {
        if (!SchematicUtil.isAvailable()) {
            throw new IOException("WorldEdit/FAWE is not installed or enabled");
        }
        if (arena.getPos1() == null || arena.getPos2() == null) {
            throw new IOException("Set both pos1 and pos2 before saving");
        }
        File target = new File(schematicsFolder, arena.schematicFileName());
        SchematicUtil.save(target, arena.getPos1(), arena.getPos2());
    }

    /** Pastes the arena's saved schematic back over the region, undoing all duel damage. */
    public void restoreArena(Arena arena) throws IOException {
        if (!SchematicUtil.isAvailable()) {
            throw new IOException("WorldEdit/FAWE is not installed or enabled");
        }
        File source = new File(schematicsFolder, arena.schematicFileName());

        Location min = new Location(arena.getWorld(),
                Math.min(arena.getPos1().getBlockX(), arena.getPos2().getBlockX()),
                Math.min(arena.getPos1().getBlockY(), arena.getPos2().getBlockY()),
                Math.min(arena.getPos1().getBlockZ(), arena.getPos2().getBlockZ()));

        SchematicUtil.restore(source, min);
    }
}
