package vanguard.sim;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static vanguard.sim.TestSpecs.FULL_ARMOUR;
import static vanguard.sim.TestSpecs.INFINITE;
import static vanguard.sim.TestSpecs.LOADOUT;
import static vanguard.sim.TestSpecs.NEEDLER;
import static vanguard.sim.TestSpecs.RULES;
import static vanguard.sim.TestSpecs.SKITTER;
import static vanguard.sim.TestSpecs.wave;
import static vanguard.sim.WaveSpec.Edge.NONE;
import static vanguard.sim.WaveSpec.Entry.FRONT;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * Level 04's new units: the Brood Pod (design/enemies/air/brood-pod), a spawner that bursts into
 * Skitters, killed or on its own, with Needlers circling it as escorts; and the Scuttler
 * (design/enemies/ground/scuttler), a walker on an authored ground path with a frontal armour, a
 * fan along its facing and a spit while the ship is behind it.
 */
class BroodAndWalkerTest {
    private static final EnemySpec POD = pod();
    private static final EnemyGun FAN =
            new EnemyGun(2.8, 0, 1, 140, 4, false, 5, Math.toRadians(60), INFINITE, INFINITE);
    private static final EnemyGun SPIT = EnemyGun.aimed(4, 0, 1, 120, 6, false);
    private static final EnemySpec SCUTTLER = scuttler();

    private static EnemySpec pod() {
        return new EnemySpec(
                "brood-pod",
                30,
                new Hitbox(50, 50),
                Layer.AIR,
                15,
                false,
                20,
                40,
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                false,
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.of(new EnemySpec.Sine(20, 4)),
                Optional.of(new EnemySpec.Brood(SKITTER, 6, 8, 3, Math.toRadians(120), 160, 8)),
                Optional.empty());
    }

    private static EnemySpec scuttler() {
        return new EnemySpec(
                "scuttler",
                28,
                new Hitbox(46, 40),
                Layer.GROUND,
                15,
                false,
                25,
                40,
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.of(FAN),
                Optional.empty(),
                Optional.empty(),
                false,
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.of(new EnemySpec.Walker(
                        40, Math.toRadians(90), 24, Math.toRadians(45), Optional.of(SPIT), Math.toRadians(90))));
    }

    private static WaveSpec walkers(double t, WaveSpec.Formation formation, int count, List<WaveSpec.At> path) {
        return new WaveSpec(
                t,
                formation,
                SCUTTLER,
                count,
                formation == WaveSpec.Formation.PINCER ? WaveSpec.Entry.SIDES : FRONT,
                NONE,
                Optional.empty(),
                Optional.empty(),
                1,
                Optional.empty(),
                Optional.empty(),
                List.of(),
                Optional.empty(),
                List.of(path));
    }

    private static Sortie sortie(double seconds, List<WaveSpec> waves) {
        LevelScript level = new LevelScript(
                4,
                1,
                0,
                List.of(new LevelScript.Section(seconds, 120)),
                waves,
                List.of(),
                List.of(),
                0,
                List.of(),
                new LevelScript.Secondary(0, 50, List.of(), "brood-pod"),
                List.of(),
                List.of(),
                List.of());
        return new Sortie(1, LOADOUT, level, RULES, FULL_ARMOUR);
    }

    /** A walker entering on {@code path} (play-field points, y up). */
    private static Enemy walker(double... path) {
        Enemy enemy = new Enemy();
        enemy.spawn(
                new Spawn(
                        0,
                        0,
                        SCUTTLER,
                        FlightPath.through(path[0], path[1], path[0], path[1] - 1),
                        40,
                        0,
                        Optional.empty(),
                        Spawn.Exit.DOWN,
                        false,
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.of(WalkPath.through(path))),
                0);
        return enemy;
    }

    // --- the Brood Pod ------------------------------------------------------------------------

