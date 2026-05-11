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

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.LevelResource;
import java.util.UUID;
import com.mojang.serialization.DynamicOps;
import net.minecraft.nbt.NbtOps;
import net.minecraft.resources.RegistryOps;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.effect.MobEffectInstance;

import java.nio.file.Files;
import java.nio.file.Path;

/*
 * Persistent serialized handoff-state container.
 *
 * Responsible for:
 * - storing player progression/state
 * - timer persistence
 * - ownership metadata
 * - spectator configuration
 * - world-transfer continuity
 */

public class HandoffState {

    public boolean hasSave = false;

    public double x;
    public double y;
    public double z;

    public float yaw;
    public float pitch;

    public String dimension = "minecraft:overworld";

    public String spawnDimension = "minecraft:overworld";
    public int spawnX;
    public int spawnY;
    public int spawnZ;
    public float spawnAngle;
    public boolean spawnForced;

    public String currentPlayerUuid = "";
    public int remainingTicks = 60 * 60 * 20;
    public boolean timerExpired = false;

    public float health = 20f;

    public int food = 20;
    public float saturation = 5f;

    public int xpLevel;
    public float xpProgress;
    public int totalXp;

    public int turnSeconds = 60 * 60;

    public int selectedSlot;
    public ListTag inventory = new ListTag();
    public ListTag armor = new ListTag();
    public ListTag offhand = new ListTag();
    public ListTag enderChest = new ListTag();
    public ListTag effects = new ListTag();
    public ListTag ownershipSeenPlayers = new ListTag();

    public String creatorUuid = "";
    public String spectatorName = "";

    /*
     * Resolves the persistent save location for
     * handoff_state.dat inside the active world folder.
     */

    private static Path getSavePath(MinecraftServer server) {
        return server.getWorldPath(LevelResource.ROOT).resolve("handoff_state.dat");
    }

    /*
     * Serializes all player inventory-related data.
     *
     * Includes:
     * - main inventory
     * - armour
     * - offhand
     * - ender chest
     * - potion effects
     */

    public void saveInventory(ServerPlayer player) {
        inventory = new ListTag();
        armor = new ListTag();
        offhand = new ListTag();
        enderChest = new ListTag();
        effects = new ListTag();

        DynamicOps<Tag> ops = RegistryOps.create(NbtOps.INSTANCE, player.registryAccess());

        for (int slot = 0; slot < player.getInventory().getNonEquipmentItems().size(); slot++) {
            ItemStack stack = player.getInventory().getNonEquipmentItems().get(slot);

            if (stack.isEmpty()) {
                continue;
            }

            final int savedSlot = slot;

            ItemStack.CODEC.encodeStart(ops, stack).result().ifPresent(itemTag -> {
                CompoundTag slotTag = new CompoundTag();
                slotTag.putInt("Slot", savedSlot);
                slotTag.put("Item", itemTag);
                inventory.add(slotTag);
            });
        }

        net.minecraft.world.entity.EquipmentSlot[] armorSlots = {
                net.minecraft.world.entity.EquipmentSlot.FEET,
                net.minecraft.world.entity.EquipmentSlot.LEGS,
                net.minecraft.world.entity.EquipmentSlot.CHEST,
                net.minecraft.world.entity.EquipmentSlot.HEAD
        };

        for (int slot = 0; slot < armorSlots.length; slot++) {
            ItemStack stack = player.getItemBySlot(armorSlots[slot]);

            if (stack.isEmpty()) {
                continue;
            }

            final int savedSlot = slot;

            ItemStack.CODEC.encodeStart(ops, stack).result().ifPresent(itemTag -> {
                CompoundTag slotTag = new CompoundTag();
                slotTag.putInt("Slot", savedSlot);
                slotTag.put("Item", itemTag);
                armor.add(slotTag);
            });
        }

        ItemStack offhandStack = player.getOffhandItem();

        if (!offhandStack.isEmpty()) {
            ItemStack.CODEC.encodeStart(ops, offhandStack).result().ifPresent(itemTag -> {
                CompoundTag slotTag = new CompoundTag();
                slotTag.putInt("Slot", 0);
                slotTag.put("Item", itemTag);
                offhand.add(slotTag);
            });
        }

        for (int slot = 0; slot < player.getEnderChestInventory().getContainerSize(); slot++) {
            ItemStack stack = player.getEnderChestInventory().getItem(slot);

            if (stack.isEmpty()) {
                continue;
            }

            final int savedSlot = slot;

            ItemStack.CODEC.encodeStart(ops, stack).result().ifPresent(itemTag -> {
                CompoundTag slotTag = new CompoundTag();
                slotTag.putInt("Slot", savedSlot);
                slotTag.put("Item", itemTag);
                enderChest.add(slotTag);
            });
        }

        for (MobEffectInstance effect : player.getActiveEffects()) {
            MobEffectInstance.CODEC.encodeStart(ops, effect).result().ifPresent(effectTag -> {
                effects.add(effectTag);
            });
        }
    }

