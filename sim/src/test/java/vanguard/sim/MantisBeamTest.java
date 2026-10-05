package vanguard.sim;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * The Mantis's beam starts at its eye (design/enemies/air/mantis, round 23: "move the beam to its
 * head"): the sweep's origin, its hit test and its aim, on both edges.
 */
class MantisBeamTest {
    /** The eye in the sweep pose of tools/art/mantis.py: toward the field, down. */
    private static final double IN = 14;

    private static final double DOWN = 25;

    /** The Mantis of {@link FarsideTest} hovering {@code depth} px below the top edge, its eye at the origin. */
    private static EnemySpec mantis(double depth, double in, double down) {
        EnemySpec m = FarsideTest.MANTIS;
        EnemySpec.Sweep s = m.sweep().orElseThrow();
        return new EnemySpec(
                m.slug(),
                m.hp(),
                m.hitbox(),
                m.layer(),
                m.contactDamage(),
                m.destroyedByRamming(),
                m.bounty(),
                m.speed(),
                m.snake(),
                m.streamSpeed(),
                Optional.of(new EnemySpec.Hover(new Range(6, 6), new Range(depth, depth))),
                m.orbit(),
                m.gun(),
                m.drop(),
                m.dive(),
                m.terrain(),
                m.spiral(),
                m.strafe(),
                m.deathBurst(),
                m.sine(),
                m.brood(),
                m.walker(),
                m.sideHover(),
                Optional.of(new EnemySpec.Sweep(
                        s.arcRadians(),
                        s.sweepSeconds(),
                        s.telegraphSeconds(),
                        s.length(),
                        s.width(),
                        s.intervalSeconds(),
                        s.firstDelaySeconds(),
                        s.damage(),
                        in,
                        down)),
                m.chain());
    }

    private static Sortie sortie(EnemySpec mantis, WaveSpec.Formation formation, WaveSpec.Edge edge, int count) {
        WaveSpec wave = new WaveSpec(
                3.5,
                formation,
                mantis,
                count,
                WaveSpec.Entry.SIDES,
                edge,
                Optional.empty(),
                Optional.empty(),
                1,
                Optional.empty(),
                Optional.empty(),
                List.of(),
                Optional.empty());
        LevelScript level = new LevelScript(
                6,
                1,
                0,
                List.of(new LevelScript.Section(40, 130)),
                List.of(wave),
                List.of(),
                List.of(),
                0,
                List.of(),
                new LevelScript.Secondary(0, 50, List.of(), "mantis", List.of(), ""),
                List.of(),
                List.of(),
                List.of(),
                Optional.empty(),
                Optional.empty(),
                List.of(),
                Optional.empty(),
                Optional.empty(),
                List.of(),
                Optional.empty());
        return new Sortie(1, TestSpecs.LOADOUT, level, TestSpecs.RULES, TestSpecs.FULL_ARMOUR);
    }

    /** The sweep hits on a lone Mantis's hover at the left edge, the ship staying at its start. */
    private static int hits(EnemySpec mantis) {
        Sortie sortie = sortie(mantis, WaveSpec.Formation.SINGLE, WaveSpec.Edge.LEFT, 1);
        int hits = 0;
        while (sortie.levelSeconds() < 14) {
            sortie.step(Command.NONE);
            for (int e = 0; e < sortie.events().size(); e++) {
                hits += sortie.events().type(e) == SimEvents.Type.SWEEP_HIT ? 1 : 0;
            }
        }
        return hits;
    }

    @Test
    void theBeamStartsAtTheEyeMirroredOnTheRightEdge() {
        Sortie sortie = sortie(mantis(300, IN, DOWN), WaveSpec.Formation.PINCER, WaveSpec.Edge.NONE, 2);
        int checked = 0;
        while (sortie.levelSeconds() < 14) {
            sortie.step(Command.NONE);
            for (int i = 0; i < sortie.enemyCount(); i++) {
                Enemy enemy = sortie.enemy(i);
                if (enemy.sweeping()) {
                    boolean left = enemy.x() < PlayField.WIDTH / 2.0;
                    assertEquals(left ? IN : -IN, enemy.beamOffsetX(), 1e-9, "toward the field");
                    assertEquals(-DOWN, enemy.beamOffsetY(), 1e-9, "below its centre (the field's y runs up)");
                    checked |= left ? 1 : 2;
                }
            }
        }
        assertEquals(3, checked, "both edges swept");
    }

    @Test
    void theShipJustOutOfReachOfTheCentreIsInReachOfTheEye() {
        // Hovering 184 px below the top edge at x 40, the Mantis's centre is ~328 px from the ship's
        // start (240, 96): its hull stays beyond a 300 px beam from the centre, inside one from the eye.
        assertEquals(0, hits(mantis(184, 0, 0)), "a beam from the centre falls short");
        assertEquals(2, hits(mantis(184, IN, DOWN)), "the beam from the eye reaches it, once per sweep");
    }

    @Test
    void theSweepIsCentredOnTheShipsBearingFromTheEye() {
        Sortie sortie = sortie(mantis(184, IN, DOWN), WaveSpec.Formation.SINGLE, WaveSpec.Edge.LEFT, 1);
        boolean telegraphed = false;
        while (!telegraphed && sortie.levelSeconds() < 14) {
            sortie.step(Command.NONE);
            for (int e = 0; e < sortie.events().size(); e++) {
                if (sortie.events().type(e) == SimEvents.Type.SWEEP_TELEGRAPH) {
                    Enemy mantis = sortie.enemy(0);
                    Ship ship = sortie.ship();
                    double dx = ship.x() - mantis.x() - IN;
                    double dy = ship.y() - mantis.y() + DOWN;
                    // Radians clockwise from straight down, the field's y running up.
                    assertEquals(Math.atan2(-dx, -dy), mantis.sweepCentre(), 1e-6);
                    telegraphed = true;
                }
            }
        }
        assertTrue(telegraphed);
    }
}
