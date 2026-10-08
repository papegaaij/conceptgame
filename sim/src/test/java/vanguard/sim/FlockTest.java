package vanguard.sim;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static vanguard.sim.PartCSpecs.count;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * The Mote Swarm's flock (design/enemies/air/mote-swarm, user decision D8 = a of M5 part D): a
 * boids cloud round a leader point flying the wave's route, deterministic and allocation-free; its
 * loop-backs warned at the bottom edge and raising {@code LOOP_BACK}; every mote an ordinary unit.
 */
class FlockTest {
    private static Sortie swarm(int count, int loops) {
        return PartDSpecs.sortie(PartDSpecs.level(30, List.of(PartDSpecs.swarm(1, count, loops))));
    }

    /** The members on the field of the first flock. */
    private static List<Enemy> members(Sortie sortie) {
        List<Enemy> members = new ArrayList<>();
        if (sortie.flockCount() == 0) {
            return members;
        }
        Flock flock = sortie.flock(0);
        for (int i = 0; i < flock.size(); i++) {
            if (flock.member(i) != null) {
                members.add(flock.member(i));
            }
        }
        return members;
    }

    @Test
    void theSameSeedFliesTheSameFlock() {
        Sortie a = swarm(20, 2);
        Sortie b = swarm(20, 2);
        for (int step = 0; step < SimStep.ticks(16); step++) {
            a.step(Command.NONE);
            b.step(Command.NONE);
            assertEquals(a.stateHash(), b.stateHash(), "at step " + step);
        }
    }

    @Test
    void everyMoteIsAUnitOfTheLevel() {
        Sortie sortie = swarm(20, 1);
        for (int step = 0; step < SimStep.ticks(1.5); step++) {
            sortie.step(Command.NONE);
        }

        assertEquals(20, sortie.enemyTotal(), "the density counts every mote");
        assertEquals(1, sortie.flockCount());
        assertEquals(20, PartDSpecs.all(sortie, "mote-swarm").size());
        assertEquals(20, members(sortie).size());
    }

    @Test
    void theMotesKeepApartAndStayACloudRoundTheLeaderPoint() {
        Sortie sortie = swarm(20, 0);
        EnemySpec.FlockSpec spec = PartDSpecs.FLOCK;
        int checked = 0;
        double nearestSum = 0;
        int nearestCount = 0;
        for (int step = 0; step < SimStep.ticks(6); step++) {
            sortie.step(Command.NONE);
            List<Enemy> motes = members(sortie);
            if (motes.size() < 20 || !motes.stream().allMatch(m -> PlayField.overlaps(m.x(), m.y(), m.hitbox()))) {
                continue;
            }
            checked++;
            Flock flock = sortie.flock(0);
            for (Enemy mote : motes) {
                double nearest = Double.MAX_VALUE;
                for (Enemy other : motes) {
                    if (other != mote) {
                        nearest = Math.min(nearest, Math.hypot(mote.x() - other.x(), mote.y() - other.y()));
                    }
                }
                assertTrue(nearest >= spec.separation() / 2, "no clumping: " + nearest + " px at step " + step);
                nearestSum += nearest;
                nearestCount++;
                double toLeader = Math.hypot(mote.x() - flock.leaderX(), mote.y() - flock.leaderY());
                assertTrue(toLeader <= 2 * spec.radius(), "a cloud round the leader point: " + toLeader);
            }
        }

        assertTrue(checked > SimStep.ticks(3), "the whole swarm was on the screen a while: " + checked);
        double mean = nearestSum / nearestCount;
        assertTrue(mean >= 0.7 * spec.separation(), "about the separation apart: " + mean);
    }

    @Test
    void eachLoopBackIsWarnedAtTheBottomEdgeAndDivesAtItsDiveSpeed() {
        Sortie sortie = swarm(20, 2);
        List<Integer> loopBacks = new ArrayList<>();
        List<Integer> values = new ArrayList<>();
        List<Integer> warned = new ArrayList<>();
        double cruise = 0;
        int cruiseSteps = 0;
        double dive = 0;
        int diveSteps = 0;
        for (int step = 0; step < SimStep.ticks(16); step++) {
            List<double[]> before = new ArrayList<>();
            for (Enemy mote : members(sortie)) {
                before.add(new double[] {mote.x(), mote.y()});
            }
            int loopsBefore = sortie.flockCount() > 0 ? sortie.flock(0).loopsFlown() : 0;
            sortie.step(Command.NONE);
            if ((sortie.edgeWarnings() & WarningEdge.BOTTOM.bit()) != 0) {
                warned.add(step);
            }
            for (int k = 0; k < sortie.events().size(); k++) {
                if (sortie.events().type(k) == SimEvents.Type.LOOP_BACK) {
                    loopBacks.add(step);
                    values.add(sortie.events().value(k));
                }
            }
            List<Enemy> motes = members(sortie);
            if (motes.size() == before.size()
                    && sortie.flockCount() > 0
                    && sortie.flock(0).loopsFlown() == loopsBefore) {
                for (int m = 0; m < motes.size(); m++) {
                    double moved = Math.hypot(
                                    motes.get(m).x() - before.get(m)[0],
                                    motes.get(m).y() - before.get(m)[1])
                            * SimStep.PER_SECOND;
                    if (loopsBefore == 0) {
                        cruise += moved;
                        cruiseSteps++;
                    } else {
                        dive += moved;
                        diveSteps++;
                    }
                }
            }
            if (sortie.flockCount() > 0) {
                assertEquals(
                        loopBacks.isEmpty() ? WaveSpec.Entry.FRONT : WaveSpec.Entry.REAR,
                        sortie.flock(0).entry(),
                        "a rear wave for Trail from its first loop-back");
            }
        }

        assertEquals(List.of(1, 2), values, "two loop-backs, numbered");
        for (int at : loopBacks) {
            for (int step = at - SimStep.ticks(3) + 1; step < at; step++) {
                assertTrue(warned.contains(step), "the bottom edge warned at " + step + ", 3 s ahead of " + at);
            }
        }
        assertFalse(warned.contains(loopBacks.getFirst() + 1), "the warning ends at the re-entry");
        assertEquals(200, cruise / cruiseSteps, 30, "its cruise");
        assertEquals(260, dive / diveSteps, 40, "diving after a loop-back");
        assertTrue(dive / diveSteps > cruise / cruiseSteps + 30, "faster after its loop-back");
    }