    /*
     * Restores serialized inventory/effect data
     * onto the active player.
     */

    public void loadInventory(ServerPlayer player) {
        player.getInventory().clearContent();

        DynamicOps<Tag> ops = RegistryOps.create(NbtOps.INSTANCE, player.registryAccess());

        for (Tag rawTag : inventory) {
            if (!(rawTag instanceof CompoundTag slotTag)) {
                continue;
            }

            int slot = slotTag.getInt("Slot").orElse(-1);

            if (slot < 0 || slot >= player.getInventory().getNonEquipmentItems().size()) {
                continue;
            }

            Tag itemTag = slotTag.get("Item");

            if (itemTag == null) {
                continue;
            }

            ItemStack.CODEC.parse(ops, itemTag).result().ifPresent(stack -> {
                player.getInventory().setItem(slot, stack);
            });
        }

        for (Tag rawTag : armor) {
            if (!(rawTag instanceof CompoundTag slotTag)) {
                continue;
            }

            int slot = slotTag.getInt("Slot").orElse(-1);
            Tag itemTag = slotTag.get("Item");

            if (itemTag == null) {
                continue;
            }

            ItemStack.CODEC.parse(ops, itemTag).result().ifPresent(stack -> {
                switch (slot) {
                    case 0 -> player.setItemSlot(net.minecraft.world.entity.EquipmentSlot.FEET, stack);
                    case 1 -> player.setItemSlot(net.minecraft.world.entity.EquipmentSlot.LEGS, stack);
                    case 2 -> player.setItemSlot(net.minecraft.world.entity.EquipmentSlot.CHEST, stack);
                    case 3 -> player.setItemSlot(net.minecraft.world.entity.EquipmentSlot.HEAD, stack);
                }
            });
        }

        for (Tag rawTag : offhand) {
            if (!(rawTag instanceof CompoundTag slotTag)) {
                continue;
            }

            Tag itemTag = slotTag.get("Item");

            if (itemTag == null) {
                continue;
            }

            ItemStack.CODEC.parse(ops, itemTag).result().ifPresent(stack -> {
                player.setItemSlot(net.minecraft.world.entity.EquipmentSlot.OFFHAND, stack);
            });
        }

        player.getEnderChestInventory().clearContent();

        for (Tag rawTag : enderChest) {
            if (!(rawTag instanceof CompoundTag slotTag)) {
                continue;
            }

            int slot = slotTag.getInt("Slot").orElse(-1);

            if (slot < 0 || slot >= player.getEnderChestInventory().getContainerSize()) {
                continue;
            }

            Tag itemTag = slotTag.get("Item");

            if (itemTag == null) {
                continue;
            }

            ItemStack.CODEC.parse(ops, itemTag).result().ifPresent(stack -> {
                player.getEnderChestInventory().setItem(slot, stack);
            });
        }

        player.getEnderChestInventory().setChanged();

        player.getInventory().setSelectedSlot(selectedSlot);

        player.removeAllEffects();

        for (Tag rawTag : effects) {
            MobEffectInstance.CODEC.parse(ops, rawTag).result().ifPresent(player::addEffect);
        }

        player.getInventory().setChanged();
    }

    /*
     * Writes the complete handoff state into
     * handoff_state.dat using NBT serialization.
     */

