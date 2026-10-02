package vanguard.content;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import vanguard.sim.EnemyGun;
import vanguard.sim.EnemySpec;
import vanguard.sim.Hitbox;
import vanguard.sim.Hull;
import vanguard.sim.Layer;
import vanguard.sim.LevelScript;
import vanguard.sim.Loadout;
import vanguard.sim.PickupType;
import vanguard.sim.Plating;
import vanguard.sim.PulseCannon;
import vanguard.sim.Range;
import vanguard.sim.Rules;
import vanguard.sim.ShieldModel;
import vanguard.sim.ShipSpec;
import vanguard.sim.WaveSpec;

/** The specs built from the design data carry the numbers of the documents, with the difficulty levers applied. */
class SimSpecsTest {
    private static final String LEVEL = Level01Test.LEVEL;
    private final Content content = ContentLoader.fromClasspath();

    /** design/player/ship/data.yaml's hull boxes, turned into offsets around the centre (y up). */
    private static final Hull STORMHAWK_HULL = new Hull(List.of(
            new Hull.Part(0, 15, new Hitbox(4, 10)),
            new Hull.Part(0, -5.5, new Hitbox(12, 31)),
            new Hull.Part(0, -3, new Hitbox(36, 2)),
            new Hull.Part(0, -6, new Hitbox(28, 4)),
            new Hull.Part(-15, 0.5, new Hitbox(6, 5)),
            new Hull.Part(15, 0.5, new Hitbox(6, 5))));

    @Test
    void theStarterLoadoutComesFromTheShipAndItsStarterParts() {
        assertEquals(
                new Loadout(
                        new ShipSpec(270, 0.08, 0.06, 0.5, 48, 12, STORMHAWK_HULL, 0.25, 21, 3),
                        new PulseCannon(10, 2.0, 900, new Hitbox(4, 12)),
                        new ShieldModel(20, 2, 2.0, 1.0),
                        new Plating(60)),
                SimSpecs.starterLoadout(content, Difficulty.MEDIUM));
    }

    @Test
    void theShieldRegeneratesFasterOnEasy() {
        assertEquals(
                2.5, SimSpecs.starterLoadout(content, Difficulty.EASY).shield().regenPerSecond());
    }

    @Test
    void theSkitterComesFromItsStatBlockAndTheBalancingBasis() {
        assertEquals(
                new EnemySpec(
                        "skitter",
                        1,
                        new Hitbox(16, 16),
                        Layer.AIR,
                        6,
                        true,
                        5,
                        190,
                        Optional.of(new EnemySpec.Snake(0.25)),
                        Optional.of(160.0),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty()),
                SimSpecs.enemy(content, "skitter", Difficulty.MEDIUM, Optional.empty()));
    }

    @Test
    void theNeedlerAtMediumIsItsStatBlock() {
        EnemySpec needler = SimSpecs.enemy(content, "needler", Difficulty.MEDIUM, Optional.empty());

        assertEquals(4, needler.hp());
        assertEquals(
                new EnemySpec.Hover(new Range(2, 4), new Range(80, 220)),
                needler.hover().orElseThrow());
        assertEquals(new EnemySpec.Orbit(90, 60), needler.orbit().orElseThrow());
        assertEquals(new EnemyGun(2.5, 0.8, 1, 150, 4, false), needler.gun().orElseThrow());
        assertEquals(
                new EnemySpec.Drop(PickupType.SHIELD_CELL, 4), needler.drop().orElseThrow());
        assertEquals(10, needler.contactDamage());
        assertTrue(needler.destroyedByRamming(), "a small unit dies ramming the ship");
    }

