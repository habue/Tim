package com.example.addon.mixin;

import com.example.addon.modules.DungeonAssistant;
import com.example.addon.modules.Inventory101;
import com.example.addon.modules.LootLens;
import meteordevelopment.meteorclient.systems.modules.Modules;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.gui.screens.inventory.ShulkerBoxScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.function.BooleanSupplier;

@Mixin(value = AbstractContainerScreen.class, remap = false)
public abstract class HandledScreenMixin extends Screen {
    @Shadow protected int imageWidth;
    @Shadow protected int leftPos;
    @Shadow protected int topPos;
    @Shadow protected Slot hoveredSlot;

    @Unique private Button s1Button;
    @Unique private Button s2Button;

    protected HandledScreenMixin(Component title) {
        super(title);
    }

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void onMouseClicked(net.minecraft.client.input.MouseButtonEvent event, boolean doubleClick, CallbackInfoReturnable<Boolean> cir) {
        if (event.button() == 2) { // Middle click
            Inventory101 inv101 = Modules.get().get(Inventory101.class);
            if (inv101 != null && inv101.isActive()) {
                if (this.hoveredSlot != null && this.hoveredSlot.hasItem()) {
                    ItemStack stack = this.hoveredSlot.getItem();
                    if (Inventory101.isShulker(stack)) {
                        inv101.openPreview(stack);
                        cir.setReturnValue(true);
                    } else if (Inventory101.isEnderChest(stack)) {
                        inv101.openEnderChestPreview(stack);
                        cir.setReturnValue(true);
                    }
                }
            }
        }
    }

