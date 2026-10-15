package dev.nodelll.displaymodeloptimizer.compat;

import dev.nodelll.displaymodeloptimizer.GeometryCache;
import java.util.function.Predicate;
import net.fabricmc.fabric.api.renderer.v1.Renderer;
import net.fabricmc.fabric.api.renderer.v1.mesh.Mesh;
import net.fabricmc.fabric.api.renderer.v1.mesh.MutableMesh;
import net.fabricmc.fabric.api.renderer.v1.mesh.QuadEmitter;
import net.fabricmc.fabric.api.renderer.v1.model.FabricBlockStateModel;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;

/** Renderer-independent cache logic; exercised with actual renderer meshes by the offline harness. */
public final class GeometryReplayCache {
    public static final int MAXIMUM_QUADS = 65536;
    public static final int MAXIMUM_ENTRY_QUADS = 4096;
    private static final Predicate<Direction> NEVER_CULL = face -> false;
    private static final class Key {
        final FabricBlockStateModel model;
        final BlockState state;
        final Object geometry;
        final int hash;

        Key(FabricBlockStateModel model, BlockState state, Object geometry) {
            this.model = model;
            this.state = state;
            this.geometry = geometry;
            hash = 31 * (31 * System.identityHashCode(model) + System.identityHashCode(state)) + geometry.hashCode();
        }
        @Override public int hashCode() { return hash; }
        @Override public boolean equals(Object value) {
            return value instanceof Key other && model == other.model && state == other.state && geometry.equals(other.geometry);
        }
    }
    private record Cached(Mesh mesh) {}
    private static final Cached OVERSIZED = new Cached(null);
    private final GeometryCache<Key, Cached> meshes = new GeometryCache<>(MAXIMUM_QUADS);
    private final RandomSource keyRandom = RandomSource.create(0);
    private long generation = Long.MIN_VALUE;
    private Renderer renderer;
    private boolean cutoutLeaves;

    public void emit(FabricBlockStateModel model, QuadEmitter output, BlockAndTintGetter level,
                     BlockPos pos, BlockState state, RandomSource random, Predicate<Direction> cullTest,
                     long seed, Renderer currentRenderer, boolean leaves, long currentGeneration, int limit) {
        if (generation != currentGeneration || renderer != currentRenderer || cutoutLeaves != leaves) {
            meshes.clear();
            generation = currentGeneration;
            renderer = currentRenderer;
            cutoutLeaves = leaves;
        }
        meshes.configure(limit);
        if (limit == 0) {
            model.emitQuads(output, level, pos, state, random, cullTest);
            return;
        }
        // Key creation may consume randomness. It must not advance the emitter's random source.
        keyRandom.setSeed(seed);
        Object key = model.createGeometryKey(level, pos, state, keyRandom);
        if (key == null) {
            model.emitQuads(output, level, pos, state, random, cullTest);
            return;
        }
        Key cacheKey = new Key(model, state, key);
        Cached cached = meshes.get(cacheKey);
        Mesh geometry;
        if (cached == null) {
            MutableMesh builder = renderer.mutableMesh();
            model.emitQuads(builder.emitter(), level, pos, state, random, NEVER_CULL);
            if (builder.size() > MAXIMUM_ENTRY_QUADS) {
                // A bounded marker avoids rebuilding an extra mesh on subsequent calls.
                meshes.put(cacheKey, OVERSIZED, 0);
                builder.outputTo(output);
                return;
            }
            geometry = builder.immutableCopy();
            meshes.put(cacheKey, new Cached(geometry), geometry.size());
        } else if (cached == OVERSIZED) {
            model.emitQuads(output, level, pos, state, random, cullTest);
            return;
        } else {
            geometry = cached.mesh();
        }
        // The production caller is FRAPI's simple entity renderer, which never culls a face.
        // Copying through the renderer preserves material, normals, UVs, emission and caller transforms.
        geometry.outputTo(output);
    }
}
