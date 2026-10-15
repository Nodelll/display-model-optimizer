package dev.nodelll.displaymodeloptimizer.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.nodelll.displaymodeloptimizer.LegacyDisplayScope;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.feature.BlockFeatureRenderer;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(BlockFeatureRenderer.class)
public abstract class BlockFeatureScopeMixin {
    @Unique private boolean displayModelOptimizer$nextDisplay;
    @Redirect(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/SubmitNodeStorage$BlockSubmit;state()Lnet/minecraft/world/level/block/state/BlockState;"), require = 2, allow = 2)
    private BlockState displayModelOptimizer$selectCommand(SubmitNodeStorage.BlockSubmit command) {
        displayModelOptimizer$nextDisplay = ((LegacyDisplayScope.Command) (Object) command).displayModelOptimizer$isDisplay();
        return command.state();
    }
    @Redirect(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/block/BlockRenderDispatcher;renderSingleBlock(Lnet/minecraft/world/level/block/state/BlockState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;II)V"), require = 2, allow = 2)
    private void displayModelOptimizer$renderDisplay(BlockRenderDispatcher dispatcher, BlockState state,
                                                     PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        boolean previous = LegacyDisplayScope.rendering(displayModelOptimizer$nextDisplay);
        displayModelOptimizer$nextDisplay = false;
        try { dispatcher.renderSingleBlock(state, pose, buffers, light, overlay); }
        finally { LegacyDisplayScope.rendering(previous); }
    }
}
