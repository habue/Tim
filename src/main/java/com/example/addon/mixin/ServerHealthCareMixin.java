package com.example.addon.mixin;

import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(value = LocalPlayer.class, remap = false)
public class ServerHealthCareMixin {
    // The Healthcare module primarily uses Meteor Client's event system.
    // This mixin file is created as requested but is not strictly required for the module's current logic.
}
