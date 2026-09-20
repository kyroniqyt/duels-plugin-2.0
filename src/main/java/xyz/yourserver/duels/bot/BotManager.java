package xyz.yourserver.duels.bot;

import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.npc.NPC;
import net.citizensnpcs.api.npc.NPCRegistry;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import xyz.yourserver.duels.DuelsPlugin;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class BotManager {

    private final DuelsPlugin plugin;
    private final Map<UUID, DuelBot> botsByEntityUuid = new ConcurrentHashMap<>();
    private int botCounter = 0;

    public BotManager(DuelsPlugin plugin) {
        this.plugin = plugin;
    }

    public static boolean isCitizensAvailable() {
        return Bukkit.getPluginManager().isPluginEnabled("Citizens");
    }

    public DuelBot spawnBot(Location at, BotDifficulty difficulty) {
        NPCRegistry registry = CitizensAPI.getNPCRegistry();
        botCounter++;
        NPC npc = registry.createNPC(EntityType.PLAYER, "Bot_" + botCounter);
        npc.spawn(at);

        DuelBot bot = new DuelBot(npc, difficulty);
        bot.equipSword();
        botsByEntityUuid.put(bot.getEntityUuid(), bot);
        return bot;
    }

    public boolean isBotEntity(Entity entity) {
        return botsByEntityUuid.containsKey(entity.getUniqueId());
    }

    public DuelBot getBotByEntity(Entity entity) {
        return botsByEntityUuid.get(entity.getUniqueId());
    }

    public DuelBot getBotByUuid(UUID uuid) {
        return botsByEntityUuid.get(uuid);
    }

    public void removeBot(DuelBot bot) {
        botsByEntityUuid.remove(bot.getEntityUuid());
        bot.stopAi();
        if (bot.getNpc().isSpawned()) {
            bot.getNpc().despawn();
        }
        CitizensAPI.getNPCRegistry().deregister(bot.getNpc());
    }
}
