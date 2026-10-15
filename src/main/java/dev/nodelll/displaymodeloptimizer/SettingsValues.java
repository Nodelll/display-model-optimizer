package dev.nodelll.displaymodeloptimizer;

import java.util.Properties;
import java.util.Set;

public record SettingsValues(boolean enabled, int minimumListSize, int verifyEvery,
                             boolean cacheBlockDisplayModels, int modelCacheLimit, int displayRenderDistance,
                             boolean cullOccludedDisplays) {
    public static final SettingsValues DEFAULTS = new SettingsValues(true, 32, 256, true, 1024, 18, true);
    public static final Set<String> KEYS = Set.of("enabled", "minimumListSize", "verifyEvery",
            "cacheBlockDisplayModels", "modelCacheLimit", "displayRenderDistance", "cullOccludedDisplays");

    public SettingsValues(boolean enabled, int minimumListSize, int verifyEvery,
                          boolean cacheBlockDisplayModels, int modelCacheLimit, int displayRenderDistance) {
        this(enabled, minimumListSize, verifyEvery, cacheBlockDisplayModels, modelCacheLimit, displayRenderDistance, true);
    }

    public SettingsValues {
        minimumListSize = clamp(minimumListSize, 65536);
        verifyEvery = clamp(verifyEvery, 1048576);
        modelCacheLimit = clamp(modelCacheLimit, 16384);
        displayRenderDistance = clamp(displayRenderDistance, 1024);
    }

    public static SettingsValues from(Properties values) {
        return new SettingsValues(bool(values, "enabled", true), integer(values, "minimumListSize", 32),
                integer(values, "verifyEvery", 256), bool(values, "cacheBlockDisplayModels", true),
                integer(values, "modelCacheLimit", 1024), integer(values, "displayRenderDistance", 18),
                bool(values, "cullOccludedDisplays", true));
    }

    public void writeTo(Properties values) {
        values.setProperty("enabled", Boolean.toString(enabled));
        values.setProperty("minimumListSize", Integer.toString(minimumListSize));
        values.setProperty("verifyEvery", Integer.toString(verifyEvery));
        values.setProperty("cacheBlockDisplayModels", Boolean.toString(cacheBlockDisplayModels));
        values.setProperty("modelCacheLimit", Integer.toString(modelCacheLimit));
        values.setProperty("displayRenderDistance", Integer.toString(displayRenderDistance));
        values.setProperty("cullOccludedDisplays", Boolean.toString(cullOccludedDisplays));
    }

    private static int integer(Properties values, String key, int fallback) {
        try { return Integer.parseInt(values.getProperty(key, Integer.toString(fallback)).trim()); }
        catch (NumberFormatException e) { return fallback; }
    }

    private static boolean bool(Properties values, String key, boolean fallback) {
        String value = values.getProperty(key, Boolean.toString(fallback)).trim();
        return value.equalsIgnoreCase("true") || (!value.equalsIgnoreCase("false") && fallback);
    }

    private static int clamp(int value, int maximum) {
        return Math.max(0, Math.min(maximum, value));
    }
}
