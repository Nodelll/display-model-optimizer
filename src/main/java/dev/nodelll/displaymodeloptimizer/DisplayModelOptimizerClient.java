package dev.nodelll.displaymodeloptimizer;

import net.fabricmc.api.ClientModInitializer;

public final class DisplayModelOptimizerClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        OptimizerSettings.logStartup();
    }
}
