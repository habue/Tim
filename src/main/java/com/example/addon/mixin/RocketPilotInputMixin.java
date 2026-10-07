package com.example.addon.mixin;

import com.example.addon.modules.RocketPilot;
import meteordevelopment.meteorclient.systems.modules.Modules;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.ClientInput;
import net.minecraft.client.player.KeyboardInput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = KeyboardInput.class, remap = false)
public abstract class RocketPilotInputMixin extends ClientInput {

    // 1.21.4: KeyboardInput#tick takes no parameters beyond CallbackInfo.
    @Inject(method = "tick", at = @At("TAIL"))
    private void onTick(CallbackInfo ci) {
        RocketPilot rocketPilot = Modules.get().get(RocketPilot.class);
        if (rocketPilot != null && rocketPilot.isActive() && rocketPilot.useFreeLookY.get()) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player != null && mc.player.isFallFlying()) {
                ClientInput input = (ClientInput) (Object) this;
                moveVector = net.minecraft.world.phys.Vec2.ZERO;
                keyPresses = new net.minecraft.world.entity.player.Input(false, false, false, false, keyPresses.jump(), keyPresses.shift(), keyPresses.sprint());
            }
        }
    }
}
