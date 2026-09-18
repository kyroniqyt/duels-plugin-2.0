# DuelsPlugin

A PvP-Club-style duels plugin for a single Paper/Spigot server: direct
challenges, matchmaking queue, kits, party team-duels, party FFA ("host
battles"), and arenas that auto-restore via WorldEdit/FAWE after every
match.

## Requirements

- Java 21
- Paper (or Spigot) 1.21.x
- **WorldEdit or FastAsyncWorldEdit installed on the server** — required
  for `/duelarena save` and for arena auto-restore. Without it the plugin
  still loads, but arenas won't reset between duels.
- Maven, to build the jar

## Build

```
mvn clean package
```

The output jar will be at `target/duelsplugin-1.0.0.jar`. Drop it into
your server's `plugins/` folder and restart.

> I wrote and reviewed this code carefully, but couldn't compile it
> myself — this container has no network access to pull the Paper/
> WorldEdit Maven dependencies. Run `mvn clean package` yourself and send
> me any compile errors; I'll fix them fast.

## First-time setup (as an op)

1. **Create a kit** — hold the items you want in your hotbar/inventory,
   then:
   ```
   /duelkit create sword
   ```
2. **Build and register an arena** — stand at one corner of the arena:
   ```
   /duelarena create arena1
   /duelarena pos1 arena1        (at one corner, ground level)
   /duelarena pos2 arena1        (at the opposite corner, ground level)
   /duelarena setspawn1 arena1   (where side A starts)
   /duelarena setspawn2 arena1   (where side B starts)
   /duelarena save arena1        (captures the schematic for auto-restore)
   ```
3. Repeat for as many arenas as you want — duels pick the first free one.

## Player commands

```
/duel <player> [kit]     challenge someone directly
/duel accept | deny      respond to a challenge
/duel queue <kit>        join matchmaking for that kit
/duel leavequeue
/duel kits                list available kits

/party create
/party invite <player>
/party accept <leader>
/party leave
/party kick <player>
/party ffa <kit>          host battle — all party members fight, last one standing wins
/party duel <leader>      challenge another equal-size party to a team duel
```

## Design notes (why it's built this way)

- **One engine for 1v1, party duels, and FFA.** `DuelSession` holds two
  *team* lists (`sideA`/`sideB`) instead of two players. A 1v1 is just
  two teams of one; an FFA is `sideB` empty with an `ffa` flag. This means
  team duels and host battles didn't need separate combat code.
- **No real deaths.** Lethal damage is cancelled right before it lands and
  handled as an "elimination" (heal, flip to spectator, check win
  condition). This avoids respawn screens, item drops, and keepInventory
  edge cases entirely.
- **Arena restore via WorldEdit/FAWE schematic paste**, not a manual
  block-change log — see the comparison I gave you in chat. Simpler and
  always exactly correct, at the cost of restore time scaling with arena
  volume (fine for duel-sized arenas).

## Known gaps / good next steps

- No GUI menus yet — everything is command-based. A kit-select inventory
  GUI on `/duel queue` would be a natural next step.
- Queue matching is plain FIFO, not ELO-based. There's no ranking system
  yet at all. `QueueManager.tryMatch` is the one place to change if you
  add ELO — `DuelManager.startSession` doesn't care how two teams were
  chosen.
- No per-arena "disabled kits" or region-based PvP blocking outside
  arenas — currently PvP is blocked everywhere except inside an active
  session.
- No stats/leaderboards persistence yet.