    @Test
    void aPodBurstsOnItsOwnItsTimeAfterEnteringIntoSkittersFannedAtTheShip() {
        Sortie sortie = sortie(30, List.of(wave(0, WaveSpec.Formation.SINGLE, POD, 1, FRONT, NONE)));
        int burstTick = -1;
        double podX = 0;
        double podY = 0;
        for (int tick = 1; tick <= SimStep.ticks(12) && burstTick < 0; tick++) {
            sortie.step(0);
            SimEvents events = sortie.events();
            for (int i = 0; i < events.size(); i++) {
                if (events.type(i) == SimEvents.Type.BROOD_BURST) {
                    burstTick = tick;
                    podX = events.x(i);
                    podY = events.y(i);
                }
            }
        }
        // It enters 40 px above the top edge at 40 px/s: its centre crosses the edge after 1 s.
        assertEquals(SimStep.ticks(9), burstTick, 2);
        assertEquals(6, sortie.enemyCount());
        double toShip = Math.atan2(sortie.ship().y() - podY, sortie.ship().x() - podX);
        double[] before = new double[6];
        for (int i = 0; i < 6; i++) {
            assertEquals("skitter", sortie.enemy(i).spec().slug());
            before[i] = sortie.enemy(i).renderX(1);
        }
        sortie.step(0);
        List<Double> angles = new ArrayList<>();
        for (int i = 0; i < 6; i++) {
            Enemy skitter = sortie.enemy(i);
            double dx = skitter.renderX(1) - skitter.renderX(0);
            double dy = skitter.renderY(1) - skitter.renderY(0);
            assertEquals(160 * SimStep.SECONDS, Math.hypot(dx, dy), 1e-6);
            angles.add(Math.toDegrees(Math.IEEEremainder(Math.atan2(dy, dx) - toShip, 2 * Math.PI)));
        }
        angles.sort(null);
        assertEquals(-60, angles.getFirst(), 1e-6);
        assertEquals(60, angles.getLast(), 1e-6);

        LevelResult result = sortie.result();
        assertEquals(0, result.kills(), "a self-burst is not a kill");
        assertEquals(8, result.credits().kills(), "its burst bounty");
        assertTrue(sortie.secondaryFailed(), "an escape for the escapes objective");
    }

    @Test
    void aKilledPodPaysItsBountyIsAKillAndReleasesItsSkittersToo() {
        Sortie sortie = sortie(30, List.of(wave(0, WaveSpec.Formation.SINGLE, POD, 1, FRONT, NONE)));
        int hatched = 0;
        int bursts = 0;
        for (int tick = 0; tick < SimStep.ticks(9) && hatched == 0; tick++) {
            sortie.step(Command.FIRE.bit());
            hatched += sortie.events().count(SimEvents.Type.BROOD_HATCHED);
            bursts += sortie.events().count(SimEvents.Type.BROOD_BURST);
        }

        assertEquals(1, hatched);
        assertEquals(0, bursts);
        assertEquals(1, sortie.result().kills());
        assertEquals(1, sortie.escapesDestroyed());
        assertFalse(sortie.secondaryFailed());
        assertTrue(sortie.enemyCount() >= 5, "its Skitters fly out");
        assertEquals(1 + 6, sortie.enemyTotal(), "the released Skitters count among the level's enemies");
    }

