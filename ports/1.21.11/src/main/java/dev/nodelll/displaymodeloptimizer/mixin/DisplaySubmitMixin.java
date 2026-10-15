package dev.nodelll.displaymodeloptimizer.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.nodelll.displaymodeloptimizer.LegacyDisplayScope;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(targets = "net.minecraft.client.renderer.entity.DisplayRenderer$BlockDisplayRenderer")
public abstract class DisplaySubmitMixin {
    @Redirect(method = "submitInner(Lnet/minecraft/client/renderer/entity/state/BlockDisplayEntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;IF)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/SubmitNodeCollector;submitBlock(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/world/level/block/state/BlockState;III)V"), require = 1, allow = 1)
    private void displayModelOptimizer$tagDisplay(SubmitNodeCollector collector, PoseStack pose, BlockState state,
                                                int light, int overlay, int outline) {
        boolean previous = LegacyDisplayScope.submitting(true);
        try { collector.submitBlock(pose, state, light, overlay, outline); }
        finally { LegacyDisplayScope.submitting(previous); }
    }
}
