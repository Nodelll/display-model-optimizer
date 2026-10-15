package dev.nodelll.displaymodeloptimizer.mixin;

import dev.nodelll.displaymodeloptimizer.BlockDisplayModelCache;
import dev.nodelll.displaymodeloptimizer.GeometryCacheLifecycle;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "net.minecraft.client.renderer.GameRenderer", remap = false)
public abstract class FrameExtractionMixin {
    @Inject(method = "extract(Lnet/minecraft/client/DeltaTracker;Z)V", at = @At("HEAD"))
    private void displayModelOptimizer$beginModelCache(CallbackInfo ci) {
        BlockDisplayModelCache.beginFrame();
        GeometryCacheLifecycle.beginFrame(Minecraft.getInstance().level);
    }

    @Inject(method = "extract(Lnet/minecraft/client/DeltaTracker;Z)V", at = @At("RETURN"))
    private void displayModelOptimizer$finishModelCache(CallbackInfo ci) {
        BlockDisplayModelCache.finishFrame();
    }

}
