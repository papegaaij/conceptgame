package vanguard.sim;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * The Coilwyrm's chained death (design/enemies/air/coilwyrm, Chained death): after the head dies
 * the members burst one by one from the head down; while they wait for their turn they fly on,
 * but nothing hits them and they pay nothing, and each burst says which member burst.
 */
class ChainDeathTest {
    private static final int FIRE = Command.FIRE.bit();

    private static WaveSpec chainWave(double x) {
        return new WaveSpec(
                1,
                WaveSpec.Formation.SNAKE,
                FarsideTest.COILWYRM,
                1,
                WaveSpec.Entry.FRONT,
                WaveSpec.Edge.NONE,
                Optional.empty(),
                Optional.empty(),
                1,
                Optional.empty(),
                Optional.empty(),
                List.of(),
                Optional.empty(),
                List.of(List.of(new WaveSpec.At(x, -60), new WaveSpec.At(x, 300), new WaveSpec.At(x, 700))),
                Optional.empty());
    }

    private static Sortie sortie() {
        LevelScript level = new LevelScript(
                6,
                1,
                0,
                List.of(new LevelScript.Section(40, 130)),
                List.of(chainWave(PlayField.WIDTH / 2.0)),
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
        return new Sortie(1, TestSpecs.LOADOUT, level, TestSpecs.RULES.withInvulnerableShip(), TestSpecs.FULL_ARMOUR);
    }

    private static int headIndex(Sortie sortie, Chain chain) {
        for (int i = 0; i < sortie.enemyCount(); i++) {
            if (sortie.enemy(i).chain() == chain && sortie.enemy(i).link() == 0) {
                return i;
            }
        }
        throw new AssertionError("no head");
    }

    @Test
    void theMembersWaitingForTheirBurstCannotBeHitAndPayNothing() {
        Sortie sortie = sortie();
        for (int i = 0; i < SimStep.ticks(3.5); i++) {
            sortie.step(Command.NONE);
        }
        Chain chain = sortie.chain(0);
        List<Enemy> members = new ArrayList<>();
        for (int k = 1; k < chain.size(); k++) {
            assertFalse(Chain.doomed(chain.member(k)), "alive before the head dies");
            members.add(chain.member(k));
        }

        sortie.destroyEnemy(headIndex(sortie, chain));
        int credits = sortie.credits();
        for (Enemy member : members) {
            assertTrue(Chain.doomed(member));
            assertFalse(PlayerFire.onField(member), "no shot, blast or special reaches it");
        }

        // The ship fires straight up into the body for the whole ripple: nothing is hit.
        List<Double> widths = new ArrayList<>();
        int previousTick = -1;
        int gaps = 0;
        for (int i = 0; i < SimStep.ticks(2); i++) {
            sortie.step(FIRE);
            for (int e = 0; e < sortie.events().size(); e++) {
                assertFalse(
                        sortie.events().type(e) == SimEvents.Type.ENEMY_HIT
                                || sortie.events().type(e) == SimEvents.Type.ENEMY_DESTROYED,
                        "a waiting member is hit");
                if (sortie.events().type(e) == SimEvents.Type.CHAIN_POP) {
                    int value = sortie.events().value(e);
                    String slug = sortie.enemyKinds()
                            .get(SimEvents.chainPopKind(value))
                            .slug();
                    assertTrue(slug.equals("coilwyrm-segment") || slug.equals("coilwyrm-tail"), slug);
                    widths.add(SimEvents.chainPopWidth(value));
                    if (previousTick >= 0) {
                        assertEquals(SimStep.ticks(0.06), i - previousTick, "one burst every pop interval");
                        gaps++;
                    }
                    previousTick = i;
                }
            }
        }

        assertEquals(credits, sortie.credits(), "the chained bursts pay nothing");
        assertEquals(members.size(), widths.size(), "every member burst");
        assertEquals(members.size() - 1, gaps);
        assertEquals(54 * 0.7, widths.getFirst(), 0.05, "the first segment first");
        for (int k = 1; k < widths.size() - 1; k++) {
            assertTrue(widths.get(k) < widths.get(k - 1), "down the taper");
        }
        assertEquals(28, widths.getLast(), 0.05, "the tail last");
    }

    @Test
    void theHeadlessBodyDriftsToAHaltWhileItBursts() {
        Sortie sortie = sortie();
        for (int i = 0; i < SimStep.ticks(3.5); i++) {
            sortie.step(Command.NONE);
        }
        Chain chain = sortie.chain(0);
        Enemy tail = chain.member(chain.size() - 1);
        sortie.destroyEnemy(headIndex(sortie, chain));
        double startX = tail.x();
        double startY = tail.y();
        double last = 0;
        for (int i = 0; i < SimStep.ticks(0.7); i++) {
            double x = tail.x();
            double y = tail.y();
            sortie.step(Command.NONE);
            last = Math.hypot(tail.x() - x, tail.y() - y);
        }
        double moved = Math.hypot(tail.x() - startX, tail.y() - startY);
        assertTrue(moved < 70, "at 180 px/s it would fly 126 px: " + moved);
        assertTrue(last < 0.5, "slowing to rest (3 px a step at full speed): " + last);
    }

    @Test
    void aChainPopValueCarriesTheKindAndTheWidth() {
        int value = SimEvents.chainPopValue(7, 18.9);
        assertEquals(7, SimEvents.chainPopKind(value));
        assertEquals(18.9, SimEvents.chainPopWidth(value), 1e-9);
    }
}
