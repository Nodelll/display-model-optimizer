package dev.nodelll.displaymodeloptimizer;

import java.lang.ref.WeakReference;
import java.util.concurrent.atomic.AtomicLong;

/** No dependency on the optional renderer API; does not keep a world alive. */
public final class GeometryCacheLifecycle {
    private static final AtomicLong GENERATION = new AtomicLong();
    private static WeakReference<Object> world = new WeakReference<>(null);

    private GeometryCacheLifecycle() {}
    public static long generation() { return GENERATION.get(); }
    public static void invalidate() { GENERATION.incrementAndGet(); }

    public static void beginFrame(Object currentWorld) {
        if (world.get() != currentWorld) {
            world = new WeakReference<>(currentWorld);
            invalidate();
        }
    }
}
