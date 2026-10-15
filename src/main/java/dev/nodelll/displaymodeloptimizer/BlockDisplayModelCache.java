package dev.nodelll.displaymodeloptimizer;

import dev.nodelll.displaymodeloptimizer.mixin.BlockModelStateAccessor;
import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.client.renderer.block.model.BlockDisplayContext;
import net.minecraft.client.renderer.block.model.BlockModel;
import net.minecraft.client.renderer.entity.DisplayRenderer;
import net.minecraft.world.level.block.state.BlockState;

public final class BlockDisplayModelCache {
    private static final ThreadLocal<FrameModelCache<BlockModelSnapshot>> FRAMES = ThreadLocal.withInitial(
            () -> new FrameModelCache<>(0));

    private BlockDisplayModelCache() {}

    public static void beginFrame() {
        FRAMES.get().begin(OptimizerSettings.current().modelCacheLimit());
    }

    public static void finishFrame() {
        FRAMES.get().finish();
    }

    public static void update(BlockModel model, BlockModelRenderState target, BlockState block,
                              BlockDisplayContext context, long seed) {
        FrameModelCache<BlockModelSnapshot> cache = FRAMES.get();
        if (!OptimizerSettings.isEnabled() || !OptimizerSettings.current().cacheBlockDisplayModels() || !cache.isActive()
                || context != DisplayRenderer.BLOCK_DISPLAY_CONTEXT) {
            model.update(target, block, context, seed);
            return;
        }
        BlockModelSnapshot previous = cache.get(model, block, context, seed);
        if (previous != null) {
            previous.apply((BlockModelStateAccessor) target);
            return;
        }
        model.update(target, block, context, seed);
        if (cache.canStore()) cache.put(model, block, context, seed, BlockModelSnapshot.capture(target));
    }
}
