package dev.nodelll.displaymodeloptimizer.mixin;

import dev.nodelll.displaymodeloptimizer.compat.FabricBlockGeometryCache;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.block.model.BlockStateModel;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Wrap before Indigo or Sodium redirects drawing; their world context and tint remain original. */
@Mixin(value = BlockRenderDispatcher.class, priority = 1100)
public abstract class FabricBlockGeometryMixin {
    @Redirect(method = "renderSingleBlock", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/block/BlockRenderDispatcher;getBlockModel(Lnet/minecraft/world/level/block/state/BlockState;)Lnet/minecraft/client/renderer/block/model/BlockStateModel;"), require = 1, allow = 1)
    private BlockStateModel displayModelOptimizer$cacheDisplayGeometry(BlockRenderDispatcher dispatcher, BlockState state) {
        return FabricBlockGeometryCache.wrap(dispatcher.getBlockModel(state));
    }
}
