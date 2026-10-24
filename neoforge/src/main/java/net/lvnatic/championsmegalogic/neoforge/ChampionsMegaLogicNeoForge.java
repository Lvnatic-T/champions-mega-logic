package net.lvnatic.championsmegalogic.neoforge;

import com.cobblemon.mod.common.Cobblemon;
import com.cobblemon.mod.common.battles.runner.graal.GraalShowdownService;
import kotlin.Unit;
import net.lvnatic.championsmegalogic.ChampionsMegaLogic;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;

@Mod(ChampionsMegaLogic.MOD_ID)
public final class ChampionsMegaLogicNeoForge {
    public ChampionsMegaLogicNeoForge() {
        ChampionsMegaLogic.init();
        NeoForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent
    public void onServerStarted(ServerStartedEvent event) {
        injectShowdownPatch();
    }

    @SubscribeEvent
    public void onDatapackSync(OnDatapackSyncEvent event) {
        if (event.getPlayer() == null) injectShowdownPatch();
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