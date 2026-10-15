package dev.nodelll.displaymodeloptimizer.compat;

import com.terraformersmc.modmenu.api.UpdateChannel;
import com.terraformersmc.modmenu.api.UpdateChecker;
import com.terraformersmc.modmenu.api.UpdateInfo;
import dev.nodelll.displaymodeloptimizer.ProjectLinks;
import dev.nodelll.displaymodeloptimizer.update.GitHubReleaseFeed;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.network.chat.Component;

/** Invoked only by Mod Menu, on its update-checking worker and with its user preferences. */
public final class GitHubUpdateChecker implements UpdateChecker {
    private static final URI FEED = URI.create("https://api.github.com/repos/Nodelll/display-model-optimizer/releases?per_page=100");
    private static final int MAX_RESPONSE = 2 * 1024 * 1024;

    @Override
    public UpdateInfo checkForUpdates() {
        if (Thread.currentThread().isInterrupted()) return null;
        try {
            var loader = FabricLoader.getInstance();
            String current = loader.getModContainer(ProjectLinks.MOD_ID).orElseThrow().getMetadata().getVersion().getFriendlyString();
            String minecraft = loader.getModContainer("minecraft").orElseThrow().getMetadata().getVersion().getFriendlyString();
            var preference = GitHubReleaseFeed.Channel.valueOf(UpdateChannel.getUserPreference().name());
            return GitHubReleaseFeed.findUpdate(fetch(current), current, minecraft, preference)
                    .<UpdateInfo>map(UpdateNotice::new).orElse(null);
        } catch (IOException | RuntimeException e) {
            System.getLogger("Display Model Optimizer").log(System.Logger.Level.DEBUG,
                    "[Display Model Optimizer] Update check unavailable: " + e.getClass().getSimpleName());
            return null;
        }
    }

    private static String fetch(String current) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) FEED.toURL().openConnection();
        connection.setConnectTimeout(5000);
        connection.setReadTimeout(5000);
        connection.setInstanceFollowRedirects(false);
        connection.setUseCaches(false);
        connection.setRequestProperty("Accept", "application/vnd.github+json");
        connection.setRequestProperty("User-Agent", "DisplayModelOptimizer/" + current + " (" + ProjectLinks.HOME + ")");
        try {
            if (connection.getResponseCode() != 200) throw new IOException("Release feed unavailable");
            try (var input = connection.getInputStream()) {
                byte[] bytes = input.readNBytes(MAX_RESPONSE + 1);
                if (bytes.length > MAX_RESPONSE) throw new IOException("Release feed too large");
                return new String(bytes, StandardCharsets.UTF_8);
            }
        } finally {
            connection.disconnect();
        }
    }

    private record UpdateNotice(GitHubReleaseFeed.Release release) implements UpdateInfo {
        @Override public boolean isUpdateAvailable() { return true; }
        @Override public String getDownloadLink() { return release.pageUrl(); }
        @Override public UpdateChannel getUpdateChannel() { return UpdateChannel.valueOf(release.channel().name()); }
        @Override public Component getUpdateMessage() {
            return Component.translatable("display_model_optimizer.update.available", release.version());
        }
    }
}
