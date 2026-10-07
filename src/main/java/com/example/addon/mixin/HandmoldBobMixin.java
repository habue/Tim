package com.example.addon.mixin;

import com.example.addon.modules.Handmold;
import com.mojang.blaze3d.vertex.PoseStack;
import meteordevelopment.meteorclient.systems.modules.Modules;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = GameRenderer.class, remap = false)
public abstract class HandmoldBobMixin {

    @Inject(
        method = "bobView",
        at = @At("HEAD"),
        cancellable = true
    )
    private void onBobView(net.minecraft.client.renderer.state.level.CameraRenderState cameraState, PoseStack matrices, CallbackInfo ci) {
        Handmold mod = Modules.get().get(Handmold.class);
        if (mod != null && mod.shouldDisableHandBob()) ci.cancel();
    }
}
