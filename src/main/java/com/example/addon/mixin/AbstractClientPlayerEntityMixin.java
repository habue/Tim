package com.example.addon.mixin;

import com.example.addon.modules.ThirdSight;
import meteordevelopment.meteorclient.systems.modules.Modules;
import net.minecraft.client.player.AbstractClientPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = AbstractClientPlayer.class, remap = false)
public abstract class AbstractClientPlayerEntityMixin {

    // Forces the vanilla FOV multiplier to 1.0 when ThirdSight is active,
    // completely cancelling the beacon effect FOV changes.
    @Inject(method = "getFieldOfViewModifier", at = @At("RETURN"), cancellable = true)
    private void onGetFovMultiplier(CallbackInfoReturnable<Float> cir) {
        ThirdSight thirdSight = Modules.get().get(ThirdSight.class);
        if (thirdSight != null && thirdSight.isBeaconEffectCountered()) {
            cir.setReturnValue(1.0f);
        }
    }
}