    @Test
    void theDifficultyLeversScaleTheNeedler() {
        EnemySpec easy = SimSpecs.enemy(content, "needler", Difficulty.EASY, Optional.empty());
        EnemySpec hard = SimSpecs.enemy(
                content, "needler", Difficulty.HARD, Optional.of(new LevelData.EnemyChange(Optional.of(2))));

        assertEquals(3, easy.hp(), "4 × 0.75");
        assertEquals(2.5 / 0.7, easy.gun().orElseThrow().intervalSeconds(), 1e-12, "≈3.6 s");
        assertEquals(120, easy.gun().orElseThrow().bulletSpeed(), 1e-9);
        assertEquals(5, hard.hp(), "4 × 1.3 = 5.2, rounded");
        assertEquals(
                new EnemyGun(2.5 / 1.3, 0.8, 2, 150 * 1.15, 4, true), hard.gun().orElseThrow());
    }

    @Test
    void scaledHpRoundsHalfToEvenAndIsAtLeastOne() {
        DifficultyData levers = content.difficulty();

        assertEquals(1, levers.enemyHp(1, Difficulty.EASY), "0.75 rounds up to the minimum");
        assertEquals(2, levers.enemyHp(2.5 / 0.75, Difficulty.EASY), "2.5 rounds to 2");
        assertEquals(4, levers.enemyHp(3.5 / 0.75, Difficulty.EASY), "3.5 rounds to 4");
    }

    @Test
    void level01AtMediumHasItsWavesSplitIntoGroups() {
        LevelScript level = SimSpecs.level(content, LEVEL, Difficulty.MEDIUM);

        assertEquals(1, level.number());
        assertEquals(1, level.act());
        assertEquals(190, level.seconds());
        assertEquals(15, level.waves().size(), "14 waves, one of them mixed");
        assertEquals(95, level.waves().stream().mapToInt(WaveSpec::count).sum());
        WaveSpec pincer = wave(level, 122);
        assertEquals(List.of(new WaveSpec.Carried(PickupType.ARMOUR_PATCH, true)), pincer.carried());
        assertEquals(WaveSpec.Edge.NONE, pincer.edge(), "sides alone: both side edges");
        assertEquals(Optional.of(120.0), wave(level, 75).speed());
        assertEquals(11, level.groundObjects().size(), "10 containers and the beacon");
        assertEquals(1, level.secrets());
        assertEquals(11, level.radio().size(), "10 cues and the beacon cache's line");
    }

    @Test
    void level01OnEasyAndHardAppliesItsChanges() {
        LevelScript easy = SimSpecs.level(content, LEVEL, Difficulty.EASY);
        LevelScript hard = SimSpecs.level(content, LEVEL, Difficulty.HARD);

        assertEquals(WaveSpec.Entry.FRONT, wave(easy, 134).entry(), "no rear wave on easy");
        assertFalse(wave(easy, 40).carried().isEmpty(), "an extra armour patch");
        assertFalse(wave(easy, 86).carried().isEmpty(), "an extra armour patch");
        assertEquals(4, wave(easy, 40).count(), "the formation size lever: 5 × 0.8");
        assertEquals(10, wave(hard, 134).count(), "an authored count replaces the lever");
        assertEquals(2, wave(hard, 162).breakGroup());
        assertEquals(2, wave(hard, 40).enemy().gun().orElseThrow().burst());
    }

    @Test
    void theRulesComeFromTheDifficultyPlayerAndScoringData() {
        Rules medium = SimSpecs.rules(content, LEVEL, Difficulty.MEDIUM);
        Rules easy = SimSpecs.rules(content, LEVEL, Difficulty.EASY);

        assertEquals(120, medium.bulletBudget());
        assertEquals(10, medium.pickups().smallSalvageCredits());
        assertEquals(0.25, medium.pickups().shieldCellShare());
        assertEquals(6, medium.pickups().seconds());
        assertEquals(36, medium.pickups().collectionRadius());
        assertEquals(1, medium.scoring().creditFactor());
        assertEquals(1.25, easy.scoring().creditFactor());
        assertEquals(0.75, easy.scoring().scoreFactor());
        assertEquals("S", medium.scoring().grades().getFirst().letter());
    }

    private static WaveSpec wave(LevelScript level, double t) {
        return level.waves().stream().filter(w -> w.t() == t).findFirst().orElseThrow();
    }
}
