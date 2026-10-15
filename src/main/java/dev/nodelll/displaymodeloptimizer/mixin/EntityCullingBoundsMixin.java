package dev.nodelll.displaymodeloptimizer.mixin;

import dev.nodelll.displaymodeloptimizer.OptimizerSettings;
import dev.nodelll.displaymodeloptimizer.culling.DisplayOcclusion;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Dynamic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Optional bridge methods are supplied by Entity Culling's EntityRendererMixin. */
@Mixin(value = EntityRenderer.class, remap = false)
public abstract class EntityCullingBoundsMixin {
    @Dynamic("Added by Entity Culling when installed")
    @Inject(method = "entityCullingIgnoresCulling(Lnet/minecraft/world/entity/Entity;)Z", at = @At("HEAD"), cancellable = true, require = 0)
    private void displayModelOptimizer$occlusionEligibility(Entity entity, CallbackInfoReturnable<Boolean> ci) {
        if (OptimizerSettings.isEnabled() && entity instanceof Display display && DisplayOcclusion.supports(display))
            ci.setReturnValue(!DisplayOcclusion.active() || DisplayOcclusion.bounds(display) == null);
    }

    @Dynamic("Added by Entity Culling when installed")
    @Inject(method = "entityCullingGetCullingBox(Lnet/minecraft/world/entity/Entity;)Lnet/minecraft/world/phys/AABB;", at = @At("HEAD"), cancellable = true, require = 0)
    private void displayModelOptimizer$occlusionBounds(Entity entity, CallbackInfoReturnable<AABB> ci) {
        if (DisplayOcclusion.active() && entity instanceof Display display && DisplayOcclusion.supports(display)) {
            AABB bounds = DisplayOcclusion.bounds(display);
            if (bounds != null) ci.setReturnValue(bounds);
        }
    }
}
