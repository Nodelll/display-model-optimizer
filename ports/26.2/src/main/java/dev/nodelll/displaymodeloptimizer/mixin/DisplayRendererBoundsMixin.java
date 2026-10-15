package dev.nodelll.displaymodeloptimizer.mixin;

import dev.nodelll.displaymodeloptimizer.culling.DisplayOcclusion;
import net.minecraft.world.entity.Display;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "net.minecraft.client.renderer.entity.DisplayRenderer", remap = false)
public abstract class DisplayRendererBoundsMixin {
    @Inject(method = "affectedByCulling(Lnet/minecraft/world/entity/Display;)Z", at = @At("HEAD"), cancellable = true)
    private void displayModelOptimizer$enableDisplayCulling(Display entity, CallbackInfoReturnable<Boolean> ci) {
        if (DisplayOcclusion.frustumActive() && DisplayOcclusion.supports(entity))
            ci.setReturnValue(DisplayOcclusion.bounds(entity) != null);
    }
    @Inject(method = "getBoundingBoxForCulling(Lnet/minecraft/world/entity/Display;)Lnet/minecraft/world/phys/AABB;", at = @At("HEAD"), cancellable = true)
    private void displayModelOptimizer$useModelBounds(Display entity, CallbackInfoReturnable<AABB> ci) {
        AABB box = DisplayOcclusion.frustumActive() ? DisplayOcclusion.bounds(entity) : null;
        if (box != null) ci.setReturnValue(box);
    }
}
