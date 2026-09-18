package xyz.yourserver.duels.model;

import org.bukkit.Location;
import org.bukkit.World;

/**
 * A duel arena: a cuboid region (for schematic capture/restore) plus two
 * team spawn points. pos1/pos2 define the corners used when saving the
 * schematic; spawn1/spawn2 are where the two sides are teleported.
 */
public class Arena {

    private final String name;
    private World world;
    private Location pos1;
    private Location pos2;
    private Location spawn1;
    private Location spawn2;
    private boolean inUse = false;

    public Arena(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public World getWorld() {
        return world;
    }

    public void setWorld(World world) {
        this.world = world;
    }

    public Location getPos1() {
        return pos1;
    }

    public void setPos1(Location pos1) {
        this.pos1 = pos1;
    }

    public Location getPos2() {
        return pos2;
    }

    public void setPos2(Location pos2) {
        this.pos2 = pos2;
    }

    public Location getSpawn1() {
        return spawn1;
    }

    public void setSpawn1(Location spawn1) {
        this.spawn1 = spawn1;
    }

    public Location getSpawn2() {
        return spawn2;
    }

    public void setSpawn2(Location spawn2) {
        this.spawn2 = spawn2;
    }

    public boolean isInUse() {
        return inUse;
    }

    public void setInUse(boolean inUse) {
        this.inUse = inUse;
    }

    /** True once pos1/pos2/spawn1/spawn2 are all set — arena is ready to fight in. */
    public boolean isConfigured() {
        return pos1 != null && pos2 != null && spawn1 != null && spawn2 != null;
    }

    public String schematicFileName() {
        return name + ".schem";
    }
}
