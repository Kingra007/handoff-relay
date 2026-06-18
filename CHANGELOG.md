# Changelog

All notable changes to Handoff Relay will be documented in this file.

## [1.0.1] - 2026-06-18

### Changed

- Allowed multiple clients per-account (removed single active-player enforcement).
- LAN/cheat escalation checks and anti-tamper disconnects are turned off.
- Integrity-lock auto-enforcement is disabled. Dangerous commands will no longer disconnect you automatically, they will just be disabled.
- Anti-creative/flight is disabled (lockPlayer became a no-op).
- Short disconnects will now keep your turn (don't create new session). Fix causes reconnects to interfere with turn timers.
- currentPlayerNumber will only increment when starting a new turn/session (aka after timer ends or first player in fresh session), not on light reconnects.
- handoff time now takes effect immediately on issuer AND keeps its time on reconnect by disconnecting the player first.
- Timer/action-bar now properly restores and displays its saved remaining time.

### Added

-  Added per-turn session UUIDs, uniquely identifies player-turns. (distinct from your account UUID).
- Big visually obvious countdown during last 10 seconds (title APIs with bold/red action-bar fallback)
- More robust backups: atomic saves, stores timestamped backups in world/handoff_ backups, keeps last 3, and automatically replaces your current with the newest readable backup on load.

### Fixed

- Disconnects no longer create new sessions on short reconnects (because per turn session UUIDs are now preserved).
- Fixed countdown/action-bar being inaccurate/not visibly obvious for last seconds.

### Notes

These changes relax enforcement against modded clients, improve handoff tracking by making turn identities explicit (per turn session IDs), polishUX during last seconds, and improve resilience of saves without overly-punishing clients for potential abuse.

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