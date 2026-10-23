package net.lvnatic.megarevivefix;

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

import java.util.IdentityHashMap;
import java.util.Map;

public final class MegaReviveFix {
    public static final String MOD_ID = "champions_mega_logic";
    private static final String REVERT_TAG = "battle_end_revert";
    private static final String MEGA_REVERT = "mega_evolution=none";
    private static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    private static final Map<Pokemon, ListTag> PENDING = new IdentityHashMap<>();
    private static boolean registered;

    private MegaReviveFix() {
    }

    public static void init() {
        if (registered) {
            return;
        }
        registered = true;
        CobblemonEvents.BATTLE_FAINTED.subscribe(Priority.HIGHEST, MegaReviveFix::beforeFaint);
        CobblemonEvents.BATTLE_FAINTED.subscribe(Priority.LOWEST, MegaReviveFix::afterFaint);
        LOGGER.info("Mega reversion fix initialized");
    }

    private static void beforeFaint(BattleFaintedEvent event) {
        Pokemon pokemon = event.getKilled().getEffectedPokemon();
        CompoundTag persistentData = pokemon.getPersistentData();
        Tag original = persistentData.get(REVERT_TAG);
        if (!(original instanceof ListTag entries)) {
            return;
        }

        ListTag megaEntries = new ListTag();
        ListTag otherEntries = new ListTag();

        for (Tag entry : entries) {
            if (entry instanceof CompoundTag compound && isMegaRevert(compound)) {
                megaEntries.add(entry.copy());
            } else {
                otherEntries.add(entry.copy());
            }
        }

        if (megaEntries.isEmpty()) {
            return;
        }

        PENDING.put(pokemon, megaEntries);
        if (otherEntries.isEmpty()) {
            persistentData.remove(REVERT_TAG);
        } else {
            persistentData.put(REVERT_TAG, otherEntries);
        }
    }

    private static void afterFaint(BattleFaintedEvent event) {
        Pokemon pokemon = event.getKilled().getEffectedPokemon();
        ListTag megaEntries = PENDING.remove(pokemon);
        if (megaEntries == null) {
            return;
        }

        CompoundTag persistentData = pokemon.getPersistentData();
        ListTag remaining = new ListTag();
        Tag current = persistentData.get(REVERT_TAG);
        if (current instanceof ListTag currentEntries) {
            for (Tag entry : currentEntries) {
                remaining.add(entry.copy());
            }
        }

        for (Tag entry : megaEntries) {
            remaining.add(entry.copy());
        }

        persistentData.put(REVERT_TAG, remaining);
        pokemon.onChange(null);
    }

    private static boolean isMegaRevert(CompoundTag effectPair) {
        Tag rawAspects = effectPair.get("aspects");
        if (!(rawAspects instanceof ListTag aspects)) {
            return false;
        }

        for (Tag aspect : aspects) {
            if (aspect instanceof StringTag value && MEGA_REVERT.equals(value.getAsString())) {
                return true;
            }
        }
        return false;
    }
}
