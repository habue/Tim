package com.example.addon.modules;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import com.example.addon.Tim;

import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.BlockUpdateEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.ColorSetting;
import meteordevelopment.meteorclient.settings.DoubleSetting;
import meteordevelopment.meteorclient.settings.EnumSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.AABB;

/**
 * LavaMarker — highlights fully-flowed lava falls in the Nether.
 *
 * Supports three render modes:
 *   GLOW     – original layered bloom-box renderer (default).
 *   SPECTRAL – subtle filled box only (outline shader is entity-only;
 *              lava is a block so SPECTRAL falls back to a configurable fill).
 *   PULSE    – fading in/out layered bloom-box renderer.
 */
public class LavaMarker extends Module {

    // ═══════════════════════════════════════════════════════════════════════════
    // Enum
    // ═══════════════════════════════════════════════════════════════════════════

    public enum RenderMode {
        GLOW,
        SPECTRAL,
        PULSE
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // Setting Groups
    // ═══════════════════════════════════════════════════════════════════════════

    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgRender  = settings.createGroup("Render");

    // ═══════════════════════════════════════════════════════════════════════════
    // Settings — General
    // ═══════════════════════════════════════════════════════════════════════════

    private final Setting<Integer> chunkRadius = sgGeneral.add(new IntSetting.Builder()
        .name("chunk-radius")
        .description("Horizontal scan radius in chunks.")
        .defaultValue(4).min(1).sliderMax(128)
        .build()
    );

    private final Setting<Integer> verticalRadius = sgGeneral.add(new IntSetting.Builder()
        .name("vertical-radius")
        .description("Vertical scan radius in blocks.")
        .defaultValue(64).min(0).sliderMax(128)
        .build()
    );

    private final Setting<SettingColor> color = sgGeneral.add(new ColorSetting.Builder()
        .name("flowing-lava")
        .description("Color for fully-flowed lava falls. (Alpha is used for GLOW and SPECTRAL outlines).")
        .defaultValue(new SettingColor(255, 100, 0, 200))
        .build()
    );

    private final Setting<Integer> minFallHeight = sgGeneral.add(new IntSetting.Builder()
        .name("min-fall-height")
        .description("Lava falls shorter than this will be ignored.")
        .defaultValue(5).min(0).sliderMax(32)
        .build()
    );

    private final Setting<Integer> maxRenderBlocks = sgGeneral.add(new IntSetting.Builder()
        .name("max-render-blocks")
        .description("Maximum number of blocks to render per frame to prevent crashes.")
        .defaultValue(5000).min(100).sliderMax(20000)
        .build()
    );

    // ═══════════════════════════════════════════════════════════════════════════
    // Settings — Render
    // ═══════════════════════════════════════════════════════════════════════════

    private final Setting<RenderMode> renderMode = sgRender.add(new EnumSetting.Builder<RenderMode>()
        .name("render-mode")
        .description("GLOW = layered bloom boxes. SPECTRAL = subtle fill box. PULSE = fading in/out highlight.")
        .defaultValue(RenderMode.GLOW)
        .build()
    );

    // ── Glow-only settings ────────────────────────────────────────────────────

    private final Setting<Integer> glowLayers = sgRender.add(new IntSetting.Builder()
        .name("glow-layers")
        .description("Number of bloom layers rendered around each lava block.")
        .defaultValue(3).min(1).sliderMax(6)
        .visible(() -> renderMode.get() == RenderMode.GLOW || renderMode.get() == RenderMode.PULSE)
        .build()
    );

    private final Setting<Double> glowSpread = sgRender.add(new DoubleSetting.Builder()
        .name("glow-spread")
        .description("How far each bloom layer expands outward (in blocks).")
        .defaultValue(0.04).min(0.01).sliderMax(0.15)
        .visible(() -> renderMode.get() == RenderMode.GLOW || renderMode.get() == RenderMode.PULSE)
        .build()
    );

    private final Setting<Integer> glowBaseAlpha = sgRender.add(new IntSetting.Builder()
        .name("glow-base-alpha")
        .description("Opacity of the outer glow layers in GLOW mode (0-255).")
        .defaultValue(40).min(4).sliderMax(120)
        .visible(() -> renderMode.get() == RenderMode.GLOW)
        .build()
    );

    // ── Spectral-only settings ────────────────────────────────────────────────

    private final Setting<Integer> spectralFillAlpha = sgRender.add(new IntSetting.Builder()
        .name("spectral-fill-alpha")
        .description("Opacity of the fill box in SPECTRAL mode (0 = invisible, 80 = subtle).")
        .defaultValue(40).min(0).max(200).sliderMax(120)
        .visible(() -> renderMode.get() == RenderMode.SPECTRAL)
        .build()
    );

