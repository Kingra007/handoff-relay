# Changelog

All notable changes to Handoff Relay will be documented in this file.

## [1.0.2] - 2026-06-22

### Added

- Short disconnects will now keep your turn (don't create a new session)
- Per-turn session UUIDs, which uniquely identify player-turns
- Big visually obvious countdown during the last 10 seconds (title APIs with bold/red action-bar fallback)

### Fixed

- Disconnects no longer create new sessions on short reconnects
- Countdown/action-bar being inaccurate/not visibly obvious for the last seconds

### Notes

- Improve handoff tracking by making turn identities explicit (per-turn session IDs)
- Polish UX during the last seconds of a turn

## [1.0.1] - 2026-06-18

### Notes

Replace integrity lockouts with warning flags (LAN)


## [1.0.0] - 2026-05-24

### Release

Stable public release.

Validated:
- Full Player A -> Player B handoff lifecycle
- Player A expiry disconnect enforcement
- Player A rejoin restriction
- Player B takeover validation
- Fresh timer assignment for Player B
- Full second-account handoff validation
- Dangerous command tamper protection
- LAN cheat escalation mitigation
- Integrity backup restoration
- Persistent player progression continuity

Release notes:
Handoff Relay is now considered stable for public use.

## [1.0.0-beta] - 2026-05-23

### Added

- Phase 13.1 save integrity protection
- Automatic handoff_state backup restoration
- World initialization integrity marker
- Integrity lock enforcement for missing/corrupt handoff state
- Dangerous command tamper detection
- Command abuse interception for:
    - /gamemode
    - /give
    - /tp
    - /teleport
    - /effect
    - /enchant
    - /summon
    - /setblock
    - /fill
    - /item
    - /experience
    - /xp
    - /advancement
    - /op
    - /deop
- LAN / cheat escalation detection
- Anti-tamper disconnect enforcement

### External Validation

Confirmed:
- World transfer loading
- External install/load success
- Inventory restoration
- Armour/offhand restoration
- Ender chest restoration
- XP restoration
- Health/hunger restoration
- Potion effect restoration
- Selected hotbar restoration
- Timer persistence
- Second-account transferred world join
- Save deletion recovery from backup
- Dangerous command blocking
- Cheat escalation prevention

## [1.0.0-dev] - 2026-05-11

### Added

- Main inventory persistence
- Hotbar persistence
- Selected hotbar slot persistence
- Armour persistence
- Offhand persistence
- Ender chest persistence
- XP persistence
- Health persistence
- Hunger/saturation persistence
- Potion/status effect persistence
- Location and dimension persistence
- Respawn point persistence
- Timer countdown system
- Timer resume on reconnect
- Timer expiry enforcement
- Expired player reconnect blocking
- Rejoin message with remaining time
- Single active-player enforcement
- Spectator account allowance command
- First-player-only timer configuration command
- Production survival enforcement
- Creative/flying prevention
- Console ownership banner
- First-join in-game ownership notice
- README ownership notice
- LICENSE implementation
- Source ownership headers
- GitHub repository setup
- World-transfer documentation

### Validated Locally

- State persistence across reconnect
- State persistence across server restart
- Inventory and player state restoration
- Production survival enforcement
- Ownership attribution systems
- Dedicated server runtime through Gradle
- World-save state location inside world folder

### Pending External Validation

- Multi-machine world-save transfer
- Built `.jar` runtime outside Gradle/IntelliJ
- Live Player A to Player B handoff
- Spectator live behaviour with a second account
- Hosted multiplayer edge cases

### Known Missing Features

- Custom HUD above hotbar
- Full public release packaging
- Expanded user-facing configuration