package vanguard.game.render;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class DeathChainTest {
    private static final int TICKS = 180;

    @Test
    void noseDownTheTailBurstsFirstAndTheHeadAtTheChainsEnd() {
        // Parts: a sac near the tail (dy up), the core in the middle, a turret at the head (dy down).
        List<DeathChain.Burst> chain =
                DeathChain.plan(new double[] {0, 0, 0}, new double[] {250, 0, -313}, 0, 626, 288, TICKS);

        assertEquals(3 + DeathChain.HULL_BURSTS, chain.size());
        int tail = at(chain, 0);
        int core = at(chain, 1);
        int head = at(chain, 2);
        assertTrue(tail < core && core < head, tail + " " + core + " " + head);
        assertEquals(TICKS, head, "the head end bursts at the chain's end");
        assertEquals(TICKS / 2, core, 1);
        for (int i = 1; i < chain.size(); i++) {
            assertTrue(chain.get(i - 1).at() <= chain.get(i).at(), "earliest first");
        }
    }

    @Test
    void broadsideWithTheHeadToTheRightTheChainRunsLeftToRight() {
        double broadside = Math.PI / 2;
        List<DeathChain.Burst> chain =
                DeathChain.plan(new double[] {-250, 280}, new double[] {0, 0}, broadside, 626, 288, TICKS);

        assertTrue(at(chain, 0) < at(chain, 1));
        assertEquals(313, DeathChain.headX(broadside, 626), 1e-9);
        assertEquals(0, DeathChain.headY(broadside, 626), 1e-9);
        for (DeathChain.Burst burst : chain) {
            if (burst.part() < 0) {
                assertTrue(Math.abs(burst.dy()) < 288 / 2.0, "the hull bursts lie on the hull");
            }
        }
    }

    private static int at(List<DeathChain.Burst> chain, int part) {
        return chain.stream()
                .filter(b -> b.part() == part)
                .findFirst()
                .orElseThrow()
                .at();
    }
}
