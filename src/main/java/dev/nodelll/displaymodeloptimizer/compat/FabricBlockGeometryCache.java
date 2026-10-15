package dev.nodelll.displaymodeloptimizer.compat;

import dev.nodelll.displaymodeloptimizer.GeometryCacheLifecycle;
import dev.nodelll.displaymodeloptimizer.OptimizerSettings;
import java.util.function.Predicate;
import net.fabricmc.fabric.api.client.renderer.v1.Renderer;
import net.fabricmc.fabric.api.client.renderer.v1.mesh.QuadEmitter;
import net.fabricmc.fabric.api.client.renderer.v1.model.FabricBlockStateModel;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.model.BlockDisplayContext;
import net.minecraft.client.renderer.entity.DisplayRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;

/** Optional FRAPI bridge. Cache model-space geometry, never light, tint, pose or an entity's state. */
public final class FabricBlockGeometryCache {
    private static final ThreadLocal<GeometryReplayCache> LOCAL = ThreadLocal.withInitial(GeometryReplayCache::new);

    private FabricBlockGeometryCache() {}

    public static void emit(FabricBlockStateModel model, QuadEmitter output, BlockAndTintGetter level,
                            BlockPos pos, BlockState state, RandomSource random, Predicate<Direction> cullTest,
                            BlockDisplayContext context, long seed) {
        var settings = OptimizerSettings.current();
        if (!OptimizerSettings.isEnabled() || !settings.cacheBlockDisplayGeometry() || settings.modelCacheLimit() == 0
                || context != DisplayRenderer.BLOCK_DISPLAY_CONTEXT || level != BlockAndTintGetter.EMPTY
                || !BlockPos.ZERO.equals(pos)) {
            model.emitQuads(output, level, pos, state, random, cullTest);
            return;
        }
        LOCAL.get().emit(model, output, level, pos, state, random, cullTest, seed, Renderer.get(),
                Minecraft.getInstance().options.cutoutLeaves().get(), GeometryCacheLifecycle.generation(), settings.modelCacheLimit());
    }

}
