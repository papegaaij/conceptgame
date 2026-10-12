package vanguard.sim;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * M5 part F, the weather (design/campaign Level 12, Weather): the gusts with their ramps and
 * telegraph, the drift, the push on the ship (no banking), on Lampreys but not latched ones, on a
 * Whirl Seed's spiral centre, the lightning events; exactly no push and nothing hashed without it.
 */
class WindTest {
    /** Level 12's medium numbers: gusts of 40 px/s for 2.5 s with 0.25 s ramps, telegraphed 1 s ahead. */
    static LevelScript.Weather storm(
            List<LevelScript.Gust> gusts, List<LevelScript.Drift> drifts, List<Double> flashes) {
        return new LevelScript.Weather(gusts, 2.5, 0.25, 40, 1.0, drifts, flashes);
    }

    private static Sortie sortie(LevelScript level) {
        return new Sortie(1, TestSpecs.LOADOUT, level, TestSpecs.RULES, TestSpecs.FULL_ARMOUR);
    }

    @Test
    void aGustRampsUpHoldsAndRampsDownAndIsTelegraphedAhead() {
        Wind wind = new Wind(Optional.of(storm(List.of(new LevelScript.Gust(2, true)), List.of(), List.of())));
        int start = SimStep.ticks(2);
        assertEquals(0, wind.push(start - 1));
        assertEquals(0, wind.push(start));
        assertEquals(40.0 * 6 / 15, wind.push(start + 6), 1e-9);
        assertEquals(40, wind.push(start + SimStep.ticks(1)), 1e-9);
        assertEquals(0, wind.push(start + SimStep.ticks(2.5)));
        assertEquals(0, wind.telegraph(start - SimStep.ticks(1) - 1));
        assertEquals(1, wind.telegraph(start - SimStep.ticks(1)));
        assertEquals(0, wind.telegraph(start + SimStep.ticks(0.5)));
        Wind right = new Wind(Optional.of(storm(List.of(new LevelScript.Gust(2, false)), List.of(), List.of())));
        assertEquals(-40, right.push(start + SimStep.ticks(1)), 1e-9);
        assertEquals(-1, right.telegraph(start - 1));
    }

    @Test
    void aDriftAddsToTheGusts() {
        Wind wind = new Wind(Optional.of(storm(
                List.of(new LevelScript.Gust(5, true)), List.of(new LevelScript.Drift(4, 10, 20, true)), List.of())));
        assertEquals(20, wind.push(SimStep.ticks(4.5)), 1e-9);
        assertEquals(60, wind.push(SimStep.ticks(6)), 1e-9);
        assertEquals(0, wind.push(SimStep.ticks(10)));
    }

    @Test
    void withoutWeatherThereIsNoPushAtAll() {
        Wind none = new Wind(Optional.empty());
        for (int tick = 0; tick < 1000; tick += 7) {
            assertEquals(0, Double.doubleToRawLongBits(none.push(tick)));
        }
        assertEquals(0, none.telegraph(100));
    }

    @Test
    void theGustPushesTheShipWithoutBankingIt() {
        LevelScript level = TestSpecs.level(10, List.of())
                .withWeather(storm(List.of(new LevelScript.Gust(1, true)), List.of(), List.of()));
        Sortie sortie = sortie(level);
        double x = sortie.ship().x();
        for (int i = 0; i < SimStep.ticks(5); i++) {
            sortie.step(Command.NONE);
            assertEquals(0, sortie.ship().bank());
            assertEquals(0, sortie.ship().vx());
        }
        // The full push for 2 s plus the two ramps' half: 40 x 2.25 = 90 px.
        assertEquals(x + 90, sortie.ship().x(), 1e-6);
    }

    @Test
    void theEdgesStillClampThePushedShip() {
        LevelScript level = TestSpecs.level(30, List.of())
                .withWeather(storm(List.of(), List.of(new LevelScript.Drift(0, 30, 400, true)), List.of()));
        Sortie sortie = sortie(level);
        for (int i = 0; i < SimStep.ticks(5); i++) {
            sortie.step(Command.NONE);
        }
        assertEquals(PlayField.WIDTH - TestSpecs.SHIP.edgeLimit(), sortie.ship().x(), 1e-9);
    }

    @Test
    void lampreysTakeTheWindButNotWhileLatched() {
        WaveSpec lamprey = TestSpecs.wave(
                0, WaveSpec.Formation.SINGLE, LampreyTest.LAMPREY, 1, WaveSpec.Entry.FRONT, WaveSpec.Edge.NONE);
        LevelScript calm = TestSpecs.level(30, List.of(lamprey));
        LevelScript windy =
                calm.withWeather(storm(List.of(), List.of(new LevelScript.Drift(0, 30, 30, false)), List.of()));
        Sortie still = sortie(calm);
        Sortie blown = sortie(windy);
        // The ship holds against the drift by steering, so both ships stay put for a while: compare one step.
        still.step(Command.NONE);
        blown.step(Command.NONE);
        still.step(Command.NONE);
        blown.step(Command.NONE);
        double drift = blown.enemy(0).x() - still.enemy(0).x();
        assertTrue(drift < 0, "pushed to the left: " + drift);
        for (int i = 0; i < 600 && blown.latched() == 0; i++) {
            blown.step(Command.NONE);
        }
        assertEquals(1, blown.latched());
        blown.step(Command.NONE);
        assertEquals(blown.ship().x() - 22, blown.enemy(0).x(), 1e-9, "latched it sits on its spot");
    }

