package com.example.addon.mixin;

import com.example.addon.modules.Datamine;
import meteordevelopment.meteorclient.systems.modules.Modules;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = ClientPacketListener.class, remap = false)
public class ClientPlayNetworkHandlerMixin {

    @Inject(method = "handleBlockUpdate", at = @At("HEAD"))
    private void onBlockUpdate(ClientboundBlockUpdatePacket packet, CallbackInfo ci) {
        Datamine datamine = Modules.get().get(Datamine.class);
        if (datamine != null && datamine.isActive()) {
            // Tell Datamine the server officially confirmed this block changed
            datamine.onServerBlockUpdate(packet.getPos(), packet.getBlockState());
        }
    }
}
