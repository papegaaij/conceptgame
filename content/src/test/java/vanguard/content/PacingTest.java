package vanguard.content;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import vanguard.sim.Enemy;
import vanguard.sim.Hitbox;
import vanguard.sim.Loadout;
import vanguard.sim.PlayField;
import vanguard.sim.SetPiece;
import vanguard.sim.Sortie;

/**
 * The campaign's pacing rule (design/campaign, Pacing rules): from Level 04 on, the screen is never
 * empty of enemies for longer than {@link #EMPTY_SECONDS} after the launch, with at most {@link
 * #LONG_PAUSES} longer pauses per level. Levels 01-03 are the warm-up and are exempt.
 *
 * <p>The autopilot flies each level, so the waves it kills quickly leave the screen early, as for a
 * good player. A stretch is empty when no live enemy overlaps the play field: air and ground units
 * alike (turrets, walkers, released broods). A set piece on screen (a crane, a boss) counts as an
 * enemy: it is a scripted moment. The stretches are measured on the attempt that completes the
 * level, from the end of its launch to the end of the scroll, in real seconds (M5 part C: in a hold
 * zone the level clock slows with the scroll, {@link Sortie#realSeconds()}).
 */
class PacingTest {
    /** The first level the rule applies to; Levels 01-03 are the warm-up. */
    static final int FIRST_LEVEL = 4;
    /** The longest a screen may stay empty of enemies, in seconds. */
    static final double EMPTY_SECONDS = 3;
    /** How many longer pauses a level may have. */
    static final int LONG_PAUSES = 2;

    private static final int MAX_STEPS = 60 * 60 * 20;
    private static final Content CONTENT = ContentLoader.fromClasspath();

    static Stream<Arguments> levels() {
        return IntStream.rangeClosed(FIRST_LEVEL, 50)
                .mapToObj(CONTENT::levelKey)
                .flatMap(java.util.Optional::stream)
                .flatMap(key -> Stream.of(Difficulty.values()).map(difficulty -> Arguments.of(key, difficulty)));
    }

    @ParameterizedTest
    @MethodSource("levels")
    void theScreenIsNeverEmptyForLong(String key, Difficulty difficulty) {
        List<double[]> pauses = emptyStretches(key, difficulty).stream()
                .filter(stretch -> stretch[1] - stretch[0] > EMPTY_SECONDS)
                .toList();

        StringBuilder text = new StringBuilder();
        for (double[] pause : pauses) {
            text.append(String.format(" %.1f-%.1f s (%.1f s)", pause[0], pause[1], pause[1] - pause[0]));
        }
        System.out.printf(
                "Pacing %s %s: %d pauses over %.0f s:%s%n", key, difficulty, pauses.size(), EMPTY_SECONDS, text);
        assertTrue(pauses.size() <= LONG_PAUSES, "pauses over " + EMPTY_SECONDS + " s:" + text);
    }

    /** The empty stretches of the completed attempt, as [from, to] in real seconds since the attempt's start. */
    static List<double[]> emptyStretches(String key, Difficulty difficulty) {
        // The starter fit; for Levels 05 to 07, where the starter cannot clear the batteries in time
        // (Level 05), finish hard (Level 06) or bring down the Brood Carrier (Level 07),
        // the balance plan's fit for the level; from Level 08 on the plan's fit with Rook on its wing
        // (Level 09: the Bomb Rack and Rook's Mortar for the hardened nodes, D8 = c of M5 part C;
        // Level 10: the Tail Gun and Rook's Autocannon, D7 = a of M5 part D; Level 11: the front at L4
        // and Rook's Mortar again, M5 part E).
        Loadout loadout = key.equals(Level05Test.LEVEL)
                ? Level05Test.planLoadout(CONTENT, difficulty)
                : key.equals(Level06Test.LEVEL)
                        ? Level06Test.planLoadout(CONTENT, difficulty)
                        : key.equals(Level07Test.LEVEL)
                                ? Level07Test.planLoadout(CONTENT, difficulty)
                                : key.equals(Level08Test.LEVEL)
                                        ? Level08Test.planLoadout(CONTENT, difficulty)
                                        : key.equals(Level09Test.LEVEL)
                                                ? Level09Test.planLoadout(CONTENT, difficulty)
                                                : key.equals(Level10Test.LEVEL)
                                                        ? Level10Test.planLoadout(CONTENT, difficulty)
                                                        : key.equals(Level11Test.LEVEL)
                                                                ? Level11Test.planLoadout(CONTENT, difficulty)
                                                                : SimSpecs.starterLoadout(CONTENT, difficulty);
        Sortie sortie = new Sortie(
                2185,
                loadout,
                SimSpecs.level(CONTENT, key, difficulty),
                SimSpecs.rules(CONTENT, key, difficulty),
                loadout.plating().maxArmour());
        List<double[]> stretches = new ArrayList<>();
        int attempt = sortie.attempt();
        double emptySince = -1;
        int steps = 0;
        while (!sortie.complete() && steps++ < MAX_STEPS) {
            Level04Test.step(sortie);
            if (sortie.attempt() != attempt) {
                attempt = sortie.attempt();
                stretches.clear();
                emptySince = -1;
            }
            if (sortie.launching()) {
                continue;
            }
            boolean empty = !occupied(sortie);
            if (empty && emptySince < 0) {
                emptySince = sortie.realSeconds();
            } else if (!empty && emptySince >= 0) {
                stretches.add(new double[] {emptySince, sortie.realSeconds()});
                emptySince = -1;
            }
        }
        assertTrue(sortie.complete(), key + " " + difficulty + " completes");
        if (emptySince >= 0) {
            stretches.add(new double[] {emptySince, sortie.realSeconds()});
        }
        return stretches;
    }

    private static boolean occupied(Sortie sortie) {
        for (int i = 0; i < sortie.enemyCount(); i++) {
            Enemy enemy = sortie.enemy(i);
            if (onScreen(enemy.renderX(1), enemy.renderY(1), enemy.spec().hitbox())) {
                return true;
            }
        }
        for (int i = 0; i < sortie.setPieceCount(); i++) {
            SetPiece piece = sortie.setPiece(i);
            if (piece.present() && !piece.destroyed() && !piece.escaped()) {
                return true;
            }
        }
        return false;
    }

    private static boolean onScreen(double x, double y, Hitbox box) {
        return x + box.width() / 2 > 0
                && x - box.width() / 2 < PlayField.WIDTH
                && y + box.height() / 2 > 0
                && y - box.height() / 2 < PlayField.HEIGHT;
    }
}
