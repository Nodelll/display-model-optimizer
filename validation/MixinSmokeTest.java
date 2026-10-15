package dev.nodelll.displaymodeloptimizer.validation;

import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.zip.ZipFile;
import net.minecraft.launchwrapper.Launch;
import net.minecraft.launchwrapper.LaunchClassLoader;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.analysis.Analyzer;
import org.objectweb.asm.tree.analysis.BasicVerifier;
import org.spongepowered.asm.launch.MixinBootstrap;
import org.spongepowered.asm.mixin.MixinEnvironment;
import org.spongepowered.asm.mixin.Mixins;
import org.spongepowered.asm.mixin.transformer.IMixinTransformer;

/** Offline transformation check against the official 26.3 bytecode, without launching the game. */
public final class MixinSmokeTest {
    private static final String TARGET = "net.minecraft.client.renderer.feature.RenderTypeFeatureRenderer$Group";

    public static void main(String[] args) throws Exception {
        Path game = Path.of(args[0]).toAbsolutePath();
        Path classes = Path.of(args[1]).toAbsolutePath();
        Path resources = Path.of(args[2]).toAbsolutePath();
        var urls = new ArrayList<URL>();
        urls.add(classes.toUri().toURL()); urls.add(resources.toUri().toURL()); urls.add(game.toUri().toURL());
        if (args.length > 4) urls.add(Path.of(args[4]).toAbsolutePath().toUri().toURL());
        if (args.length > 5) urls.add(Path.of(args[5]).toAbsolutePath().toUri().toURL());
        Launch.classLoader = new LaunchClassLoader(urls.toArray(URL[]::new));
        Launch.blackboard = new HashMap<>();
        Launch.blackboard.put("TweakClasses", new ArrayList<>());
        Launch.blackboard.put("Tweaks", new ArrayList<>());
        MixinBootstrap.init();
        MixinEnvironment.getDefaultEnvironment().setSide(MixinEnvironment.Side.CLIENT);
        MixinEnvironment.getCurrentEnvironment().setSide(MixinEnvironment.Side.CLIENT);
        Mixins.addConfiguration("display-model-optimizer.mixins.json");
        if (args.length > 4) Mixins.addConfiguration("ec-compat-test.mixins.json");
        MixinBootstrap.getPlatform().inject();
        // Standalone LaunchWrapper has no game launcher to advance Mixin's phase.
        var advancePhase = MixinEnvironment.class.getDeclaredMethod("gotoPhase", MixinEnvironment.Phase.class);
        advancePhase.setAccessible(true);
        advancePhase.invoke(null, MixinEnvironment.Phase.DEFAULT);
        IMixinTransformer transformer = (IMixinTransformer) MixinEnvironment.getCurrentEnvironment().getActiveTransformer();
        if (transformer == null) throw new AssertionError("Mixin transformer was not initialized");
        checkAdditionalTarget(transformer, game, "net.minecraft.client.renderer.entity.EntityRenderer", (oldClass, newClass) -> {
            require(calls(method(oldClass, "shouldRender"), "net/minecraft/client/renderer/culling/Frustum", "isVisible")
                    == calls(method(newClass, "shouldRender"), "net/minecraft/client/renderer/culling/Frustum", "isVisible"),
                    "Original vanilla frustum calls removed");
            if (args.length > 4) {
                require(newClass.interfaces.contains("dev/tr7zw/entityculling/access/EntityRendererInter"), "Actual Entity Culling renderer bridge did not apply");
                require(hookCalls(method(newClass, "entityCullingIgnoresCulling"), "displayModelOptimizer$occlusionEligibility") == 1,
                        "Optional occlusion eligibility hook did not apply");
                require(hookCalls(method(newClass, "entityCullingGetCullingBox"), "displayModelOptimizer$occlusionBounds") == 1,
                        "Optional model-box hook did not apply");
            } else {
                require(newClass.methods.stream().noneMatch(m -> m.name.startsWith("entityCulling")),
                        "Optional Entity Culling API must stay absent without the mod");
            }
        });
        if (args.length > 4) {
            checkAdditionalTarget(transformer, game, "net.minecraft.client.renderer.extract.LevelExtractor", (oldClass, newClass) -> {
                require(newClass.methods.stream().anyMatch(m -> m.name.contains("extractEntityRedir")), "Actual Entity Culling extraction hook missing");
            });
        }
        byte[] original;
        try (ZipFile zip = new ZipFile(game.toFile())) {
            original = zip.getInputStream(zip.getEntry(TARGET.replace('.', '/') + ".class")).readAllBytes();
        }
        byte[] transformed = transformer.transformClass(MixinEnvironment.getDefaultEnvironment(), TARGET, original);
        ClassNode before = read(original);
        ClassNode after = read(transformed);
        MethodNode oldLookup = method(before, "getOrAddDraw");
        MethodNode newLookup = method(after, "getOrAddDraw");
        require(calls(oldLookup, "java/util/List", "indexOf") == 1, "Unexpected original lookup");
        require(calls(newLookup, "java/util/List", "indexOf") == 0, "Mixin did not replace the lookup");
        require(after.fields.stream().anyMatch(f -> f.desc.equals("Ldev/nodelll/displaymodeloptimizer/DrawLookupIndex;")
                && (f.access & Opcodes.ACC_STATIC) == 0), "Missing group-owned cache field");
        for (String call : new String[]{"prepare", "canConsolidateConsecutiveGeometry", "sortOnUpload", "format", "primitiveTopology"}) {
            require(calls(oldLookup, "net/minecraft/client/renderer/rendertype/RenderType", call)
                    == calls(newLookup, "net/minecraft/client/renderer/rendertype/RenderType", call), "Changed original operation: " + call);
        }
        require(calls(oldLookup, "java/util/List", "add") == calls(newLookup, "java/util/List", "add"), "Changed draw insertion");
        require(calls(oldLookup, "net/minecraft/client/renderer/StagedVertexBuffer", "appendDraw")
                == calls(newLookup, "net/minecraft/client/renderer/StagedVertexBuffer", "appendDraw"), "Changed draw creation");
        require(calls(method(before, "getVertexBuilder"), "net/minecraft/client/renderer/rendertype/RenderType", "canConsolidateConsecutiveGeometry")
                == calls(method(after, "getVertexBuilder"), "net/minecraft/client/renderer/rendertype/RenderType", "canConsolidateConsecutiveGeometry"), "Changed consecutive draw handling");
        for (MethodNode node : after.methods) {
            if ((node.access & (Opcodes.ACC_ABSTRACT | Opcodes.ACC_NATIVE)) == 0) {
                new Analyzer<>(new BasicVerifier()).analyze(after.name, node);
            }
        }
        Files.write(Path.of(args[3]), transformed);
        System.out.println("PASS: Mixin applied to official Minecraft 26.3 Group; lookup replaced once, draw operations preserved, all method stacks verified.");

        checkAdditionalTarget(transformer, game, "net.minecraft.client.renderer.block.BlockModelRenderState", (oldClass, newClass) -> {
            require(newClass.interfaces.contains("dev/nodelll/displaymodeloptimizer/mixin/BlockModelStateAccessor"), "Missing model access interface");
            require(newClass.methods.stream().filter(m -> m.name.startsWith("displayModelOptimizer$")).count() == 10, "Incorrect model accessor count");
            require(oldClass.fields.size() == newClass.fields.size(), "Unexpected additional model state field");
        });
        checkAdditionalTarget(transformer, game, "net.minecraft.client.renderer.block.BlockModelResolver", (oldClass, newClass) -> {
            MethodNode oldUpdate = method(oldClass, "update"), newUpdate = method(newClass, "update");
            require(calls(oldUpdate, "net/minecraft/client/renderer/block/model/BlockModel", "update") == 1, "Unexpected original model update");
            require(calls(newUpdate, "net/minecraft/client/renderer/block/model/BlockModel", "update") == 0, "Model update hook did not apply");
            require(calls(newUpdate, "net/minecraft/client/renderer/block/BlockModelRenderState", "clear") == 1, "Original model clearing removed");
            for (String originalCall : new String[]{"emissiveRendering", "getLightEmission"}) {
                require(calls(oldUpdate, "net/minecraft/world/level/block/state/BlockState", originalCall)
                        == calls(newUpdate, "net/minecraft/world/level/block/state/BlockState", originalCall), "Changed block light operation");
            }
            require(newClass.methods.stream().anyMatch(m -> calls(m, "dev/nodelll/displaymodeloptimizer/BlockDisplayModelCache", "update") == 1), "Missing cache entrypoint");
        });
        checkAdditionalTarget(transformer, game, "net.minecraft.client.renderer.GameRenderer", (oldClass, newClass) -> {
            MethodNode extract = method(newClass, "extract");
            require(newClass.methods.stream().anyMatch(m -> calls(m, "dev/nodelll/displaymodeloptimizer/BlockDisplayModelCache", "beginFrame") == 1), "Missing frame begin hook");
            require(newClass.methods.stream().anyMatch(m -> calls(m, "dev/nodelll/displaymodeloptimizer/BlockDisplayModelCache", "finishFrame") == 1), "Missing frame finish hook");
            require(calls(extract, "net/minecraft/client/renderer/extract/LevelExtractor", "extract") == 1, "Original level extraction removed");
            require(newClass.methods.stream().anyMatch(m -> calls(m, "dev/nodelll/displaymodeloptimizer/culling/DisplayOcclusion", "invalidateAll") == 1), "Missing resource-reload invalidation");
        });
        checkAdditionalTarget(transformer, game, "net.minecraft.world.entity.Display", (oldClass, newClass) -> {
            MethodNode oldDistance = method(oldClass, "shouldRenderAtSqrDistance"), newDistance = method(newClass, "shouldRenderAtSqrDistance");
            require(calls(oldDistance, "net/minecraft/world/entity/Display", "getViewRange")
                    == calls(newDistance, "net/minecraft/world/entity/Display", "getViewRange"), "Original server view range removed");
            require(newClass.methods.stream().anyMatch(m -> calls(m, "dev/nodelll/displaymodeloptimizer/DisplayDistancePolicy", "isOutside") == 1), "Missing distance predicate");
            require(newClass.interfaces.contains("dev/nodelll/displaymodeloptimizer/culling/DisplayBoundsAccess"), "Missing entity-owned bounds cache");
            require(newClass.fields.stream().anyMatch(f -> f.desc.equals("Ldev/nodelll/displaymodeloptimizer/culling/DisplayBoundsCache;")
                    && (f.access & Opcodes.ACC_STATIC) == 0 && (f.access & Opcodes.ACC_VOLATILE) != 0), "Display bounds should be owned and safely published by its entity");
        });
        checkAdditionalTarget(transformer, game, "net.minecraft.client.renderer.entity.DisplayRenderer", (oldClass, newClass) -> {
            require(newClass.methods.stream().filter(m -> calls(m, "dev/nodelll/displaymodeloptimizer/culling/DisplayOcclusion", "bounds") == 1).count() == 2,
                    "Missing visibility eligibility or model-box hook");
            require(newClass.methods.stream().filter(m -> calls(m, "dev/nodelll/displaymodeloptimizer/culling/DisplayOcclusion", "frustumActive") == 1).count() == 2,
                    "Vanilla frustum hooks must use their independent switch");
            require(newClass.methods.stream().noneMatch(m -> calls(m, "dev/nodelll/displaymodeloptimizer/culling/DisplayOcclusion", "active") > 0),
                    "Vanilla frustum must not depend on Entity Culling occlusion eligibility");
            require(calls(method(newClass, "getBoundingBoxForCulling"), "net/minecraft/world/entity/Display", "getInterpolatedBoundingBox") == 1,
                    "Original culling-box fallback removed");
        });
        checkAdditionalTarget(transformer, game, "net.minecraft.client.renderer.entity.DisplayRenderer$ItemDisplayRenderer", (oldClass, newClass) -> {
            require(newClass.methods.stream().anyMatch(m -> calls(m, "dev/nodelll/displaymodeloptimizer/culling/DisplayOcclusion", "captureItem") == 1), "Missing item model capture");
            require(calls(method(newClass, "extractRenderState"), "net/minecraft/client/renderer/item/ItemModelResolver", "updateForNonLiving") == 1,
                    "Original item preparation removed");
        });
        checkAdditionalTarget(transformer, game, "net.minecraft.client.renderer.entity.DisplayRenderer$BlockDisplayRenderer", (oldClass, newClass) -> {
            require(newClass.methods.stream().anyMatch(m -> calls(m, "dev/nodelll/displaymodeloptimizer/culling/DisplayOcclusion", "captureBlock") == 1), "Missing block model capture");
            require(calls(method(newClass, "extractRenderState"), "net/minecraft/client/renderer/block/BlockModelResolver", "update") == 1,
                    "Original block preparation removed");
        });
    }

