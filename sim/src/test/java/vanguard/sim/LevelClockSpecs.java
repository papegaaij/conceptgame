package vanguard.sim;

import java.util.List;
import java.util.Optional;

/**
 * Test levels for M5 part C's level clock (design/campaign Level 09, user decisions D1, D2, D3, D5
 * and D6 = a): hold zones over ground-target groups, the collapse, wave tags and the new radio cues,
 * built here rather than from Level 09's data.
 */
final class LevelClockSpecs {
    private LevelClockSpecs() {}

    /** A plain ground unit on {@code layer} (a turret-like target fixed to the ground), no gun. */
    static EnemySpec groundUnit(String slug, Layer layer, double hp, Hitbox box, int bounty) {
        return new EnemySpec(
                slug,
                hp,
                box,
                layer,
                15,
                false,
                bounty,
                0,
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                true,
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                false,
                Optional.empty(),
                Optional.empty());
    }

    /** A 56×56 ground target of 64 HP paying 45: Level 09's node without its spawner. */
    static final EnemySpec NODE = groundUnit("node", Layer.GROUND, 64, new Hitbox(56, 56), 45);

    /** A cue of {@code trigger} at {@code t} (0 for an event) requiring the {@code requires} bits. */
    static LevelScript.RadioCue cue(LevelScript.CueTrigger trigger, double t, int requires) {
        return new LevelScript.RadioCue(
                trigger,
                t,
                "",
                "Rook",
                trigger + "@" + t,
                false,
                "neutral",
                "Rook",
                false,
                0,
                Integer.MAX_VALUE,
                requires,
                0);
    }

    /** {@code wave} with the tag {@code tag}. */
    static WaveSpec tagged(WaveSpec wave, String tag) {
        return new WaveSpec(
                wave.t(),
                wave.formation(),
                wave.enemy(),
                wave.count(),
                wave.entry(),
                wave.edge(),
                wave.holdSeconds(),
                wave.warningSeconds(),
                wave.breakGroup(),
                wave.speed(),
                wave.intervalSeconds(),
                wave.carried(),
                wave.at(),
                wave.paths(),
                wave.loopBack(),
                tag);
    }

    /**
     * A one-section level of {@code seconds} at {@code speed} px/s with the destroy-targets primary
     * {@code targets} (none for reach-end), its ground units and waves, the radio cues, the secondary,
     * the hold zones and the collapse.
     */
    static LevelScript level(
            double seconds,
            double speed,
            List<WaveSpec> waves,
            List<LevelScript.GroundUnit> units,
            List<String> targets,
            LevelScript.Secondary secondary,
            List<LevelScript.RadioCue> radio,
            List<LevelScript.Hold> holds,
            Optional<LevelScript.Collapse> collapse) {
        return new LevelScript(
                9,
                2,
                0,
                List.of(new LevelScript.Section(seconds, speed)),
                waves,
                List.of(),
                units,
                0,
                radio,
                secondary,
                List.of(),
                List.of(),
                List.of(),
                Optional.empty(),
                Optional.empty(),
                targets,
                Optional.empty(),
                Optional.empty(),
                List.of(),
                Optional.empty(),
                List.of(),
                List.of(),
                holds,
                collapse);
    }

    /** Steps with {@code commands} until an event of {@code type}; returns the steps taken. */
    static int until(Sortie sortie, SimEvents.Type type, int most, int commands) {
        for (int step = 1; step <= most; step++) {
            sortie.step(commands);
            if (PartCSpecs.count(sortie, type) > 0) {
                return step;
            }
        }
        throw new AssertionError("no " + type + " in " + most + " steps");
    }

    /** The index of the first unit of {@code slug} on the field; -1 for none. */
    static int indexOf(Sortie sortie, String slug) {
        for (int i = 0; i < sortie.enemyCount(); i++) {
            if (sortie.enemy(i).spec().slug().equals(slug)) {
                return i;
            }
        }
        return -1;
    }

    /** Whether the last step's events carry a radio cue with index {@code cue}. */
    static boolean radioed(Sortie sortie, int cue) {
        SimEvents events = sortie.events();
        for (int i = 0; i < events.size(); i++) {
            if (events.type(i) == SimEvents.Type.RADIO && events.value(i) == cue) {
                return true;
            }
        }
        return false;
    }
}
