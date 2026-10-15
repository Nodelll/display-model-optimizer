package dev.nodelll.displaymodeloptimizer.gui;

import dev.nodelll.displaymodeloptimizer.SettingsValues;
import java.util.EnumMap;

/** Edits stay local until the user saves; an invalid draft cannot become runtime settings. */
public final class SettingsDraft {
    public enum NumberOption {
        DISTANCE("distance", 1024), MODEL_LIMIT("model_limit", 16384),
        MINIMUM_SIZE("minimum_size", 65536), VERIFY_EVERY("verify_every", 1048576);
        public final String key;
        public final int maximum;
        NumberOption(String key, int maximum) { this.key = "display_model_optimizer.option." + key; this.maximum = maximum; }
    }

    private final EnumMap<NumberOption, String> numbers = new EnumMap<>(NumberOption.class);
    private boolean enabled, cacheModels, occlusion, offscreen, cacheGeometry;

    public SettingsDraft(SettingsValues values) { restore(values); }
    public boolean enabled() { return enabled; }
    public void enabled(boolean value) { enabled = value; }
    public boolean cacheModels() { return cacheModels; }
    public void cacheModels(boolean value) { cacheModels = value; }
    public boolean cacheGeometry() { return cacheGeometry; }
    public void cacheGeometry(boolean value) { cacheGeometry = value; }
    public boolean occlusion() { return occlusion; }
    public void occlusion(boolean value) { occlusion = value; }
    public boolean offscreen() { return offscreen; }
    public void offscreen(boolean value) { offscreen = value; }
    public String text(NumberOption option) { return numbers.get(option); }
    public void text(NumberOption option, String text) { numbers.put(option, text); }
    public void reset() { restore(SettingsValues.DEFAULTS); }

    private void restore(SettingsValues values) {
        enabled = values.enabled();
        cacheModels = values.cacheBlockDisplayModels();
        cacheGeometry = values.cacheBlockDisplayGeometry();
        occlusion = values.cullOccludedDisplays();
        offscreen = values.cullOffscreenDisplays();
        numbers.put(NumberOption.DISTANCE, Integer.toString(values.displayRenderDistance()));
        numbers.put(NumberOption.MODEL_LIMIT, Integer.toString(values.modelCacheLimit()));
        numbers.put(NumberOption.MINIMUM_SIZE, Integer.toString(values.minimumListSize()));
        numbers.put(NumberOption.VERIFY_EVERY, Integer.toString(values.verifyEvery()));
    }

    public boolean valid(NumberOption option) {
        String input = text(option);
        if (input == null || !input.matches("[0-9]{1,7}")) return false;
        return Integer.parseInt(input) <= option.maximum;
    }

    public boolean isValid() {
        for (NumberOption option : NumberOption.values()) if (!valid(option)) return false;
        return true;
    }

    public SettingsValues values() {
        if (!isValid()) throw new IllegalStateException("Invalid settings draft");
        return new SettingsValues(enabled, number(NumberOption.MINIMUM_SIZE), number(NumberOption.VERIFY_EVERY),
                cacheModels, number(NumberOption.MODEL_LIMIT), number(NumberOption.DISTANCE), occlusion, offscreen, cacheGeometry);
    }

    private int number(NumberOption option) { return Integer.parseInt(text(option)); }
}
