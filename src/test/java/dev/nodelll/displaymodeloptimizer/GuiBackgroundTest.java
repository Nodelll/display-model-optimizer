package dev.nodelll.displaymodeloptimizer;

import java.io.InputStream;
import java.lang.classfile.ClassFile;
import java.lang.classfile.ClassModel;
import java.lang.classfile.MethodModel;
import java.lang.classfile.instruction.ConstantInstruction;
import java.lang.classfile.instruction.InvokeInstruction;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipFile;

/** Exercises the real Minecraft background lifecycle and its once-per-frame blur guard. */
public final class GuiBackgroundTest {
    private static final String SCREEN = "net/minecraft/client/gui/screens/Screen";
    private static final String CONFIG = "dev/nodelll/displaymodeloptimizer/gui/DisplayModelOptimizerConfigScreen";
    private static final String GUI_STATE = "net/minecraft/client/gui/render/state/GuiRenderState";
    private static final String BLUR_ERROR = "Can only blur once per frame";
    private static int assertions;

    public static void main(String[] args) throws Exception {
        ClassModel screen = read(SCREEN);
        List<String> wrapper = calls(method(screen, "extractRenderStateWithTooltipAndSubtitles"));
        check(count(wrapper, "extractBackground") == 1,
                "Minecraft's screen wrapper must extract the background exactly once");
        check(index(wrapper, "extractBackground") >= 0
                        && index(wrapper, "extractBackground") < index(wrapper, "extractRenderState"),
                "Minecraft must extract the background before invoking screen content");

        ClassModel config;
        if (args.length == 0) {
            config = read(CONFIG);
        } else {
            try (ZipFile jar = new ZipFile(Path.of(args[0]).toFile())) {
                config = ClassFile.of().parse(jar.getInputStream(jar.getEntry(CONFIG + ".class")).readAllBytes());
            }
        }
        List<String> content = calls(method(config, "extractRenderState"));
        check(count(content, "extractBackground") == 0 && count(content, "blurBeforeThisStratum") == 0,
                "DMO content must not request a second background or blur");
        check(content.stream().filter(call -> call.equals(SCREEN + "#extractRenderState")).count() == 1,
                "DMO must retain the base screen's widget extraction");
        check(count(calls(method(screen, "extractRenderState")), "extractBackground") == 0,
                "Base widget extraction must not add another background");

        ClassModel stateModel = read(GUI_STATE);
        MethodModel guardModel = stateModel.methods().stream()
                .filter(candidate -> candidate.code().isPresent())
                .filter(candidate -> candidate.code().orElseThrow().elementList().stream()
                        .anyMatch(element -> element instanceof ConstantInstruction constant
                                && BLUR_ERROR.equals(constant.constantValue())))
                .findFirst().orElseThrow(() -> new AssertionError("Official Minecraft double-blur guard missing"));
        check(guardModel.methodType().equalsString("()V"), "The official blur guard is a no-argument frame operation");

        Class<?> stateClass = Class.forName(GUI_STATE.replace('/', '.'));
        Object state = stateClass.getConstructor().newInstance();
        Method blur = stateClass.getMethod(guardModel.methodName().stringValue());
        Method reset = stateClass.getMethod("reset");
        blur.invoke(state);
        boolean rejected = false;
        try {
            blur.invoke(state);
        } catch (InvocationTargetException expected) {
            rejected = expected.getCause() instanceof IllegalStateException
                    && BLUR_ERROR.equals(expected.getCause().getMessage());
        }
        check(rejected, "A second blur must reproduce the user's exact Minecraft exception");

        for (int frame = 0; frame < 128; frame++) {
            reset.invoke(state);
            blur.invoke(state);
            check(true, "One background remains valid after resetting frame " + frame);
        }
        System.out.println("PASS: " + assertions
                + " actual Minecraft background-order, double-blur guard and frame-reset assertions.");
    }

    private static ClassModel read(String internalName) throws Exception {
        try (InputStream input = GuiBackgroundTest.class.getClassLoader()
                .getResourceAsStream(internalName + ".class")) {
            if (input == null) throw new AssertionError("Class missing: " + internalName);
            return ClassFile.of().parse(input.readAllBytes());
        }
    }

    private static MethodModel method(ClassModel owner, String name) {
        return owner.methods().stream().filter(method -> method.methodName().equalsString(name))
                .findFirst().orElseThrow(() -> new AssertionError("Method missing: " + name));
    }

    private static List<String> calls(MethodModel method) {
        List<String> result = new ArrayList<>();
        for (var element : method.code().orElseThrow()) {
            if (element instanceof InvokeInstruction call)
                result.add(call.owner().asInternalName() + "#" + call.name().stringValue());
        }
        return result;
    }

    private static long count(List<String> calls, String name) {
        return calls.stream().filter(call -> call.endsWith("#" + name)).count();
    }

    private static int index(List<String> calls, String name) {
        for (int i = 0; i < calls.size(); i++)
            if (calls.get(i).endsWith("#" + name)) return i;
        return -1;
    }

    private static void check(boolean valid, String message) {
        assertions++;
        if (!valid) throw new AssertionError(message);
    }
}
