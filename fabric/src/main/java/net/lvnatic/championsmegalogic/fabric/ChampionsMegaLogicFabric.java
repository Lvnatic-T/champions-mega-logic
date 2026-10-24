package net.lvnatic.championsmegalogic.fabric;

import com.cobblemon.mod.common.Cobblemon;
import com.cobblemon.mod.common.battles.runner.graal.GraalShowdownService;
import kotlin.Unit;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.lvnatic.championsmegalogic.ChampionsMegaLogic;

public final class ChampionsMegaLogicFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        ChampionsMegaLogic.init();

        ServerLifecycleEvents.SERVER_STARTED.register(server -> injectShowdownPatch());
        ServerLifecycleEvents.END_DATA_PACK_RELOAD.register((server, manager, success) -> {
            if (success) injectShowdownPatch();
        });
    }

    private static void injectShowdownPatch() {
        Cobblemon.INSTANCE.getShowdownThread().queue(showdownService -> {
            if (showdownService instanceof GraalShowdownService service) {
                try {
                    var injector = service.context.getBindings("js").getMember("receiveScriptData");

                    if (injector != null && injector.canExecute()) {
                        injector.execute("pokemon", ChampionsMegaLogic.showdownScript());
                        ChampionsMegaLogic.LOGGER.info("Installed Showdown form persistence patch");
                    } else {
                        ChampionsMegaLogic.LOGGER.warn("Showdown does not expose receiveScriptData");
                    }
                } catch (Exception e) {
                    ChampionsMegaLogic.LOGGER.error("Failed to inject Showdown form persistence patch", e);
                }
            } else {
                ChampionsMegaLogic.LOGGER.warn("Unsupported Showdown engine; form persistence patch not installed");
            }

            return Unit.INSTANCE;
        });
    }
}