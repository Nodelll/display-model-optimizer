package dev.nodelll.displaymodeloptimizer.validation;

import dev.nodelll.displaymodeloptimizer.compat.GeometryReplayCache;
import java.util.Arrays;
import java.util.function.Consumer;
import java.util.function.Predicate;
import net.caffeinemc.mods.sodium.client.render.frapi.mesh.MutableMeshImpl;
import net.fabricmc.fabric.api.client.renderer.v1.Renderer;
import net.fabricmc.fabric.api.client.renderer.v1.mesh.Mesh;
import net.fabricmc.fabric.api.client.renderer.v1.mesh.MutableMesh;
import net.fabricmc.fabric.api.client.renderer.v1.mesh.MutableQuadView;
import net.fabricmc.fabric.api.client.renderer.v1.mesh.QuadEmitter;
import net.fabricmc.fabric.api.client.renderer.v1.model.FabricBlockStateModel;
import net.fabricmc.fabric.api.client.renderer.v1.render.AltModelBlockRenderer;
import net.fabricmc.fabric.api.util.TriState;
import net.minecraft.client.color.block.BlockColors;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.texture.SpriteContents;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.SimpleModelWrapper;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.client.resources.model.geometry.QuadCollection;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.client.model.geom.builders.UVPair;
import net.minecraft.resources.Identifier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.fabricmc.fabric.impl.client.renderer.VanillaBlockModelPartEncoder;
import org.joml.Vector3f;

/** Uses Sodium's actual int-packed mesh implementation; no GL context or Minecraft client is started. */
public final class GeometryPipelineCases {
    private static final Predicate<Direction> NEVER_CULL = face -> false;
    private static int assertions;
    private static volatile long blackhole;

    private static final class TestRenderer implements Renderer {
        int builders;
        public MutableMesh mutableMesh() { builders++; return new MutableMeshImpl(); }
        public QuadEmitter quadEmitter(Consumer<? super MutableQuadView> consumer) { throw new UnsupportedOperationException(); }
        public AltModelBlockRenderer altModelBlockRenderer(boolean ao, boolean cull, BlockColors colors) { throw new UnsupportedOperationException(); }
    }

    private static final class Model implements FabricBlockStateModel {
        int emissions, quads = 24, variant;
        boolean dynamic, randomKey;
        public Object createGeometryKey(BlockAndTintGetter level, BlockPos pos, BlockState state, RandomSource random) {
            return dynamic ? null : randomKey ? Long.valueOf(random.nextLong()) : Integer.valueOf(variant);
        }
        public void emitQuads(QuadEmitter out, BlockAndTintGetter level, BlockPos pos, BlockState state,
                              RandomSource random, Predicate<Direction> cull) {
            emissions++;
            float offset = random.nextFloat() * .25f;
            for (int i = 0; i < quads; i++) {
                Direction face = i % 7 == 6 ? null : Direction.values()[i % 7];
                if (cull.test(face)) continue;
                out.cullFace(face).nominalFace(Direction.NORTH).chunkLayer(i % 2 == 0 ? ChunkSectionLayer.SOLID : ChunkSectionLayer.TRANSLUCENT)
                        .tintIndex(i % 3 - 1).emissive(i % 2 == 0).animated(i % 2 != 0)
                        .ambientOcclusion(TriState.values()[i % 3]).tag(i + variant);
                for (int v = 0; v < 4; v++) {
                    out.pos(v, (v == 1 || v == 2 ? 1 : 0) + offset, v > 1 ? 1 : 0, i * .001f)
                            .uv(v, v * .1f, i * .02f).color(v, 0xffaabb00 + i + v)
                            .normal(v, 0, 0, -1).lightmap(v, 0x00100020 + i);
                }
                out.emit();
            }
        }
    }

