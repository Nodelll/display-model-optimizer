package dev.nodelll.displaymodeloptimizer;

import dev.nodelll.displaymodeloptimizer.mixin.BlockModelStateAccessor;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.lang.reflect.Modifier;
import java.util.List;
import java.util.Set;
import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.rendertype.RenderType;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;

/** Copies vanilla model output, without retaining any entity render state. */
public final class BlockModelSnapshot {
    private static final Set<String> VANILLA_FIELDS = Set.of("modelParts", "transformation", "renderType",
            "specialRenderer", "specialRendererTransformation", "tintLayers", "blockLightCoords", "randomSource");
    private static final ClassValue<Boolean> SUPPORTED = new ClassValue<>() {
        @Override
        protected Boolean computeValue(Class<?> type) {
            // Iris updates this field in BlockModelResolver.update's TAIL, after our hook.
            boolean iris = false;
            for (Class<?> implemented : type.getInterfaces()) {
                if (implemented.getName().equals("net.irisshaders.iris.mixinterface.BlockModelRenderStateExtension")) iris = true;
            }
            for (var field : type.getDeclaredFields()) {
                if (Modifier.isStatic(field.getModifiers()) || VANILLA_FIELDS.contains(field.getName())) continue;
                if (iris && field.getName().equals("block")
                        && field.getType().getName().equals("net.minecraft.world.level.block.state.BlockState")) continue;
                System.getLogger("Display Model Optimizer").log(System.Logger.Level.INFO,
                        "[Display Model Optimizer] Model cache bypassed for additional state field: " + field.getName());
                return false;
            }
            return type.getName().equals("net.minecraft.client.renderer.block.BlockModelRenderState");
        }
    };

    private final List<BlockStateModelPart> parts;
    private final Matrix4fc transformation;
    private final RenderType renderType;
    private final int[] tints;

    private BlockModelSnapshot(BlockModelStateAccessor state) {
        var originalParts = state.displayModelOptimizer$getModelParts();
        parts = originalParts == null ? null : new ObjectArrayList<>(originalParts);
        var matrix = state.displayModelOptimizer$getTransformation();
        transformation = matrix == null ? null : new Matrix4f(matrix);
        renderType = state.displayModelOptimizer$getRenderType();
        var originalTints = state.displayModelOptimizer$getTintLayers();
        tints = originalTints == null ? null : originalTints.toIntArray();
    }

    public static BlockModelSnapshot capture(BlockModelRenderState target) {
        if (!SUPPORTED.get(target.getClass())) return null;
        return captureOutput((BlockModelStateAccessor) target);
    }

    static BlockModelSnapshot captureOutput(BlockModelStateAccessor state) {
        // Special renderers may extract animated state; keep their original update path.
        if (state.displayModelOptimizer$getSpecialRenderer() != null
                || state.displayModelOptimizer$getSpecialTransformation() != null) return null;
        return new BlockModelSnapshot(state);
    }

    public void apply(BlockModelStateAccessor target) {
        // These containers remain private to the destination; later hooks cannot corrupt the cache.
        target.displayModelOptimizer$setModelParts(parts == null ? null : new ObjectArrayList<>(parts));
        target.displayModelOptimizer$setTransformation(transformation == null ? null : new Matrix4f(transformation));
        target.displayModelOptimizer$setRenderType(renderType);
        target.displayModelOptimizer$setTintLayers(tints == null ? null : new IntArrayList(tints));
    }
}