    @Test
    void escortsCircleTheMovingPodAndBreakOffHalfASecondApartWhenItEnds() {
        Sortie sortie = sortie(
                30,
                List.of(
                        wave(0, WaveSpec.Formation.CARRIER_ESCORTS, POD, 1, FRONT, NONE),
                        wave(0, WaveSpec.Formation.CARRIER_ESCORTS, NEEDLER, 3, FRONT, NONE)));
        for (int tick = 0; tick < SimStep.ticks(4); tick++) {
            sortie.step(0);
        }
        Enemy pod = null;
        List<Enemy> escorts = new ArrayList<>();
        for (int i = 0; i < sortie.enemyCount(); i++) {
            Enemy enemy = sortie.enemy(i);
            if (enemy.spec().brood().isPresent()) {
                pod = enemy;
            } else {
                escorts.add(enemy);
            }
        }
        assertEquals(3, escorts.size());
        List<Double> angles = new ArrayList<>();
        for (Enemy escort : escorts) {
            double dx = escort.renderX(1) - pod.renderX(1);
            double dy = escort.renderY(1) - pod.renderY(1);
            assertEquals(90, Math.hypot(dx, dy), 1.5, "the orbit's radius around the moving pod");
            angles.add(Math.toDegrees(Math.atan2(dy, dx)));
        }
        angles.sort(null);
        assertEquals(120, angles.get(1) - angles.get(0), 2);
        assertEquals(120, angles.get(2) - angles.get(1), 2);

        // The pod bursts on its own at about 9 s; each escort keeps circling its last position
        // until it breaks off: one at once, the others 0.5 s apart.
        int[] broke = {-1, -1, -1};
        int burstTick = -1;
        for (int tick = SimStep.ticks(4) + 1; tick < SimStep.ticks(12); tick++) {
            sortie.step(0);
            if (burstTick < 0 && sortie.events().count(SimEvents.Type.BROOD_BURST) > 0) {
                burstTick = tick;
            }
            if (burstTick < 0) {
                continue;
            }
            for (int k = 0; k < 3; k++) {
                Enemy escort = escorts.get(k);
                // Circling it moves 90 px × 60°/s = 1.57 px a step; broken off, at its 120 px/s = 2 px.
                double step = Math.hypot(escort.renderX(1) - escort.renderX(0), escort.renderY(1) - escort.renderY(0));
                if (broke[k] < 0 && tick > burstTick && step > 1.8) {
                    broke[k] = tick - burstTick;
                }
            }
        }
        int[] sorted = broke.clone();
        java.util.Arrays.sort(sorted);
        // Each turns toward the ship in the step it breaks off and flies from the next.
        assertEquals(2, sorted[0], "the first breaks off at once: " + java.util.Arrays.toString(broke));
        assertEquals(2 + 30, sorted[1], java.util.Arrays.toString(broke));
        assertEquals(2 + 60, sorted[2], java.util.Arrays.toString(broke));
    }

    // --- the Scuttler -------------------------------------------------------------------------

    @Test
    void aWalkerWalksItsPathOverTheScrollingGroundTurningAtItsTurnRate() {
        // Up the screen 100 px, then a right turn toward (340, 400).
        Enemy enemy = walker(240, 300, 240, 400, 340, 400);
        double facing = enemy.facing();
        assertEquals(Math.PI, Math.abs(facing), 1e-9, "it faces up its first leg");
        double most = Math.toRadians(90) * SimStep.SECONDS + 1e-9;
        for (int tick = 1; tick <= SimStep.ticks(6); tick++) {
            enemy.move(240, 96, 1);
            double turned = Math.abs(Math.IEEEremainder(enemy.facing() - facing, 2 * Math.PI));
            assertTrue(turned <= most, "turned " + Math.toDegrees(turned) + "° in a step");
            facing = enemy.facing();
        }
        assertEquals(40 * 6, enemy.walked(), 1e-6, "40 px/s over the ground");
        // Walking on to the right after its turn (slightly down: it swung wide round the corner and
        // came back to the point), it has come down with the ground: 1 px a step.
        assertEquals(-Math.PI / 2, enemy.facing(), Math.toRadians(20));
        assertTrue(enemy.renderX(1) > 300, "x " + enemy.renderX(1));
        assertTrue(enemy.renderY(1) < 400 - 300, "y " + enemy.renderY(1));
    }

    @Test
    void directShotsFromAheadGlanceButNotFromTheSidesOrBehind() {
        // Walking down the screen: its front faces the ship below.
        Enemy enemy = walker(240, 400, 240, 100);
        double up = 900;

        assertTrue(enemy.glances(0, up), "head on");
        assertTrue(enemy.glances(up * Math.sin(Math.toRadians(40)), up * Math.cos(Math.toRadians(40))));
        assertFalse(enemy.glances(up * Math.sin(Math.toRadians(50)), up * Math.cos(Math.toRadians(50))));
        assertFalse(enemy.glances(up, 0), "from the side");
        assertFalse(enemy.glances(0, -up), "from behind");
        assertFalse(enemy.glances(0, 0), "a bomb or shell");
        Enemy needler = new Enemy();
        needler.spawn(
                new Spawn(
                        0,
                        0,
                        NEEDLER,
                        FlightPath.through(240, 400, 240, 100),
                        120,
                        0,
                        Optional.empty(),
                        Spawn.Exit.DOWN,
                        false,
                        Optional.empty()),
                0);
        assertFalse(needler.glances(0, up), "only walkers have the armour");
        assertTrue(!enemy.damage(10, true) && enemy.hp() == 18, "damage from above ignores it");
    }

