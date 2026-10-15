package dev.nodelll.displaymodeloptimizer.compat;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import dev.nodelll.displaymodeloptimizer.gui.DisplayModelOptimizerConfigScreen;

public final class DisplayModelOptimizerModMenu implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return DisplayModelOptimizerConfigScreen::new;
    }
}
