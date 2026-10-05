package vanguard.sim;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/** The chain window pauses while no enemy is on screen (design/systems/scoring, Chain multiplier). */
class ChainPauseTest {
    private static final int FIRE = Command.of(Command.FIRE);

    private static EnemySpec turret(String slug, double hp) {
        return new EnemySpec(
                slug,
                hp,
                new Hitbox(60, 30),
                Layer.GROUND,
                0,
                false,
                5,
                0,
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                true);
    }

    /** A turret in the ship's line of fire at 0 s, and a tough one far to the left at 10 s. */
    private static Sortie sortie() {
        LevelScript level = new LevelScript(
                1,
                1,
                0,
                List.of(new LevelScript.Section(40, 130)),
                List.of(),
                List.of(),
                List.of(
                        new LevelScript.GroundUnit(0, Ship.START_X, turret("ahead", 5), -1),
                        new LevelScript.GroundUnit(10, 60, turret("aside", 1000), -1)),
                0,
                List.of(),
                new LevelScript.Secondary(0.8, 50),
                List.of());
        return new Sortie(1, TestSpecs.LOADOUT, level, TestSpecs.RULES.withInvulnerableShip(), TestSpecs.FULL_ARMOUR);
    }

    private static void runTo(Sortie sortie, double seconds, int commands) {
        while (sortie.levelSeconds() < seconds) {
            sortie.step(commands);
        }
    }

    @Test
    void aGapWithAnEmptyScreenKeepsTheChainAndAnEnemyLeftAliveOnScreenBreaksIt() {
        Sortie sortie = sortie();
        while (sortie.kills() == 0 && sortie.levelSeconds() < 6) {
            sortie.step(FIRE);
        }
        assertEquals(1, sortie.kills(), "the turret ahead is shot down");
        double killed = sortie.levelSeconds();
        assertTrue(killed < 8 - 2, "killed at " + killed);

        // Nothing on screen until the second turret enters at 10 s: far longer than the 2 s window.
        runTo(sortie, 9.9, Command.NONE);
        assertEquals(1, sortie.chain(), "the empty screen paused the window");

        // The second turret comes on screen and is left alive; the window runs again.
        runTo(sortie, 11, Command.NONE);
        assertEquals(1, sortie.chain(), "the window has not run out yet");
        runTo(sortie, 12.5, Command.NONE);
        assertEquals(0, sortie.chain(), "an enemy alive on screen past the window breaks the chain");
    }
}
