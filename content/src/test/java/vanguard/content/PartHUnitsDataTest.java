package vanguard.content;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Optional;
import java.util.function.UnaryOperator;
import org.junit.jupiter.api.Test;
import vanguard.sim.EnemySpec;
import vanguard.sim.Layer;

/**
 * M5 part H, step H4: the Siege Spire's keys (design/enemies/bosses/siege-spire; decisions H3–H5 = a): a
 * boss's {@code staggered} attack, its {@code mortar} and {@code laser-sweep} attacks fired from parts, a
 * window spawn's {@code ambush} launch, the sway of an anchored boss after its lift; and the root-burst
 * growths (the Spine Turret and the Polyp Mortar, unchanged units placed by the level). Loaded strictly,
 * checked ({@link PartHChecks}) and mapped ({@link PartHRules}, {@code SiegeSpireTest} for the whole stat
 * block).
 */
class PartHUnitsDataTest {
    private static final Content CONTENT = ContentLoader.fromClasspath();
    private static final String SPIRE = "enemies/bosses/siege-spire/data.yaml";
    private static final String POLYP = "enemies/ground/polyp-mortar/data.yaml";

    private static String replace(String text, String what, String with) {
        assertTrue(text.contains(what), "the data has " + what);
        return text.replace(what, with);
    }

    private static List<DataFile> files(String path, UnaryOperator<String> change) {
        return DesignTree.dataFiles().stream()
                .map(f -> f.path().equals(path) ? new DataFile(f.path(), change.apply(f.text())) : f)
                .toList();
    }

    private static void assertProblem(String path, UnaryOperator<String> change, String message) {
        Exception e = assertThrows(Exception.class, () -> ContentLoader.load(files(path, change)));
        assertTrue(e.getMessage().contains(message), e.getMessage());
    }

    @Test
    void theSpiresKeysAreRead() {
        EnemyData spire = CONTENT.enemy("siege-spire");
        assertTrue(spire.attack("orbs").orElseThrow().staggered().orElseThrow());
        assertTrue(spire.attack("acid").orElseThrow().staggered().orElseThrow());
        assertEquals("mortar", spire.attack("acid").orElseThrow().pattern());
        assertEquals("laser-sweep", spire.attack("sweep").orElseThrow().pattern());
        EnemyData.SpawnData wraiths = spire.boss()
                .orElseThrow()
                .phases()
                .get(1)
                .windows()
                .orElseThrow()
                .spawns()
                .orElseThrow()
                .getFirst();
        assertEquals(4, wraiths.ambush().orElseThrow().most());
        assertEquals(14, spire.firstLevel());
        assertTrue(spire.movement().anchored().isPresent());
        assertEquals(100, spire.movement().sine().orElseThrow().amplitude());
    }

    @Test
    void theSpiresKeysAreChecked() {
        assertProblem(
                POLYP,
                text -> replace(
                        text, "    first_shot_delay: 1.5     # s after it enters the screen", "    staggered: true"),
                "only a boss's attack fired by several parts is staggered");
        assertProblem(
                SPIRE,
                text -> replace(
                        text,
                        "    staggered: true     # each pod",
                        "    rotate: true\n    staggered: true     # each pod"),
                "an attack rotates or is staggered, not both");
        assertProblem(
                SPIRE,
                text -> replace(
                        text,
                        "enemy: wraith, count: 2, speed: 200, ambush:",
                        "enemy: skitter, count: 2, speed: 200, ambush:"),
                "an ambush launch flies its enemy's rear-ambush path: 'skitter'");
        assertProblem(
                SPIRE,
                text -> replace(text, "      move: {to: [240, 120], layer: air, descend: 3, pose: free}", ""),
                "an anchored boss sways only after a phase's move lifts it");
        assertProblem(
                SPIRE,
                text -> replace(text, "ambush: {most: 4}}", "ambush: {most: 4}, arc: 40}"),
                "an ambush launch flies its enemy's rear path: no arc or glide");
    }

    /** The growths are the units' own stat blocks at Level 14 (small units: no act HP factor). */
    @Test
    void theGrowthsAreTheirUnits() {
        EnemySpec turret = SimSpecs.enemy(CONTENT, "spine-turret", Difficulty.MEDIUM, Optional.empty(), 2, 14);
        EnemySpec mortar = SimSpecs.enemy(CONTENT, "polyp-mortar", Difficulty.MEDIUM, Optional.empty(), 2, 14);
        assertEquals(Layer.GROUND, turret.layer());
        assertEquals(CONTENT.enemy("spine-turret").hp(), turret.hp());
        assertEquals(CONTENT.enemy("polyp-mortar").hp(), mortar.hp());
        assertTrue(turret.terrain() && mortar.terrain(), "they stand on the ground and scroll with it");
    }
}
