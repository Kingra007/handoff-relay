# License

## Copyright

Copyright (c) 2026 - 2036 Kingra007

All Rights Reserved.

# Handoff Relay

Handoff Relay is a timed Minecraft handoff challenge mod developed by Kingra007.

The project is designed to support serialized world-transfer gameplay sessions where individual players receive a Minecraft world for a limited period of time before handing it off to the next participant.

The mod currently supports both:
- Singleplayer/world-transfer workflows
- Dedicated server hosted workflows

---

# Features

- Timed player handoff system
- Persistent player inventory saving
- Armour/offhand persistence
- Ender chest persistence
- XP persistence
- Health/hunger persistence
- Potion/status effect persistence
- Position/dimension persistence
- Respawn point persistence
- Reconnect timer continuation
- Timer expiry enforcement
- Expired reconnect blocking
- Single active-player enforcement
- Spectator account allowance system
- Production survival enforcement
- Ownership attribution systems
- World-transfer persistence support

---

# Supported Workflows

## Singleplayer / World-Transfer Workflow

A player receives:
- the world save
- the required mod .jar

The player records their gameplay session, then transfers the updated world save to the next participant.

The mod preserves:
- player state
- timer state
- world continuity
- ownership metadata

through the handoff process.

---

## Dedicated Server Workflow

The mod can also operate on hosted Fabric servers.

Current architecture supports:
- persistent handoff tracking
- reconnect enforcement
- spectator support
- timed gameplay restriction systems

Some multiplayer edge-case verification is still pending.

---

# Installation

## Requirements

- Minecraft 1.21.11
- Fabric Loader
- Fabric API
- Java 21

## Install Steps

1. Install Fabric Loader
2. Install Fabric API
3. Place the Handoff Relay .jar into:
   .minecraft/mods
4. Launch Minecraft

For dedicated servers:
- place the mod into the server mods folder
- ensure Fabric API is installed server-side

---

# Usage

## Main Workflow

1. Launch the world/server
2. The first player joins
3. The timer begins
4. Player state is continuously protected and persisted
5. When time expires:
   - the player is disconnected
   - reconnect is blocked
6. The updated world can then be transferred to the next participant

---

## World Transfer Files

When handing the project world to the next participant, include:

```text
world/
handoff-relay-x.x.x.jar
fabric-api-x.x.x.jar
```
---

# Recommended Handoff Checklist

Before transferring the project to the next participant:

- [ ] Stop the Minecraft server/game completely
- [ ] Verify `handoff_state.dat` exists
- [ ] Verify the latest world save is included
- [ ] Include the required mod `.jar`
- [ ] Include the required Fabric API `.jar`
- [ ] Compress files into a `.zip`
- [ ] Verify the `.zip` opens correctly before sending
- [ ] Transfer the package to the next participant

Recommended package contents:

```text
handoff-package.zip
├── world/
│   ├── region/
│   ├── playerdata/
│   ├── level.dat
│   └── handoff_state.dat
├── handoff-relay-x.x.x.jar
└── fabric-api-x.x.x.jar
```