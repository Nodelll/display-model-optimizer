package dev.nodelll.displaymodeloptimizer.mixin;

import dev.nodelll.displaymodeloptimizer.culling.DisplayOcclusion;
import net.minecraft.client.renderer.entity.state.BlockDisplayEntityRenderState;
import net.minecraft.world.entity.Display;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "net.minecraft.client.renderer.entity.DisplayRenderer$BlockDisplayRenderer", remap = false)
public abstract class BlockDisplayBoundsMixin {
    @Inject(method = "extractRenderState(Lnet/minecraft/world/entity/Display$BlockDisplay;Lnet/minecraft/client/renderer/entity/state/BlockDisplayEntityRenderState;F)V", at = @At("RETURN"))
    private void displayModelOptimizer$captureBlockBounds(Display.BlockDisplay entity, BlockDisplayEntityRenderState state, float partialTick, CallbackInfo ci) {
        DisplayOcclusion.captureBlock(entity, state);
    }
}
