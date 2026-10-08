package vanguard.content;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import vanguard.sim.EnemyGun;
import vanguard.sim.EnemySpec;
import vanguard.sim.Layer;
import vanguard.sim.LevelScript;
import vanguard.sim.WaveSpec;

/**
 * M5 part D's units from their data files (user decisions D6–D8 of 2026-10-08): the Wraith
 * (design/enemies/air/wraith), cloaked on its rear ambush's path, and the Mote Swarm
 * (design/enemies/air/mote-swarm), a flock; the loader's rules for their keys and for the wave keys
 * that fly them ({@code swarm}, {@code rear ambush}, {@code loop_back.count}, {@code loops}).
 */
class PartDUnitsDataTest {
    private static final Content CONTENT = ContentLoader.fromClasspath();
    private static final String WRAITH = "enemies/air/wraith/data.yaml";
    private static final String MOTE = "enemies/air/mote-swarm/data.yaml";
    private static final String LEVEL_06 = "campaign/act-1-first-contact/level-06-farside/data.yaml";
    private static final String LEVEL_06_KEY = "act-1-first-contact/level-06-farside";
    /** Level 06's last wave, which the level-key tests replace. */
    private static final String LAST_WAVE =
            "  - {t: 179, formation: snake, enemy: skitter, count: 6, from: front, edge: left, notes: Past the open airlock}";

    private static EnemySpec spec(String slug, Difficulty difficulty) {
        return SimSpecs.enemy(CONTENT, slug, difficulty, Optional.empty());
    }

    @Test
    void theWraithIsCloakedOnHighAirUntilItsDecloak() {
        EnemySpec wraith = spec("wraith", Difficulty.MEDIUM);
        EnemySpec.Cloak cloak = wraith.cloak().orElseThrow();
        EnemySpec.Ambush ambush = wraith.ambush().orElseThrow();

        assertEquals(Layer.HIGH_AIR, wraith.layer(), "cloaked");
        assertEquals(Layer.AIR, cloak.layer(), "decloaked");
        assertEquals(0.4, cloak.flashSeconds());
        assertEquals(1.5, ambush.gapSeconds());
        assertEquals(40, ambush.lane());
        assertEquals(120, ambush.exitSpeed(), "its straight speed");
        assertEquals(200, wraith.speed(), "its swoop and re-entry");
        assertEquals(16, wraith.hp(), "D7 = a");
        assertEquals(15, wraith.contactDamage(), "`medium` contact once decloaked");
        assertFalse(wraith.destroyedByRamming());
        assertEquals(30, wraith.bounty());
        assertEquals(2.5, wraith.hover().orElseThrow().seconds().min());
        assertEquals(2.5, wraith.hover().orElseThrow().seconds().max());
        assertEquals(470, wraith.hover().orElseThrow().depth().min());
        assertEquals(520, wraith.hover().orElseThrow().depth().max());
        assertTrue(wraith.flock().isEmpty());
    }

    @Test
    void theWraithFiresFiveShotBurstsAndOnHardSevenAndHoldsThreeSeconds() {
        EnemyGun gun = spec("wraith", Difficulty.MEDIUM).gun().orElseThrow();
        assertEquals(5, gun.burst());
        assertEquals(0.12, gun.burstGapSeconds());
        assertEquals(0.9, gun.firstShotDelay());
        assertEquals(1.2 / CONTENT.difficulty().enemyFireRate().of(Difficulty.MEDIUM), gun.intervalSeconds(), 1e-9);
        assertEquals(6, gun.damage(), "`medium` bullets");
        assertTrue(gun.up(), "2026-10-08 (user): straight up the screen, not at the ship");
        assertEquals(Math.toRadians(40), gun.spreadRadians(), 1e-9, "a fixed 40° fan");

        EnemySpec hard = spec("wraith", Difficulty.HARD);
        assertEquals(7, hard.gun().orElseThrow().burst());
        assertEquals(3.0, hard.hover().orElseThrow().seconds().min(), "hard's hover_seconds");
        assertEquals(3.0, hard.hover().orElseThrow().seconds().max());
        assertEquals(
                2.5,
                spec("wraith", Difficulty.EASY).hover().orElseThrow().seconds().min());
        assertEquals(21, hard.hp(), 1, "16 by the HP lever");
    }

