package vanguard.sim;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static vanguard.sim.TestSpecs.FULL_ARMOUR;
import static vanguard.sim.TestSpecs.INFINITE;
import static vanguard.sim.TestSpecs.LOADOUT;
import static vanguard.sim.TestSpecs.RULES;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * The Creeper (design/enemies/ground/creeper, M5 part B): a walker whose five-way fan is aimed at the
 * player, the units of a convoy sharing a volley clock that ripples their fans 0.5 s apart, front to
 * back, a unit off the screen skipping its turn.
 */
class CreeperTest {
    private static final EnemyGun FAN =
            new EnemyGun(3.0, 0, 1, 140, 4, false, 5, Math.toRadians(50), INFINITE, INFINITE);
    private static final EnemySpec CREEPER = creeper(0.5);

    private static EnemySpec creeper(double stagger) {
        return new EnemySpec(
                "creeper",
                30,
                new Hitbox(40, 44),
                Layer.GROUND,
                15,
                false,
                22,
                35,
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
                Optional.of(new EnemySpec.Walker(35, Math.toRadians(90), 22, 0, Optional.empty(), 0, true, stagger)));
    }

    /** A convoy of {@code count} walking from the left edge across the upper screen (screen points, y down). */
    private static WaveSpec convoy(double t, EnemySpec enemy, int count) {
        return new WaveSpec(
                t,
                WaveSpec.Formation.CONVOY,
                enemy,
                count,
                WaveSpec.Entry.FRONT,
                WaveSpec.Edge.NONE,
                Optional.empty(),
                Optional.empty(),
                1,
                Optional.empty(),
                Optional.empty(),
                List.of(),
                Optional.empty(),
                List.of(List.of(new WaveSpec.At(-40, 150), new WaveSpec.At(PlayField.WIDTH + 200, 150))));
    }

    /** A slow-scrolling level, so the convoy stays on the screen; nothing hurts the ship. */
    private static Sortie sortie(List<WaveSpec> waves) {
        LevelScript level = new LevelScript(
                8,
                2,
                0,
                List.of(new LevelScript.Section(30, 5)),
                waves,
                List.of(),
                List.of(),
                0,
                List.of(),
                new LevelScript.Secondary(0, 50, List.of(), "creeper"),
                List.of(),
                List.of(),
                List.of());
        return new Sortie(1, LOADOUT, level, RULES.withInvulnerableShip(), FULL_ARMOUR);
    }

    /** A fan fired: its step and the firing unit's x. */
    private record Volley(int tick, double x) {}

    private static List<Volley> volleys(Sortie sortie, int steps) {
        List<Volley> fired = new ArrayList<>();
        for (int tick = 0; tick < steps; tick++) {
            sortie.step(Command.NONE);
            SimEvents events = sortie.events();
            for (int i = 0; i < events.size(); i++) {
                if (events.type(i) == SimEvents.Type.ENEMY_FIRED) {
                    fired.add(new Volley(tick, events.x(i)));
                }
            }
        }
        return fired;
    }

    @Test
    void aConvoysFansRippleFrontToBackAndAUnitOffTheScreenSkipsItsTurn() {
        // Three units 1.5 s apart: each is on the screen 20 px (0.57 s) after it starts walking in.
        // The clock starts with the first; volleys at 1.5 s and 4.5 s after that, unit i i × 0.5 s
        // later. In the first volley the third unit is still off the screen (it comes on at 3.0 s).
        List<Volley> fired = volleys(sortie(List.of(convoy(0, CREEPER, 3))), SimStep.ticks(8));

        int first = fired.getFirst().tick();
        int half = SimStep.ticks(0.5);
        int interval = SimStep.ticks(3.0);
        assertEquals(
                List.of(first, first + half, first + interval, first + interval + half, first + interval + 2 * half),
                fired.stream().map(Volley::tick).toList());
        assertEquals(SimStep.ticks(0.57 + 1.5), first, 2, "half an interval after the first comes on");
        assertTrue(fired.get(0).x() > fired.get(1).x(), "front first");
        assertTrue(
                fired.get(2).x() > fired.get(3).x()
                        && fired.get(3).x() > fired.get(4).x(),
                "front to back");
    }

    @Test
    void withoutAStaggerEachUnitKeepsItsOwnClock() {
        List<Volley> fired = volleys(sortie(List.of(convoy(0, creeper(0), 2))), SimStep.ticks(6));

        int first = fired.getFirst().tick();
        // The second unit comes on about 1.5 s after the first (the scroll moves its path a step), so
        // its clock runs that far behind.
        assertEquals(3, fired.size());
        assertEquals(first + SimStep.ticks(1.5), fired.get(1).tick(), 1);
        assertEquals(first + SimStep.ticks(3.0), fired.get(2).tick());
    }

    @Test
    void itsFanIsAimedAtTheShipNotAlongItsFacing() {
        Sortie sortie = sortie(List.of(convoy(0, CREEPER, 1)));
        for (int tick = 0; tick < SimStep.ticks(3) && sortie.bulletCount() == 0; tick++) {
            sortie.step(Command.NONE);
        }
        assertEquals(5, sortie.bulletCount());
        Enemy creeper = sortie.enemy(0);
        // It walks right (facing -90°); the ship is below it.
        assertEquals(-Math.PI / 2, creeper.facing(), 1e-6);
        double toShip =
                Math.atan2(sortie.ship().y() - creeper.y(), sortie.ship().x() - creeper.x());
        EnemyBullet middle = sortie.bullet(2);
        assertEquals(toShip, Math.atan2(middle.vy(), middle.vx()), 1e-6, "the middle bullet goes at the ship");
    }

    @Test
    void aKilledCreeperLeavesItsHuskAtItsLastHeading() {
        Sortie sortie = sortie(List.of(convoy(0, CREEPER, 1)));
        for (int tick = 0; tick < SimStep.ticks(1); tick++) {
            sortie.step(Command.NONE);
        }
        double facing = sortie.enemy(0).facing();
        sortie.destroyEnemy(0);

        SimEvents events = sortie.events();
        int down = -1;
        for (int i = 0; i < events.size(); i++) {
            down = events.type(i) == SimEvents.Type.WALKER_DOWN ? i : down;
        }
        assertTrue(down >= 0, "the husk's event");
        assertEquals(0, Math.IEEEremainder(SimEvents.walkerFacing(events.value(down)) - facing, 2 * Math.PI), 1e-3);
    }

    @Test
    void aWaveWhoseRippleOutlastsTheIntervalIsRejected() {
        // Seven units 0.5 s apart take 3.0 s: as long as the interval, so the ripple would overlap.
        assertThrows(IllegalArgumentException.class, () -> sortie(List.of(convoy(0, CREEPER, 7))));
    }

    @Test
    void twoConvoysStepDeterministicallyWithoutAllocating() {
        List<WaveSpec> waves = List.of(convoy(0, CREEPER, 5), convoy(4, CREEPER, 3));
        List<Sortie> flown = new ArrayList<>();
        long allocated = Allocations.least(
                () -> {
                    Sortie sortie = sortie(waves);
                    flown.add(sortie);
                    return sortie;
                },
                sortie -> {
                    for (int i = 0; i < SimStep.ticks(20); i++) {
                        sortie.step(SortieTest.Pilot.commands(i));
                    }
                });

        assertEquals(0, allocated, "20 s allocated " + allocated + " bytes");
        assertEquals(flown.getFirst().stateHash(), flown.getLast().stateHash());
    }
}
