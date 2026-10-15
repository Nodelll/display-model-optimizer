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

/** Loads each target renderer with its actual mesh bridge mixins before executing parity checks. */
public final class PortMeshLauncher {
    public static void main(String[] args) throws Exception {
        var urls = new ArrayList<URL>();
        for (String path : System.getProperty("java.class.path").split(java.io.File.pathSeparator))
            urls.add(Path.of(path).toAbsolutePath().toUri().toURL());
        Launch.classLoader = new LaunchClassLoader(urls.toArray(URL[]::new));
        Launch.blackboard = new HashMap<>();
        Launch.blackboard.put("TweakClasses", new ArrayList<>());
        Launch.blackboard.put("Tweaks", new ArrayList<>());
        MixinBootstrap.init();
        com.llamalad7.mixinextras.MixinExtrasBootstrap.init();
        MixinEnvironment.getDefaultEnvironment().setSide(MixinEnvironment.Side.CLIENT);
        MixinEnvironment.getCurrentEnvironment().setSide(MixinEnvironment.Side.CLIENT);
        Mixins.addConfiguration("mesh.mixins.json");
        MixinBootstrap.getPlatform().inject();
        var phase = MixinEnvironment.class.getDeclaredMethod("gotoPhase", MixinEnvironment.Phase.class);
        phase.setAccessible(true);
        phase.invoke(null, MixinEnvironment.Phase.DEFAULT);
        Launch.classLoader.registerTransformer("org.spongepowered.asm.mixin.transformer.Proxy");
        Class<?> cases = Class.forName("dev.nodelll.displaymodeloptimizer.validation.PortMeshCases", true, Launch.classLoader);
        try { cases.getMethod("main", String[].class).invoke(null, (Object) args); }
        catch (java.lang.reflect.InvocationTargetException e) { throw new RuntimeException(e.getCause()); }
    }
}