    public void save(MinecraftServer server) {
        try {
            CompoundTag tag = new CompoundTag();

            tag.putBoolean("HasSave", hasSave);
            tag.putString("CurrentPlayerUuid", currentPlayerUuid);
            tag.putInt("RemainingTicks", remainingTicks);
            tag.putInt("TurnSeconds", turnSeconds);
            tag.putBoolean("TimerExpired", timerExpired);

            tag.putDouble("X", x);
            tag.putDouble("Y", y);
            tag.putDouble("Z", z);

            tag.putFloat("Yaw", yaw);
            tag.putFloat("Pitch", pitch);

            tag.putString("Dimension", dimension);

            tag.putString("SpawnDimension", spawnDimension);
            tag.putInt("SpawnX", spawnX);
            tag.putInt("SpawnY", spawnY);
            tag.putInt("SpawnZ", spawnZ);
            tag.putFloat("SpawnAngle", spawnAngle);
            tag.putBoolean("SpawnForced", spawnForced);

            tag.putFloat("Health", health);

            tag.putInt("Food", food);
            tag.putFloat("Saturation", saturation);

            tag.putInt("XpLevel", xpLevel);
            tag.putFloat("XpProgress", xpProgress);
            tag.putInt("TotalXp", totalXp);

            tag.putInt("SelectedSlot", selectedSlot);
            tag.put("Inventory", inventory);
            tag.put("Armor", armor);
            tag.put("Offhand", offhand);
            tag.put("EnderChest", enderChest);
            tag.put("Effects", effects);

            tag.putString("CreatorUuid", creatorUuid);
            tag.putString("SpectatorName", spectatorName);

            tag.put("OwnershipSeenPlayers", ownershipSeenPlayers);

            NbtIo.write(tag, getSavePath(server));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /*
     * Loads previously serialized handoff state
     * from handoff_state.dat.
     *
     * Returns a default empty state if no save exists.
     */

    public static HandoffState load(MinecraftServer server) {
        HandoffState state = new HandoffState();

        try {
            Path path = getSavePath(server);

            if (!Files.exists(path)) {
                return state;
            }

            CompoundTag tag = NbtIo.read(path);

            if (tag == null) {
                return state;
            }

            state.hasSave = tag.getBoolean("HasSave").orElse(false);

            state.currentPlayerUuid = tag.getString("CurrentPlayerUuid").orElse("");
            state.remainingTicks = tag.getInt("RemainingTicks").orElse(60 * 60 * 20);
            state.timerExpired = tag.getBoolean("TimerExpired").orElse(false);

            state.x = tag.getDouble("X").orElse(0.0);
            state.y = tag.getDouble("Y").orElse(64.0);
            state.z = tag.getDouble("Z").orElse(0.0);

            state.yaw = tag.getFloat("Yaw").orElse(0.0f);
            state.pitch = tag.getFloat("Pitch").orElse(0.0f);

            state.dimension = tag.getString("Dimension").orElse("minecraft:overworld");

            state.spawnDimension = tag.getString("SpawnDimension").orElse("minecraft:overworld");
            state.spawnX = tag.getInt("SpawnX").orElse(0);
            state.spawnY = tag.getInt("SpawnY").orElse(64);
            state.spawnZ = tag.getInt("SpawnZ").orElse(0);
            state.spawnAngle = tag.getFloat("SpawnAngle").orElse(0.0f);
            state.spawnForced = tag.getBoolean("SpawnForced").orElse(true);

            state.health = tag.getFloat("Health").orElse(20.0f);

            state.food = tag.getInt("Food").orElse(20);
            state.saturation = tag.getFloat("Saturation").orElse(5.0f);

            state.xpLevel = tag.getInt("XpLevel").orElse(0);
            state.xpProgress = tag.getFloat("XpProgress").orElse(0.0f);
            state.totalXp = tag.getInt("TotalXp").orElse(0);

            state.selectedSlot = tag.getInt("SelectedSlot").orElse(0);

            state.creatorUuid = tag.getString("CreatorUuid").orElse("");
            state.spectatorName = tag.getString("SpectatorName").orElse("");

            if (tag.contains("Inventory")) {
                state.inventory = tag.getList("Inventory").orElse(new ListTag());
            }
            if (tag.contains("Armor")) {
                state.armor = tag.getList("Armor").orElse(new ListTag());
            }

            if (tag.contains("Offhand")) {
                state.offhand = tag.getList("Offhand").orElse(new ListTag());
            }

            if (tag.contains("EnderChest")) {
                state.enderChest = tag.getList("EnderChest").orElse(new ListTag());
            }

            if (tag.contains("Effects")) {
                state.effects = tag.getList("Effects").orElse(new ListTag());
            }

            if (tag.contains("OwnershipSeenPlayers")) {
                state.ownershipSeenPlayers = tag.getList("OwnershipSeenPlayers").orElse(new ListTag());
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return state;
    }
}