    private final Setting<Boolean> spectralOutline = sgRender.add(new BoolSetting.Builder()
        .name("spectral-outline")
        .description("Draw a solid outline around lava blocks in SPECTRAL mode.")
        .defaultValue(true)
        .visible(() -> renderMode.get() == RenderMode.SPECTRAL)
        .build()
    );

    // ── Pulse-only settings ───────────────────────────────────────────────────

    private final Setting<Double> pulseSpeed = sgRender.add(new DoubleSetting.Builder()
        .name("pulse-speed")
        .description("Pulse cycle speed. 1.0 = one full fade in/out per second.")
        .defaultValue(1.0).min(0.1).max(5.0).sliderMax(3.0)
        .visible(() -> renderMode.get() == RenderMode.PULSE)
        .build()
    );

    private final Setting<Integer> pulseMinAlpha = sgRender.add(new IntSetting.Builder()
        .name("pulse-min-alpha")
        .description("Lowest opacity reached during the pulse (0 = invisible).")
        .defaultValue(15).min(0).max(255).sliderMax(100)
        .visible(() -> renderMode.get() == RenderMode.PULSE)
        .build()
    );

    private final Setting<Integer> pulseMaxAlpha = sgRender.add(new IntSetting.Builder()
        .name("pulse-max-alpha")
        .description("Peak opacity reached during the pulse.")
        .defaultValue(220).min(15).max(255).sliderMax(255)
        .visible(() -> renderMode.get() == RenderMode.PULSE)
        .build()
    );

    // ═══════════════════════════════════════════════════════════════════════════
    // State
    // ═══════════════════════════════════════════════════════════════════════════

    private final Map<ChunkPos, Set<BlockPos>> fallsByChunk = new ConcurrentHashMap<>();
    private final Set<ChunkPos>                scannedChunks = ConcurrentHashMap.newKeySet();
    private final Set<ChunkPos>                dirtyChunks   = ConcurrentHashMap.newKeySet();

    private String lastDimension = "";

    // ═══════════════════════════════════════════════════════════════════════════
    // Constructor
    // ═══════════════════════════════════════════════════════════════════════════

    public LavaMarker() {
        super(Tim.CATEGORY, "lava-marker", "Highlights fully-flowed lava falls in the Nether.");
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // Lifecycle
    // ═══════════════════════════════════════════════════════════════════════════

    @Override
    public void onActivate() {
        clearData();
        if (mc.level != null) lastDimension = mc.level.dimension().identifier().toString();
    }

    @Override
    public void onDeactivate() {
        clearData();
    }

    private void clearData() {
        fallsByChunk.clear();
        scannedChunks.clear();
        dirtyChunks.clear();
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // Tick — progressive chunk scanning
    // ═══════════════════════════════════════════════════════════════════════════

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (mc.player == null || mc.level == null) return;

        String dim = mc.level.dimension().identifier().toString();
        if (!dim.equals("minecraft:the_nether")) {
            if (!fallsByChunk.isEmpty()) clearData();
            return;
        }
        if (!dim.equals(lastDimension)) {
            lastDimension = dim;
            clearData();
        }

        BlockPos playerPos = mc.player.blockPosition();
        int radius = chunkRadius.get();
        int pX = playerPos.getX() >> 4;
        int pZ = playerPos.getZ() >> 4;

        scannedChunks.removeIf(cp -> isOutOfRange(cp, pX, pZ, radius));
        fallsByChunk.keySet().removeIf(cp -> isOutOfRange(cp, pX, pZ, radius));
        dirtyChunks.removeIf(cp -> isOutOfRange(cp, pX, pZ, radius));

        List<ChunkPos> todo = new ArrayList<>();
        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                ChunkPos cp = new ChunkPos(pX + x, pZ + z);
                if (!scannedChunks.contains(cp) && mc.level.getChunkSource().hasChunk(cp.x(), cp.z())) {
                    todo.add(cp);
                }
            }
        }
        todo.sort(Comparator.comparingDouble(cp -> {
            double dx = cp.x() - pX, dz = cp.z() - pZ;
            return dx * dx + dz * dz;
        }));

        int processed = 0;
        while (!dirtyChunks.isEmpty() && processed < 4) {
            ChunkPos cp = dirtyChunks.iterator().next();
            dirtyChunks.remove(cp);
            scannedChunks.remove(cp);
            if (mc.level.getChunkSource().hasChunk(cp.x(), cp.z())) {
                scanChunk(mc.level.getChunk(cp.x(), cp.z()));
                scannedChunks.add(cp);
                processed++;
            }
        }
        for (ChunkPos cp : todo) {
            if (processed >= 4) break;
            scanChunk(mc.level.getChunk(cp.x(), cp.z()));
            scannedChunks.add(cp);
            processed++;
        }
    }

