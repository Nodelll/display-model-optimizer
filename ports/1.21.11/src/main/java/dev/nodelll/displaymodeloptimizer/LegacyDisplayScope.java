package dev.nodelll.displaymodeloptimizer;

/** Tags queued display commands, then scopes only their draw calls. No entity or world is retained. */
public final class LegacyDisplayScope {
    private static final ThreadLocal<Boolean> SUBMITTING = ThreadLocal.withInitial(() -> false);
    private static final ThreadLocal<Boolean> RENDERING = ThreadLocal.withInitial(() -> false);
    private LegacyDisplayScope() {}
    public interface Command { boolean displayModelOptimizer$isDisplay(); }
    public static boolean submitting() { return SUBMITTING.get(); }
    public static boolean rendering() { return RENDERING.get(); }
    public static boolean submitting(boolean value) { boolean previous = SUBMITTING.get(); SUBMITTING.set(value); return previous; }
    public static boolean rendering(boolean value) { boolean previous = RENDERING.get(); RENDERING.set(value); return previous; }
}
