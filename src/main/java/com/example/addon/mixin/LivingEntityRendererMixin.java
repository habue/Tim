package com.example.addon.mixin;

import com.example.addon.modules.Illushine;
import com.mojang.blaze3d.vertex.PoseStack;
import meteordevelopment.meteorclient.systems.modules.Modules;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = LivingEntityRenderer.class, remap = false)
public class LivingEntityRendererMixin {

    @Unique
    private static final java.util.Map<LivingEntityRenderState, java.lang.ref.WeakReference<Mob>> tim$renderedMobs = new java.util.WeakHashMap<>();

    @Inject(method = "extractRenderState(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;F)V", at = @At("TAIL"))
    private void tim$captureMob(LivingEntity entity, LivingEntityRenderState state, float tickDelta, CallbackInfo ci) {
        if (entity instanceof Mob mob) tim$renderedMobs.put(state, new java.lang.ref.WeakReference<>(mob));
        else tim$renderedMobs.remove(state);
    }

    @Inject(method = "scale(Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;)V", at = @At("TAIL"))
    private void illushine$onScale(LivingEntityRenderState state, PoseStack matrices, CallbackInfo ci) {
        var reference = tim$renderedMobs.get(state);
        Mob mob = reference == null ? null : reference.get();
        if (mob != null && mob.isAlive()) {
            Illushine illushine = Modules.get().get(Illushine.class);
            if (illushine == null || !illushine.isActive()) return;

            double scale = illushine.getMobScale(mob);

            if (scale != 1.0) {
                matrices.scale((float) scale, (float) scale, (float) scale);
            }

        }
    }
}
