package dev.nodelll.displaymodeloptimizer.validation;

import java.net.URL;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import net.minecraft.launchwrapper.Launch;
import net.minecraft.launchwrapper.LaunchClassLoader;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.analysis.Analyzer;
import org.objectweb.asm.tree.analysis.BasicVerifier;
import org.spongepowered.asm.launch.MixinBootstrap;
import org.spongepowered.asm.mixin.MixinEnvironment;
import org.spongepowered.asm.mixin.Mixins;
import org.spongepowered.asm.mixin.transformer.IMixinTransformer;

/** Applies the real FRAPI overwrite before checking the new redirect, then runs real Sodium meshes. */
public final class GeometryPipelineLauncher {
    public static void main(String[] args) throws Exception {
        var urls = new ArrayList<URL>();
        for (String path : System.getProperty("java.class.path").split(java.io.File.pathSeparator))
            urls.add(Path.of(path).toAbsolutePath().toUri().toURL());
        Launch.classLoader = new LaunchClassLoader(urls.toArray(URL[]::new));
        Launch.blackboard = new HashMap<>();
        Launch.blackboard.put("TweakClasses", new ArrayList<>());
        Launch.blackboard.put("Tweaks", new ArrayList<>());
        MixinBootstrap.init();
        MixinEnvironment.getDefaultEnvironment().setSide(MixinEnvironment.Side.CLIENT);
        MixinEnvironment.getCurrentEnvironment().setSide(MixinEnvironment.Side.CLIENT);
        // Standalone FabricLoader has no discovered mods. This selects the optional mixin for this harness only.
        Mixins.addConfiguration("geometry-dmo-test.mixins.json");
        Mixins.addConfiguration("display-model-optimizer.mixins.json");
        Mixins.addConfiguration("geometry-frapi-test.mixins.json");
        Mixins.addConfiguration("geometry-sodium-test.mixins.json");
        MixinBootstrap.getPlatform().inject();
        var phase = MixinEnvironment.class.getDeclaredMethod("gotoPhase", MixinEnvironment.Phase.class);
        phase.setAccessible(true);
        phase.invoke(null, MixinEnvironment.Phase.DEFAULT);
        var transformer = (IMixinTransformer) MixinEnvironment.getCurrentEnvironment().getActiveTransformer();
        String target = "net.minecraft.client.renderer.block.model.BlockStateModelWrapper";
        byte[] bytes = Launch.classLoader.getClassBytes(target);
        byte[] transformed = transformer.transformClass(MixinEnvironment.getDefaultEnvironment(), target, bytes);
        ClassNode node = new ClassNode();
        new ClassReader(transformed).accept(node, 0);
        int hooks = 0, tintCalls = 0, setupCalls = 0;
        for (MethodNode method : node.methods) {
            if ((method.access & (0x400 | 0x100)) == 0) new Analyzer<>(new BasicVerifier()).analyze(node.name, method);
            for (var instruction : method.instructions) if (instruction instanceof MethodInsnNode call) {
                if (call.owner.equals("dev/nodelll/displaymodeloptimizer/compat/FabricBlockGeometryCache") && call.name.equals("emit")) hooks++;
                if (method.name.equals("update") && call.name.equals("updateTints")) tintCalls++;
                if (method.name.equals("update") && call.name.equals("setupMesh")) setupCalls++;
            }
        }
        if (hooks != 1 || tintCalls != 1 || setupCalls != 1)
            throw new AssertionError("Combined FRAPI/DMO hook counts: cache=" + hooks + ", tint=" + tintCalls + ", setup=" + setupCalls);
        System.out.println("PASS: actual FRAPI overwrite + DMO redirect apply together; current tints/setup preserved; method stacks verified.");
        Launch.classLoader.registerTransformer("org.spongepowered.asm.mixin.transformer.Proxy");
        Class<?> cases = Class.forName("dev.nodelll.displaymodeloptimizer.validation.GeometryPipelineCases", true, Launch.classLoader);
        try { cases.getMethod("main", String[].class).invoke(null, (Object) args); }
        catch (java.lang.reflect.InvocationTargetException e) { throw new RuntimeException(e.getCause()); }
    }
}
