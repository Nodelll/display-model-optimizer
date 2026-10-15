package dev.nodelll.displaymodeloptimizer;

import java.util.List;
import net.minecraft.client.renderer.block.model.BlockModelPart;
import net.minecraft.client.renderer.block.model.BlockStateModel;
import net.minecraft.client.renderer.block.model.SingleVariant;
import net.minecraft.client.renderer.block.model.multipart.MultiPartModel;
import net.minecraft.client.resources.model.WeightedVariants;
import net.minecraft.util.RandomSource;

/** 1.21.11 prepares model parts during drawing rather than extracting BlockModelRenderState. */
public final class BlockDisplayModelCache {
    private static final ThreadLocal<FrameModelCache<List<BlockModelPart>>> FRAMES =
            ThreadLocal.withInitial(() -> new FrameModelCache<>(0));
    private BlockDisplayModelCache() {}
    public static void beginFrame() { FRAMES.get().begin(OptimizerSettings.current().modelCacheLimit()); }
    public static void finishFrame() { FRAMES.get().finish(); }
    public static boolean supported(BlockStateModel model) {
        // Unknown or animated mod models retain their normal preparation on every draw.
        return model.getClass() == SingleVariant.class || model.getClass() == WeightedVariants.class
                || model.getClass() == MultiPartModel.class;
    }
    public static List<BlockModelPart> collect(BlockStateModel model, RandomSource random) {
        var frame = FRAMES.get();
        if (!LegacyDisplayScope.rendering() || !OptimizerSettings.isEnabled()
                || !OptimizerSettings.current().cacheBlockDisplayModels() || !frame.isActive() || !supported(model))
            return model.collectParts(random);
        // Vanilla ModelBlockRenderer.renderModel constructs a fresh random source with seed 42.
        var parts = frame.get(model, null, null, 42);
        if (parts != null) return parts;
        parts = model.collectParts(random);
        if (frame.canStore()) frame.put(model, null, null, 42, List.copyOf(parts));
        return parts;
    }
}
