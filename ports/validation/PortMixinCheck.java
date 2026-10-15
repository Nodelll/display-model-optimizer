package dev.nodelll.displaymodeloptimizer.validation;

import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;
import java.util.zip.ZipFile;
import net.minecraft.launchwrapper.Launch;
import net.minecraft.launchwrapper.LaunchClassLoader;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.analysis.Analyzer;
import org.objectweb.asm.tree.analysis.BasicVerifier;
import org.spongepowered.asm.launch.MixinBootstrap;
import org.spongepowered.asm.mixin.MixinEnvironment;
import org.spongepowered.asm.mixin.Mixins;
import org.spongepowered.asm.mixin.transformer.IMixinTransformer;

/** Transforms the shipped JAR against actual runtime Minecraft names, including intermediary on 1.21.11. */
public final class PortMixinCheck {
    public static void main(String[] args) throws Exception {
        Path game = Path.of(args[0]);
        Properties checks = new Properties();
        try (InputStream in = Files.newInputStream(Path.of(args[1]))) { checks.load(in); }
        var urls = new ArrayList<URL>();
        for (String entry : System.getProperty("java.class.path").split(java.io.File.pathSeparator))
            urls.add(Path.of(entry).toUri().toURL());
        Launch.classLoader = new LaunchClassLoader(urls.toArray(URL[]::new));
        Launch.blackboard = new HashMap<>();
        Launch.blackboard.put("TweakClasses", new ArrayList<>());
        Launch.blackboard.put("Tweaks", new ArrayList<>());
        MixinBootstrap.init();
        com.llamalad7.mixinextras.MixinExtrasBootstrap.init();
        MixinEnvironment.getDefaultEnvironment().setSide(MixinEnvironment.Side.CLIENT);
        MixinEnvironment.getCurrentEnvironment().setSide(MixinEnvironment.Side.CLIENT);
        for (int i = 2; i < args.length; i++) Mixins.addConfiguration(args[i]);
        MixinBootstrap.getPlatform().inject();
        var phase = MixinEnvironment.class.getDeclaredMethod("gotoPhase", MixinEnvironment.Phase.class);
        phase.setAccessible(true);
        phase.invoke(null, MixinEnvironment.Phase.DEFAULT);
        var transformer = (IMixinTransformer) MixinEnvironment.getCurrentEnvironment().getActiveTransformer();
        if (transformer == null) throw new AssertionError("No Mixin transformer");
        for (String key : checks.stringPropertyNames().stream().filter(k -> k.startsWith("target.")).sorted().toList()) {
            String[] expected = checks.getProperty(key).split("\\|");
            String name = expected[0];
            byte[] original = bytes(game, name);
            byte[] transformed = transformer.transformClass(MixinEnvironment.getDefaultEnvironment(), name, original);
            ClassNode before = read(original), after = read(transformed);
            if (before.version != after.version) throw new AssertionError("Minecraft class version changed: " + name);
            for (int i = 1; i < expected.length; i++) {
                String[] spec = expected[i].split("=");
                long count = after.methods.stream().flatMap(m -> java.util.stream.StreamSupport.stream(m.instructions.spliterator(), false))
                        .filter(in -> in instanceof MethodInsnNode call && (call.owner + "#" + call.name).equals(spec[0])).count();
                if (count < Integer.parseInt(spec[1])) throw new AssertionError(name + " missing hook " + expected[i] + ", actual " + count);
            }
            for (var method : after.methods)
                if ((method.access & (Opcodes.ACC_ABSTRACT | Opcodes.ACC_NATIVE)) == 0)
                    new Analyzer<>(new BasicVerifier()).analyze(after.name, method);
            if (key.equals("target.command")) verifyDisplayCommand(name, transformed);
            if (checks.getProperty("ec", "false").equals("true") && key.equals("target.00-entity-renderer")) {
                if (!after.interfaces.contains("dev/tr7zw/entityculling/access/EntityRendererInter"))
                    throw new AssertionError("Actual Entity Culling bridge missing");
                for (String hook : List.of("occlusionEligibility", "occlusionBounds")) {
                    long calls = after.methods.stream().flatMap(m -> java.util.stream.StreamSupport.stream(m.instructions.spliterator(), false))
                            .filter(in -> in instanceof MethodInsnNode call && call.name.contains("displayModelOptimizer$" + hook)).count();
                    if (calls != 1) throw new AssertionError("Entity Culling hook " + hook + ": " + calls);
                }
            }
            System.out.println("PASS: shipped mixins transform " + name + "; required hooks and JVM stacks verified.");
        }
        // Both screen wrappers already draw the background. Content must leave the once-per-frame blur alone.
        ClassNode screen = read(bytes(game, checks.getProperty("gui.screen")));
        var wrapper = screen.methods.stream().filter(m -> m.name.equals(checks.getProperty("gui.wrapper"))).findFirst().orElseThrow();
        long backgrounds = java.util.stream.StreamSupport.stream(wrapper.instructions.spliterator(), false)
                .filter(in -> in instanceof MethodInsnNode call && call.name.equals(checks.getProperty("gui.background"))).count();
        if (backgrounds != 1) throw new AssertionError("Unexpected vanilla background lifecycle: " + backgrounds);
        String config = "dev/nodelll/displaymodeloptimizer/gui/DisplayModelOptimizerConfigScreen.class";
        ClassNode content;
        try (InputStream in = PortMixinCheck.class.getClassLoader().getResourceAsStream(config)) { content = read(in.readAllBytes()); }
        for (var method : content.methods) for (var instruction : method.instructions)
            if (instruction instanceof MethodInsnNode call && call.name.equals(checks.getProperty("gui.background")))
                throw new AssertionError("DMO requests a duplicate screen background");
        System.out.println("PASS: actual screen background lifecycle and shipped settings screen verified.");
    }
    private static byte[] bytes(Path game, String name) throws Exception {
        try (ZipFile jar = new ZipFile(game.toFile())) {
            var entry = jar.getEntry(name.replace('.', '/') + ".class");
            if (entry == null) throw new AssertionError("Runtime class missing: " + name);
            return jar.getInputStream(entry).readAllBytes();
        }
    }
    private static ClassNode read(byte[] bytes) {
        ClassNode node = new ClassNode(); new ClassReader(bytes).accept(node, 0); return node;
    }
    private static void verifyDisplayCommand(String name, byte[] bytes) throws Exception {
        ClassLoader isolated = new ClassLoader(PortMixinCheck.class.getClassLoader()) {
            @Override protected Class<?> loadClass(String target, boolean resolve) throws ClassNotFoundException {
                if (!target.equals(name)) return super.loadClass(target, resolve);
                Class<?> type = findLoadedClass(target);
                if (type == null) type = defineClass(target, bytes, 0, bytes.length);
                if (resolve) resolveClass(type);
                return type;
            }
        };
        Class<?> command = Class.forName(name, true, isolated);
        Class<?> scope = Class.forName("dev.nodelll.displaymodeloptimizer.LegacyDisplayScope");
        var set = scope.getMethod("submitting", boolean.class);
        var constructor = command.getConstructors()[0];
        var tagged = command.getMethod("displayModelOptimizer$isDisplay");
        Object ordinary = constructor.newInstance(null, null, 77, 3, 8);
        if (!Boolean.FALSE.equals(tagged.invoke(ordinary))) throw new AssertionError("Non-display command was tagged");
        Object previous = set.invoke(null, true);
        try {
            Object display = constructor.newInstance(null, null, 77, 3, 8);
            if (!Boolean.TRUE.equals(tagged.invoke(display))) throw new AssertionError("Display tag did not survive queue construction");
        } finally { set.invoke(null, previous); }
        if (!Boolean.FALSE.equals(tagged.invoke(ordinary))) throw new AssertionError("Queued command source was mutable");
        System.out.println("PASS: transformed game queue record executes; only display commands retain the display tag.");
    }
}
