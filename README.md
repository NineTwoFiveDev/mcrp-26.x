# DarkRP for Paper 26.x

A self-contained recreation of Garry's Mod's DarkRP gamemode as a Paper plugin,
targeting Minecraft/Paper **26.2 "Chaos Cubed"** (Java 25). No Vault, LuckPerms
or PlaceholderAPI required — economy, jobs and permissions are all handled
internally.

## Features

- **Jobs** — `/jobs` opens a GUI to pick a class (Citizen, Police, Mayor, Medic,
  Gun Dealer, Hobo by default). Jobs can have a salary, a slot limit, a starter
  kit, whitelist permissions, and a "police" flag used by the crime system.
  Fully configurable in `jobs.yml`.
- **Economy** — every player has a balance, paid a salary on an interval,
  with `/money`, `/pay`, and admin tools (`/darkrp setbalance`, `/darkrp give`).
- **Doors** — sneak + right-click any door to see a buy/sell/lock menu (classic
  DarkRP). Double doors are detected and bought/sold/locked as one unit.
  `/door buy|sell|lock|unlock|add|kick|price` manages the door you're looking at.
- **Crime & law** — `/wanted`, `/unwanted`, `/arrest`, `/release` (police-only,
  either via the `police` job flag or the `darkrp.police` permission) and
  `/mug` (anyone can attempt to mug a nearby, non-police player for a cut of
  their cash).
- **Money printers** — `/printer buy` places a marker block on top of the
  block you're looking at; it pays its owner on an interval, with a
  configurable per-cycle chance of "busting" (being destroyed). `/printer
  sell` cashes one back in, `/printer confiscate` lets police seize one.
- **Shipments** — `/shipment list`/`buy <type>` places a chest filled with a
  configured item (weapons, medkits, ...) on top of the block you're looking
  at, gated by job where configured (`shipments.yml`). `/shipment confiscate`
  lets police seize one.
- **Kidnapping & ransom** — `/kidnap <player> <ransom>` restrains a nearby
  player (heavily slowed/weakened, snapped back if they wander off) and
  broadcasts a ransom demand; anyone can free them with `/payransom
  <player>`, police can override with `/freekidnap <player>`, and it
  auto-releases after a timeout.
- **Warrants** — `/warrant <player> [seconds]` (police-only) lets police
  bypass that player's locked doors for the warrant's duration, for raids.

## Project layout

```
src/main/java/com/mcrp/darkrp/
  DarkRPPlugin.java        - main class, wiring and scheduled tasks
  model/                   - PlayerRecord, Job, DoorRecord, PrinterRecord, ShipmentType
  storage/                 - DataStore (players.yml persistence)
  economy/                 - EconomyManager
  job/                     - JobManager, job GUI
  door/                    - DoorManager, door interact listener
  crime/                   - WantedManager, JailManager, MugManager, KidnapManager, WarrantManager
  printer/                 - PrinterManager, printer interact listener
  shipment/                - ShipmentManager
  commands/                - one CommandExecutor per command
  listeners/               - join/quit
  util/                    - Msg (MiniMessage), ItemUtil, LocUtil
src/main/resources/
  plugin.yml, config.yml, jobs.yml, shipments.yml
```

## Building

This project uses Gradle. It depends on `paper-api`, which is only published
on PaperMC's own repository (`repo.papermc.io`) — **not** Maven Central.
Build it from a network that can reach that host.

The Gradle wrapper *scripts* (`gradlew`, `gradlew.bat`,
`gradle/wrapper/gradle-wrapper.properties`) are included, but the wrapper
*jar* itself (a binary file) is intentionally not committed — it couldn't be
pushed through the text-only API path this session had to use. Regenerate it
once with a local Gradle install:

```
gradle wrapper --gradle-version 8.14.3
./gradlew build
```

Or, if you already have Gradle 8.x installed, just run `gradle build`
directly without the wrapper.

The resulting jar is at `build/libs/DarkRP-1.0.0.jar`. Drop it into your
server's `plugins/` folder.

> **Note on this session:** the sandbox this plugin was written in blocks
> outbound access to `repo.papermc.io` (an organization egress policy, not a
> transient error), and only has JDK 21 installed, not the JDK 25 that Paper
> 26.x's toolchain requires. So `paper-api` could not be downloaded and
> `./gradlew build` could not be run to verify compilation here. The code was
> written and reviewed carefully against the standard Bukkit/Paper API, but
> please run a real build (and a smoke test on a local server) before
> deploying — and let me know if anything doesn't compile so it can be fixed.

If your server actually runs an older/newer Paper version, bump the
`compileOnly("io.papermc.paper:paper-api:...")` coordinate in
`build.gradle.kts` and the `api-version` in `plugin.yml` to match.

## Configuration

- `config.yml` — currency symbol, starting balance, salary interval, door
  prices/resale %, jail location & default sentence, wanted duration, mug
  tuning (range, channel time, steal %, cooldown), printer tuning (price,
  payout, bust chance, limits), kidnap tuning (range, radius, timeout,
  cooldown) and the default warrant duration.
- `jobs.yml` — add/edit/remove jobs. Kit items use `MATERIAL:AMOUNT` strings.
- `shipments.yml` — add/edit/remove buyable shipment types (item, amount,
  price, allowed jobs).
- `/darkrp reload` reloads `config.yml`, `jobs.yml` and `shipments.yml`
  without a restart.

## Permissions

- `darkrp.admin` (default: op) — full admin access (`/setjob`, `/darkrp ...`,
  and viewing other players' balances).
- `darkrp.police` (default: op) — staff override for police-only commands.
  Normally, players get police powers simply by being on a job with
  `police: true` in `jobs.yml` — no permission plugin needed.
- `darkrp.job.<id>` — required to select a job marked `whitelisted: true`.

## Known limitations / good next steps

- Door groups are detected by scanning contiguous door blocks in the 4
  horizontal directions at the same Y level (handles single and double
  doors); L-shaped or diagonal door clusters won't be grouped.
- Money printers only tick while their chunk is loaded (a printer in an
  unloaded area just pauses rather than accruing income), to avoid forcing
  chunks to stay loaded.
- Kidnap and warrant state is in-memory only (reset on restart) rather than
  persisted, since both are meant to be short-lived.
- No raid-timer/NLR (new-life-rule) enforcement — those remain server rules
  rather than plugin-enforced mechanics.
