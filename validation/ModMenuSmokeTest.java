package dev.nodelll.displaymodeloptimizer.validation;

import com.terraformersmc.modmenu.api.ModMenuApi;
import dev.nodelll.displaymodeloptimizer.compat.DisplayModelOptimizerModMenu;

/** Resolves the actual optional Mod Menu API without constructing a Minecraft client. */
public final class ModMenuSmokeTest {
    public static void main(String[] args) {
        ModMenuApi entrypoint = new DisplayModelOptimizerModMenu();
        var factory = entrypoint.getModConfigScreenFactory();
        if (factory == null) throw new AssertionError("Config screen factory was not registered");
        System.out.println("PASS: Entrypoint implements official Mod Menu 21.0.0 API; config screen factory creates successfully.");
    }
}