    private static final class VanillaModel implements FabricBlockStateModel {
        final SimpleModelWrapper part;
        VanillaModel(int count) throws Exception {
            // Test-only sprite metadata, without allocating an image or starting OpenGL.
            var field = sun.misc.Unsafe.class.getDeclaredField("theUnsafe"); field.setAccessible(true);
            var unsafe = (sun.misc.Unsafe) field.get(null);
            var contents = (SpriteContents) unsafe.allocateInstance(SpriteContents.class);
            var sprite = (TextureAtlasSprite) unsafe.allocateInstance(TextureAtlasSprite.class);
            var spriteContents = TextureAtlasSprite.class.getDeclaredField("contents"); spriteContents.setAccessible(true);
            spriteContents.set(sprite, contents);
            var atlas = TextureAtlasSprite.class.getDeclaredField("atlasLocation"); atlas.setAccessible(true);
            atlas.set(sprite, Identifier.withDefaultNamespace("textures/atlas/blocks.png"));
            var builder = new QuadCollection.Builder();
            for (int i = 0; i < count; i++) {
                var material = BakedQuad.MaterialInfo.of(new Material.Baked(sprite, false),
                        i % 2 == 0 ? com.mojang.blaze3d.platform.Transparency.NONE : com.mojang.blaze3d.platform.Transparency.TRANSLUCENT,
                        i % 3 - 1, null, i % 2 == 0 ? 15 : 0);
                var quad = new BakedQuad(new Vector3f(0, 0, i * .001f), new Vector3f(1, 0, i * .001f),
                        new Vector3f(1, 1, i * .001f), new Vector3f(0, 1, i * .001f),
                        UVPair.pack(0, 0), UVPair.pack(1, 0), UVPair.pack(1, 1), UVPair.pack(0, 1), Direction.NORTH, material);
                if (i % 7 == 6) builder.addUnculledFace(quad);
                else builder.addCulledFace(Direction.values()[i % 7], quad);
            }
            part = new SimpleModelWrapper(builder.build(), true, new Material.Baked(sprite, false));
        }
        public Object createGeometryKey(BlockAndTintGetter level, BlockPos pos, BlockState state, RandomSource random) { return this; }
        public void emitQuads(QuadEmitter out, BlockAndTintGetter level, BlockPos pos, BlockState state,
                              RandomSource random, Predicate<Direction> cull) {
            VanillaBlockModelPartEncoder.emitQuads(part, out, cull);
        }
    }

    public static void main(String[] args) throws Exception {
        var renderer = new TestRenderer();
        var model = new Model();
        var cache = new GeometryReplayCache();
        var destination = renderer.mutableMesh();
        var expected = renderer.mutableMesh();
        for (int i = 0; i < 25; i++) {
            model.emitQuads(expected.emitter(), null, BlockPos.ZERO, null, RandomSource.create(42), NEVER_CULL);
            int emissions = model.emissions;
            cache.emit(model, destination.emitter(), null, BlockPos.ZERO, null, RandomSource.create(42), NEVER_CULL,
                    42, renderer, true, 0, 8);
            check(Arrays.equals(raw(expected), raw(destination)), "full packed geometry parity on miss/hit");
            check(model.emissions == emissions + (i == 0 ? 1 : 0), "one conversion across repeated frames");
            expected.clear(); destination.clear();
        }
        var otherModel = new Model(); otherModel.quads = 12;
        cache.emit(otherModel, destination.emitter(), null, BlockPos.ZERO, null, RandomSource.create(42), NEVER_CULL,
                42, renderer, true, 0, 8);
        check(destination.size() == 12 && otherModel.emissions == 1, "different model identity cannot reuse an equal custom key");
        destination.clear();
        // Caller transforms run after replay, rather than becoming part of the shared geometry.
        destination.emitter().pushTransform(q -> { q.tintIndex(12); q.translate(2, 3, 4); return true; });
        cache.emit(model, destination.emitter(), null, BlockPos.ZERO, null, RandomSource.create(42), NEVER_CULL,
                42, renderer, true, 0, 8);
        destination.emitter().popTransform();
        destination.forEach(q -> check(q.tintIndex() == 12 && q.x(0) >= 2, "current caller transform retained"));
        destination.clear();
        var clean = renderer.mutableMesh();
        cache.emit(model, clean.emitter(), null, BlockPos.ZERO, null, RandomSource.create(42), NEVER_CULL,
                42, renderer, true, 0, 8);
        clean.forEach(q -> check(q.tintIndex() != 12 && q.x(0) < 1, "cache immutable after transformed replay"));
        int before = model.emissions;
        model.variant++;
        emit(cache, renderer, model, 0, true, 8, 42);
        check(model.emissions == before + 1, "geometry key change rebuilds");
        emit(cache, renderer, model, 1, true, 8, 42);
        emit(cache, renderer, model, 1, false, 8, 42);
        check(model.emissions == before + 3, "resource generation and graphics setting rebuild");
        emit(cache, new TestRenderer(), model, 1, false, 8, 42);
        check(model.emissions == before + 4, "renderer change rebuilds");
        model.dynamic = true;
        emit(cache, renderer, model, 1, false, 8, 42); emit(cache, renderer, model, 1, false, 8, 42);
        check(model.emissions == before + 6, "unknown dynamic geometry bypasses cache");
        model.dynamic = false; model.randomKey = true;
        for (long seed : new long[]{3, 6, 3, 6}) {
            expected.clear(); destination.clear();
            model.emitQuads(expected.emitter(), null, BlockPos.ZERO, null, RandomSource.create(seed), NEVER_CULL);
            cache.emit(model, destination.emitter(), null, BlockPos.ZERO, null, RandomSource.create(seed), NEVER_CULL,
                    seed, renderer, false, 1, 8);
            check(Arrays.equals(raw(expected), raw(destination)), "key RNG never advances emission RNG; weighted seed selection retained");
        }
        before = model.emissions;
        emit(cache, renderer, model, 1, false, 0, 3); emit(cache, renderer, model, 1, false, 0, 3);
        check(model.emissions == before + 2, "zero limit bypasses");
        model.randomKey = false; model.quads = GeometryReplayCache.MAXIMUM_ENTRY_QUADS + 1;
        int builders = renderer.builders;
        emit(cache, renderer, model, 2, false, 8, 42); emit(cache, renderer, model, 2, false, 8, 42);
        check(renderer.builders == builders + 3, "oversized model builds cache once, then falls back");
        for (int count : new int[]{6, 24, 96, 384}) {
            var vanilla = new VanillaModel(count);
            cache = new GeometryReplayCache();
            for (int repeat = 0; repeat < 5; repeat++) {
                expected.clear(); destination.clear();
                vanilla.emitQuads(expected.emitter(), null, BlockPos.ZERO, null, RandomSource.create(42), NEVER_CULL);
                cache.emit(vanilla, destination.emitter(), null, BlockPos.ZERO, null, RandomSource.create(42), NEVER_CULL,
                        42, renderer, true, 0, 1024);
                check(Arrays.equals(raw(expected), raw(destination)), "actual vanilla encoder and baked quad parity");
            }
        }
        System.out.println("PASS: " + assertions + " actual Sodium mesh parity, metadata, transforms, key RNG and bypass assertions.");
        if (Arrays.asList(args).contains("--benchmark")) benchmark(renderer);
    }