    private static void checkAdditionalTarget(IMixinTransformer transformer, Path game, String target,
                                              java.util.function.BiConsumer<ClassNode, ClassNode> checks) throws Exception {
        byte[] original;
        try (ZipFile zip = new ZipFile(game.toFile())) {
            original = zip.getInputStream(zip.getEntry(target.replace('.', '/') + ".class")).readAllBytes();
        }
        byte[] transformed = transformer.transformClass(MixinEnvironment.getDefaultEnvironment(), target, original);
        ClassNode before = read(original), after = read(transformed);
        checks.accept(before, after);
        require(before.version == after.version, "Changed Minecraft bytecode version");
        for (MethodNode node : after.methods) {
            if ((node.access & (Opcodes.ACC_ABSTRACT | Opcodes.ACC_NATIVE)) == 0) {
                new Analyzer<>(new BasicVerifier()).analyze(after.name, node);
            }
        }
        if (target.equals("net.minecraft.client.renderer.block.BlockModelRenderState")) verifyModelAccessors(transformed);
        System.out.println("PASS: " + target + " transformed; expected hooks present; all method stacks verified.");
    }

    private static void verifyModelAccessors(byte[] bytecode) throws Exception {
        String name = "net.minecraft.client.renderer.block.BlockModelRenderState";
        ClassLoader isolated = new ClassLoader(MixinSmokeTest.class.getClassLoader()) {
            @Override
            protected Class<?> loadClass(String className, boolean resolve) throws ClassNotFoundException {
                if (!className.equals(name)) return super.loadClass(className, resolve);
                Class<?> loaded = findLoadedClass(className);
                if (loaded == null) loaded = defineClass(className, bytecode, 0, bytecode.length);
                if (resolve) resolveClass(loaded);
                return loaded;
            }
        };
        Class<?> type = Class.forName(name, true, isolated);
        Object first = type.getConstructor().newInstance();
        var input = (dev.nodelll.displaymodeloptimizer.mixin.BlockModelStateAccessor) first;
        var parts = new ArrayList<net.minecraft.client.renderer.block.dispatch.BlockStateModelPart>();
        parts.add(null);
        input.displayModelOptimizer$setModelParts(parts);
        input.displayModelOptimizer$setTransformation(new org.joml.Matrix4f().translation(1, 2, 3));
        input.displayModelOptimizer$setTintLayers(new it.unimi.dsi.fastutil.ints.IntArrayList(new int[]{123, 456}));
        var capture = dev.nodelll.displaymodeloptimizer.BlockModelSnapshot.class.getDeclaredMethod("captureOutput",
                dev.nodelll.displaymodeloptimizer.mixin.BlockModelStateAccessor.class);
        capture.setAccessible(true);
        var snapshot = (dev.nodelll.displaymodeloptimizer.BlockModelSnapshot) capture.invoke(null, input);
        require(snapshot != null, "Actual model state could not be captured");
        Object second = type.getConstructor().newInstance();
        type.getField("blockLightCoords").setInt(second, 77);
        var output = (dev.nodelll.displaymodeloptimizer.mixin.BlockModelStateAccessor) second;
        snapshot.apply(output);
        require(output.displayModelOptimizer$getModelParts().size() == 1, "Actual model list copy failed");
        require(output.displayModelOptimizer$getTintLayers().getInt(1) == 456, "Actual tint copy failed");
        require(output.displayModelOptimizer$getTransformation().m30() == 1, "Actual transform copy failed");
        require(type.getField("blockLightCoords").getInt(second) == 77, "Cache overwrote per-entity light");
        input.displayModelOptimizer$getModelParts().clear();
        require(output.displayModelOptimizer$getModelParts().size() == 1, "Actual state containers alias");
        System.out.println("PASS: Transformed Minecraft model state instantiated; accessor copies execute; destination light preserved.");
    }

    private static ClassNode read(byte[] bytes) {
        ClassNode node = new ClassNode();
        new ClassReader(bytes).accept(node, 0);
        return node;
    }

    private static MethodNode method(ClassNode node, String name) {
        return node.methods.stream().filter(m -> m.name.equals(name)).findFirst().orElseThrow();
    }

    private static long calls(MethodNode node, String owner, String name) {
        long count = 0;
        for (var instruction : node.instructions) {
            if (instruction instanceof MethodInsnNode call && call.owner.equals(owner) && call.name.equals(name)) count++;
        }
        return count;
    }

    private static long hookCalls(MethodNode node, String fragment) {
        long count = 0;
        for (var instruction : node.instructions)
            if (instruction instanceof MethodInsnNode call && call.name.contains(fragment)) count++;
        return count;
    }

    private static void require(boolean valid, String message) {
        if (!valid) throw new AssertionError(message);
    }
}