    @Test
    void itsFanGoesAlongItsFacingAndItSpitsOnlyWithTheShipBehindIt() {
        // Walking up the screen, away from a ship below it: the ship is behind.
        Enemy away = walker(240, 100, 240, 500);
        Enemy toward = walker(240, 500, 240, 0);
        int spits = 0;
        int fans = 0;
        int towardSpits = 0;
        for (int tick = 0; tick < SimStep.ticks(9); tick++) {
            away.move(240, 96, 0);
            toward.move(240, 96, 0);
            fans += away.trigger() ? 1 : 0;
            spits += away.spit(240, 96) ? 1 : 0;
            toward.trigger();
            towardSpits += toward.spit(240, 96) ? 1 : 0;
        }
        assertEquals(2, spits, "at half its interval, then every 4 s");
        assertEquals(3, fans, "at half its interval, then every 2.8 s");
        assertEquals(0, towardSpits, "facing the ship it only fans");
    }

    @Test
    void aWalkerPincerMirrorsItsPathAndAConvoyRepeatsIt() {
        List<WaveSpec.At> path = List.of(new WaveSpec.At(-40, 100), new WaveSpec.At(100, 200));
        List<Spawn> pincer = new ArrayList<>();
        Formations.plan(walkers(10, WaveSpec.Formation.PINCER, 4, path), 0, new SplitMix64(1), pincer);
        List<Spawn> convoy = new ArrayList<>();
        Formations.plan(walkers(10, WaveSpec.Formation.CONVOY, 2, path), 0, new SplitMix64(1), convoy);

        assertEquals(-40, pincer.get(0).walk().orElseThrow().x(0), 1e-9);
        assertEquals(PlayField.WIDTH + 40, pincer.get(1).walk().orElseThrow().x(0), 1e-9);
        assertEquals(PlayField.HEIGHT - 100, pincer.get(1).walk().orElseThrow().y(0), 1e-9);
        assertEquals(
                List.of(600, 600, 690, 690), pincer.stream().map(Spawn::tick).toList());
        assertEquals(List.of(600, 690), convoy.stream().map(Spawn::tick).toList());
        assertEquals(100, convoy.get(1).walk().orElseThrow().x(1), 1e-9);
    }

    @Test
    void theNewUnitsStepDeterministicallyWithoutAllocating() {
        List<WaveSpec> waves = List.of(
                wave(0, WaveSpec.Formation.CARRIER_ESCORTS, POD, 1, FRONT, NONE),
                wave(0, WaveSpec.Formation.CARRIER_ESCORTS, NEEDLER, 3, FRONT, NONE),
                walkers(1, WaveSpec.Formation.PINCER, 2, List.of(new WaveSpec.At(-40, 0), new WaveSpec.At(200, -60))),
                walkers(4, WaveSpec.Formation.CONVOY, 2, List.of(new WaveSpec.At(240, -40), new WaveSpec.At(240, 600))),
                wave(6, WaveSpec.Formation.LINE_ABREAST, POD, 2, FRONT, NONE));
        List<Sortie> flown = new ArrayList<>();
        long allocated = Allocations.least(
                () -> {
                    Sortie sortie = sortie(30, waves);
                    flown.add(sortie);
                    return sortie;
                },
                sortie -> {
                    for (int i = 0; i < SimStep.ticks(20); i++) {
                        sortie.step(SortieTest.Pilot.commands(i));
                    }
                });

        assertEquals(0, allocated, "20 s allocated " + allocated + " bytes");
        assertEquals(
                flown.getFirst().stateHash(), flown.getLast().stateHash(), "the same commands give the same state");
    }
}
