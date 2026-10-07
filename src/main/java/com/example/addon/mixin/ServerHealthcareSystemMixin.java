package com.example.addon.mixin;

import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(value = LocalPlayer.class, remap = false)
public class ServerHealthcareSystemMixin {
    // The ServerHealthcareSystem module primarily uses Meteor Client's event system.
    // This mixin file is a placeholder and not strictly required for the module's current logic.
}