    @Test
    void aKilledMoteDropsOutAndTheRestFlyOn() {
        Sortie sortie = swarm(20, 1);
        for (int step = 0; step < SimStep.ticks(2); step++) {
            sortie.step(Command.NONE);
        }
        Flock flock = sortie.flock(0);
        int killed = 0;
        int firstCredits = 0;
        for (int round = 0; round < 5; round++) {
            for (int i = 0; i < sortie.enemyCount(); i++) {
                if (sortie.enemy(i).flock() == flock && sortie.enemy(i).member() == 2 * round) {
                    sortie.destroyEnemy(i);
                    killed++;
                    break;
                }
            }
            firstCredits = round == 0 ? sortie.credits() : firstCredits;
        }

        assertEquals(5, killed);
        assertEquals(5, sortie.kills(), "each mote a kill of its own");
        assertTrue(firstCredits > 0);
        assertEquals(5 * firstCredits, sortie.credits(), "each pays its bounty");
        assertTrue(sortie.chain() >= 5, "a chain step each: " + sortie.chain());
        for (int round = 0; round < 5; round++) {
            assertNull(flock.member(2 * round), "member " + 2 * round + " dropped out");
        }
        assertEquals(15, members(sortie).size());
        for (int step = 0; step < SimStep.ticks(2); step++) {
            sortie.step(Command.NONE);
        }
        assertEquals(15, members(sortie).size(), "the rest fly on");
        assertEquals(15, PartDSpecs.all(sortie, "mote-swarm").size());
    }

    @Test
    void theSwarmLeavesOnceItHasFlownItsCourse() {
        Sortie sortie = swarm(20, 1);
        boolean seen = false;
        int step = 0;
        for (; step < SimStep.ticks(25) && (!seen || sortie.flockCount() > 0); step++) {
            sortie.step(Command.NONE);
            seen |= sortie.flockCount() > 0;
        }

        assertTrue(seen);
        assertEquals(0, sortie.flockCount(), "gone after its loop-back's path");
        assertEquals(0, sortie.enemyCount());
        assertEquals(0, sortie.kills(), "they escaped");
    }

    @Test
    void aFlockDoesNotAllocate() {
        long allocated = Allocations.least(
                () -> PartDSpecs.sortie(PartDSpecs.level(
                        30,
                        List.of(
                                PartDSpecs.swarm(1, 24, 2),
                                PartDSpecs.ambush(2, PartDSpecs.wraith(), 4),
                                PartDSpecs.swarm(4, 12, 1)))),
                sortie -> {
                    for (int step = 0; step < SimStep.ticks(16); step++) {
                        sortie.step(
                                step % 120 < 60
                                        ? Command.FIRE.bit() | Command.LEFT.bit()
                                        : Command.FIRE.bit() | Command.RIGHT.bit());
                    }
                });

        assertEquals(0, allocated);
    }

    @Test
    void aMoteThatTouchesTheShipDealsTinyContactAndDies() {
        // The route straight down through the ship's lane.
        WaveSpec wave = new WaveSpec(
                1,
                WaveSpec.Formation.SWARM,
                PartDSpecs.MOTE,
                6,
                WaveSpec.Entry.FRONT,
                WaveSpec.Edge.NONE,
                java.util.Optional.empty(),
                java.util.Optional.empty(),
                1,
                java.util.Optional.empty(),
                java.util.Optional.empty(),
                List.of(),
                java.util.Optional.empty(),
                List.of(List.of(new WaveSpec.At(240, -60), new WaveSpec.At(240, 700))));
        Sortie sortie = new Sortie(
                1, TestSpecs.LOADOUT, PartDSpecs.level(30, List.of(wave)), TestSpecs.RULES, TestSpecs.FULL_ARMOUR);
        Defences defences = sortie.ship().defences();
        int rammed = 0;
        for (int step = 0; step < SimStep.ticks(6); step++) {
            double before = defences.shield() + defences.armour();
            sortie.step(Command.NONE);
            int now = count(sortie, SimEvents.Type.ENEMY_DESTROYED);
            if (now > 0 && rammed == 0) {
                // The first contact: `tiny` (6); the armour's mercy window spares the ship the others.
                assertEquals(6, before - (defences.shield() + defences.armour()), 0.1, "`tiny` contact");
            }
            rammed += now;
        }

        assertTrue(rammed > 0, "some motes rammed the ship and died");
        assertEquals(6 - rammed, PartDSpecs.all(sortie, "mote-swarm").size(), "each destroyed by the impact");
    }
}
