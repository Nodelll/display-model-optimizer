package dev.nodelll.displaymodeloptimizer.mixin;

import dev.nodelll.displaymodeloptimizer.GeometryCacheLifecycle;
import dev.nodelll.displaymodeloptimizer.culling.DisplayOcclusion;
import net.minecraft.client.Minecraft;
import java.util.concurrent.CompletableFuture;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Minecraft.class)
public abstract class ResourceReloadMixin {
    @Inject(method = "reloadResourcePacks()Ljava/util/concurrent/CompletableFuture;", at = @At("HEAD"))
    private void displayModelOptimizer$invalidateModels(CallbackInfoReturnable<CompletableFuture<Void>> ci) {
        DisplayOcclusion.invalidateAll();
        GeometryCacheLifecycle.invalidate();
    }
    @Inject(method = "reloadResourcePacks()Ljava/util/concurrent/CompletableFuture;", at = @At("RETURN"))
    private void displayModelOptimizer$invalidateLoadedModels(CallbackInfoReturnable<CompletableFuture<Void>> ci) {
        ci.getReturnValue().whenComplete((ignored, error) -> {
            DisplayOcclusion.invalidateAll();
            GeometryCacheLifecycle.invalidate();
        });
    }
}
