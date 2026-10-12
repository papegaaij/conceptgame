package vanguard.content;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Optional;
import java.util.function.UnaryOperator;
import org.junit.jupiter.api.Test;
import vanguard.sim.AllySpec;
import vanguard.sim.EnemyGun;
import vanguard.sim.EnemySpec;
import vanguard.sim.Layer;

/**
 * M5 part G, step G4: the Skimmer from its data file (design/enemies/naval/skimmer: a {@code ground} unit
 * with a {@code sine} weave that walks its waves' lanes, its aimed shot with hard's 2-shot burst) and the
 * Nansen Relay from the allies' (design/allies: {@code follows: ground}, {@code damaged_by} {@code
 * objective_aimed}, {@code dives} and {@code lobs}), loaded strictly, checked and mapped for the simulation.
 */
class PartGUnitsDataTest {
    private static final Content CONTENT = ContentLoader.fromClasspath();
    private static final String ALLIES = "allies/data.yaml";

    private static EnemySpec skimmer(Difficulty difficulty) {
        return SimSpecs.enemy(CONTENT, "skimmer", difficulty, Optional.empty());
    }

    @Test
    void theSkimmerWeavesAndFiresAimedShots() {
        EnemySpec skimmer = skimmer(Difficulty.MEDIUM);
        EnemyGun gun = skimmer.gun().orElseThrow();

        assertEquals(Layer.GROUND, skimmer.layer());
        assertEquals(5, skimmer.hp());
        assertEquals(14, skimmer.bounty());
        assertEquals(180, skimmer.speed());
        assertEquals(22, skimmer.hitbox().width());
        assertEquals(32, skimmer.hitbox().height());
        assertEquals(new EnemySpec.Sine(40, 1.2), skimmer.sine().orElseThrow());
        assertFalse(skimmer.terrain(), "it walks lanes, it does not stand on the ground");
        assertTrue(skimmer.walker().isEmpty(), "a lane walker, not a walker on a path");
        assertEquals(1.8, gun.intervalSeconds());
        assertEquals(0.5, gun.firstShotDelay());
        assertEquals(190, gun.bulletSpeed());
        assertEquals(4, gun.damage(), "small");
        assertEquals(1, gun.burst());
        assertEquals(2, skimmer(Difficulty.HARD).gun().orElseThrow().burst(), "hard: 2-shot bursts");
        assertEquals(4, skimmer(Difficulty.EASY).hp(), "the HP lever");
        assertEquals(6, skimmer(Difficulty.HARD).hp());
        assertEquals(13, CONTENT.enemy("skimmer").firstLevel());
        assertEquals(
                5,
                SimSpecs.actHpFactor(CONTENT, "skimmer", 2, 13)
                        * CONTENT.enemy("skimmer").hp(),
                1e-9,
                "no act HP factor at its first level");
    }

    @Test
    void theRelayStandsOnTheGround() {
        AlliesData.Ally relay = CONTENT.allies().allies().get("nansen-relay");
        assertEquals("Nansen Relay", relay.name());
        assertTrue(relay.standing());
        assertEquals(Optional.of(13), relay.firstLevel());
        assertEquals(10, relay.damagedBy().diveDamage());
        assertTrue(relay.damagedBy().hitByLobs());
        assertTrue(relay.damagedBy().objectiveAimed());
        AllySpec spec = PartGRules.ally(CONTENT, "nansen-relay", Optional.empty());
        assertEquals(relay.hp(), spec.hp());
        assertEquals(96, spec.size().width());
        assertEquals(80, spec.hitbox().height());
        assertEquals(0.5, spec.smokeBelow());
        assertTrue(spec.damageable());
        assertFalse(spec.bullets(), "only relay-aimed attacks hurt it");
        assertFalse(spec.contact());
        assertEquals(0, spec.clawsPerSecond());
        assertEquals(
                450,
                PartGRules.ally(CONTENT, "nansen-relay", Optional.of(450.0)).hp());
        assertThrows(
                IllegalArgumentException.class,
                () -> PartGRules.ally(CONTENT, "civilian-crawler", Optional.empty()),
                "a defended ally follows the ground");
    }

    private static String replace(String text, String what, String with) {
        assertTrue(text.contains(what), "the allies' data has " + what);
        return text.replace(what, with);
    }

    private static void assertProblem(UnaryOperator<String> change, String message) {
        List<DataFile> files = DesignTree.dataFiles().stream()
                .map(f -> f.path().equals(ALLIES) ? new DataFile(f.path(), change.apply(f.text())) : f)
                .toList();
        Exception e = assertThrows(Exception.class, () -> ContentLoader.load(files));
        assertTrue(e.getMessage().contains(message), e.getMessage());
    }

    @Test
    void onlyAStandingAllyIsHurtByDivesAndLobs() {
        assertProblem(
                text -> replace(
                        text,
                        "    claws: 10             # damage per second",
                        "    dives: 10\n    claws: 10             # damage per second"),
                "only a defended ally (follows: ground) is hurt by dives and lobs");
        assertProblem(
                text -> replace(
                        text,
                        "    dives: 10             # a hooked diver",
                        "    bullets: true\n    dives: 10             # a hooked diver"),
                "a defended ally (follows: ground) is hurt only by objective-aimed attacks, dives and lobs");
    }
}
