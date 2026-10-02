package vanguard.sim;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static vanguard.sim.TestSpecs.NEEDLER;
import static vanguard.sim.TestSpecs.SKITTER;
import static vanguard.sim.TestSpecs.level;
import static vanguard.sim.TestSpecs.sortie;
import static vanguard.sim.TestSpecs.wave;
import static vanguard.sim.WaveSpec.Edge.LEFT;
import static vanguard.sim.WaveSpec.Edge.NONE;
import static vanguard.sim.WaveSpec.Entry.FRONT;
import static vanguard.sim.WaveSpec.Entry.REAR;
import static vanguard.sim.WaveSpec.Entry.SIDES;
import static vanguard.sim.WaveSpec.Formation.LINE_ABREAST;
import static vanguard.sim.WaveSpec.Formation.SNAKE;

import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class SortieTest {
    /** A single Skitter straight down the ship's start column (a line abreast of one is centred). */
    private static WaveSpec skitterAt(double t) {
        return wave(t, LINE_ABREAST, SKITTER, 1, FRONT, NONE);
    }

    /** A single Needler hovering above the ship's start column. */
    private static WaveSpec needlerAt(double t) {
        return wave(t, LINE_ABREAST, NEEDLER, 1, FRONT, NONE);
    }

    @Test
    void aWaveEntersAtItsTimeUnitByUnit() {
        var sortie = sortie(level(30, List.of(wave(1, SNAKE, SKITTER, 6, FRONT, LEFT))));

        run(sortie, SimStep.PER_SECOND - 1, Command.NONE);
        assertEquals(0, sortie.enemyCount());
        sortie.step(Command.NONE);
        assertEquals(1, sortie.enemyCount());
        run(sortie, 5 * SimStep.ticks(0.25), Command.NONE);

        assertEquals(6, sortie.enemyCount());
        assertEquals(6, sortie.enemyTotal());
    }

    @Test
    void theLevelEndsWhenTheScrollReachesTheEndOfTheLastSection() {
        var cue = new LevelScript.RadioCue(LevelScript.CueTrigger.LEVEL_END, 0, "", "Okafor", "Good work.", false);
        var sortie = sortie(level(2, List.of(), List.of(), List.of(cue)));

        run(sortie, 2 * SimStep.PER_SECOND - 1, Command.NONE);
        assertFalse(sortie.complete());
        sortie.step(Command.NONE);

        assertTrue(sortie.complete());
        assertEquals(1, sortie.events().count(SimEvents.Type.LEVEL_COMPLETE));
        assertEquals(1, sortie.events().count(SimEvents.Type.RADIO));
    }

    @Test
    void theGroundScrollsAtEachSectionsSpeed() {
        var script = new LevelScript(
                1,
                1,
                0,
                List.of(new LevelScript.Section(1, 130), new LevelScript.Section(2, 60)),
                List.of(),
                List.of(),
                0,
                List.of(),
                new LevelScript.Secondary(0.8, 50));
        var sortie = sortie(script);

        run(sortie, 2 * SimStep.PER_SECOND, Command.NONE);

        assertEquals(190, sortie.groundScroll(), 1e-6);
    }

    @Test
    void theLaunchIgnoresCommandsAndEndsAtTheStartPosition() {
        var script = new LevelScript(
                1,
                1,
                1,
                List.of(new LevelScript.Section(10, 130)),
                List.of(),
                List.of(),
                0,
                List.of(),
                new LevelScript.Secondary(0.8, 50));
        var sortie = sortie(script);
        assertTrue(sortie.ship().y() < 0, "on the rail below the play field");

        int fired = 0;
        for (int i = 0; i < SimStep.PER_SECOND; i++) {
            sortie.step(Command.of(Command.LEFT, Command.FIRE));
            fired += sortie.events().count(SimEvents.Type.SHOT_FIRED);
        }

        assertEquals(0, fired);
        assertEquals(Ship.START_X, sortie.ship().x());
        assertEquals(Ship.START_Y, sortie.ship().y(), 1e-9);
        sortie.step(Command.FIRE.bit());
        assertFalse(sortie.launching());
        assertEquals(1, sortie.events().count(SimEvents.Type.SHOT_FIRED));
    }

    @Test
    void sideAndRearWavesShowAnEdgeWarningAhead() {
        var rear = new WaveSpec(
                10,
                LINE_ABREAST,
                SKITTER,
                6,
                REAR,
                NONE,
                Optional.empty(),
                Optional.of(4.0),
                1,
                Optional.empty(),
                Optional.empty(),
                List.of());
        var sortie = sortie(level(20, List.of(wave(4, SNAKE, SKITTER, 6, SIDES, LEFT), rear)));

        run(sortie, SimStep.ticks(1) - 1, Command.NONE);
        assertEquals(0, sortie.edgeWarnings());
        sortie.step(Command.NONE);
        assertTrue(WarningEdge.LEFT.in(sortie.edgeWarnings()), "at least 3 s ahead");
        assertFalse(WarningEdge.RIGHT.in(sortie.edgeWarnings()));
        run(sortie, SimStep.ticks(3), Command.NONE);
        assertEquals(0, sortie.edgeWarnings(), "gone when the wave enters");

        run(sortie, SimStep.ticks(2), Command.NONE);
        assertTrue(WarningEdge.BOTTOM.in(sortie.edgeWarnings()), "an authored 4 s ahead of the rear wave");
    }

    @Test
    void oneBoltDestroysASkitterAndPaysItsBounty() {
        var sortie = sortie(level(10, List.of(skitterAt(0))));

        int destroyed = 0;
        for (int i = 0; i < 3 * SimStep.PER_SECOND; i++) {
            sortie.step(Command.FIRE.bit());
            destroyed += sortie.events().count(SimEvents.Type.ENEMY_DESTROYED);
        }

        assertEquals(1, destroyed);
        assertEquals(1, sortie.kills());
        // The bounty, plus the secondary objective: one kill of one enemy is 100 %.
        assertEquals(5 + 50, sortie.credits());
        assertEquals(50 + 500, sortie.score());
        assertEquals(20, sortie.ship().defences().shield(), "it never reached the ship");
    }

    @Test
    void aRammingSkitterIsDestroyedDealsContactDamageAndEndsTheChain() {
        var sortie = sortie(level(10, List.of(skitterAt(0))));

        run(sortie, 3 * SimStep.PER_SECOND, Command.NONE);

        assertEquals(1, sortie.kills());
        assertEquals(17, sortie.ship().defences().shield());
        assertEquals(57, sortie.ship().defences().armour());
        assertEquals(0, sortie.chain(), "armour damage ends the chain");
    }

    @Test
    void aDestroyedShipRestartsTheLevelAndLosesTheAttemptsEarnings() {
        List<WaveSpec> waves = new ArrayList<>();
        for (int i = 0; i < 40; i++) {
            waves.add(skitterAt(0.3 * i));
        }
        var sortie = sortie(level(30, waves));
        run(sortie, SimStep.ticks(2.6), Command.FIRE.bit());
        assertTrue(sortie.credits() > 0);

        int steps = 0;
        while (sortie.flying()) {
            sortie.step(Command.NONE);
            assertTrue(++steps < 20 * SimStep.PER_SECOND, "the Skitters should wear the ship down");
        }
        assertEquals(1, sortie.attempt());
        run(sortie, SimStep.ticks(Sortie.RESTART_SECONDS) - 1, Command.LEFT.bit());
        assertFalse(sortie.flying());
        sortie.step(Command.NONE);

        assertTrue(sortie.flying());
        assertEquals(2, sortie.attempt());
        assertEquals(1, sortie.events().count(SimEvents.Type.SORTIE_RESTARTED));
        assertEquals(0, sortie.credits());
        assertEquals(0, sortie.score());
        assertEquals(0, sortie.kills());
        assertEquals(60, sortie.ship().defences().armour());
        assertEquals(1, sortie.enemyCount(), "the waves start again from the beginning");
    }

    @Test
    void theWreckIgnoresCommands() {
        List<WaveSpec> waves = new ArrayList<>();
        for (int i = 0; i < 40; i++) {
            waves.add(skitterAt(0.3 * i));
        }
        var sortie = sortie(level(30, waves));
        while (sortie.flying()) {
            sortie.step(Command.NONE);
        }
        double x = sortie.ship().x();

        sortie.step(Command.of(Command.LEFT, Command.FIRE));

        assertEquals(x, sortie.ship().x());
        assertEquals(0, sortie.events().count(SimEvents.Type.SHOT_FIRED));
    }

    @Test
    void aHoveringNeedlerFiresAimedThornsAtTheShip() {
        var sortie = sortie(level(20, List.of(needlerAt(0))));

        EnemyBullet thorn = null;
        for (int i = 0; i < 5 * SimStep.PER_SECOND && thorn == null; i++) {
            sortie.step(Command.NONE);
            thorn = sortie.bulletCount() > 0 ? sortie.bullet(0) : null;
        }

        assertTrue(thorn != null, "it fires");
        double speed = Math.hypot(thorn.renderX(1) - thorn.renderX(0), thorn.renderY(1) - thorn.renderY(0));
        assertEquals(150 * SimStep.SECONDS, speed, 1e-9);
        assertEquals(StrictMath.PI, Math.abs(thorn.heading()), 1e-9, "straight down at the ship below");
    }

    @Test
    void easyAimedShotsSpreadWithinFourDegreesAndMediumOnesAreExact() {
        List<Double> easy = aimDeviations(Math.toRadians(4));
        List<Double> medium = aimDeviations(0);

        assertTrue(easy.size() > 10 && medium.size() == easy.size(), "it fires a stream of thorns");
        assertTrue(easy.stream().allMatch(d -> Math.abs(d) <= Math.toRadians(4) + 1e-9));
        double low = easy.stream().mapToDouble(d -> d).min().orElseThrow();
        double high = easy.stream().mapToDouble(d -> d).max().orElseThrow();
        assertTrue(low < -Math.toRadians(1) && high > Math.toRadians(1), "easy shots vary to both sides");
        assertTrue(medium.stream().allMatch(d -> Math.abs(d) < 1e-9), "medium shots are exact");
    }

    /** How far each thorn of a fast-firing Needler above the ship leaves from straight down, in radians. */
    private static List<Double> aimDeviations(double spread) {
        var rules = new Rules(120, spread, TestSpecs.RULES.pickups(), TestSpecs.SCORING);
        var needler = TestSpecs.needler(new EnemyGun(0.1, 0, 1, 150, 0, false));
        var sortie = new Sortie(
                1, TestSpecs.LOADOUT, level(20, List.of(wave(0, LINE_ABREAST, needler, 1, FRONT, NONE))), rules);
        List<Double> deviations = new ArrayList<>();
        for (int i = 0; i < 6 * SimStep.PER_SECOND; i++) {
            sortie.step(Command.NONE);
            if (sortie.events().count(SimEvents.Type.ENEMY_FIRED) > 0) {
                double heading = sortie.bullet(sortie.bulletCount() - 1).heading();
                deviations.add(Math.signum(heading) * (StrictMath.PI - Math.abs(heading)));
            }
        }
        return deviations;
    }

    @Test
    void thornsHitTheShield() {
        var sortie = sortie(level(20, List.of(needlerAt(0))));

        run(sortie, 6 * SimStep.PER_SECOND, Command.NONE);

        assertEquals(16, sortie.ship().defences().shield(), 1e-9);
    }

    @Test
    void noEnemyBulletSpawnsCloseToTheShip() {
        var sortie = sortie(level(20, List.of(needlerAt(0))));
        // Fly up right under the Needler's hover point and stay there.
        for (int i = 0; i < 6 * SimStep.PER_SECOND; i++) {
            sortie.step(sortie.ship().y() < 340 ? Command.UP.bit() : Command.NONE);
            assertEquals(0, sortie.events().count(SimEvents.Type.ENEMY_FIRED));
        }
    }

    @Test
    void theBulletBudgetCapsEnemyBulletsOnScreen() {
        var rules = new Rules(1, 0, TestSpecs.RULES.pickups(), TestSpecs.SCORING);
        var waves = List.of(wave(0, LINE_ABREAST, NEEDLER, 3, FRONT, NONE));
        var sortie = new Sortie(1, TestSpecs.LOADOUT, level(20, waves), rules);

        for (int i = 0; i < 4 * SimStep.PER_SECOND; i++) {
            sortie.step(Command.NONE);
            assertTrue(sortie.bulletCount() <= 1);
        }
    }

    @Test
    void everyFourthNeedlerKillDropsAShieldCell() {
        List<WaveSpec> waves = new ArrayList<>();
        for (int i = 0; i < 8; i++) {
            waves.add(needlerAt(2 * i));
        }
        var sortie = sortie(level(30, waves));

        int kills = 0;
        List<PickupType> drops = new ArrayList<>();
        for (int i = 0; i < 18 * SimStep.PER_SECOND; i++) {
            int before = sortie.pickupCount();
            sortie.step(Command.FIRE.bit());
            kills += sortie.events().count(SimEvents.Type.ENEMY_DESTROYED);
            if (sortie.pickupCount() > before) {
                drops.add(sortie.pickup(sortie.pickupCount() - 1).type());
            }
        }

        assertEquals(8, kills);
        assertEquals(List.of(PickupType.SHIELD_CELL, PickupType.SHIELD_CELL), drops);
    }

    @Test
    void theLastUnitOfAWaveDropsTheCarriedPickup() {
        var carrying = new WaveSpec(
                0,
                LINE_ABREAST,
                NEEDLER,
                1,
                FRONT,
                NONE,
                Optional.empty(),
                Optional.empty(),
                1,
                Optional.empty(),
                Optional.empty(),
                List.of(new WaveSpec.Carried(PickupType.ARMOUR_PATCH, true)));
        var sortie = sortie(level(20, List.of(carrying)));

        while (sortie.kills() == 0) {
            sortie.step(Command.FIRE.bit());
        }

        assertEquals(1, sortie.pickupCount());
        assertEquals(PickupType.ARMOUR_PATCH, sortie.pickup(0).type());
    }

    @Test
    void aDestroyedContainerPaysItsBountyAndItsSalvageIsCollected() {
        var container = new LevelScript.GroundObjectSpec(
                0, Ship.START_X, new Hitbox(32, 24), 3, 5, Optional.of(PickupType.SMALL_SALVAGE), 0, 0, "");
        var sortie = sortie(level(20, List.of(), List.of(container), List.of()));

        int picked = 0;
        for (int i = 0; i < 8 * SimStep.PER_SECOND; i++) {
            boolean demolished = sortie.groundObjectCount() == 0;
            sortie.step(demolished ? Command.UP.bit() : Command.FIRE.bit());
            for (int e = 0; e < sortie.events().size(); e++) {
                if (sortie.events().type(e) == SimEvents.Type.CREDITS_PICKED_UP) {
                    picked += sortie.events().value(e);
                }
            }
        }

        assertEquals(10, picked);
        assertEquals(15, sortie.credits());
        assertEquals(150, sortie.score());
    }

    @Test
    void uncollectedPickupsAreGoneAfterTheirTime() {
        var container = new LevelScript.GroundObjectSpec(
                0, Ship.START_X, new Hitbox(32, 24), 2, 5, Optional.of(PickupType.SMALL_SALVAGE), 0, 0, "");
        var sortie = sortie(level(20, List.of(), List.of(container), List.of()));
        while (sortie.pickupCount() == 0) {
            sortie.step(Command.FIRE.bit());
        }

        run(sortie, SimStep.ticks(6) - 1, Command.NONE);
        assertEquals(1, sortie.pickupCount());
        sortie.step(Command.NONE);

        assertEquals(0, sortie.pickupCount());
        assertEquals(5, sortie.credits(), "the bounty only");
    }

    @Test
    void threeHitsOnTheBeaconReleaseTheHiddenCrate() {
        var beacon = new LevelScript.GroundObjectSpec(
                0, Ship.START_X, new Hitbox(12, 12), 0, 0, Optional.empty(), 3, 80, "beacon cache");
        var line = new LevelScript.RadioCue(
                LevelScript.CueTrigger.SECRET, 0, "beacon cache", "Rook", "Nice shooting.", false);
        var sortie = sortie(level(20, List.of(), List.of(beacon), List.of(line)));

        int hits = 0;
        int found = 0;
        int cues = 0;
        for (int i = 0; i < 2 * SimStep.PER_SECOND; i++) {
            sortie.step(Command.FIRE.bit());
            hits += sortie.events().count(SimEvents.Type.GROUND_HIT);
            found += sortie.events().count(SimEvents.Type.SECRET_FOUND);
            cues += sortie.events().count(SimEvents.Type.RADIO);
        }

        assertEquals(3, hits, "spent after the third hit");
        assertEquals(1, found);
        assertEquals(1, cues);
        assertTrue(sortie.groundObject(0).spent());
        assertEquals(PickupType.HIDDEN_CRATE, sortie.pickup(0).type());
    }

    @Test
    void theSecondaryObjectivePaysAt80PercentOfAllEnemies() {
        List<WaveSpec> waves = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            waves.add(skitterAt(i));
        }
        var cues = List.of(
                new LevelScript.RadioCue(LevelScript.CueTrigger.FIRST_KILL, 0, "skitter", "Rook", "Bugs.", false),
                new LevelScript.RadioCue(
                        LevelScript.CueTrigger.SECONDARY_OBJECTIVE, 0, "", "Okafor", "Clean sweep.", false));
        var sortie = sortie(level(20, waves, List.of(), cues));
        assertEquals(4, sortie.requiredKills());

        List<Integer> radio = new ArrayList<>();
        while (sortie.kills() < 4) {
            assertFalse(sortie.secondaryMet());
            sortie.step(Command.FIRE.bit());
            for (int e = 0; e < sortie.events().size(); e++) {
                if (sortie.events().type(e) == SimEvents.Type.RADIO) {
                    radio.add(sortie.events().value(e));
                }
            }
        }

        assertTrue(sortie.secondaryMet());
        assertEquals(4 * 5 + 50, sortie.credits());
        assertEquals(List.of(0, 1), radio);
    }

    @Test
    void sameSeedAndCommandsGiveTheSameHash() {
        assertEquals(run(7, 1800, -1), run(7, 1800, -1));
    }

    @Test
    void oneDifferentCommandOrSeedChangesTheHash() {
        assertNotEquals(run(7, 901, -1), run(7, 901, 900));
        assertNotEquals(run(7, 1800, -1), run(8, 1800, -1));
    }

    @Test
    void steppingDoesNotAllocate() {
        var threads = (com.sun.management.ThreadMXBean) ManagementFactory.getThreadMXBean();
        var sortie = new Sortie(3, TestSpecs.LOADOUT, mixedLevel(), TestSpecs.RULES);
        for (int i = 0; i < 600; i++) {
            sortie.step(Pilot.commands(i));
        }
        long before = threads.getCurrentThreadAllocatedBytes();
        for (int i = 600; i < 600 + 3600; i++) {
            sortie.step(Pilot.commands(i));
        }
        long allocated = threads.getCurrentThreadAllocatedBytes() - before;

        assertTrue(allocated < 1024, "3600 steps allocated " + allocated + " bytes");
    }

    /** Snakes, a V of Needlers that hover and fire, and a circle, repeating every 10 s. */
    private static LevelScript mixedLevel() {
        List<WaveSpec> waves = new ArrayList<>();
        for (int t = 0; t < 70; t += 10) {
            waves.add(wave(t + 1, SNAKE, SKITTER, 6, FRONT, LEFT));
            waves.add(wave(t + 3, WaveSpec.Formation.V_WING, NEEDLER, 5, FRONT, NONE));
            waves.add(new WaveSpec(
                    t + 6,
                    WaveSpec.Formation.CIRCLE,
                    NEEDLER,
                    8,
                    FRONT,
                    NONE,
                    Optional.of(2.0),
                    Optional.empty(),
                    2,
                    Optional.empty(),
                    Optional.empty(),
                    List.of()));
        }
        return level(80, waves);
    }

    /** Runs the scripted pilot over the mixed level; at step {@code changedStep} it presses left as well. */
    private static long run(long seed, int steps, int changedStep) {
        var sortie = new Sortie(seed, TestSpecs.LOADOUT, mixedLevel(), TestSpecs.RULES);
        for (int i = 0; i < steps; i++) {
            int commands = Pilot.commands(i);
            sortie.step(i == changedStep ? commands | Command.LEFT.bit() : commands);
        }
        return sortie.stateHash();
    }

    private static void run(Sortie sortie, int steps, int commands) {
        for (int i = 0; i < steps; i++) {
            sortie.step(commands);
        }
    }

    /** A scripted pilot that weaves left and right, sometimes in precision mode, firing most of the time. */
    static final class Pilot {
        private Pilot() {}

        static int commands(int step) {
            int phase = step % 240;
            int commands = phase < 120 ? Command.LEFT.bit() : Command.RIGHT.bit();
            if (step % 600 > 450) {
                commands |= Command.PRECISION.bit();
            }
            if (step % 300 < 270) {
                commands |= Command.FIRE.bit();
            }
            if (step % 500 < 60) {
                commands |= Command.UP.bit();
            }
            return commands;
        }
    }
}
