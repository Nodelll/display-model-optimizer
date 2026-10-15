package dev.nodelll.displaymodeloptimizer.mixin;

import dev.nodelll.displaymodeloptimizer.LegacyDisplayScope;
import net.minecraft.client.renderer.SubmitNodeStorage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SubmitNodeStorage.BlockSubmit.class)
public abstract class BlockSubmitMixin implements LegacyDisplayScope.Command {
    @Unique private boolean displayModelOptimizer$display;
    @Inject(method = "<init>", at = @At("RETURN"))
    private void displayModelOptimizer$rememberSource(CallbackInfo ci) {
        displayModelOptimizer$display = LegacyDisplayScope.submitting();
    }
    @Override public boolean displayModelOptimizer$isDisplay() { return displayModelOptimizer$display; }
}
