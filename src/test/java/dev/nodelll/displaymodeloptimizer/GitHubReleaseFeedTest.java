package dev.nodelll.displaymodeloptimizer;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import dev.nodelll.displaymodeloptimizer.update.GitHubReleaseFeed;
import dev.nodelll.displaymodeloptimizer.update.GitHubReleaseFeed.Channel;
import java.util.Optional;

/** No network: release payloads must not advertise incompatible updates or downgrades. */
public final class GitHubReleaseFeedTest {
    private static int assertions;

    public static void main(String[] args) {
        JsonObject beta = release("0.7.0", "26.3", true);
        check(find("0.6.0+mc26.3", Channel.BETA, beta).orElseThrow().version().equals("0.7.0"), "Compatible beta is an update");
        check(find("0.7.0+mc26.3", Channel.BETA, beta).isEmpty(), "Equal version is not an update");
        check(find("0.8.0+mc26.3", Channel.BETA, beta).isEmpty(), "Never advertise a downgrade");
        check(find("0.6.0", Channel.RELEASE, beta).isEmpty(), "Stable preference rejects beta");
        check(find("0.6.0", Channel.RELEASE, release("0.7.0", "26.3", false)).isPresent(), "Stable preference accepts stable");
        check(find("0.6.0", Channel.BETA, release("0.7.0", "26.3", false)).isPresent(), "Beta preference also accepts stable");
        check(find("0.6.0", Channel.BETA, release("0.8.0-alpha.1", "26.3", true)).isEmpty(), "Beta preference rejects alpha");
        check(find("0.6.0", Channel.ALPHA, release("0.8.0-alpha.1", "26.3", true)).orElseThrow().channel() == Channel.ALPHA, "Alpha preference accepts alpha");
        check(find("0.6.0", Channel.BETA, release("0.8.0-beta.1", "26.3", true)).orElseThrow().channel() == Channel.BETA, "Semantic beta channel");
        check(find("0.6.0", Channel.BETA, release("0.8.0-rc.1", "26.3", true)).orElseThrow().channel() == Channel.BETA, "Release candidate channel");
        check(find("0.6.0", Channel.BETA, release("0.8.0-dev.1", "26.3", true)).isEmpty(), "Unknown prerelease is treated as alpha");
        check(find("0.6.0", Channel.RELEASE, release("0.8.0-alpha.1", "26.3", false)).isEmpty(), "Prerelease tag cannot become stable through metadata");
        check(find("0.7.0-rc.1+mc26.3", Channel.RELEASE, release("0.7.0", "26.3", false)).isPresent(), "Stable replaces same-core release candidate");
        check(find("0.7.0", Channel.ALPHA, release("0.7.0-beta.1", "26.3", true)).isEmpty(), "Stable cannot downgrade to same-core beta");
        check(find("1.0.0", Channel.RELEASE, release("1.9.9", "26.3", false), release("1.10.0", "26.3", false)).orElseThrow().version().equals("1.10.0"), "Versions compare numerically");
        check(find("0.6.0", Channel.BETA, release("0.9.0", "26.3", true), beta, release("0.8.0", "26.3", true)).orElseThrow().version().equals("0.9.0"), "Newest release selected independently of API order");
        for (String mc : new String[]{"26.4", "26.30", "26.3.1"})
            check(find("0.6.0", Channel.ALPHA, release("9.0.0", mc, false)).isEmpty(), "Reject different Minecraft version " + mc);
        JsonObject draft = release("9.0.0", "26.3", false);
        draft.addProperty("draft", true);
        check(find("0.6.0", Channel.ALPHA, draft).isEmpty(), "Reject draft release");
        JsonObject pending = release("9.0.0", "26.3", false);
        pending.getAsJsonArray("assets").get(0).getAsJsonObject().addProperty("state", "new");
        check(find("0.6.0", Channel.ALPHA, pending).isEmpty(), "Reject incomplete binary upload");
        JsonObject sources = release("9.0.0", "26.3", false);
        sources.getAsJsonArray("assets").get(0).getAsJsonObject().addProperty("name", "display-model-optimizer-9.0.0+mc26.3-sources.jar");
        check(find("0.6.0", Channel.ALPHA, sources).isEmpty(), "Reject source archive");
        JsonObject otherTag = release("9.0.0", "26.3", false);
        otherTag.addProperty("tag_name", "v9.0.1");
        otherTag.addProperty("html_url", ProjectLinks.RELEASES + "/tag/v9.0.1");
        check(find("0.6.0", Channel.ALPHA, otherTag).isEmpty(), "Tag must match binary version");
        JsonObject external = release("9.0.0", "26.3", false);
        for (String page : new String[]{"https://example.com/releases/tag/v9.0.0", ProjectLinks.RELEASES + "/tag/v9.0.0?redirect=elsewhere"}) {
            external.addProperty("html_url", page);
            check(find("0.6.0", Channel.ALPHA, external).isEmpty(), "Only the exact project release URL is returned");
        }
        check(find("0.6.0", Channel.ALPHA, release("not-a-version", "26.3", true)).isEmpty(), "Reject unknown target version");
        check(find("not-a-version", Channel.ALPHA, beta).isEmpty(), "Unknown installed version cannot prove an update");
        JsonObject noAssets = release("9.0.0", "26.3", false);
        noAssets.add("assets", new JsonArray());
        check(find("0.6.0", Channel.ALPHA, noAssets).isEmpty(), "Reject release without binary");
        noAssets.remove("draft");
        check(find("0.6.0", Channel.ALPHA, noAssets, beta).orElseThrow().version().equals("0.7.0"), "Malformed release does not hide a valid one");
        check(GitHubReleaseFeed.findUpdate("[]", "0.6.0", "26.3", Channel.BETA).isEmpty(), "Empty feed is harmless");
        check(GitHubReleaseFeed.findUpdate("{\"message\":\"API rate limit exceeded\"}", "0.6.0", "26.3", Channel.BETA).isEmpty(), "API error is not an update");
        check(GitHubReleaseFeed.findUpdate("[null,3,{}]", "0.6.0", "26.3", Channel.BETA).isEmpty(), "Unexpected entries ignored");
        try {
            GitHubReleaseFeed.findUpdate("{invalid", "0.6.0", "26.3", Channel.BETA);
            throw new AssertionError("Malformed JSON accepted");
        } catch (JsonParseException expected) { assertions++; }
        System.out.println("PASS: " + assertions + " update version, Minecraft compatibility, release channel and malformed-feed assertions.");
    }

    private static Optional<GitHubReleaseFeed.Release> find(String current, Channel preference, JsonElement... releases) {
        JsonArray array = new JsonArray();
        for (JsonElement release : releases) array.add(release);
        return GitHubReleaseFeed.findUpdate(array.toString(), current, "26.3", preference);
    }

    private static JsonObject release(String version, String minecraft, boolean prerelease) {
        JsonObject result = new JsonObject();
        result.addProperty("draft", false);
        result.addProperty("prerelease", prerelease);
        result.addProperty("tag_name", "v" + version);
        result.addProperty("html_url", ProjectLinks.RELEASES + "/tag/v" + version);
        JsonObject asset = new JsonObject();
        asset.addProperty("name", "display-model-optimizer-" + version + "+mc" + minecraft + ".jar");
        asset.addProperty("state", "uploaded");
        JsonArray assets = new JsonArray();
        assets.add(asset);
        result.add("assets", assets);
        return result;
    }

    private static void check(boolean result, String message) {
        assertions++;
        if (!result) throw new AssertionError(message);
    }
}
