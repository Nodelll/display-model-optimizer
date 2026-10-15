package dev.nodelll.displaymodeloptimizer;

import dev.nodelll.displaymodeloptimizer.mixin.BlockModelStateAccessor;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.special.SpecialModelRenderer;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;

public final class ModelCacheTest {
    private static int assertions;

    public static void main(String[] args) {
        Object model = new Object(), block = new Object(), context = new Object();
        var cache = new FrameModelCache<String>(2);
        cache.put(model, block, context, 42, "old");
        check(cache.get(model, block, context, 42) == null, "No caching outside extraction");
        cache.begin();
        cache.put(model, block, context, 42, "first");
        check("first".equals(cache.get(model, block, context, 42)), "Identical input reuses output");
        check(cache.get(model, new Object(), context, 42) == null, "Changed block misses");
        check(cache.get(new Object(), block, context, 42) == null, "Changed resource model misses");
        check(cache.get(model, block, new Object(), 42) == null, "Changed display context misses");
        check(cache.get(model, block, context, 43) == null, "Changed seed misses");
        cache.put(new Object(), block, context, 42, "second");
        cache.put(new Object(), block, context, 42, "third");
        check(cache.size() == 2 && !cache.canStore(), "Memory limit is respected");
        cache.finish();
        check(cache.size() == 0 && !cache.isActive(), "References released at end of frame");
        cache.begin();
        check(cache.get(model, block, context, 42) == null, "Next frame cannot reuse stale output");
        cache.put(model, block, context, 42, "new");
        cache.begin();
        check(cache.size() == 0, "Next frame clears an interrupted previous frame");
        var disabled = new FrameModelCache<String>(0);
        disabled.begin();
        disabled.put(model, block, context, 42, "ignored");
        check(disabled.size() == 0 && !disabled.canStore(), "Zero cap disables storage");
        cache.put(new String("same"), block, context, 42, "identity");
        check(cache.get(new String("same"), block, context, 42) == null, "Resource identity is preserved");

        FakeState source = new FakeState();
        source.parts = new ArrayList<>();
        source.parts.add(null);
        source.matrix = new Matrix4f().translation(1, 2, 3);
        source.tints = new IntArrayList(new int[]{0x123456, 0xabcdef});
        var snapshot = BlockModelSnapshot.captureOutput(source);
        check(snapshot != null, "Ordinary model can be captured");
        source.parts.clear();
        ((Matrix4f) source.matrix).translation(100, 200, 300);
        source.tints.set(0, 0);
        FakeState first = new FakeState();
        snapshot.apply(first);
        check(first.parts.size() == 1, "Source list mutation cannot corrupt snapshot");
        check(first.matrix.m30() == 1 && first.matrix.m31() == 2 && first.matrix.m32() == 3, "Source transform is copied");
        check(first.tints.getInt(0) == 0x123456, "Source tint mutation cannot corrupt snapshot");
        first.parts.clear();
        ((Matrix4f) first.matrix).translation(-1, -2, -3);
        first.tints.clear();
        FakeState second = new FakeState();
        snapshot.apply(second);
        check(second.parts.size() == 1, "Destination list has independent ownership");
        check(second.matrix.m30() == 1, "Destination transform has independent ownership");
        check(second.tints.size() == 2 && second.tints.getInt(1) == 0xabcdef, "Destination tints have independent ownership");
        source.specialMatrix = new Matrix4f();
        check(BlockModelSnapshot.captureOutput(source) == null, "Special model output bypasses cache");
        var empty = BlockModelSnapshot.captureOutput(new FakeState());
        empty.apply(first);
        check(first.parts == null && first.matrix == null && first.tints == null, "Empty state replaces old containers");

        check(!DisplayDistancePolicy.isOutside(0, 1e10), "Zero distance retains vanilla visibility");
        check(!DisplayDistancePolicy.isOutside(32, 32 * 32), "Limit boundary remains visible");
        check(DisplayDistancePolicy.isOutside(32, 32 * 32 + .01), "Beyond limit is rejected");
        check(!DisplayDistancePolicy.isOutside(32, Double.NaN), "NaN retains vanilla handling");
        check(!DisplayDistancePolicy.isOutside(32, Double.POSITIVE_INFINITY), "Infinite distance retains vanilla handling");
        check(!DisplayDistancePolicy.isOutside(32, -1), "Invalid negative distance retains vanilla handling");
        check(!DisplayDistancePolicy.isOutside(32, 31 * 31), "Nearby display remains visible");
        System.out.println("PASS: " + assertions + " model cache ownership, lifetime, key isolation, capacity and distance assertions.");
    }

    private static void check(boolean valid, String message) {
        assertions++;
        if (!valid) throw new AssertionError(message);
    }

    private static final class FakeState implements BlockModelStateAccessor {
        List<BlockStateModelPart> parts;
        Matrix4fc matrix, specialMatrix;
        IntList tints;
        RenderType type;
        public List<BlockStateModelPart> displayModelOptimizer$getModelParts() { return parts; }
        public void displayModelOptimizer$setModelParts(List<BlockStateModelPart> value) { parts = value; }
        public Matrix4fc displayModelOptimizer$getTransformation() { return matrix; }
        public void displayModelOptimizer$setTransformation(Matrix4fc value) { matrix = value; }
        public RenderType displayModelOptimizer$getRenderType() { return type; }
        public void displayModelOptimizer$setRenderType(RenderType value) { type = value; }
        public SpecialModelRenderer<?> displayModelOptimizer$getSpecialRenderer() { return null; }
        public Matrix4fc displayModelOptimizer$getSpecialTransformation() { return specialMatrix; }
        public IntList displayModelOptimizer$getTintLayers() { return tints; }
        public void displayModelOptimizer$setTintLayers(IntList value) { tints = value; }
    }
}
