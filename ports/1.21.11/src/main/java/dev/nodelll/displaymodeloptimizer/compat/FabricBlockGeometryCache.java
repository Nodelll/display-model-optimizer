package dev.nodelll.displaymodeloptimizer.compat;

import dev.nodelll.displaymodeloptimizer.GeometryCacheLifecycle;
import dev.nodelll.displaymodeloptimizer.LegacyDisplayScope;
import dev.nodelll.displaymodeloptimizer.OptimizerSettings;
import java.util.List;
import java.util.function.Predicate;
import net.fabricmc.fabric.api.renderer.v1.Renderer;
import net.fabricmc.fabric.api.renderer.v1.mesh.QuadEmitter;
import net.fabricmc.fabric.api.renderer.v1.model.FabricBlockStateModel;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.BlockModelPart;
import net.minecraft.client.renderer.block.model.BlockStateModel;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.EmptyBlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;

/** The old FRAPI renders directly. Wrap model emission, leaving its renderer's light/tint/pose intact. */
public final class FabricBlockGeometryCache {
    private static final ThreadLocal<GeometryReplayCache> LOCAL = ThreadLocal.withInitial(GeometryReplayCache::new);
    private FabricBlockGeometryCache() {}
    public static BlockStateModel wrap(BlockStateModel model) {
        var settings = OptimizerSettings.current();
        if (!LegacyDisplayScope.rendering() || !OptimizerSettings.isEnabled()
                || !settings.cacheBlockDisplayGeometry() || settings.modelCacheLimit() == 0
                || model instanceof CachedModel) return model;
        return new CachedModel(model);
    }

    private record CachedModel(BlockStateModel delegate) implements BlockStateModel, FabricBlockStateModel {
        private FabricBlockStateModel fabric() { return (FabricBlockStateModel) delegate; }
        @Override public void collectParts(RandomSource random, List<BlockModelPart> target) { delegate.collectParts(random, target); }
        @Override public TextureAtlasSprite particleIcon() { return delegate.particleIcon(); }
        @Override public TextureAtlasSprite particleSprite(BlockAndTintGetter level, BlockPos pos, BlockState state) {
            return fabric().particleSprite(level, pos, state);
        }
        @Override public Object createGeometryKey(BlockAndTintGetter level, BlockPos pos, BlockState state, RandomSource random) {
            return fabric().createGeometryKey(level, pos, state, random);
        }
        @Override public void emitQuads(QuadEmitter output, BlockAndTintGetter level, BlockPos pos, BlockState state,
                                        RandomSource random, Predicate<Direction> cullTest) {
            var settings = OptimizerSettings.current();
            if (level != EmptyBlockAndTintGetter.INSTANCE || !BlockPos.ZERO.equals(pos)) {
                fabric().emitQuads(output, level, pos, state, random, cullTest);
                return;
            }
            // Indigo and Sodium's simple renderers both initialize their emission random to 42.
            LOCAL.get().emit(fabric(), output, level, pos, state, random, cullTest, 42, Renderer.get(),
                    Minecraft.getInstance().options.cutoutLeaves().get(), GeometryCacheLifecycle.generation(), settings.modelCacheLimit());
        }
    }
}
