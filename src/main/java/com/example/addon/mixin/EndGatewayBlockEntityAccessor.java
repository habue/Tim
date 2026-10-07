package com.example.addon.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.TheEndGatewayBlockEntity;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value = TheEndGatewayBlockEntity.class, remap = false)
public interface EndGatewayBlockEntityAccessor {

    @Accessor("exitPortal")
    @Nullable
    BlockPos getExitPortalPos();
}
