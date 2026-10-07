package com.example.addon.mixin;

import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;

/**
 * Mixin stub for DungeonAssistant.
 * Container-button injection is handled by HandledScreenMixin.
 * Add targeted injections here if specific client-level hooks are needed.
 */
@Mixin(value = Minecraft.class, remap = false)
public class DungeonAssistantMixin {
    // Reserved for future DungeonAssistant-specific mixin hooks.
}
