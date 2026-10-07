package vanguard.sim;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static vanguard.sim.TestSpecs.FULL_ARMOUR;
import static vanguard.sim.TestSpecs.LOADOUT;
import static vanguard.sim.TestSpecs.RULES;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * The hit that spends a ground trigger raises {@link SimEvents.Type#TRIGGER_SPENT} at it, with its
 * index in the script, for each trigger (also those that only count towards a shared secret), in
 * the step its secret is found if it is the last; the presentation breaks a toppling one with it.
 */
class TriggerSpentTest {
    private static LevelScript.GroundObjectSpec light(double t, int triggers) {
        return new LevelScript.GroundObjectSpec(
                t,
                Ship.START_X,
                new Hitbox(12, 12),
                0,
                0,
                Optional.empty(),
                triggers == 1 ? 3 : 1,
                75,
                "cache",
                false,
                0,
                triggers);
    }

    private static Sortie sortie(List<LevelScript.GroundObjectSpec> ground) {
        var script = new LevelScript(
                1,
                1,
                0,
                List.of(new LevelScript.Section(20, 130)),
                List.of(),
                ground,
                List.of(),
                1,
                List.of(),
                new LevelScript.Secondary(0.8, 50),
                List.of());
        return new Sortie(1, LOADOUT, script, RULES, FULL_ARMOUR);
    }

    /** Fires for {@code seconds}; per step with a trigger spent: the step, its value, whether a secret was found then. */
    private static List<int[]> spent(Sortie sortie, double seconds) {
        List<int[]> spent = new ArrayList<>();
        for (int i = 0; i < SimStep.ticks(seconds); i++) {
            sortie.step(Command.FIRE.bit());
            SimEvents events = sortie.events();
            for (int e = 0; e < events.size(); e++) {
                if (events.type(e) == SimEvents.Type.TRIGGER_SPENT) {
                    spent.add(new int[] {i, events.value(e), events.count(SimEvents.Type.SECRET_FOUND)});
                }
            }
        }
        return spent;
    }

    @Test
    void theThirdHitSpendsATriggerAndFindsItsSecretInTheSameStep() {
        var sortie = sortie(List.of(light(0, 1)));

        List<int[]> spent = spent(sortie, 2);

        assertEquals(1, spent.size());
        assertEquals(0, spent.get(0)[1], "its index in the script");
        assertEquals(1, spent.get(0)[2], "the secret found with it");
    }

    @Test
    void eachTriggerOfASharedSecretIsSpentTheLastFindsIt() {
        var sortie = sortie(List.of(light(0, 2), light(1, 2)));

        List<int[]> spent = spent(sortie, 3);

        assertEquals(2, spent.size());
        assertEquals(0, spent.get(0)[1]);
        assertEquals(0, spent.get(0)[2], "the first light alone finds nothing");
        assertEquals(1, spent.get(1)[1]);
        assertEquals(1, spent.get(1)[2]);
    }
}
