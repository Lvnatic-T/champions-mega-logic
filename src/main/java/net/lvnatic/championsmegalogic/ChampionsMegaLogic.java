package net.lvnatic.championsmegalogic;

import com.cobblemon.mod.common.api.Priority;
import com.cobblemon.mod.common.api.events.CobblemonEvents;
import com.cobblemon.mod.common.api.events.battles.BattleFaintedEvent;
import com.cobblemon.mod.common.pokemon.Pokemon;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.IdentityHashMap;
import java.util.Map;

public final class ChampionsMegaLogic {
    public static final String MOD_ID = "champions_mega_logic";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    private static final String REVERT_TAG = "battle_end_revert";
    private static final Map<Pokemon, ListTag> PENDING = new IdentityHashMap<>();
    private static boolean registered;
    private static String showdownScript;

    private ChampionsMegaLogic() {
    }

    public static void init() {
        if (registered) return;
        registered = true;
        CobblemonEvents.BATTLE_FAINTED.subscribe(Priority.HIGHEST, ChampionsMegaLogic::beforeFaint);
        CobblemonEvents.BATTLE_FAINTED.subscribe(Priority.LOWEST, ChampionsMegaLogic::afterFaint);
        LOGGER.info("Champions Mega Logic initialized");
    }

    private static void beforeFaint(BattleFaintedEvent event) {
        Pokemon pokemon = event.getKilled().getEffectedPokemon();
        CompoundTag persistentData = pokemon.getPersistentData();
        Tag original = persistentData.get(REVERT_TAG);
        if (!(original instanceof ListTag entries)) return;

        ListTag protectedEntries = new ListTag();
        ListTag otherEntries = new ListTag();

        for (Tag entry : entries) {
            if (entry instanceof CompoundTag compound && isTemporaryMaxRevert(compound)) {
                otherEntries.add(entry.copy());
            } else {
                protectedEntries.add(entry.copy());
            }
        }

        if (protectedEntries.isEmpty()) return;

        PENDING.put(pokemon, protectedEntries);

        if (otherEntries.isEmpty()) {
            persistentData.remove(REVERT_TAG);
        } else {
            persistentData.put(REVERT_TAG, otherEntries);
        }
    }

    private static void afterFaint(BattleFaintedEvent event) {
        Pokemon pokemon = event.getKilled().getEffectedPokemon();
        ListTag protectedEntries = PENDING.remove(pokemon);
        if (protectedEntries == null) return;

        CompoundTag persistentData = pokemon.getPersistentData();
        ListTag combined = new ListTag();
        Tag current = persistentData.get(REVERT_TAG);

        if (current instanceof ListTag remaining) {
            for (Tag entry : remaining) {
                combined.add(entry.copy());
            }
        }

        for (Tag entry : protectedEntries) {
            combined.add(entry.copy());
        }

        persistentData.put(REVERT_TAG, combined);
        pokemon.onChange(null);
    }

    private static boolean isTemporaryMaxRevert(CompoundTag effectPair) {
        Tag rawAspects = effectPair.get("aspects");
        if (!(rawAspects instanceof ListTag aspects)) return false;

        for (Tag aspect : aspects) {
            if (aspect instanceof StringTag value
                    && "dynamax_form=none".equals(value.getAsString())) {
                return true;
            }
        }

        return false;
    }

    public static String showdownScript() {
        if (showdownScript != null) return showdownScript;

        try (InputStream resource = ChampionsMegaLogic.class.getResourceAsStream(
                "/assets/champions_mega_logic/showdown/form-persistence.js")) {
            if (resource == null) {
                throw new IllegalStateException("Missing embedded Showdown patch");
            }

            showdownScript = new String(resource.readAllBytes(), StandardCharsets.UTF_8);
            return showdownScript;
        } catch (IOException e) {
            throw new IllegalStateException("Unable to read embedded Showdown patch", e);
        }
    }
}