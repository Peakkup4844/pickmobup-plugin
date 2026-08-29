# PickMobUp

![Minecraft](https://img.shields.io/badge/Minecraft-1.20.1%E2%80%93latest-brightgreen)
![Server](https://img.shields.io/badge/Server-Spigot%20%7C%20Paper%20%7C%20Folia-blue)
![Java](https://img.shields.io/badge/Java-17%2B-orange)

A Minecraft plugin that lets you **pick an entity up onto your head** and **launch it like a slingshot**.

Supports **Spigot / Paper / Folia** from **1.20.1** through the latest version (build-tested against 1.21.x).

## Requirements

- A Spigot / Paper / Folia server, version 1.20.1 or newer
- Java 17 or newer
- (Optional) [PacketEvents](https://github.com/retrooper/packetevents) — only needed when `mount-mode: PACKET` is selected

## Installation

1. Download or build `PickMobUp-<version>.jar` (see [Building](#building))
2. Drop the file into your server's `plugins/` folder
3. Restart the server — this generates `plugins/PickMobUp/config.yml` and `lang.yml` for you to edit
4. Run `/pmu reload` to apply config changes without restarting

## How to play

| Action | Effect |
|---|---|
| **Sneak + right-click** an entity | Pick it up onto your head |
| **Tap Shift briefly** (while carrying) | Set the entity down where you're standing |
| **Hold Shift** (while carrying) | Charge power — a bar animates on the actionbar |
| **Release Shift** (while charging) | Launch the entity toward your look direction, with force based on the charge at release |

While airborne, the entity gets **Slow Falling** until it touches the ground.

## Commands / Permissions

- `/pmu reload` — reload config and lang files (`pickmobup.admin`)
- `pickmobup.use` — can pick up/throw entities (default: true)
- `pickmobup.carryplayers` — can pick up other players (default: op)

## Configuration

- `config.yml` — carry mode, entity type white/blacklist, whether players can be carried, allowed worlds,
  max throw force, speed, sounds, etc.
- `lang.yml` — all plugin messages (`&` color codes)

### Carry mode (`mount-mode`)

- `PASSENGER` *(recommended)* — uses vanilla mechanics, no extra plugin required
- `PACKET` — renders via packets (requires the **PacketEvents** plugin)
  Falls back to `PASSENGER` automatically if PacketEvents isn't found

## Building

```bash
./gradlew build      # Linux / macOS
gradlew.bat build    # Windows
```

The output jar is at `build/libs/PickMobUp-<version>.jar` (already shaded — drop it straight into `plugins/`).

## Technical notes

- Compiled to **Java 17** bytecode, so it runs on both 1.20.1 (Java 17) and 1.21.x (Java 21+)
- Uses **FoliaLib** (shaded + relocated) to automatically switch schedulers between Bukkit and Folia region threads
- Actionbar messages are sent via the BungeeCord Chat API, available on both Spigot and Paper
