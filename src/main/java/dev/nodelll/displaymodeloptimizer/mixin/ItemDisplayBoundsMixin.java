package dev.nodelll.displaymodeloptimizer.mixin;

import dev.nodelll.displaymodeloptimizer.culling.DisplayOcclusion;
import net.minecraft.client.renderer.entity.state.ItemDisplayEntityRenderState;
import net.minecraft.world.entity.Display;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "net.minecraft.client.renderer.entity.DisplayRenderer$ItemDisplayRenderer", remap = false)
public abstract class ItemDisplayBoundsMixin {
    @Inject(method = "extractRenderState(Lnet/minecraft/world/entity/Display$ItemDisplay;Lnet/minecraft/client/renderer/entity/state/ItemDisplayEntityRenderState;F)V", at = @At("RETURN"))
    private void displayModelOptimizer$captureItemBounds(Display.ItemDisplay entity, ItemDisplayEntityRenderState state, float partialTick, CallbackInfo ci) {
        DisplayOcclusion.captureItem(entity, state);
    }
}
