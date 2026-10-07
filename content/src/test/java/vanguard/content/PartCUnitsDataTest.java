package vanguard.content;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import vanguard.sim.EnemySpec;
import vanguard.sim.Layer;

/**
 * M5 part C's units from their data files: the Hive Node (design/enemies/ground/hive-node), hardened
 * with a periodic spawn, and the Ravager (design/enemies/ground/ravager), a walker with a pounce and
 * an authored hard interval; the loader's rules for the new keys.
 */
class PartCUnitsDataTest {
    private static final Content CONTENT = ContentLoader.fromClasspath();
    private static final String HIVE_NODE = "enemies/ground/hive-node/data.yaml";
    private static final String RAVAGER = "enemies/ground/ravager/data.yaml";
    private static final String BROOD_POD = "enemies/air/brood-pod/data.yaml";

    private static EnemySpec spec(String slug, Difficulty difficulty) {
        return SimSpecs.enemy(CONTENT, slug, difficulty, Optional.empty());
    }

    @Test
    void theHiveNodeIsAHardenedPeriodicSpawnerOfSkitters() {
        EnemySpec node = spec("hive-node", Difficulty.MEDIUM);
        EnemySpec.Spawner spawner = node.spawner().orElseThrow();

        assertTrue(node.hardened());
        assertTrue(node.terrain());
        assertTrue(node.brood().isEmpty(), "it never bursts on its own");
        assertTrue(node.gun().isEmpty(), "no direct fire");
        assertEquals("skitter", spawner.enemy().slug());
        assertEquals(2, spawner.count());
        assertEquals(4.0, spawner.everySeconds());
        assertEquals(0.5, spawner.telegraphSeconds());
        assertEquals(Math.toRadians(120), spawner.arcRadians(), 1e-12);
        assertEquals(160, spawner.speed());
        assertEquals(96, spawner.shutWithin());
        assertEquals(
                3, spec("hive-node", Difficulty.HARD).spawner().orElseThrow().count(), "hard: spawn_count");
    }

    @Test
    void theRavagerPouncesEveryThreeSecondsAndTwoOnHard() {
        EnemySpec ravager = spec("ravager", Difficulty.MEDIUM);
        EnemySpec.Pounce pounce = ravager.pounce().orElseThrow();

        assertFalse(ravager.hardened());
        assertEquals(Layer.GROUND, ravager.layer());
        assertEquals(160, ravager.walker().orElseThrow().speed());
        assertTrue(ravager.gun().isEmpty(), "no ranged attack");
        assertEquals(15, ravager.contactDamage(), "`medium` contact");
        assertEquals(200, pounce.range());
        assertEquals(0.75, pounce.leapSeconds());
        assertEquals(0.3, pounce.airSeconds());
        assertEquals(1.43, pounce.scale());
        assertEquals(3.0 / CONTENT.difficulty().enemyFireRate().of(Difficulty.MEDIUM), pounce.intervalSeconds(), 1e-9);
        assertEquals(
                2.0, spec("ravager", Difficulty.HARD).pounce().orElseThrow().intervalSeconds(), 1e-9);
    }

    @Test
    void theBroodPodKeepsItsBrood() {
        EnemySpec pod = spec("brood-pod", Difficulty.MEDIUM);

        assertTrue(pod.brood().isPresent());
        assertTrue(pod.spawner().isEmpty());
        assertFalse(pod.hardened());
    }

    @Test
    void aPeriodicSpawnHasNoBurstBounty() {
        assertProblem(replace(HIVE_NODE, "      shut_within: 96", "      shut_within: 96\n      burst_bounty: 8"));
    }

    @Test
    void aSpawnHasAnAfterOrAnEveryNotBoth() {
        assertProblem(replace(HIVE_NODE, "      every: 4.0", "      every: 4.0\n      after: 8"));
        assertProblem(replace(BROOD_POD, "      after: 8", "      after: 8\n      every: 4"));
    }

    @Test
    void theTelegraphLiesInsideTheCycle() {
        assertProblem(replace(HIVE_NODE, "      telegraph: 0.5", "      telegraph: 4.0"));
    }

    @Test
    void onlyAPeriodicSpawnHasAShutDistance() {
        assertProblem(replace(BROOD_POD, "      after: 8", "      after: 8\n      shut_within: 50"));
    }

    @Test
    void aPounceHasNoBulletAndItsAirWindowLiesInsideTheLeap() {
        assertProblem(replace(RAVAGER, "    interval: 3.0", "    interval: 3.0\n    bullet: small"));
        assertProblem(replace(RAVAGER, "      air: 0.3", "      air: 0.9"));
        assertProblem(replace(RAVAGER, "      range: 200", "      range: 200\n      homing: true"));
    }

    @Test
    void onlyAWalkerPounces() {
        List<DataFile> files = replace(RAVAGER, "  walk: {speed: 160, turn_rate: 180, stride: 46}", "  terrain: {}");

        var e = assertThrows(ContentException.class, () -> ContentLoader.load(files));
        assertTrue(e.getMessage().contains("only a walker pounces"), e.getMessage());
    }

    private static void assertProblem(List<DataFile> files) {
        assertThrows(ContentException.class, () -> ContentLoader.load(files));
    }

    private static List<DataFile> replace(String path, String from, String to) {
        assertTrue(DesignTree.dataFiles().stream().anyMatch(f -> f.path().equals(path)), path);
        return DesignTree.dataFiles().stream()
                .map(f -> {
                    if (!f.path().equals(path)) {
                        return f;
                    }
                    assertTrue(f.text().contains(from), path + " has " + from);
                    return new DataFile(path, f.text().replace(from, to));
                })
                .toList();
    }
}
