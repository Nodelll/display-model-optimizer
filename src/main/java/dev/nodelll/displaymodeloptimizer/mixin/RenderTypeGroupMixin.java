package dev.nodelll.displaymodeloptimizer.mixin;

import dev.nodelll.displaymodeloptimizer.DrawLookupIndex;
import dev.nodelll.displaymodeloptimizer.OptimizerSettings;
import java.util.List;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(targets = "net.minecraft.client.renderer.feature.RenderTypeFeatureRenderer$Group", remap = false)
public abstract class RenderTypeGroupMixin {
    @Unique
    private DrawLookupIndex displayModelOptimizer$lookup;

    // The original method controls consolidation, strict ordering, preparation and draw creation.
    @Redirect(
            method = "getOrAddDraw",
            at = @At(value = "INVOKE", target = "Ljava/util/List;indexOf(Ljava/lang/Object;)I"),
            require = 1,
            allow = 1,
            remap = false
    )
    private int displayModelOptimizer$findPreparedDraw(List<?> renderTypes, Object preparedRenderType) {
        var settings = OptimizerSettings.current();
        if (!OptimizerSettings.isEnabled() || renderTypes.size() < settings.minimumListSize()) {
            return renderTypes.indexOf(preparedRenderType);
        }
        if (displayModelOptimizer$lookup == null || !displayModelOptimizer$lookup.usesSettings(settings.minimumListSize(), settings.verifyEvery())) {
            displayModelOptimizer$lookup = new DrawLookupIndex(
                    settings.minimumListSize(),
                    settings.verifyEvery(),
                    OptimizerSettings::disableAfterMismatch
            );
        }
        return displayModelOptimizer$lookup.findFirst(renderTypes, preparedRenderType);
    }
}
