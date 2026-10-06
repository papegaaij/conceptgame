package vanguard.content;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Optional;
import org.junit.jupiter.api.Test;
import vanguard.sim.EnemySpec;

/**
 * The act HP factor (design/enemies, Balancing basis; user decision D5 = c of M5 part A): from Act 2
 * on, a returning unit of tier {@code medium} or larger flies with its HP times the reference DPS at
 * the level over that at its first level, on every part and segment, rounded once with the
 * difficulty's HP lever; {@code tiny} and {@code small} units, Act 1's levels and the bounties stay
 * as they are.
 */
class ActHpFactorTest {
    private static final Content CONTENT = ContentLoader.fromClasspath();

    private static EnemySpec at(String slug, Difficulty difficulty, int act, int level) {
        return SimSpecs.enemy(CONTENT, slug, difficulty, Optional.empty(), act, level);
    }

    @Test
    void aReturningMediumUnitGetsTheReferenceDpsRatioFromLevel08() {
        // The Spore Bomber (medium, first at L03, reference 32): ×60/32 at L08, ×80/32 at L14.
        assertEquals(60.0 / 32, SimSpecs.actHpFactor(CONTENT, "spore-bomber", 2, 8), 1e-12);
        assertEquals(45, at("spore-bomber", Difficulty.MEDIUM, 2, 8).hp());
        assertEquals(60, at("spore-bomber", Difficulty.MEDIUM, 2, 14).hp());
        // The Mantis (medium, L06, reference 52) at L14: 26 × 80 / 52 = 40.
        assertEquals(40, at("mantis", Difficulty.MEDIUM, 2, 14).hp());
        // The Scuttler (L04, reference 38) at L13: 28 × 76 / 38 = 56.
        assertEquals(56, at("scuttler", Difficulty.MEDIUM, 2, 13).hp());
    }

    @Test
    void theFactorIsRoundedOnceWithTheDifficultysHpLever() {
        // 24 × 60/32 = 45; easy 45 × 0.75 = 33.75 → 34, hard 45 × 1.3 = 58.5 → 58 (half to even).
        assertEquals(34, at("spore-bomber", Difficulty.EASY, 2, 8).hp());
        assertEquals(58, at("spore-bomber", Difficulty.HARD, 2, 8).hp());
    }

    @Test
    void tinyAndSmallUnitsKeepTheirHp() {
        for (String slug : new String[] {"skitter", "needler"}) {
            assertEquals(1, SimSpecs.actHpFactor(CONTENT, slug, 2, 14), slug);
            assertEquals(
                    SimSpecs.enemy(CONTENT, slug, Difficulty.MEDIUM, Optional.empty())
                            .hp(),
                    at(slug, Difficulty.MEDIUM, 2, 14).hp(),
                    slug);
        }
    }

    @Test
    void actOneLevelsAndAUnitsOwnLevelAreUnchanged() {
        EnemySpec base = SimSpecs.enemy(CONTENT, "mantis", Difficulty.MEDIUM, Optional.empty());
        assertEquals(26, base.hp());
        assertEquals(1, SimSpecs.actHpFactor(CONTENT, "mantis", 1, 7), "Act 1");
        assertEquals(base, at("mantis", Difficulty.MEDIUM, 1, 7));
        assertEquals(1, SimSpecs.actHpFactor(CONTENT, "mantis", 2, 6), "its own first level");
    }

    @Test
    void aChainsHeadSegmentsTailAndRegrownHeadAllScaleButNotTheirBounties() {
        // The Coilwyrm (large, L06): ×80/52 at L14.
        EnemySpec base = SimSpecs.enemy(CONTENT, "coilwyrm", Difficulty.MEDIUM, Optional.empty());
        EnemySpec act2 = at("coilwyrm", Difficulty.MEDIUM, 2, 14);
        EnemySpec.ChainSpec chain = act2.chain().orElseThrow();
        assertEquals(Math.rint(40 * 80.0 / 52), act2.hp(), "head");
        assertEquals(Math.rint(4 * 80.0 / 52), chain.segment().hp(), "segment");
        assertEquals(Math.rint(10 * 80.0 / 52), chain.tail().hp(), "tail");
        assertEquals(Math.rint(20 * 80.0 / 52), chain.regrown().hp(), "regrown head");
        assertEquals(base.bounty(), act2.bounty());
        assertEquals(
                base.chain().orElseThrow().segment().bounty(), chain.segment().bounty());
    }

    @Test
    void aLevelBeyondTheReferenceCurveIsAnError() {
        assertThrows(IllegalArgumentException.class, () -> SimSpecs.actHpFactor(CONTENT, "mantis", 3, 15));
    }
}
