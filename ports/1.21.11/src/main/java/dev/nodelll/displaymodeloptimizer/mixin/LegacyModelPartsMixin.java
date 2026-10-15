package dev.nodelll.displaymodeloptimizer.mixin;

import dev.nodelll.displaymodeloptimizer.BlockDisplayModelCache;
import java.util.List;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.client.renderer.block.model.BlockModelPart;
import net.minecraft.client.renderer.block.model.BlockStateModel;
import net.minecraft.util.RandomSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = ModelBlockRenderer.class, priority = 1100)
public abstract class LegacyModelPartsMixin {
    @Redirect(method = "renderModel", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/block/model/BlockStateModel;collectParts(Lnet/minecraft/util/RandomSource;)Ljava/util/List;"), require = 0, allow = 1)
    private static List<BlockModelPart> displayModelOptimizer$reuseParts(BlockStateModel model, RandomSource random) {
        return BlockDisplayModelCache.collect(model, random);
    }
}
