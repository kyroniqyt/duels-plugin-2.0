package xyz.yourserver.duels;

import de.eisi05.npc.api.NpcApi;
import de.eisi05.npc.api.objects.NpcConfig;
import org.bukkit.plugin.java.JavaPlugin;
import xyz.yourserver.duels.command.DuelArenaCommand;
import xyz.yourserver.duels.command.DuelCommand;
import xyz.yourserver.duels.command.DuelKitCommand;
import xyz.yourserver.duels.command.PartyCommand;
import xyz.yourserver.duels.listener.ConnectionListener;
import xyz.yourserver.duels.listener.DuelListener;
import xyz.yourserver.duels.manager.ArenaManager;
import xyz.yourserver.duels.manager.DuelManager;
import xyz.yourserver.duels.manager.KitManager;
import xyz.yourserver.duels.manager.PartyManager;
import xyz.yourserver.duels.manager.QueueManager;
import xyz.yourserver.duels.bot.BotManager;
import xyz.yourserver.duels.util.Msg;
import xyz.yourserver.duels.util.SchematicUtil;

public class DuelsPlugin extends JavaPlugin {

    private KitManager kitManager;
    private ArenaManager arenaManager;
    private DuelManager duelManager;
    private QueueManager queueManager;
    private PartyManager partyManager;
    private BotManager botManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        Msg.init(getConfig());

        getDataFolder().mkdirs();

        // NpcAPI is shaded directly into this jar, so it needs to be
        // initialized/torn down here rather than relying on a separate
        // NPC plugin being installed on the server.
        NpcApi.createInstance(this, new NpcConfig().debug(false).autoUpdate(false));

        kitManager = new KitManager(this);
        arenaManager = new ArenaManager(this);
        duelManager = new DuelManager(this);
        queueManager = new QueueManager(this);
        partyManager = new PartyManager(this);
        botManager = new BotManager(this);

        getCommand("duel").setExecutor(new DuelCommand(this));
        getCommand("duelkit").setExecutor(new DuelKitCommand(this));
        getCommand("duelarena").setExecutor(new DuelArenaCommand(this));
        getCommand("party").setExecutor(new PartyCommand(this));

        getServer().getPluginManager().registerEvents(new DuelListener(this), this);
        getServer().getPluginManager().registerEvents(new ConnectionListener(this), this);

        if (!SchematicUtil.isAvailable()) {
            getLogger().warning("WorldEdit/FAWE not found — arena auto-restore will fail until it's installed.");
        }

        getLogger().info("DuelsPlugin enabled.");
    }

    @Override
    public void onDisable() {
        NpcApi.disable();
        getLogger().info("DuelsPlugin disabled.");
    }

    public KitManager getKitManager() {
        return kitManager;
    }

    public ArenaManager getArenaManager() {
        return arenaManager;
    }

    public DuelManager getDuelManager() {
        return duelManager;
    }

    public QueueManager getQueueManager() {
        return queueManager;
    }

    public PartyManager getPartyManager() {
        return partyManager;
    }

    public BotManager getBotManager() {
        return botManager;
    }
}
