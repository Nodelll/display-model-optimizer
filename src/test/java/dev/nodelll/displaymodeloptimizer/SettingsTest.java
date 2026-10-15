package dev.nodelll.displaymodeloptimizer;

import dev.nodelll.displaymodeloptimizer.gui.SettingsDraft;
import dev.nodelll.displaymodeloptimizer.gui.SettingsDraft.NumberOption;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public final class SettingsTest {
    private static int assertions;

    public static void main(String[] args) throws Exception {
        Path folder = Files.createTempDirectory("display-model-optimizer-settings-test-");
        try {
            Path file = folder.resolve("display-model-optimizer.properties");
            check(SettingsStore.load(file).equals(SettingsValues.DEFAULTS), "First launch creates defaults");
            check(Files.isRegularFile(file), "Settings file exists");
            Files.writeString(file, "enabled=false\nminimumListSize=64\nverifyEvery=0\ncustomNote=Сохранить ё\n");
            SettingsValues migrated = SettingsStore.load(file);
            check(!migrated.enabled() && migrated.minimumListSize() == 64 && migrated.verifyEvery() == 0, "Old preferences retained");
            check(migrated.cacheBlockDisplayModels() && migrated.displayRenderDistance() == 18, "New defaults appended");
            check(migrated.cullOffscreenDisplays(), "Off-screen culling defaults on for an existing configuration");
            Properties contents = properties(file);
            check(contents.keySet().containsAll(SettingsValues.KEYS), "Migration writes all known keys");
            check("Сохранить ё".equals(contents.getProperty("customNote")), "Unknown UTF-8 property preserved");
            SettingsValues wanted = new SettingsValues(true, 16, 4096, false, 2048, 32);
            SettingsStore.save(file, wanted);
            check(SettingsStore.load(file).equals(wanted), "All six options survive save and reload");
            check("Сохранить ё".equals(properties(file).getProperty("customNote")), "Save retains unknown property");
            try (var paths = Files.list(folder)) {
                check(paths.filter(p -> p.getFileName().toString().endsWith(".tmp")).findAny().isEmpty(), "No temporary file left after save");
            }

            var draft = new SettingsDraft(wanted);
            draft.text(NumberOption.DISTANCE, "48");
            draft.cacheModels(true);
            check(wanted.displayRenderDistance() == 32 && !wanted.cacheBlockDisplayModels(), "Draft cannot alter current settings before save");
            check(draft.values().displayRenderDistance() == 48 && draft.values().cacheBlockDisplayModels(), "Valid draft produces edited settings");
            draft.occlusion(false);
            check(!draft.values().cullOccludedDisplays() && wanted.cullOccludedDisplays(), "Occlusion switch remains a draft until save");
            SettingsStore.save(file, draft.values());
            check(!SettingsStore.load(file).cullOccludedDisplays(), "Occlusion switch survives save and reload");
            draft.offscreen(false);
            check(!draft.values().cullOffscreenDisplays() && wanted.cullOffscreenDisplays(), "Off-screen edits remain local to the draft");
            SettingsStore.save(file, draft.values());
            check(!SettingsStore.load(file).cullOffscreenDisplays(), "Off-screen switch survives save and reload");
            SettingsStore.save(file, wanted);
            for (String invalid : new String[]{"", "-1", "3.2", "x", "9999999", "1025"}) {
                draft.text(NumberOption.DISTANCE, invalid);
                check(!draft.isValid(), "Reject invalid distance: " + invalid);
                try { draft.values(); throw new AssertionError("Invalid draft accepted"); }
                catch (IllegalStateException expected) { assertions++; }
            }
            draft.text(NumberOption.DISTANCE, "0");
            check(draft.isValid(), "Zero distance is valid");
            for (NumberOption option : NumberOption.values()) {
                draft.text(option, Integer.toString(option.maximum));
                check(draft.valid(option), "Maximum boundary is valid");
                draft.text(option, Integer.toString(option.maximum + 1));
                check(!draft.valid(option), "Beyond boundary rejected");
                draft.reset();
            }
            check(draft.values().equals(SettingsValues.DEFAULTS), "Reset affects all draft fields");
            check(draft.values().cullOccludedDisplays(), "Reset restores display occlusion default");
            check(draft.values().cullOffscreenDisplays(), "Reset restores off-screen culling default");
            check(SettingsStore.load(file).equals(wanted), "Reset draft does not save implicitly");

            Properties malformedNumbers = new Properties();
            malformedNumbers.setProperty("minimumListSize", "invalid");
            malformedNumbers.setProperty("modelCacheLimit", "-9");
            malformedNumbers.setProperty("displayRenderDistance", "9999");
            SettingsValues normalized = SettingsValues.from(malformedNumbers);
            check(normalized.minimumListSize() == 32 && normalized.modelCacheLimit() == 0
                    && normalized.displayRenderDistance() == 1024, "Bad manual values fall back or clamp");
            Files.writeString(file, "customNote=\\uNOTX\n");
            byte[] before = Files.readAllBytes(file);
            try { SettingsStore.save(file, wanted); throw new AssertionError("Malformed file overwritten"); }
            catch (IOException expected) { assertions++; }
            check(java.util.Arrays.equals(before, Files.readAllBytes(file)), "Failed save leaves original bytes intact");

            var cache = new FrameModelCache<String>(8);
            Object key = new Object();
            cache.begin();
            cache.put(key, key, key, 42, "old");
            cache.begin(1);
            check(cache.get(key, key, key, 42) == null, "Changed capacity starts a fresh frame");
            cache.put(key, key, key, 42, "new");
            cache.put(new Object(), key, key, 42, "overflow");
            check(cache.size() == 1, "Changed capacity is used without restart");
            cache.begin(0);
            check(!cache.canStore(), "Live zero capacity disables storage");
            var index = new DrawLookupIndex(32, 256, () -> {});
            check(index.usesSettings(32, 256) && !index.usesSettings(64, 256)
                    && !index.usesSettings(32, 0), "Changed lookup options require a new index");
            checkRenamedConfigMigration(folder);
            checkSevenOptionUpgrade(folder);
        } finally {
            try (var paths = Files.walk(folder)) {
                for (Path path : paths.sorted(java.util.Comparator.reverseOrder()).toList()) Files.delete(path);
            }
        }
        System.out.println("PASS: " + assertions + " configuration migration, save, draft validation, cancellation and live option assertions.");
    }

    private static void checkRenamedConfigMigration(Path folder) throws Exception {
        SettingsValues preferences = new SettingsValues(false, 64, 512, false, 2048, 48, false);
        Path single = Files.createDirectory(folder.resolve("single-match"));
        Path source = single.resolve("previous-settings.properties");
        SettingsStore.save(source, preferences);
        Files.writeString(source, "#Previous configuration\n" + Files.readString(source) + "customNote=Перенести ё\n");
        Files.writeString(single.resolve("unrelated.properties"), "enabled=true\nserviceOption=keep\n");
        byte[] sourceBytes = Files.readAllBytes(source);
        Path target = single.resolve("display-model-optimizer.properties");
        check(SettingsStore.load(target).equals(preferences), "Unique complete configuration is transferred to the new filename");
        check(Files.isRegularFile(target), "Transfer creates the newly named configuration");
        check("Перенести ё".equals(properties(target).getProperty("customNote")), "Transfer retains unknown UTF-8 properties");
        check(java.util.Arrays.equals(sourceBytes, Files.readAllBytes(source)), "Transfer leaves original configuration intact");
        check(Files.readString(target).startsWith("#Display Model Optimizer"), "Transferred file uses the current header");
        SettingsStore.save(source, SettingsValues.DEFAULTS);
        check(SettingsStore.load(target).equals(preferences), "Existing destination takes priority over previous files");

        Path ambiguous = Files.createDirectory(folder.resolve("multiple-matches"));
        Path first = ambiguous.resolve("first-settings.properties");
        Path second = ambiguous.resolve("second-settings.properties");
        SettingsStore.save(first, preferences);
        SettingsStore.save(second, new SettingsValues(true, 16, 0, true, 256, 64, false));
        byte[] firstBytes = Files.readAllBytes(first);
        byte[] secondBytes = Files.readAllBytes(second);
        check(SettingsStore.load(ambiguous.resolve("display-model-optimizer.properties")).equals(SettingsValues.DEFAULTS),
                "Ambiguous configurations are never chosen automatically");
        check(java.util.Arrays.equals(firstBytes, Files.readAllBytes(first))
                && java.util.Arrays.equals(secondBytes, Files.readAllBytes(second)), "Ambiguous source files stay intact");

        Path malformed = Files.createDirectory(folder.resolve("malformed-neighbor"));
        Files.writeString(malformed.resolve("unrelated.properties"), "note=\\uNOTX\n");
        SettingsStore.save(malformed.resolve("previous-settings.properties"), preferences);
        check(SettingsStore.load(malformed.resolve("display-model-optimizer.properties")).equals(preferences),
                "Malformed unrelated files do not block a matching configuration");

        Path incomplete = Files.createDirectory(folder.resolve("incomplete-match"));
        Files.writeString(incomplete.resolve("unrelated.properties"), "enabled=false\nminimumListSize=64\nverifyEvery=0\n");
        check(SettingsStore.load(incomplete.resolve("display-model-optimizer.properties")).equals(SettingsValues.DEFAULTS),
                "Partial key sets cannot identify an unrelated configuration for transfer");
    }

    private static Properties properties(Path file) throws IOException {
        var result = new Properties();
        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) { result.load(reader); }
        return result;
    }

    private static void checkSevenOptionUpgrade(Path folder) throws Exception {
        Path oldFolder = Files.createDirectory(folder.resolve("seven-options"));
        Path oldFile = oldFolder.resolve("previous-settings.properties");
        Files.writeString(oldFile, "enabled=true\nminimumListSize=64\nverifyEvery=512\ncacheBlockDisplayModels=false\n"
                + "modelCacheLimit=2048\ndisplayRenderDistance=48\ncullOccludedDisplays=false\ncustomNote=Сохранить ё\n");
        byte[] original = Files.readAllBytes(oldFile);
        Path file = oldFolder.resolve("display-model-optimizer.properties");
        SettingsValues upgraded = SettingsStore.load(file);
        check(upgraded.displayRenderDistance() == 48 && !upgraded.cullOccludedDisplays() && upgraded.cullOffscreenDisplays(),
                "Complete seven-option file transfers preferences and appends off-screen default");
        check(java.util.Arrays.equals(original, Files.readAllBytes(oldFile)), "Seven-option transfer retains original source");
        check("Сохранить ё".equals(properties(file).getProperty("customNote")), "Seven-option transfer preserves unknown UTF-8 data");
        check(properties(file).keySet().containsAll(SettingsValues.KEYS), "Upgraded configuration contains all eight options");
        for (boolean offscreen : new boolean[]{false, true}) for (boolean occlusion : new boolean[]{false, true}) {
            SettingsValues wanted = new SettingsValues(true, 64, 512, false, 2048, 48, occlusion, offscreen);
            SettingsStore.save(file, wanted);
            check(SettingsStore.load(file).equals(wanted), "Independent visibility switches round-trip: " + offscreen + "/" + occlusion);
        }
        Properties stored = properties(file);
        stored.remove("cullOffscreenDisplays");
        try (var writer = Files.newBufferedWriter(file)) { stored.store(writer, "previous version"); }
        check(SettingsStore.load(file).cullOffscreenDisplays(), "Same-name seven-option file receives the new default");
        check("Сохранить ё".equals(properties(file).getProperty("customNote")), "Same-name upgrade preserves custom data");
    }

    private static void check(boolean valid, String message) {
        assertions++;
        if (!valid) throw new AssertionError(message);
    }
}
