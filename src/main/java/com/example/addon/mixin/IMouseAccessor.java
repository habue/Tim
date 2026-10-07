package com.example.addon.mixin;

import net.minecraft.client.MouseHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Exposes the private {@code cursorDeltaX} and {@code cursorDeltaY} fields
 * from {@link MouseHandler} so {@link ThirdSightMouseMixin} can read raw mouse
 * movement without using reflection.
 *
 * Register in mixins.<modid>.json under "client":
 *   "IMouseAccessor"
 */
@Mixin(value = MouseHandler.class, remap = false)
public interface IMouseAccessor {

    @Accessor("accumulatedDX")
    double getCursorDeltaX();

    @Accessor("accumulatedDY")
    double getCursorDeltaY();
}
