package dev.nodelll.displaymodeloptimizer.compat;

import net.fabricmc.loader.api.FabricLoader;

public final class EntityCullingCompat {
    private static final boolean AVAILABLE = FabricLoader.getInstance().isModLoaded("entityculling");
    private EntityCullingCompat() {}
    public static boolean available() { return AVAILABLE; }
    public static void showUntilRefreshed(Object entity) {
        if (AVAILABLE) Bridge.show(entity);
    }
    // Optional API references are confined to a class which is not loaded without Entity Culling.
    private static final class Bridge {
        static void show(Object entity) {
            if (entity instanceof dev.tr7zw.entityculling.access.Cullable cullable) cullable.setCulled(false);
        }
    }
}
