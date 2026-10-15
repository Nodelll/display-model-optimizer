package dev.nodelll.displaymodeloptimizer.mixin;

import dev.nodelll.displaymodeloptimizer.culling.DisplayBoundsAccess;
import dev.nodelll.displaymodeloptimizer.culling.DisplayBoundsCache;
import net.minecraft.world.entity.Display;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(value = Display.class)
public abstract class DisplayBoundsMixin implements DisplayBoundsAccess {
    @Unique private volatile DisplayBoundsCache displayModelOptimizer$bounds;
    @Override
    public DisplayBoundsCache displayModelOptimizer$getBoundsCache() {
        DisplayBoundsCache value = displayModelOptimizer$bounds;
        if (value == null) {
            synchronized (this) {
                value = displayModelOptimizer$bounds;
                if (value == null) displayModelOptimizer$bounds = value = new DisplayBoundsCache();
            }
        }
        return value;
    }
}
