package vanguard.sim;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * The radio's {@code escort-first-kill} (design/player/wingmen, Scripted lines about him; M5 part
 * B): it starts on the first kill by Rook's own shot or blast in an attempt, only while he flies,
 * once; the flag goes into the state hash and the boss checkpoint.
 */
class EscortFirstKillTest {
    private static final LevelScript.RadioCue SPLASH = new LevelScript.RadioCue(
            LevelScript.CueTrigger.ESCORT_FIRST_KILL, 0, "", "Rook", "Splash one.", false, "neutral", "Rook");
    private static final LevelScript.RadioCue PLAYER_FIRST = new LevelScript.RadioCue(
            LevelScript.CueTrigger.FIRST_KILL, 0, "husk", "Okafor", "Good kill.", false, "neutral", "Okafor");

    private static final EnemySpec HUSK = new EnemySpec(
            "husk",
            4,
            new Hitbox(16, 16),
            Layer.GROUND,
            6,
            false,
            10,
            0,
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            false);

    /** Ground units in a column at {@code x}, from {@code t} every half second. */
    private static List<LevelScript.GroundUnit> column(double x, double t, int count) {
        List<LevelScript.GroundUnit> units = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            units.add(new LevelScript.GroundUnit(t + 0.5 * i, x, HUSK, -1));
        }
        return units;
    }

    private static LevelScript level(List<LevelScript.GroundUnit> units) {
        return new LevelScript(
                8,
                2,
                0,
                List.of(new LevelScript.Section(30, 130)),
                List.of(),
                List.of(),
                units,
                0,
                List.of(SPLASH, PLAYER_FIRST),
                new LevelScript.Secondary(0.8, 50),
                List.of());
    }

    private static Sortie withRook(LevelScript level) {
        return new Sortie(
                1,
                TestSpecs.LOADOUT.withWingman(WingmanTest.rook(WingmanSpec.Side.LEFT)),
                level,
                TestSpecs.RULES,
                TestSpecs.FULL_ARMOUR);
    }

    /** Steps with {@code commands}; returns how often cue {@code cue} started and the kills made. */
    private static int[] fly(Sortie sortie, int steps, int commands, int cue) {
        int started = 0;
        int kills = 0;
        for (int i = 0; i < steps; i++) {
            sortie.step(commands);
            SimEvents events = sortie.events();
            for (int e = 0; e < events.size(); e++) {
                started += events.type(e) == SimEvents.Type.RADIO && events.value(e) == cue ? 1 : 0;
                kills += events.type(e) == SimEvents.Type.ENEMY_DESTROYED ? 1 : 0;
            }
        }
        return new int[] {started, kills};
    }

    @Test
    void itStartsOnceOnHisFirstKill() {
        // A column ahead of him (his slot is 64 px left of the ship), out of the player's line of fire.
        Sortie sortie = withRook(level(column(Ship.START_X - 64, 0.5, 3)));

        int[] flown = fly(sortie, 300, Command.FIRE.bit(), 0);

        assertEquals(3, flown[1], "he kills the column");
        assertEquals(1, flown[0], "once per attempt");
        assertTrue(sortie.escortKilled());
    }

    @Test
    void thePlayersKillsDoNotStartIt() {
        Sortie sortie = withRook(level(column(Ship.START_X, 0.5, 3)));

        int[] flown = fly(sortie, 300, Command.FIRE.bit(), 0);

        assertTrue(flown[1] > 0, "the player kills the column");
        assertEquals(0, flown[0]);
        assertFalse(sortie.escortKilled());
    }

    @Test
    void withoutHimOrAfterHeEjectedItNeverStarts() {
        LevelScript level = level(column(Ship.START_X - 64, 0.5, 3));
        Sortie alone = new Sortie(1, TestSpecs.LOADOUT, level, TestSpecs.RULES, TestSpecs.FULL_ARMOUR);
        assertEquals(0, fly(alone, 300, Command.FIRE.bit(), 0)[0]);

        Sortie ejected = withRook(level);
        Wingman rook = ejected.wingman().orElseThrow();
        for (int k = 0; k < 10 && !rook.ejected(); k++) {
            ejected.fireBullet(rook.x() - 20, rook.y(), 0, 900, 40);
            fly(ejected, 5, Command.NONE, 0);
        }
        assertTrue(rook.ejected());
        assertEquals(0, fly(ejected, 300, Command.FIRE.bit(), 0)[0]);
        assertFalse(ejected.escortKilled());
    }

    @Test
    void aRetryStartsItAgainAndTheFlagIsHashed() {
        Sortie sortie = withRook(level(column(Ship.START_X - 64, 0.5, 3)));
        fly(sortie, 300, Command.FIRE.bit(), 0);
        Sortie other = withRook(level(column(Ship.START_X - 64, 0.5, 3)));
        fly(other, 300, Command.FIRE.bit(), 0);
        assertEquals(sortie.stateHash(), other.stateHash());

        sortie.retry(TestSpecs.FULL_ARMOUR);
        sortie.step(Command.NONE);
        assertFalse(sortie.escortKilled());
        assertEquals(1, fly(sortie, 300, Command.FIRE.bit(), 0)[0], "once again in the new attempt");
    }

    @Test
    void theBossCheckpointKeepsIt() {
        LevelScript.SetPieceSpec carrier = TestSpecs.carrier(false, 2);
        LevelScript level = new LevelScript(
                8,
                2,
                0,
                List.of(
                        new LevelScript.Section(2, 130),
                        new LevelScript.Section(200, 20, true),
                        new LevelScript.Section(210, 130)),
                List.of(),
                List.of(),
                List.of(new LevelScript.GroundUnit(0.05, Ship.START_X - 64, HUSK, -1)),
                0,
                List.of(SPLASH),
                new LevelScript.Secondary(0.8, 50),
                List.of(),
                List.of(),
                List.of(carrier));
        Sortie sortie = withRook(level);
        int started = 0;
        while (!sortie.bossCheckpoint()) {
            started += fly(sortie, 1, Command.FIRE.bit(), 0)[0];
        }
        assertEquals(1, started, "his kill came before the boss");
        assertTrue(sortie.escortKilled());

        sortie.retryFromBoss();
        sortie.step(Command.NONE);

        assertTrue(sortie.escortKilled(), "kept from the checkpoint");
        assertEquals(0, fly(sortie, 120, Command.FIRE.bit(), 0)[0], "it does not start again");
    }
}