    @Test
    void otherUnitsKeepTheDefaultBurstGap() {
        assertEquals(
                EnemyGun.BURST_GAP_SECONDS,
                spec("needler", Difficulty.HARD).gun().orElseThrow().burstGapSeconds());
    }

    @Test
    void theMoteSwarmIsAFlockOfTinyRammers() {
        EnemySpec mote = spec("mote-swarm", Difficulty.MEDIUM);
        EnemySpec.FlockSpec flock = mote.flock().orElseThrow();

        assertEquals(18, flock.separation());
        assertEquals(48, flock.radius());
        assertEquals(0.5, flock.alignment());
        assertEquals(0.3, flock.cohesion());
        assertEquals(0.6, flock.leader());
        assertEquals(200, flock.speed());
        assertEquals(260, flock.diveSpeed());
        assertEquals(Math.toRadians(360), flock.turnRate(), 1e-12);
        assertEquals(24, flock.max());
        assertEquals(200, mote.speed(), "its leader point's path speed");
        assertEquals(1, mote.hp());
        assertEquals(Layer.AIR, mote.layer());
        assertEquals(6, mote.contactDamage(), "`tiny` contact");
        assertTrue(mote.destroyedByRamming());
        assertEquals(2, mote.bounty());
        assertTrue(mote.gun().isEmpty(), "contact only");
        assertTrue(mote.cloak().isEmpty() && mote.ambush().isEmpty());
    }

    @Test
    void aCloakIsFlownOnAnAmbushToAnotherLayer() {
        assertProblem(
                replace(WRAITH, "  ambush:             # the path", "  unused:             # the path"),
                "unknown field");
        assertProblem(
                replace(WRAITH, "    gap: 1.5          # s below", "    gap: 1.5\n    homing: true # s below"),
                "unknown field");
        assertProblem(
                replace(WRAITH, "  layer: air          # the layer", "  layer: high-air     # the layer"),
                "another layer");
        assertProblem(
                replace(WRAITH, "  straight: {speed: 120}", "  orbit: {radius: 40, turn_rate: 90}"), "straight speed");
        assertProblem(
                replace(WRAITH, "  - {name: rear ambush, size: [1, 4]}", "  - {name: single, size: [1]}"),
                "rear ambush");
    }

    @Test
    void aFlockHoldsAtMost24Members() {
        assertProblem(replace(MOTE, "    max: 24 ", "    max: 25 "), "max");
        assertProblem(replace(MOTE, "    radius: 48 ", "    radius: 12 "), "radius");
        assertProblem(replace(MOTE, "  - {name: swarm, size: [6, 24]}", "  - {name: stream, size: [6, 24]}"), "swarm");
    }

    /** Level 06 with its last wave replaced by {@code waves} (the units' first level is not checked). */
    private static List<DataFile> level06(String waves) {
        return replace(LEVEL_06, LAST_WAVE, waves);
    }

    private static final String SWARM = String.join(
            "\n",
            "  - t: 179",
            "    formation: swarm",
            "    enemy: mote-swarm",
            "    count: 20",
            "    from: front",
            "    paths: [[[80, -60], [120, 120], [300, 200], [360, 330], [240, 420], [520, 600]]]",
            "    loop_back: {after: 1.5, count: 1}",
            "    hard: {loops: 2}");

    private static final String AMBUSH = "  - {t: 176.5, formation: rear ambush, enemy: wraith, count: 2, from: rear}";

