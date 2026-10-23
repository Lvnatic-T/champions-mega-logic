package net.lvnatic.megarevivefix.fabric;

import net.fabricmc.api.ModInitializer;
import net.lvnatic.megarevivefix.MegaReviveFix;

public final class MegaReviveFixFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        MegaReviveFix.init();
    }
}
