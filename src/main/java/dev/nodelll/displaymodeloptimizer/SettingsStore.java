package dev.nodelll.displaymodeloptimizer;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Properties;

public final class SettingsStore {
    private SettingsStore() {}

    public static SettingsValues load(Path file) throws IOException {
        boolean existing = Files.exists(file);
        Properties stored = existing ? read(file) : findCompatibleSettings(file);
        SettingsValues settings = SettingsValues.from(stored);
        if (!existing || !stored.keySet().containsAll(SettingsValues.KEYS)) write(file, settings, stored);
        return settings;
    }

    public static void save(Path file, SettingsValues settings) throws IOException {
        write(file, settings, read(file));
    }

    private static Properties findCompatibleSettings(Path file) throws IOException {
        Path parent = file.toAbsolutePath().getParent();
        if (!Files.isDirectory(parent)) return new Properties();
        Properties match = null;
        try (var paths = Files.list(parent)) {
            for (Path candidate : paths.filter(path -> path.getFileName().toString().endsWith(".properties")).toList()) {
                if (!Files.isRegularFile(candidate, LinkOption.NOFOLLOW_LINKS)) continue;
                Properties values;
                try { values = read(candidate); }
                catch (IOException ignored) { continue; }
                if (!values.keySet().containsAll(SettingsValues.KEYS)) continue;
                // A complete, unique key set identifies a previous configuration without depending on its filename.
                if (match != null) return new Properties();
                match = values;
            }
        }
        return match == null ? new Properties() : match;
    }

    private static void write(Path file, SettingsValues settings, Properties stored) throws IOException {
        settings.writeTo(stored);
        Path target = file.toAbsolutePath();
        Files.createDirectories(target.getParent());
        Path temporary = Files.createTempFile(target.getParent(), "display-model-optimizer-", ".tmp");
        try {
            try (Writer writer = Files.newBufferedWriter(temporary, StandardCharsets.UTF_8)) {
                stored.store(writer, "Display Model Optimizer by Nodelll. GUI changes apply after Save.");
            }
            try {
                Files.move(temporary, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(temporary);
        }
    }

    private static Properties read(Path file) throws IOException {
        Properties stored = new Properties();
        if (Files.exists(file)) {
            try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
                stored.load(reader);
            } catch (IllegalArgumentException e) {
                throw new IOException("Invalid Display Model Optimizer configuration", e);
            }
        }
        return stored;
    }
}
