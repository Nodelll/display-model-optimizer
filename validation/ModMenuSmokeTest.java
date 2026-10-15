package dev.nodelll.displaymodeloptimizer.validation;

import com.terraformersmc.modmenu.api.ModMenuApi;
import dev.nodelll.displaymodeloptimizer.compat.DisplayModelOptimizerModMenu;

/** Resolves the actual optional Mod Menu API without constructing a Minecraft client. */
public final class ModMenuSmokeTest {
    public static void main(String[] args) {
        ModMenuApi entrypoint = new DisplayModelOptimizerModMenu();
        var factory = entrypoint.getModConfigScreenFactory();
        if (factory == null) throw new AssertionError("Config screen factory was not registered");
        if (entrypoint.getUpdateChecker() == null) throw new AssertionError("Update checker was not registered");
        if (!entrypoint.getProvidedConfigScreenFactories().isEmpty() || !entrypoint.getProvidedUpdateCheckers().isEmpty())
            throw new AssertionError("Integration must not override other mods");
        entrypoint.attachModpackBadges(id -> { throw new AssertionError("Standalone mod must not claim modpack ownership"); });
        System.out.println("PASS: Official Mod Menu 21.0.0 API; config factory and update checker register without starting a client or network request.");
    }
}
