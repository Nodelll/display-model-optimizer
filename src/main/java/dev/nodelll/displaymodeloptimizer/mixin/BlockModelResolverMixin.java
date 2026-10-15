package dev.nodelll.displaymodeloptimizer.mixin;

import dev.nodelll.displaymodeloptimizer.BlockDisplayModelCache;
import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.client.renderer.block.BlockModelResolver;
import net.minecraft.client.renderer.block.model.BlockDisplayContext;
import net.minecraft.client.renderer.block.model.BlockModel;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = BlockModelResolver.class, remap = false)
public abstract class BlockModelResolverMixin {
    @Redirect(method = "update(Lnet/minecraft/client/renderer/block/BlockModelRenderState;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/client/renderer/block/model/BlockDisplayContext;)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/block/model/BlockModel;update(Lnet/minecraft/client/renderer/block/BlockModelRenderState;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/client/renderer/block/model/BlockDisplayContext;J)V"),
            require = 1, allow = 1)
    private void displayModelOptimizer$prepareBlockModel(BlockModel model, BlockModelRenderState target, BlockState block,
                                               BlockDisplayContext context, long seed) {
        BlockDisplayModelCache.update(model, target, block, context, seed);
    }
}
