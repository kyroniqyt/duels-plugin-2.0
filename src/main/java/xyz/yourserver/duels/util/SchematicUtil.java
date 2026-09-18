package xyz.yourserver.duels.util;

import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.extent.clipboard.BlockArrayClipboard;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormat;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormats;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardReader;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardWriter;
import com.sk89q.worldedit.function.operation.ForwardExtentCopy;
import com.sk89q.worldedit.function.operation.Operation;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.session.ClipboardHolder;
import com.sk89q.worldedit.world.World;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Wraps WorldEdit/FAWE's clipboard API for one purpose: snapshot an arena
 * region once, then paste it back over itself after every duel so arenas
 * never accumulate damage. FAWE is a drop-in for this API and is used
 * automatically (and much faster) if installed instead of vanilla WorldEdit.
 */
public class SchematicUtil {

    /** True if either WorldEdit or FAWE is present and enabled. */
    public static boolean isAvailable() {
        Plugin we = Bukkit.getPluginManager().getPlugin("WorldEdit");
        return we != null && we.isEnabled();
    }

    /**
     * Saves the cuboid between corner1 and corner2 (inclusive) to a .schem
     * file inside the plugin's schematics folder.
     */
    public static void save(File schematicFile, Location corner1, Location corner2) throws IOException {
        if (!corner1.getWorld().equals(corner2.getWorld())) {
            throw new IllegalArgumentException("Arena corners must be in the same world");
        }

        World weWorld = BukkitAdapter.adapt(corner1.getWorld());
        BlockVector3 min = BlockVector3.at(
                Math.min(corner1.getBlockX(), corner2.getBlockX()),
                Math.min(corner1.getBlockY(), corner2.getBlockY()),
                Math.min(corner1.getBlockZ(), corner2.getBlockZ()));
        BlockVector3 max = BlockVector3.at(
                Math.max(corner1.getBlockX(), corner2.getBlockX()),
                Math.max(corner1.getBlockY(), corner2.getBlockY()),
                Math.max(corner1.getBlockZ(), corner2.getBlockZ()));

        CuboidRegion region = new CuboidRegion(weWorld, min, max);
        BlockArrayClipboard clipboard = new BlockArrayClipboard(region);
        clipboard.setOrigin(min);

        try (EditSession editSession = WorldEdit.getInstance().newEditSessionBuilder()
                .world(weWorld)
                .build()) {

            ForwardExtentCopy copy = new ForwardExtentCopy(editSession, region, clipboard, region.getMinimumPoint());
            copy.setCopyingEntities(false);
            Operations.complete(copy);

            schematicFile.getParentFile().mkdirs();
            ClipboardFormat format = ClipboardFormats.findByAlias("sponge.3");
            if (format == null) {
                format = ClipboardFormats.findByAlias("sponge");
            }
            try (FileOutputStream fos = new FileOutputStream(schematicFile);
                 ClipboardWriter writer = format.getWriter(fos)) {
                writer.write(clipboard);
            }
        } catch (Exception e) {
            throw new IOException("Failed to save schematic: " + e.getMessage(), e);
        }
    }

    /**
     * Pastes a previously-saved schematic back at the same corner it was
     * captured from, overwriting whatever is there now. This is what
     * "restores" an arena between duels.
     */
    public static void restore(File schematicFile, Location pasteAtMinCorner) throws IOException {
        if (!schematicFile.exists()) {
            throw new IOException("Schematic file not found: " + schematicFile.getName());
        }

        ClipboardFormat format = ClipboardFormats.findByFile(schematicFile);
        if (format == null) {
            throw new IOException("Unrecognized schematic format: " + schematicFile.getName());
        }

        World weWorld = BukkitAdapter.adapt(pasteAtMinCorner.getWorld());
        BlockVector3 pasteOrigin = BlockVector3.at(
                pasteAtMinCorner.getBlockX(), pasteAtMinCorner.getBlockY(), pasteAtMinCorner.getBlockZ());

        try (FileInputStream fis = new FileInputStream(schematicFile);
             ClipboardReader reader = format.getReader(fis)) {

            Clipboard clipboard = reader.read();

            try (EditSession editSession = WorldEdit.getInstance().newEditSessionBuilder()
                    .world(weWorld)
                    .build()) {
                Operation operation = new ClipboardHolder(clipboard)
                        .createPaste(editSession)
                        .to(pasteOrigin)
                        .ignoreAirBlocks(false)
                        .build();
                Operations.complete(operation);
            }
        } catch (Exception e) {
            throw new IOException("Failed to restore schematic: " + e.getMessage(), e);
        }
    }
}
