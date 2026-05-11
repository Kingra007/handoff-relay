/*
 * Handoff Relay
 * Copyright (c) 2026 - 2036 Kingra007
 *
 * Developed by Kingra007.
 * All Rights Reserved.
 *
 * Unauthorized redistribution or rebranding of this source or compiled
 * project is prohibited without explicit permission.
 */

package com.kingra007.handoffrelay;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.Commands;
import com.mojang.brigadier.arguments.IntegerArgumentType;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.UUID;

/*
 * Main mod entrypoint for Handoff Relay.
 *
 * Handles:
 * - player join/disconnect lifecycle
 * - timer enforcement
 * - handoff persistence
 * - spectator handling
 * - production anti-abuse enforcement
 * - ownership attribution systems
 */

public class HandoffRelay implements ModInitializer {
	public static final String MOD_ID = "handoff-relay";

	/*
	 * Runtime timer/player tracking.
	 *
	 * TURN_SECONDS is the default fallback turn length.
	 * remainingTicks tracks the active player's current timer.
	 * activePlayer stores the UUID of the current active participant.
	 * endingDueToTimer prevents disconnect saves from overriding expiry state.
	 */

	private static final int TURN_SECONDS = 60 * 60; // 60 * 60
	private static int remainingTicks = TURN_SECONDS * 20;
	private static UUID activePlayer = null;
	private static boolean endingDueToTimer = false;

	/*
	 * Initializes all runtime systems and event handlers.
	 *
	 * Registers:
	 * - commands
	 * - join handlers
	 * - disconnect handlers
	 * - server tick timer logic
	 */