    @Test
    void aWhirlClustersCentreDriftsOnTheWind() {
        EnemySpec seed = new EnemySpec(
                "whirl-seed",
                1,
                new Hitbox(14, 14),
                Layer.AIR,
                4,
                true,
                3,
                150,
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                false,
                Optional.of(new EnemySpec.Spiral(3, 0.5, 40, 60, 0)),
                Optional.empty(),
                Optional.empty());
        WaveSpec cluster = new WaveSpec(
                0,
                WaveSpec.Formation.WHIRL_CLUSTER,
                seed,
                4,
                WaveSpec.Entry.SIDES,
                WaveSpec.Edge.LEFT,
                Optional.empty(),
                Optional.empty(),
                1,
                Optional.empty(),
                Optional.empty(),
                List.of(),
                Optional.of(new WaveSpec.At(-20, 200)),
                List.of(),
                Optional.empty(),
                "");
        LevelScript calm = TestSpecs.level(30, List.of(cluster));
        LevelScript windy =
                calm.withWeather(storm(List.of(), List.of(new LevelScript.Drift(0, 30, 40, true)), List.of()));
        Sortie still = sortie(calm);
        Sortie blown = sortie(windy);
        for (int i = 0; i < 120; i++) {
            still.step(Command.NONE);
            blown.step(Command.NONE);
        }
        // Every seed, the centre with it, drifted right by the push since its release (within 0.3 s of the
        // cluster's), at most the push for its 2 s.
        assertEquals(4, still.enemyCount());
        for (int k = 0; k < still.enemyCount(); k++) {
            double drift = blown.enemy(k).x() - still.enemy(k).x();
            assertTrue(drift > 40 * (2 - 0.125 - 0.4) && drift <= 40 * 2, "drift " + drift);
        }
    }

    @Test
    void lightningRaisesItsEventsAndIsHashedOnlyWithWeather() {
        LevelScript level =
                TestSpecs.level(10, List.of()).withWeather(storm(List.of(), List.of(), List.of(1.0, 1.0, 3.0)));
        Sortie sortie = sortie(level);
        int flashes = 0;
        int last = -1;
        for (int i = 0; i < SimStep.ticks(4); i++) {
            sortie.step(Command.NONE);
            SimEvents events = sortie.events();
            for (int e = 0; e < events.size(); e++) {
                if (events.type(e) == SimEvents.Type.LIGHTNING) {
                    assertEquals(last + 1, events.value(e));
                    last = events.value(e);
                    flashes++;
                }
            }
        }
        assertEquals(3, flashes);
        Sortie plain = sortie(TestSpecs.level(10, List.of()));
        Sortie calm = sortie(TestSpecs.level(10, List.of()).withWeather(storm(List.of(), List.of(), List.of())));
        plain.step(Command.NONE);
        calm.step(Command.NONE);
        assertTrue(plain.stateHash() != calm.stateHash(), "a weather level hashes its flash counter");
    }

    @Test
    void theWindPushesTheEscortAsThePlayer() {
        Pool<Enemy> enemies = new Pool<>(1, Enemy::new, Enemy[]::new);
        Pool<EnemyBullet> bullets = new Pool<>(1, EnemyBullet::new, EnemyBullet[]::new);
        Wingman calm = new Wingman(WingmanTest.rook(WingmanSpec.Side.LEFT), 5);
        Wingman blown = new Wingman(WingmanTest.rook(WingmanSpec.Side.LEFT), 5);
        calm.reset(80, Ship.START_X, Ship.START_Y);
        blown.reset(80, Ship.START_X, Ship.START_Y);
        blown.wind(60);
        for (int i = 0; i < 10; i++) {
            calm.fly(Ship.START_X, Ship.START_Y, 0, enemies, bullets, new SetPiece[0], -1);
            blown.fly(Ship.START_X, Ship.START_Y, 0, enemies, bullets, new SetPiece[0], -1);
        }
        assertTrue(blown.x() > calm.x() + 1, "pushed right while he steers back: " + (blown.x() - calm.x()));
        assertEquals(calm.y(), blown.y(), 1e-9);
    }

    @Test
    void theWindDoesNotAllocate() {
        LevelScript level = TestSpecs.level(30, List.of())
                .withWeather(storm(
                        List.of(new LevelScript.Gust(1, true), new LevelScript.Gust(4, false)),
                        List.of(new LevelScript.Drift(2, 8, 20, true)),
                        List.of(1.5, 3.0, 6.0)));
        long allocated = Allocations.least(() -> sortie(level), sortie -> {
            for (int i = 0; i < 600; i++) {
                sortie.step(Command.NONE);
            }
        });
        assertEquals(0, allocated);
    }
}
