package dev.nodelll.displaymodeloptimizer.update;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.nodelll.displaymodeloptimizer.ProjectLinks;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;
import net.fabricmc.loader.api.SemanticVersion;
import net.fabricmc.loader.api.Version;
import net.fabricmc.loader.api.VersionParsingException;

/** Selects compatible published binaries, independently of feed ordering and Minecraft UI. */
public final class GitHubReleaseFeed {
    public enum Channel { ALPHA, BETA, RELEASE }
    public record Release(String version, String pageUrl, Channel channel) {}

    private GitHubReleaseFeed() {}

    public static Optional<Release> findUpdate(String json, String installed, String minecraft, Channel preference) {
        final SemanticVersion current;
        try { current = SemanticVersion.parse(installed); }
        catch (VersionParsingException e) { return Optional.empty(); }
        JsonElement root = JsonParser.parseString(json);
        if (!root.isJsonArray()) return Optional.empty();
        Pattern filename = Pattern.compile("^display-model-optimizer-(.+)\\+mc" + Pattern.quote(minecraft) + "\\.jar$");
        Release best = null;
        Version bestVersion = current;
        for (JsonElement element : root.getAsJsonArray()) {
            if (!element.isJsonObject()) continue;
            JsonObject release = element.getAsJsonObject();
            if (!isBoolean(release, "draft") || release.get("draft").getAsBoolean()
                    || !isBoolean(release, "prerelease") || !release.has("assets") || !release.get("assets").isJsonArray()) continue;
            String tag = string(release, "tag_name");
            String page = string(release, "html_url");
            if (!page.equals(ProjectLinks.RELEASES + "/tag/" + tag)) continue;
            for (JsonElement assetElement : release.getAsJsonArray("assets")) {
                if (!assetElement.isJsonObject()) continue;
                JsonObject asset = assetElement.getAsJsonObject();
                if (!"uploaded".equals(string(asset, "state"))) continue;
                var match = filename.matcher(string(asset, "name"));
                if (!match.matches()) continue;
                String version = match.group(1);
                if (!tag.equals("v" + version) && !tag.equals(version)
                        && !tag.equals("v" + version + "+mc" + minecraft) && !tag.equals(version + "+mc" + minecraft)) continue;
                try {
                    SemanticVersion candidate = SemanticVersion.parse(version);
                    Channel channel = channel(candidate, release.get("prerelease").getAsBoolean());
                    if (channel.ordinal() < preference.ordinal() || candidate.compareTo(bestVersion) <= 0) continue;
                    bestVersion = candidate;
                    best = new Release(version, page, channel);
                } catch (VersionParsingException ignored) {
                    // Unknown version schemes cannot safely establish that a release is newer.
                }
            }
        }
        return Optional.ofNullable(best);
    }

    private static Channel channel(SemanticVersion version, boolean prerelease) {
        String key = version.getPrereleaseKey().orElse("").toLowerCase(Locale.ROOT);
        if (key.isEmpty()) return prerelease ? Channel.BETA : Channel.RELEASE;
        return key.startsWith("beta") || key.startsWith("rc") ? Channel.BETA : Channel.ALPHA;
    }

    private static String string(JsonObject object, String name) {
        JsonElement value = object.get(name);
        return value != null && value.isJsonPrimitive() && value.getAsJsonPrimitive().isString() ? value.getAsString() : "";
    }

    private static boolean isBoolean(JsonObject object, String name) {
        JsonElement value = object.get(name);
        return value != null && value.isJsonPrimitive() && value.getAsJsonPrimitive().isBoolean();
    }
}
