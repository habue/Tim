package com.example.addon.mixin;

import com.example.addon.modules.RocketPilot;
import meteordevelopment.meteorclient.systems.modules.Modules;
import net.minecraft.client.Camera;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.BlockGetter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = Camera.class, remap = false)
public abstract class RocketPilotMixin {
    @Shadow protected abstract void setPosition(double x, double y, double z);
    @Shadow public abstract net.minecraft.world.phys.Vec3 position();

    @Inject(method = "alignWithEntity", at = @At("RETURN"))
    private void onUpdate(float tickDelta, CallbackInfo ci) {
        RocketPilot rocketPilot = Modules.get().get(RocketPilot.class);
        if (rocketPilot != null && rocketPilot.isActive() && rocketPilot.useFreeLookY.get()) {
            if (net.minecraft.client.Minecraft.getInstance().getCameraEntity() instanceof LivingEntity living && living.isFallFlying()) {
                setPosition(position().x, rocketPilot.freeLookY.get(), position().z);
            }
        }
    }
}