    @Test
    void theWaveKeysFlyASwarmAndARearAmbush() {
        Content content = ContentLoader.load(level06(AMBUSH + "\n" + SWARM));
        LevelScript medium = SimSpecs.level(content, LEVEL_06_KEY, Difficulty.MEDIUM);
        LevelScript hard = SimSpecs.level(content, LEVEL_06_KEY, Difficulty.HARD);

        WaveSpec swarm = wave(medium, WaveSpec.Formation.SWARM);
        assertEquals("mote-swarm", swarm.enemy().slug());
        assertEquals(20, swarm.count());
        assertEquals(1, swarm.loopBack().orElseThrow().count());
        assertEquals(1.5, swarm.loopBack().orElseThrow().afterSeconds());
        assertEquals(1, swarm.paths().size());
        assertEquals(
                2, wave(hard, WaveSpec.Formation.SWARM).loopBack().orElseThrow().count(), "hard's loops");
        assertEquals(24, wave(hard, WaveSpec.Formation.SWARM).count(), "the formation lever");

        WaveSpec ambush = wave(medium, WaveSpec.Formation.REAR_AMBUSH);
        assertEquals("wraith", ambush.enemy().slug());
        assertEquals(2, ambush.count());
        assertEquals(WaveSpec.Entry.REAR, ambush.entry());
    }

    private static WaveSpec wave(LevelScript level, WaveSpec.Formation formation) {
        return level.waves().stream()
                .filter(w -> w.formation() == formation)
                .findFirst()
                .orElseThrow();
    }

    @Test
    void aSwarmFliesAFlockOnOneRoute() {
        assertProblem(level06(SWARM.replace("enemy: mote-swarm", "enemy: skitter")), "flock");
        assertProblem(level06(SWARM.replace("count: 20", "count: 24")), "the flock holds 24");
        assertProblem(
                level06(SWARM.replace("paths: [[[80, -60]", "paths: [[[100, -60], [100, 600]], [[80, -60]")),
                "one route");
        assertProblem(
                level06("  - {t: 179, formation: stream, enemy: mote-swarm, count: 6, from: front, interval: 0.5}"),
                "flies only a swarm");
    }

    @Test
    void aRearAmbushIsFromTheRearWithOneToFourUnits() {
        assertProblem(level06(AMBUSH.replace("from: rear", "from: front")), "from: rear");
        assertProblem(level06(AMBUSH.replace("count: 2", "count: 5")), "1 to 4");
        assertProblem(level06(AMBUSH.replace("enemy: wraith", "enemy: needler")), "ambush");
        assertProblem(
                level06("  - {t: 179, formation: single, enemy: wraith, count: 1, from: front}"),
                "flies only a rear ambush");
    }

    @Test
    void onlyASwarmLoopsBackMoreThanOnce() {
        assertProblem(
                replace(
                        LEVEL_06,
                        "    loop_back: {after: 6, path: [[260, 600], [220, 380], [300, 200], [240, -80]]}",
                        "    loop_back: {after: 6, count: 2, path: [[260, 600], [220, 380], [300, 200], [240, -80]]}"),
                "only a swarm");
        assertProblem(
                replace(
                        LEVEL_06,
                        "    easy: {warning: 4}\n    notes: '`loop`",
                        "    easy: {warning: 4, loops: 2}\n    notes: '`loop`"),
                "only a swarm");
        assertProblem(level06(SWARM.replace("    loop_back: {after: 1.5, count: 1}\n", "")), "loops change");
    }

    @Test
    void aSnakesPathsAreOneRoute() {
        Content content = ContentLoader.load(
                level06(
                        "  - {t: 179, formation: snake, enemy: skitter, count: 6, from: rear, paths: [[[100, 600], [300, 300], [200, -60]]]}"));
        WaveSpec snake =
                SimSpecs.level(content, LEVEL_06_KEY, Difficulty.MEDIUM).waves().getLast();
        assertEquals(WaveSpec.Formation.SNAKE, snake.formation());
        assertEquals(1, snake.paths().size());

        assertProblem(
                level06(
                        "  - {t: 179, formation: snake, enemy: skitter, count: 6, from: front, paths: [[[100, -60], [100, 600]], [[200, -60], [200, 600]]]}"),
                "one route");
    }

    private static void assertProblem(List<DataFile> files, String message) {
        var e = assertThrows(ContentException.class, () -> ContentLoader.load(files));
        assertTrue(e.getMessage().contains(message), e.getMessage());
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
