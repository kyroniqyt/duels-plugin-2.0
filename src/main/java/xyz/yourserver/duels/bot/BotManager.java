package xyz.yourserver.duels.bot;

import xyz.yourserver.duels.DuelsPlugin;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class BotManager {

    private final DuelsPlugin plugin;
    private final Map<UUID, DuelBot> bots = new ConcurrentHashMap<>();

    public BotManager(DuelsPlugin plugin) {
        this.plugin = plugin;
    }

    /** Registers a new bot. Its actual NPC isn't spawned until the duel's arena spawn point is known. */
    public DuelBot spawnBot(BotDifficulty difficulty) {
        DuelBot bot = new DuelBot(difficulty);
        bots.put(bot.getEntityUuid(), bot);
        return bot;
    }

    public DuelBot getBotByUuid(UUID uuid) {
        return bots.get(uuid);
    }

    public void removeBot(DuelBot bot) {
        bots.remove(bot.getEntityUuid());
        bot.remove();
    }
}
