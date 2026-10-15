package dev.nodelll.displaymodeloptimizer.compat;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import com.terraformersmc.modmenu.api.UpdateChecker;
import dev.nodelll.displaymodeloptimizer.gui.DisplayModelOptimizerConfigScreen;

public final class DisplayModelOptimizerModMenu implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return DisplayModelOptimizerConfigScreen::new;
    }

    @Override
    public UpdateChecker getUpdateChecker() {
        return new GitHubUpdateChecker();
    }
}