    @Inject(method = "extractRenderState", at = @At("TAIL"))
    private void onRenderGlobalShulkerIcons(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        Inventory101 inv101 = Modules.get().get(Inventory101.class);
        if (inv101 == null || !inv101.isActive()) return;

        AbstractContainerScreen<?> screen = (AbstractContainerScreen<?>) (Object) this;

        if (screen instanceof ContainerScreen containerScreen) {
            String titleStr = containerScreen.getTitle().getString().toLowerCase();
            if (titleStr.contains("ender chest") || containerScreen.getMenu().getContainer() instanceof net.minecraft.world.inventory.PlayerEnderChestContainer) {
                Inventory101.updateCachedEnderChest(containerScreen.getMenu().getContainer());
            }
        }

        for (Slot slot : screen.getMenu().slots) {
            if (slot.hasItem()) {
                ItemStack stack = slot.getItem();
                if (Inventory101.isShulker(stack)) {
                    ItemStack dominant = Inventory101.getDominantItem(stack);
                    if (!dominant.isEmpty()) {
                        float scale = (float) inv101.getIconScale();
                        float centerOffset = (16.0f * (1.0f - scale)) / 2.0f;

                        context.pose().pushMatrix();
                        context.pose().translate(this.leftPos + slot.x + 1 + centerOffset, this.topPos + slot.y + 1 + centerOffset);
                        context.pose().scale(scale, scale);
                        context.item(dominant, 0, 0);
                        context.pose().popMatrix();
                    }
                }
            }
        }
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void onInit(CallbackInfo ci) {
        s1Button = null;
        s2Button = null;

        Inventory101 inv101       = Modules.get().get(Inventory101.class);
        boolean      inv101Active = inv101 != null && inv101.isActive();

        if ((Object) this instanceof InventoryScreen && inv101Active) {
            int bx = this.leftPos - 25;
            int by = this.topPos;

            s1Button = mouseOnly(Component.literal("S1"),
                btn -> inv101.startInvSort(1), bx, by, 20, 20,
                net.minecraft.client.gui.components.Tooltip.create(Component.literal("Sort to " + inv101.getPresetName(1))),
                () -> !inv101.isPresetEmpty(1));

            s2Button = mouseOnly(Component.literal("S2"),
                btn -> inv101.startInvSort(2), bx, by + 25, 20, 20,
                net.minecraft.client.gui.components.Tooltip.create(Component.literal("Sort to " + inv101.getPresetName(2))),
                () -> !inv101.isPresetEmpty(2));

            this.addRenderableWidget(s1Button);
            this.addRenderableWidget(s2Button);
            return;
        }

        AbstractContainerScreen<?> screen        = (AbstractContainerScreen<?>) (Object) this;
        int              containerSlots = screen.getMenu().slots.size() - 36;

        if (inv101Active) {
            if ((Object) this instanceof ShulkerBoxScreen) {
                int bx = this.leftPos - 25;
                int by = this.topPos;

                this.addRenderableWidget(mouseOnly(Component.literal("S"),
                    btn -> inv101.toggleSaveMode(), bx, by, 20, 20,
                    net.minecraft.client.gui.components.Tooltip.create(Component.literal("Save Current Layout"))));
                by += 25;

                this.addRenderableWidget(mouseOnly(Component.literal("1"),
                    btn -> {
                        if (!inv101.isSaveMode() && inv101.isPresetEmpty(1)) return;
                        inv101.handlePreset(1);
                    }, bx, by, 20, 20,
                    net.minecraft.client.gui.components.Tooltip.create(Component.literal("Load " + inv101.getPresetName(1))),
                    () -> !inv101.isPresetEmpty(1)));
                by += 25;

                this.addRenderableWidget(mouseOnly(Component.literal("2"),
                    btn -> {
                        if (!inv101.isSaveMode() && inv101.isPresetEmpty(2)) return;
                        inv101.handlePreset(2);
                    }, bx, by, 20, 20,
                    net.minecraft.client.gui.components.Tooltip.create(Component.literal("Load " + inv101.getPresetName(2))),
                    () -> !inv101.isPresetEmpty(2)));
                by += 25;

                this.addRenderableWidget(mouseOnly(Component.literal("C"),
                    btn -> inv101.clearPresets(), bx, by, 20, 20,
                    net.minecraft.client.gui.components.Tooltip.create(Component.literal("Clear Presets"))));
                by += 25;

                if (inv101.isRegearButtonEnabled()) {
                    this.addRenderableWidget(mouseOnly(Component.literal("G"),
                        btn -> inv101.startRegearing(), bx, by, 20, 20,
                        net.minecraft.client.gui.components.Tooltip.create(Component.literal("Equip armor and replenish essentials"))));
                    by += 25;
                }

                if (inv101.isReplenishButtonEnabled()) {
                    this.addRenderableWidget(mouseOnly(Component.literal("R"),
                        btn -> inv101.startReplenishing(), bx, by, 20, 20,
                        net.minecraft.client.gui.components.Tooltip.create(Component.literal("Replenish whitelisted items from shulker"))));
                }

                return;
            }

            if ((Object) this instanceof ContainerScreen && inv101.isSortButtonEnabled()) {
                int bx = this.leftPos + this.imageWidth - 70;
                int by = this.topPos + 2;
                this.addRenderableWidget(mouseOnly(Component.literal("Sort"),
                    btn -> inv101.startSorting(), bx, by, 30, 14,
                    net.minecraft.client.gui.components.Tooltip.create(Component.literal("Sort shulkers by colour"))));
            }
        }

        if (containerSlots <= 0) return;

        LootLens ll = Modules.get().get(LootLens.class);
        if (ll != null && ll.shouldShowStealDumpButtons()) {
            addStealDumpButtons(screen, containerSlots);
        } else {
            DungeonAssistant da = Modules.get().get(DungeonAssistant.class);
            if (da != null && da.isActive()) {
                addStealDumpButtons(screen, containerSlots);
            }
        }
    }

    @Inject(method = "extractRenderState", at = @At("HEAD"))
    private void onRender(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        if ((Object) this instanceof InventoryScreen && s1Button != null && s2Button != null) {
            int defaultX = (this.width - this.imageWidth) / 2;
            boolean isRecipeBookOpen = this.leftPos > defaultX + 50;

            int bx = isRecipeBookOpen ? (this.leftPos + this.imageWidth + 5) : (this.leftPos - 25);
            int by = this.topPos;

            s1Button.setPosition(bx, by);
            s2Button.setPosition(bx, by + 25);
        }
    }

    private static Button mouseOnly(Component label, Button.OnPress action,
                                       int x, int y, int width, int height,
                                       net.minecraft.client.gui.components.Tooltip tooltip) {
        return mouseOnly(label, action, x, y, width, height, tooltip, null);
    }

    private static Button mouseOnly(Component label, Button.OnPress action,
                                       int x, int y, int width, int height,
                                       net.minecraft.client.gui.components.Tooltip tooltip,
                                       BooleanSupplier hasData) {
        Button btn = new Button(x, y, width, height, label, action,
            textSupplier -> textSupplier.get().copy()) {
            @Override
            public boolean keyPressed(net.minecraft.client.input.KeyEvent event) {
                return false;
            }
            @Override
            public boolean keyReleased(net.minecraft.client.input.KeyEvent event) {
                return false;
            }
            @Override
            public void extractContents(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
                if (hasData != null) {
                    boolean prev = this.active;
                    this.active = hasData.getAsBoolean();
                    extractDefaultSprite(context);
                    extractDefaultLabel(context.textRenderer());
                    this.active = prev;
                } else {
                    extractDefaultSprite(context);
                    extractDefaultLabel(context.textRenderer());
                }
            }
        };
        if (tooltip != null) btn.setTooltip(tooltip);
        return btn;
    }

    private void addStealDumpButtons(AbstractContainerScreen<?> screen, int containerSlots) {
        int buttonX, buttonY, buttonW, buttonH, buttonGap;

        if ((Object) this instanceof ContainerScreen) {
            buttonW = 14;
            buttonH = 14;
            buttonGap = 2;
            buttonX = this.leftPos + this.imageWidth - 8 - buttonW - buttonGap - buttonW;
            buttonY = this.topPos + 2;
        } else {
            buttonW = 20;
            buttonH = 20;
            buttonGap = 4;

            int screenWidth = this.width;
            int rightEdge = this.leftPos + this.imageWidth + 5 + buttonW;

            if (rightEdge <= screenWidth) {
                buttonX = this.leftPos + this.imageWidth + 5;
                buttonY = this.topPos + 5;
            } else {
                int containerRows = (containerSlots + 8) / 9;
                buttonX = this.leftPos + (this.imageWidth - (buttonW * 2 + buttonGap)) / 2;
                buttonY = this.topPos + containerRows * 18 + 2;
            }
        }

        this.addRenderableWidget(mouseOnly(Component.literal("S"),
            button -> {
                for (int i = 0; i < containerSlots; i++) {
                    if (screen.getMenu().getSlot(i).hasItem()) {
                        minecraft.gameMode.handleContainerInput(
                            screen.getMenu().containerId, i, 0,
                            ContainerInput.QUICK_MOVE, minecraft.player);
                    }
                }
            }, buttonX, buttonY, buttonW, buttonH,
            net.minecraft.client.gui.components.Tooltip.create(Component.literal("Steal all items from container"))));

        this.addRenderableWidget(mouseOnly(Component.literal("D"),
            button -> {
                for (int i = containerSlots; i < screen.getMenu().slots.size(); i++) {
                    if (screen.getMenu().getSlot(i).getItem().isEmpty()) continue;
                    minecraft.gameMode.handleContainerInput(
                        screen.getMenu().containerId, i, 0,
                        ContainerInput.QUICK_MOVE, minecraft.player);
                }
            }, buttonX + buttonW + buttonGap, buttonY, buttonW, buttonH,
            net.minecraft.client.gui.components.Tooltip.create(Component.literal("Dump all inventory items into container"))));
    }
}
