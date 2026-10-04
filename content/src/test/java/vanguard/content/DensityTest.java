package vanguard.content;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import vanguard.sim.EnemySpec;
import vanguard.sim.LevelScript;
import vanguard.sim.WaveSpec;

/**
 * The campaign's minimum density (design/campaign, Difficulty curve): every level reaches its
 * act's minimum in enemies per minute of scroll at medium, air and ground units with the released
 * spawns, the launch and the boss fight (an arena section and what follows it) excluded. The
 * warm-up Levels 01-03 have their own, lower minimum.
 */
class DensityTest {
    /** Enemies per minute per act (Act 1 first). */
    static final int[] ACT_MINIMUM = {40, 50, 55, 60, 70, 75, 80};
    /** The warm-up levels' minimum, Levels 01-03. */
    static final int WARM_UP_MINIMUM = 32;

    static final int LAST_WARM_UP = 3;

    private static final Content CONTENT = ContentLoader.fromClasspath();

    static Stream<String> levels() {
        return CONTENT.levels().keySet().stream();
    }

    @ParameterizedTest
    @MethodSource("levels")
    void theLevelReachesItsMinimumDensity(String key) {
        LevelScript level = SimSpecs.level(CONTENT, key, Difficulty.MEDIUM);
        double density = perMinute(level);
        int minimum = level.number() <= LAST_WARM_UP ? WARM_UP_MINIMUM : ACT_MINIMUM[level.act() - 1];
        System.out.printf("Density %s: %.1f enemies per minute (minimum %d)%n", key, density, minimum);
        assertTrue(density >= minimum, key + ": " + density + " enemies per minute, minimum " + minimum);
    }

    /** The level's enemies per minute of scroll, from the end of the launch to its first arena. */
    static double perMinute(LevelScript level) {
        double end = level.seconds();
        double start = 0;
        for (LevelScript.Section section : level.sections()) {
            if (section.arena()) {
                end = start;
                break;
            }
            start = section.end();
        }
        int enemies = 0;
        for (WaveSpec wave : level.waves()) {
            if (wave.t() < end) {
                int released = wave.enemy().brood().map(EnemySpec.Brood::count).orElse(0);
                enemies += wave.count() * (1 + released);
            }
        }
        for (LevelScript.GroundUnit unit : level.groundUnits()) {
            if (unit.t() < end) {
                enemies++;
            }
        }
        return enemies / ((end - level.launchSeconds()) / 60);
    }
}
