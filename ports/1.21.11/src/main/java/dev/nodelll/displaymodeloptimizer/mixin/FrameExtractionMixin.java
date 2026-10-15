package dev.nodelll.displaymodeloptimizer.mixin;

import dev.nodelll.displaymodeloptimizer.BlockDisplayModelCache;
import dev.nodelll.displaymodeloptimizer.GeometryCacheLifecycle;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public abstract class FrameExtractionMixin {
    @Inject(method = "render(Lnet/minecraft/client/DeltaTracker;Z)V", at = @At("HEAD"))
    private void displayModelOptimizer$beginFrame(CallbackInfo ci) {
        BlockDisplayModelCache.beginFrame();
        GeometryCacheLifecycle.beginFrame(Minecraft.getInstance().level);
    }
    @Inject(method = "render(Lnet/minecraft/client/DeltaTracker;Z)V", at = @At("RETURN"))
    private void displayModelOptimizer$finishFrame(CallbackInfo ci) { BlockDisplayModelCache.finishFrame(); }
}
