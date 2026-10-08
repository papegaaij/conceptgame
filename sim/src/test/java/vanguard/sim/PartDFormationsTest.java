package vanguard.sim;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * M5 part D's formations (design/enemies, formation vocabulary): the {@code swarm} (a flock round
 * its route's leader point, with its loop-backs), the {@code rear ambush} (1–4 lanes across the
 * bottom edge, warned ahead of the re-entry) and the authored route of a {@code snake} (the
 * Skitter's authored-paths item).
 */
class PartDFormationsTest {
    private static List<Spawn> plan(WaveSpec wave) {
        List<Spawn> spawns = new ArrayList<>();
        Formations.plan(wave, 0, new SplitMix64(1), spawns);
        return spawns;
    }

    @Test
    void aSwarmsMembersEnterTogetherInACloudRoundItsRoutesStart() {
        List<Spawn> spawns = plan(PartDSpecs.swarm(10, 20, 2));

        assertEquals(20, spawns.size());
        Spawn.Route route = spawns.getFirst().swarm().orElseThrow().route();
        for (int i = 0; i < spawns.size(); i++) {
            Spawn.Swarm member = spawns.get(i).swarm().orElseThrow();
            assertEquals(SimStep.ticks(10), spawns.get(i).tick());
            assertEquals(i, member.member(), "in entry order");
            assertSame(route, member.route(), "one route for the wave");
            double fromStart = Math.hypot(member.x() - 80, member.y() - (PlayField.HEIGHT + 60));
            assertTrue(fromStart <= 3 * PartDSpecs.FLOCK.separation(), "round the route's first point: " + fromStart);
        }
        assertEquals(2, route.loops().size(), "two loop-backs");
        assertEquals(200, route.speed(), "the leader point at the unit's path speed");
        assertEquals(260, route.diveSpeed(), "and its loop-backs at the dive speed");
        assertEquals(
                SimStep.ticks(route.path().length() / 200 + 1.5), route.reentryAfter(0), "1.5 s past the route's end");
        assertEquals(
                SimStep.ticks(route.path().length() / 200
                        + 1.5
                        + route.loops().getFirst().length() / 260
                        + 1.5),
                route.reentryAfter(1));
    }

    @Test
    void aSwarmNeedsItsRouteAndAtMostTheFlocksMembers() {
        WaveSpec noRoute = new WaveSpec(
                0,
                WaveSpec.Formation.SWARM,
                PartDSpecs.MOTE,
                6,
                WaveSpec.Entry.FRONT,
                WaveSpec.Edge.NONE,
                Optional.empty(),
                Optional.empty(),
                1,
                Optional.empty(),
                Optional.empty(),
                List.of());
        assertThrows(IllegalArgumentException.class, () -> plan(noRoute));
        assertThrows(IllegalArgumentException.class, () -> plan(PartDSpecs.swarm(0, 25, 0)));
        assertThrows(IllegalArgumentException.class, () -> plan(PartDSpecs.ambush(0, PartDSpecs.MOTE, 2)));
    }

    @Test
    void aRearAmbushSpreadsItsLanesAcrossTheBottomEdge() {
        assertEquals(List.of(240.0), lanes(1));
        assertEquals(List.of(160.0, 320.0), lanes(2));
        assertEquals(List.of(120.0, 240.0, 360.0), lanes(3));
        assertEquals(List.of(96.0, 192.0, 288.0, 384.0), lanes(4));
        assertThrows(IllegalArgumentException.class, () -> lanes(5));
    }

    /** The lanes of a rear ambush of {@code count}: each enters at the top in its lane and rises in it. */
    private static List<Double> lanes(int count) {
        List<Double> lanes = new ArrayList<>();
        for (Spawn spawn : plan(PartDSpecs.ambush(5, PartDSpecs.wraith(), count))) {
            Spawn.Ambush ambush = spawn.ambush().orElseThrow();
            double x = spawn.path().x(0, 0);
            assertEquals(PlayField.HEIGHT + 40, spawn.path().y(0, 0), "it enters at the top edge");
            assertEquals(x, ambush.rise().x(0, 0), "it comes back up its own lane");
            assertEquals(
                    x <= PlayField.WIDTH / 2.0 ? 40 : PlayField.WIDTH - 40,
                    ambush.exit().endX(),
                    "the nearer side lane");
            assertEquals(0.4, ambush.flashSeconds());
            assertEquals(2.5, ambush.holdSeconds());
            assertEquals(1.5, ambush.gapSeconds());
            assertEquals(120, ambush.exitSpeed());
            assertEquals(SimStep.ticks(5), spawn.tick(), "every unit at the wave's time");
            lanes.add(x);
        }
        return lanes;
    }

    @Test
    void aRearAmbushIsWarnedAheadOfItsReEntryNotOfItsTime() {
        WaveSpec wave = PartDSpecs.ambush(10, PartDSpecs.wraith(), 2);
        WaveSchedule schedule = new WaveSchedule(List.of(wave), List.of(wave.enemy()), new SplitMix64(1));
        Spawn spawn = plan(wave).getFirst();
        int reentry = spawn.ambushReentryTick();
        int bottom = WarningEdge.BOTTOM.bit();

        assertEquals(
                SimStep.ticks(10) + SimStep.ticks((PlayField.HEIGHT + 80) / 200.0 + 1.5),
                reentry,
                "its swoop down the screen and its gap");
        assertEquals(0, schedule.warnings(SimStep.ticks(10) - SimStep.ticks(3)) & bottom, "not warned at its time");
        assertEquals(0, schedule.warnings(reentry - SimStep.ticks(3) - 1) & bottom);
        assertEquals(bottom, schedule.warnings(reentry - SimStep.ticks(3)) & bottom, "3 s ahead of its re-entry");
        assertEquals(bottom, schedule.warnings(reentry - 1) & bottom);
        assertEquals(0, schedule.warnings(reentry) & bottom, "until it re-enters");
    }

    @Test
    void aSwarmsLoopBacksAreWarnedAtTheBottomEdge() {
        WaveSpec wave = PartDSpecs.swarm(10, 12, 2);
        WaveSchedule schedule = new WaveSchedule(List.of(wave), List.of(wave.enemy()), new SplitMix64(1));
        Spawn.Route route = plan(wave).getFirst().swarm().orElseThrow().route();
        int bottom = WarningEdge.BOTTOM.bit();

        for (int k = 0; k < 2; k++) {
            int reentry = SimStep.ticks(10) + route.reentryAfter(k);
            assertEquals(bottom, schedule.warnings(reentry - SimStep.ticks(3)) & bottom, "loop-back " + k);
            assertEquals(0, schedule.warnings(reentry) & bottom);
        }
        assertEquals(0, schedule.warnings(SimStep.ticks(10) - 1) & bottom, "a front swarm is not warned at its time");
        assertEquals(12, schedule.units(), "every mote a unit");
    }

    /** A snake of Skitters on an authored route, {@code from} an edge. */
    private static WaveSpec snake(WaveSpec.Entry from, List<List<WaveSpec.At>> paths) {
        return new WaveSpec(
                3,
                WaveSpec.Formation.SNAKE,
                TestSpecs.SKITTER,
                4,
                from,
                WaveSpec.Edge.NONE,
                Optional.empty(),
                Optional.empty(),
                1,
                Optional.empty(),
                Optional.empty(),
                List.of(),
                Optional.empty(),
                paths);
    }

    @Test
    void aSnakeFliesItsAuthoredRouteFromAnyEdge() {
        List<WaveSpec.At> route =
                List.of(new WaveSpec.At(100, 580), new WaveSpec.At(300, 300), new WaveSpec.At(200, -60));
        List<Spawn> spawns = plan(snake(WaveSpec.Entry.REAR, List.of(route)));

        assertEquals(4, spawns.size());
        for (int i = 0; i < 4; i++) {
            Spawn spawn = spawns.get(i);
            assertEquals(100, spawn.path().x(0, 0), 1e-9, "every unit on the route");
            assertEquals(PlayField.HEIGHT - 580, spawn.path().y(0, 0), 1e-9, "from below the bottom edge");
            assertEquals(200, spawn.path().endX(), 1e-9);
            assertEquals(SimStep.ticks(3 + i * 0.25), spawn.tick(), "its snake spacing apart");
        }
        assertThrows(
                IllegalArgumentException.class,
                () -> plan(snake(WaveSpec.Entry.REAR, List.of())),
                "no rear snake without a route");
        // Without a route the laid-out shape as before.
        assertEquals(
                330,
                plan(snake(WaveSpec.Entry.FRONT, List.of())).getFirst().path().x(0, 0),
                1e-9);
    }
}