    private boolean isOutOfRange(ChunkPos cp, int pX, int pZ, int radius) {
        return Math.abs(cp.x() - pX) > radius || Math.abs(cp.z() - pZ) > radius;
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // Block Update
    // ═══════════════════════════════════════════════════════════════════════════

    @EventHandler
    private void onBlockUpdate(BlockUpdateEvent event) {
        if (mc.level == null) return;
        if (!mc.level.dimension().identifier().toString().equals("minecraft:the_nether")) return;
        BlockState ns = event.newState;
        if (ns.is(Blocks.LAVA) || ns.isAir()) {
            ChunkPos cp = ChunkPos.containing(event.pos);
            scannedChunks.remove(cp);
            dirtyChunks.add(cp);
            dirtyChunks.add(new ChunkPos(cp.x() - 1, cp.z()));
            dirtyChunks.add(new ChunkPos(cp.x() + 1, cp.z()));
            dirtyChunks.add(new ChunkPos(cp.x(), cp.z() - 1));
            dirtyChunks.add(new ChunkPos(cp.x(), cp.z() + 1));
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // Chunk Scan
    // ═══════════════════════════════════════════════════════════════════════════

    private void scanChunk(ChunkAccess chunk) {
        if (chunk == null || mc.player == null || mc.level == null) return;

        ChunkPos cp = chunk.getPos();
        int vRadius = verticalRadius.get();
        int playerY = (int) mc.player.getY();
        int minY = Math.max(mc.level.getMinY(), playerY - vRadius);
        int maxY = Math.min(mc.level.getMinY() + mc.level.getHeight(), playerY + vRadius);

        Set<BlockPos> fallTips = new HashSet<>();
        LevelChunkSection[] sections = chunk.getSections();

        for (int i = 0; i < sections.length; i++) {
            LevelChunkSection section = sections[i];
            if (section == null || section.hasOnlyAir()) continue;

            int sectionY    = chunk.getMinSectionY() + i;
            int sectionMinY = sectionY << 4;
            int sectionMaxY = sectionMinY + 15;
            if (sectionMaxY < minY || sectionMinY > maxY) continue;
            if (!section.maybeHas(s -> s.getFluidState().is(FluidTags.LAVA))) continue;

            for (int x = 0; x < 16; x++) {
                for (int y = 0; y < 16; y++) {
                    for (int z = 0; z < 16; z++) {
                        int worldY = sectionMinY + y;
                        if (worldY < minY || worldY > maxY) continue;

                        FluidState fs = section.getBlockState(x, y, z).getFluidState();
                        if (!fs.is(FluidTags.LAVA)) continue;

                        boolean falling = fs.hasProperty(BlockStateProperties.FALLING) && fs.getValue(BlockStateProperties.FALLING);
                        if (!falling) continue;

                        BlockPos pos = new BlockPos(cp.getMinBlockX() + x, worldY, cp.getMinBlockZ() + z);
                        if (!isFalling(pos.below())) fallTips.add(pos);
                    }
                }
            }
        }

        Set<BlockPos> allValidFallBlocks = new HashSet<>();
        Set<BlockPos> visitedInScan      = new HashSet<>();
        for (BlockPos tip : fallTips) {
            if (visitedInScan.contains(tip)) continue;

            Set<BlockPos> currentFall = new HashSet<>();
            bfs(tip, currentFall, visitedInScan);
            if (currentFall.isEmpty()) continue;

            int fallMinY = Integer.MAX_VALUE;
            int fallMaxY = Integer.MIN_VALUE;
            for (BlockPos pos : currentFall) {
                fallMinY = Math.min(fallMinY, pos.getY());
                fallMaxY = Math.max(fallMaxY, pos.getY());
            }
            if (fallMaxY - fallMinY + 1 < minFallHeight.get()) continue;

            for (BlockPos pos : currentFall) {
                FluidState fs = mc.level.getFluidState(pos);
                if (fs.is(FluidTags.LAVA) && !fs.isSource()) allValidFallBlocks.add(pos);
            }
        }

        if (!allValidFallBlocks.isEmpty()) fallsByChunk.put(cp, allValidFallBlocks);
        else fallsByChunk.remove(cp);
    }

    private boolean isFalling(BlockPos pos) {
        FluidState fs = mc.level.getFluidState(pos);
        return fs.is(FluidTags.LAVA) && fs.hasProperty(BlockStateProperties.FALLING) && fs.getValue(BlockStateProperties.FALLING);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // BFS
    // ═══════════════════════════════════════════════════════════════════════════

    private void bfs(BlockPos start, Set<BlockPos> result, Set<BlockPos> visited) {
        if (visited.contains(start)) return;

        Deque<BlockPos> queue = new ArrayDeque<>();
        queue.add(start);
        visited.add(start);

        while (!queue.isEmpty()) {
            BlockPos cur = queue.poll();
            result.add(cur);

            for (BlockPos nb : new BlockPos[]{
                cur.north(), cur.south(), cur.east(), cur.west(), cur.below()
            }) {
                if (!visited.contains(nb)
                        && mc.level.getChunkSource().hasChunk(nb.getX() >> 4, nb.getZ() >> 4)) {
                    FluidState ns = mc.level.getFluidState(nb);
                    if (ns.is(FluidTags.LAVA) && !ns.isSource()) {
                        visited.add(nb);
                        queue.add(nb);
                    }
                }
            }

            BlockPos up = cur.above();
            if (!visited.contains(up)
                    && mc.level.getChunkSource().hasChunk(up.getX() >> 4, up.getZ() >> 4)) {
                if (isFalling(up)) {
                    visited.add(up);
                    queue.add(up);
                }
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // Render
    // ═══════════════════════════════════════════════════════════════════════════

    @EventHandler
    private void onRender(Render3DEvent event) {
        if (mc.level == null) return;

        boolean isSpectral = renderMode.get() == RenderMode.SPECTRAL;
        boolean isPulse    = renderMode.get() == RenderMode.PULSE;
        int count = 0;
        int max   = maxRenderBlocks.get();

        for (Set<BlockPos> set : fallsByChunk.values()) {
            for (BlockPos pos : set) {
                if (count >= max) return;

                FluidState fs = mc.level.getFluidState(pos);
                if (!fs.is(FluidTags.LAVA)) continue;
                if (fs.isSource()) continue;

                boolean isBottomBlock = !mc.level.getFluidState(pos.below()).is(FluidTags.LAVA);
                if (isBottomBlock && mc.level.getBlockState(pos.below()).isAir()) continue;

                AABB box = new AABB(pos);

                if (isSpectral) {
                    int fillAlpha = spectralFillAlpha.get();
                    ShapeMode mode = spectralOutline.get() ? ShapeMode.Both : ShapeMode.Sides;
                    SettingColor outlineColor = spectralOutline.get() ? color.get() : withAlpha(color.get(), 0);
                    event.renderer.box(box, withAlpha(color.get(), fillAlpha), outlineColor, mode, 0);
                } else if (isPulse) {
                    renderPulseBox(event, box, color.get());
                } else {
                    renderGlowLayers(event, box, color.get());
                    event.renderer.box(box, withAlpha(color.get(), color.get().a), color.get(), ShapeMode.Both, 0);
                }

                count++;
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // Bloom & Pulse Rendering
    // ═══════════════════════════════════════════════════════════════════════════

    private void renderGlowLayers(Render3DEvent event, AABB box, SettingColor color) {
        int    layers    = glowLayers.get();
        double spread    = glowSpread.get();
        int    baseAlpha = glowBaseAlpha.get();

        for (int i = layers; i >= 1; i--) {
            double expansion = spread * i;
            int    layerAlpha = Math.max(4, (int) (baseAlpha * (1.0 - (double)(i - 1) / layers)));
            event.renderer.box(
                box.inflate(expansion),
                withAlpha(color, layerAlpha),
                withAlpha(color, 0),
                ShapeMode.Sides, 0
            );
        }
    }

    private float getPulseFactor() {
        double speed = pulseSpeed.get();
        double t = System.currentTimeMillis() / 1000.0;
        double phase = t * speed * Math.PI * 2.0;
        return (float)((Math.sin(phase) + 1.0) * 0.5);
    }

    private int applyPulse() {
        float f = getPulseFactor();
        int min = pulseMinAlpha.get();
        int max = pulseMaxAlpha.get();
        return Math.min(255, Math.max(0, (int)(min + (max - min) * f)));
    }

    private void renderPulseBox(Render3DEvent event, AABB box, SettingColor base) {
        int pa = applyPulse();
        SettingColor pColor = withAlpha(base, pa);
        int layers = glowLayers.get();
        double spread = glowSpread.get();
        for (int i = layers; i >= 1; i--) {
            double expansion = spread * i;
            double taper = 1.0 - ((double)(i - 1) / layers) * 0.6;
            int layerAlpha = Math.max(4, (int)(pa * taper));
            event.renderer.box(box.inflate(expansion),
                withAlpha(pColor, layerAlpha), withAlpha(pColor, 0), ShapeMode.Sides, 0);
        }
        event.renderer.box(box, withAlpha(pColor, pa / 3), pColor, ShapeMode.Both, 0);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // Color Helper
    // ═══════════════════════════════════════════════════════════════════════════

    private SettingColor withAlpha(SettingColor color, int alpha) {
        return new SettingColor(color.r, color.g, color.b, Math.min(255, Math.max(0, alpha)));
    }
}
