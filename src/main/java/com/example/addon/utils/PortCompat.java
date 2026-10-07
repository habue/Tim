package com.example.addon.utils;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.clock.WorldClocks;

/** Small adapters for APIs changed between 1.21.4 and 26.1.2. */
public final class PortCompat {
    private PortCompat() {}

    public static void displayMessage(Component message, boolean overlay) {
        Minecraft mc = Minecraft.getInstance();
        if (overlay) mc.gui.setOverlayMessage(message, false);
        else meteordevelopment.meteorclient.utils.player.ChatUtils.sendMsg(message);
    }

    public static long dayTime() {
        var level = Minecraft.getInstance().level;
        if (level == null) return 0;
        var clock = level.registryAccess().getOrThrow(net.minecraft.core.registries.Registries.WORLD_CLOCK).value().getOrThrow(WorldClocks.OVERWORLD);
        return level.clockManager().getTotalTicks(clock);
    }

    public static Item dyeItem(DyeColor color) {
        return switch (color) {
            case WHITE -> Items.WHITE_DYE;
            case ORANGE -> Items.ORANGE_DYE;
            case MAGENTA -> Items.MAGENTA_DYE;
            case LIGHT_BLUE -> Items.LIGHT_BLUE_DYE;
            case YELLOW -> Items.YELLOW_DYE;
            case LIME -> Items.LIME_DYE;
            case PINK -> Items.PINK_DYE;
            case GRAY -> Items.GRAY_DYE;
            case LIGHT_GRAY -> Items.LIGHT_GRAY_DYE;
            case CYAN -> Items.CYAN_DYE;
            case PURPLE -> Items.PURPLE_DYE;
            case BLUE -> Items.BLUE_DYE;
            case BROWN -> Items.BROWN_DYE;
            case GREEN -> Items.GREEN_DYE;
            case RED -> Items.RED_DYE;
            case BLACK -> Items.BLACK_DYE;
        };
    }
}
