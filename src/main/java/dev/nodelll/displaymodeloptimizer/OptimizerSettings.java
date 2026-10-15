package dev.nodelll.displaymodeloptimizer;

import java.io.IOException;
import java.nio.file.Path;
import dev.nodelll.displaymodeloptimizer.culling.DisplayOcclusion;
import net.fabricmc.loader.api.FabricLoader;

public final class OptimizerSettings {
    private static final System.Logger LOG = System.getLogger("Display Model Optimizer");
    private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("display-model-optimizer.properties");
    private static volatile SettingsValues current = load();
    private static volatile boolean disabledForSession;

    private OptimizerSettings() {}

    public static boolean isEnabled() {
        return current.enabled() && !disabledForSession;
    }

    public static SettingsValues current() { return current; }

    public static boolean isDisabledForSession() { return disabledForSession; }

    public static synchronized void save(SettingsValues values) throws IOException {
        SettingsStore.save(FILE, values);
        if (current.enabled() != values.enabled() || current.cullOccludedDisplays() != values.cullOccludedDisplays())
            DisplayOcclusion.invalidateAll();
        current = values;
    }

    public static synchronized void disableAfterMismatch() {
        if (!disabledForSession) {
            disabledForSession = true;
            LOG.log(System.Logger.Level.WARNING,
                    "[Display Model Optimizer] Draw lookup verification failed. Original lookup restored for this session.");
        }
    }

    public static void logStartup() {
        LOG.log(System.Logger.Level.INFO,
                "[Display Model Optimizer] 0.6.0+mc26.3 by Nodelll: " + current);
    }

    private static SettingsValues load() {
        try {
            return SettingsStore.load(FILE);
        } catch (IOException e) {
            LOG.log(System.Logger.Level.WARNING, "[Display Model Optimizer] Cannot load configuration; using defaults.", e);
            return SettingsValues.DEFAULTS;
        }
    }
}