    private static void emit(GeometryReplayCache cache, TestRenderer renderer, Model model, long generation,
                              boolean leaves, int limit, long seed) {
        cache.emit(model, renderer.mutableMesh().emitter(), null, BlockPos.ZERO, null, RandomSource.create(seed),
                NEVER_CULL, seed, renderer, leaves, generation, limit);
    }

    private static int[] raw(MutableMesh mesh) throws Exception {
        Class<?> type = mesh.getClass().getSuperclass();
        var data = type.getDeclaredField("data"); var limit = type.getDeclaredField("limit");
        data.setAccessible(true); limit.setAccessible(true);
        return Arrays.copyOf((int[]) data.get(mesh), limit.getInt(mesh));
    }

    private static void benchmark(TestRenderer renderer) throws Exception {
        var target = renderer.mutableMesh();
        var random = RandomSource.create(42);
        for (int quads : new int[]{6, 24, 96, 384}) {
            var model = new VanillaModel(quads);
            var cache = new GeometryReplayCache();
            long[] baseline = new long[9], cached = new long[9];
            for (int round = -4; round < 9; round++) {
                for (int order = 0; order < 2; order++) {
                    boolean useCache = (round & 1) == order;
                    long start = System.nanoTime();
                    for (int call = 0; call < 12000; call++) {
                        target.clear(); random.setSeed(42);
                        if (useCache) cache.emit(model, target.emitter(), null, BlockPos.ZERO, null, random, NEVER_CULL,
                                42, renderer, false, quads, 1024);
                        else model.emitQuads(target.emitter(), null, BlockPos.ZERO, null, random, NEVER_CULL);
                        blackhole += target.size();
                    }
                    long duration = System.nanoTime() - start;
                    if (round >= 0) (useCache ? cached : baseline)[round] = duration;
                }
            }
            Arrays.sort(baseline); Arrays.sort(cached);
            System.out.printf(java.util.Locale.ROOT,
                    "CPU vanilla-encoder fixture: %d quads, direct %.1f ns, cached %.1f ns, %.2fx preparation throughput (not FPS)%n",
                    quads, baseline[4] / 12000.0, cached[4] / 12000.0, (double) baseline[4] / cached[4]);
        }
    }

    private static void check(boolean value, String message) {
        assertions++; if (!value) throw new AssertionError(message);
    }
}