	@Override
	public void onInitialize() {

		/*
		 * Console startup ownership banner.
		 *
		 * Displays developer attribution and ownership
		 * information during server initialization.
		 */

		System.out.println("====================================");
		System.out.println("Handoff Relay");
		System.out.println("Developed by Kingra007");
		System.out.println("All Rights Reserved");
		System.out.println("====================================");

		// ===== COMMAND REGISTRATION =====

		/*
		 * Registers handoff management commands.
		 *
		 * /handoff spectator <name>
		 * - allows the original creator to assign one spectator account
		 *
		 * /handoff time <minutes>
		 * - allows the original creator to configure future player turn length
		 *
		 * Both commands are restricted to the original handoff creator.
		 */

		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
			dispatcher.register(
					Commands.literal("handoff")
							.then(Commands.literal("spectator")
									.then(Commands.argument("name", StringArgumentType.word())
											.executes(context -> {
												ServerPlayer player = context.getSource().getPlayerOrException();
												MinecraftServer server = context.getSource().getServer();

												HandoffState state = HandoffState.load(server);

												if (state.creatorUuid.isEmpty()) {
													state.creatorUuid = player.getUUID().toString();
												}

												if (!player.getUUID().toString().equals(state.creatorUuid)) {
													player.sendSystemMessage(Component.literal("Only the original handoff creator can set the spectator account."));
													return 0;
												}

												String spectatorName = StringArgumentType.getString(context, "name");
												state.spectatorName = spectatorName;
												state.save(server);

												player.sendSystemMessage(Component.literal("Spectator account set to: " + spectatorName));
												return 1;
											})
									)
							)
							.then(Commands.literal("time")
									.then(Commands.argument("minutes", IntegerArgumentType.integer(1, 240))
											.executes(context -> {
												ServerPlayer player = context.getSource().getPlayerOrException();
												MinecraftServer server = context.getSource().getServer();

												HandoffState state = HandoffState.load(server);

												if (state.creatorUuid.isEmpty()) {
													state.creatorUuid = player.getUUID().toString();
												}

												if (!player.getUUID().toString().equals(state.creatorUuid)) {
													player.sendSystemMessage(Component.literal("Only the original handoff creator can change the timer."));
													return 0;
												}

												int minutes = IntegerArgumentType.getInteger(context, "minutes");
												state.turnSeconds = minutes * 60;
												state.save(server);

												player.sendSystemMessage(Component.literal("Future handoff timer set to " + minutes + " minute(s)."));
												return 1;
											})
									)
							)
			);
		});

		// ===== PLAYER LIFECYCLE EVENTS =====

		/*
		 * Handles player join behaviour.
		 *
		 * Enforces:
		 * - only one active player
		 * - approved spectator bypass
		 * - expired player lockout
		 * - timer resume for reconnecting players
		 * - fresh timer for new handoff players
		 */

		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
			ServerPlayer player = handler.player;

			HandoffState state = HandoffState.load(server);

			boolean isSpectatorAccount = !state.spectatorName.isEmpty()
					&& player.getName().getString().equalsIgnoreCase(state.spectatorName);

			if (server.getPlayerList().getPlayerCount() > 1 && !isSpectatorAccount) {
				player.connection.disconnect(Component.literal("Another player is currently playing this handoff world."));
				return;
			}

			if (isSpectatorAccount) {
				player.setGameMode(GameType.SPECTATOR);
				player.sendSystemMessage(Component.literal("You have joined as the approved spectator."));
				return;
			}

			sendOwnershipMessage(player, server);

			activePlayer = player.getUUID();

			boolean samePlayerAsLastRun = state.hasSave
					&& player.getUUID().toString().equals(state.currentPlayerUuid);

			if (state.timerExpired && samePlayerAsLastRun) {
				player.connection.disconnect(Component.literal("Your handoff time has already expired."));
				return;
			}

			if (state.timerExpired && !samePlayerAsLastRun) {
				remainingTicks = state.turnSeconds * 20;
				state.timerExpired = false;
				state.remainingTicks = remainingTicks;
				state.currentPlayerUuid = player.getUUID().toString();
				state.save(server);
			} else if (samePlayerAsLastRun) {
				remainingTicks = state.remainingTicks;
			} else {
				remainingTicks = state.turnSeconds * 20;
			}

			lockPlayer(player);

			// HandoffState state = HandoffState.load(server); // removed due to players rejoining and having full time

			if (state.hasSave) {
				applyState(player, server, state);
			} else {
				saveState(player, server);
			}

			int secondsLeft = remainingTicks / 20;
			int minutes = secondsLeft / 60;
			int seconds = secondsLeft % 60;

			if (samePlayerAsLastRun && remainingTicks > 0) {
				player.displayClientMessage(
						Component.literal(String.format("Welcome back. You have %02d:%02d remaining.", minutes, seconds))
								.withStyle(ChatFormatting.GOLD),
						false
				);
			} else {
				player.displayClientMessage(
						Component.literal(String.format("Play for %02d:%02d, have fun", minutes, seconds))
								.withStyle(ChatFormatting.GOLD),
						false
				);
			}
		});

		/*
		 * Handles player disconnects.
		 *
		 * Saves active player state on normal disconnect.
		 * Skips normal disconnect saving when the disconnect was caused
		 * by timer expiry, so expiry state is not overwritten.
		 */

		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
			ServerPlayer player = handler.player;

			if (activePlayer != null && activePlayer.equals(player.getUUID())) {
				if (!endingDueToTimer) {
					saveState(player, server);
				}

				activePlayer = null;
				endingDueToTimer = false;
			}
		});

		// ===== TIMER LOOP =====

		/*
		 * Main timer loop.
		 *
		 * Runs once per server tick while an active player exists.
		 * Decrements the timer, displays action bar updates,
		 * saves final state on expiry, and disconnects expired players.
		 */

		ServerTickEvents.END_SERVER_TICK.register(server -> {
			if (activePlayer == null) return;

			ServerPlayer player = server.getPlayerList().getPlayer(activePlayer);

			if (player == null) {
				activePlayer = null;
				return;
			}

			lockPlayer(player);

			remainingTicks--;

			if (remainingTicks % 20 == 0) {
				int secondsLeft = remainingTicks / 20;
				int minutes = secondsLeft / 60;
				int seconds = secondsLeft % 60;

				player.displayClientMessage(
						Component.literal(String.format("Time left: %02d:%02d", minutes, seconds))
								.withStyle(ChatFormatting.YELLOW),
						true
				);
			}

			if (remainingTicks <= 0) {
				saveState(player, server);

				HandoffState expiredState = HandoffState.load(server);
				expiredState.currentPlayerUuid = player.getUUID().toString();
				expiredState.remainingTicks = 0;
				expiredState.timerExpired = true;
				expiredState.save(server);

				endingDueToTimer = true;

				player.connection.disconnect(Component.literal("Your handoff time is up."));
				activePlayer = null;
			}
		});
	}

	/*
	 * Enforces production gameplay restrictions.
	 *
	 * Prevents:
	 * - creative mode abuse
	 * - flight
	 * - instabuild
	 *
	 * Used continuously during runtime to ensure
	 * the active player remains in survival mode.
	 */

	// ===== HELPER METHODS =====

	// ENABLE FOR PRODUCTION
	private static void lockPlayer(ServerPlayer player) {
	if (player.gameMode.getGameModeForPlayer() != GameType.SURVIVAL) {
		player.setGameMode(GameType.SURVIVAL);
	}

	player.getAbilities().mayfly = false;
	player.getAbilities().flying = false;
	player.getAbilities().instabuild = false;
	player.onUpdateAbilities();
}
	// ENABLE FOR DEV TESTING
	// private static void lockPlayer(ServerPlayer player) {
	// DEV TEST MODE ENABLED
	// }

	/*
	 * Displays ownership and attribution information
	 * to players on their first join only.
	 *
	 * Seen-player tracking is persisted in
	 * handoff_state.dat to avoid repeated messages.
	 */

	private static void sendOwnershipMessage(ServerPlayer player, MinecraftServer server) {
		HandoffState state = HandoffState.load(server);
		String playerUuid = player.getUUID().toString();

		for (net.minecraft.nbt.Tag rawTag : state.ownershipSeenPlayers) {
			if (rawTag instanceof net.minecraft.nbt.CompoundTag seenTag) {
				String seenUuid = seenTag.getString("Uuid").orElse("");

				if (seenUuid.equals(playerUuid)) {
					return;
				}
			}
		}

		net.minecraft.nbt.CompoundTag newSeenTag = new net.minecraft.nbt.CompoundTag();
		newSeenTag.putString("Uuid", playerUuid);
		state.ownershipSeenPlayers.add(newSeenTag);
		state.save(server);

		player.sendSystemMessage(Component.literal("====================================").withStyle(ChatFormatting.GOLD));
		player.sendSystemMessage(Component.literal("Handoff Relay").withStyle(ChatFormatting.YELLOW));
		player.sendSystemMessage(Component.literal("Developed by Kingra007").withStyle(ChatFormatting.YELLOW));
		player.sendSystemMessage(Component.literal("All Rights Reserved").withStyle(ChatFormatting.YELLOW));
		player.sendSystemMessage(Component.literal("====================================").withStyle(ChatFormatting.GOLD));
	}

	/*
	 * Serializes the active player's handoff state.
	 *
	 * Saves:
	 * - inventory
	 * - armour/offhand
	 * - ender chest
	 * - XP/health/hunger
	 * - potion effects
	 * - position/dimension
	 * - respawn data
	 * - timer state
	 * - ownership metadata
	 *
	 * Data is written into handoff_state.dat.
	 */

	private static void saveState(ServerPlayer player, MinecraftServer server) {
		HandoffState state = new HandoffState();
		HandoffState previousState = HandoffState.load(server);

		state.hasSave = true;

		state.creatorUuid = previousState.creatorUuid.isEmpty()
				? player.getUUID().toString()
				: previousState.creatorUuid;

		state.spectatorName = previousState.spectatorName;
		state.turnSeconds = previousState.turnSeconds;
		state.ownershipSeenPlayers = previousState.ownershipSeenPlayers;
		state.currentPlayerUuid = player.getUUID().toString();
		state.remainingTicks = remainingTicks;
		state.timerExpired = false;

		state.dimension = dimensionToString(player.level().dimension());
		state.x = player.getX();
		state.y = player.getY();
		state.z = player.getZ();
		state.yaw = player.getYRot();
		state.pitch = player.getXRot();

		BlockPos spawn = player.blockPosition();
		state.spawnDimension = state.dimension;
		state.spawnX = spawn.getX();
		state.spawnY = spawn.getY();
		state.spawnZ = spawn.getZ();
		state.spawnAngle = player.getYRot();
		state.spawnForced = true;

		state.health = player.getHealth();
		state.food = player.getFoodData().getFoodLevel();
		state.saturation = player.getFoodData().getSaturationLevel();
		state.xpLevel = player.experienceLevel;
		state.xpProgress = player.experienceProgress;
		state.totalXp = player.totalExperience;

		state.selectedSlot = getSelectedSlot(player);

		state.saveInventory(player);
		state.selectedSlot = player.getInventory().getSelectedSlot();
		state.save(server);
	}

	/*
	 * Restores a previously serialized handoff state
	 * onto the currently active player.
	 *
	 * Applies:
	 * - inventories
	 * - effects
	 * - health/XP
	 * - location/dimension
	 * - respawn position
	 */

	private static void applyState(ServerPlayer player, MinecraftServer server, HandoffState state) {
		state.loadInventory(player);

		setSelectedSlot(player, state.selectedSlot);

		player.setHealth(Math.max(1.0F, state.health));
		player.getFoodData().setFoodLevel(state.food);
		player.getFoodData().setSaturation(state.saturation);

		player.experienceLevel = state.xpLevel;
		player.experienceProgress = state.xpProgress;
		player.totalExperience = state.totalXp;

		ResourceKey<Level> dimensionKey = dimensionFromString(state.dimension);
		ServerLevel level = server.getLevel(dimensionKey);

		if (level != null) {
			teleportReflect(player, level, state.x, state.y, state.z, state.yaw, state.pitch);
		}

		ResourceKey<Level> spawnDimensionKey = dimensionFromString(state.spawnDimension);
		setRespawnReflect(player, spawnDimensionKey, new BlockPos(state.spawnX, state.spawnY, state.spawnZ), state.spawnAngle, state.spawnForced);
	}

	// ===== DIMENSION / REFLECTION HELPERS =====

	private static String dimensionToString(ResourceKey<Level> key) {
		if (key == Level.NETHER) return "minecraft:the_nether";
		if (key == Level.END) return "minecraft:the_end";
		return "minecraft:overworld";
	}

	private static ResourceKey<Level> dimensionFromString(String dimension) {
		return switch (dimension) {
			case "minecraft:the_nether" -> Level.NETHER;
			case "minecraft:the_end" -> Level.END;
			default -> Level.OVERWORLD;
		};
	}

	/*
	 * Reflection helper used to retrieve the currently
	 * selected hotbar slot from the player inventory.
	 *
	 * Reflection is used for compatibility across
	 * Minecraft/Fabric mappings.
	 */

	private static int getSelectedSlot(ServerPlayer player) {
		try {
			Field field = player.getInventory().getClass().getDeclaredField("selected");
			field.setAccessible(true);
			return field.getInt(player.getInventory());
		} catch (Exception ignored) {
			return 0;
		}
	}

	/*
	 * Reflection helper used to restore the player's
	 * selected hotbar slot during state application.
	 */

	private static void setSelectedSlot(ServerPlayer player, int slot) {
		try {
			Field field = player.getInventory().getClass().getDeclaredField("selected");
			field.setAccessible(true);
			field.setInt(player.getInventory(), Math.max(0, Math.min(8, slot)));
		} catch (Exception ignored) {
		}
	}

	/*
	 * Reflection-safe teleport wrapper used for
	 * cross-version/server compatibility.
	 *
	 * Falls back to basic teleportation if reflective
	 * invocation fails.
	 */

	private static void teleportReflect(ServerPlayer player, ServerLevel level, double x, double y, double z, float yaw, float pitch) {
		try {
			for (Method method : player.getClass().getMethods()) {
				if (!method.getName().equals("teleportTo")) continue;

				Class<?>[] params = method.getParameterTypes();

				if (params.length >= 6 && params[0].isAssignableFrom(ServerLevel.class)) {
					method.invoke(player, level, x, y, z, yaw, pitch);
					return;
				}
			}

			player.teleportTo(x, y, z);
		} catch (Exception ignored) {
			player.teleportTo(x, y, z);
		}
	}

	/*
	 * Reflection-safe respawn position setter used
	 * for compatibility across changing server APIs.
	 */

	private static void setRespawnReflect(ServerPlayer player, ResourceKey<Level> dimension, BlockPos pos, float angle, boolean forced) {
		try {
			for (Method method : player.getClass().getMethods()) {
				if (!method.getName().equals("setRespawnPosition")) continue;

				if (method.getParameterCount() == 5) {
					method.invoke(player, dimension, pos, angle, forced, false);
					return;
				}

				if (method.getParameterCount() == 4) {
					method.invoke(player, dimension, pos, angle, forced);
					return;
				}
			}
		} catch (Exception ignored) {
		}
	}
}