package dev.nodelll.displaymodeloptimizer.mixin;

import dev.nodelll.displaymodeloptimizer.compat.FabricBlockGeometryCache;
import java.util.function.Predicate;
import net.fabricmc.fabric.api.client.renderer.v1.mesh.QuadEmitter;
import net.fabricmc.fabric.api.client.renderer.v1.model.FabricBlockStateModel;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.model.BlockDisplayContext;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

// Injections run after overwrites; a higher priority allows injection into FRAPI's merged method.
@Mixin(targets = "net.minecraft.client.renderer.block.model.BlockStateModelWrapper", priority = 1100, remap = false)
public abstract class FabricBlockGeometryMixin {
    @Redirect(method = "update", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/block/dispatch/BlockStateModel;emitQuads(Lnet/fabricmc/fabric/api/client/renderer/v1/mesh/QuadEmitter;Lnet/minecraft/client/renderer/block/BlockAndTintGetter;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/util/RandomSource;Ljava/util/function/Predicate;)V"), require = 0)
    private void displayModelOptimizer$cachedGeometry(BlockStateModel model, QuadEmitter emitter,
            BlockAndTintGetter level, BlockPos pos, BlockState state, RandomSource random, Predicate<Direction> cullTest,
            BlockModelRenderState output, BlockState originalState, BlockDisplayContext context, long seed) {
        FabricBlockGeometryCache.emit((FabricBlockStateModel) model, emitter, level, pos, state, random, cullTest, context, seed);
    }
}
