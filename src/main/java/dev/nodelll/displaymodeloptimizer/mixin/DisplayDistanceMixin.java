package dev.nodelll.displaymodeloptimizer.mixin;

import dev.nodelll.displaymodeloptimizer.DisplayDistancePolicy;
import dev.nodelll.displaymodeloptimizer.OptimizerSettings;
import net.minecraft.world.entity.Display;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = Display.class, remap = false)
public abstract class DisplayDistanceMixin {
    @Inject(method = "shouldRenderAtSqrDistance(D)Z", at = @At("HEAD"), cancellable = true)
    private void displayModelOptimizer$limitDecorativeDisplays(double squaredDistance, CallbackInfoReturnable<Boolean> ci) {
        if (OptimizerSettings.isEnabled()
                && ((Object) this instanceof Display.ItemDisplay || (Object) this instanceof Display.BlockDisplay)
                && DisplayDistancePolicy.isOutside(OptimizerSettings.current().displayRenderDistance(), squaredDistance)) {
            ci.setReturnValue(false);
        }
    }
}
