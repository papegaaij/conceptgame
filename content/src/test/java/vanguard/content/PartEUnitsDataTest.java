package vanguard.content;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import vanguard.sim.AllySpec;
import vanguard.sim.EnemySpec;
import vanguard.sim.Layer;
import vanguard.sim.LevelScript;
import vanguard.sim.PlayField;
import vanguard.sim.WaveSpec;

/**
 * M5 part E, step E2b: the naval units from their data files (design/enemies/naval: the Driftjelly's
 * submerge and proximity ring, the Reef Spitter's drifting raft), the convoy's allies (design/allies:
 * {@code follows: stations}, {@code damaged_by.slams}, {@code flak}) and the level keys that fly them
 * ({@code field} waves, a nest's {@code current}, the {@code convoy} block, the {@code afloat}
 * secondary, the {@code ally-hit} cue), loaded strictly, checked and mapped for the simulation. Level
 * 11's data is written later, so the level keys are tried on Level 05's file with the Harbour Kraken
 * (an arena boss with four lanes, step E2c) in place of its Gorgon Frigate, the arena at speed 0.
 */
class PartEUnitsDataTest {
    private static final Content CONTENT = ContentLoader.fromClasspath();
    private static final String JELLY = "enemies/naval/driftjelly/data.yaml";
    private static final String SPITTER = "enemies/naval/reef-spitter/data.yaml";
    private static final String ALLIES = "allies/data.yaml";
    private static final String KRAKEN = "enemies/bosses/harbour-kraken/data.yaml";
    private static final String LEVEL_05 = "campaign/act-1-first-contact/level-05-crater-nest/data.yaml";
    private static final String LEVEL_05_KEY = "act-1-first-contact/level-05-crater-nest";

    private static EnemySpec spec(String slug, Difficulty difficulty) {
        return SimSpecs.enemy(CONTENT, slug, difficulty, Optional.empty());
    }

    @Test
    void theDriftjellySurfacesAndSubmergesAndPulsesItsRingWithin96Pixels() {
        EnemySpec jelly = spec("driftjelly", Difficulty.MEDIUM);
        EnemySpec.Submerge submerge = jelly.submerge().orElseThrow();
        EnemySpec.ProximityRing ring = jelly.ring().orElseThrow();
        DifficultyData levers = CONTENT.difficulty();

        assertEquals(Layer.GROUND, jelly.layer(), "surfaced; its current layer swaps to sub");
        assertEquals(6, submerge.everyMin());
        assertEquals(10, submerge.everyMax());
        assertEquals(0.6, submerge.swapSeconds());
        assertEquals(0.5, submerge.start());
        assertEquals(8, ring.count());
        assertEquals(96, ring.within());
        assertEquals(4, ring.damage(), "`small` bullets");
        assertEquals(90 * levers.enemyBulletSpeed().of(Difficulty.MEDIUM), ring.bulletSpeed(), 1e-9);
        assertEquals(2.5 / levers.enemyFireRate().of(Difficulty.MEDIUM), ring.cooldownSeconds(), 1e-9);
        assertTrue(jelly.gun().isEmpty(), "the ring is its only attack, not a gun");
        assertEquals(15, jelly.speed(), "its drift");
        assertEquals(4, jelly.hp());
        assertEquals(10, jelly.bounty());
        assertTrue(jelly.destroyedByRamming());
        assertFalse(jelly.terrain());
        assertEquals(0, jelly.drift());

        EnemySpec hard = spec("driftjelly", Difficulty.HARD);
        assertEquals(12, hard.ring().orElseThrow().count(), "hard: a 12-bullet ring");
        assertEquals(110, hard.ring().orElseThrow().within(), "within 110 px");
        assertEquals(
                96, spec("driftjelly", Difficulty.EASY).ring().orElseThrow().within());
        assertEquals(8, spec("driftjelly", Difficulty.EASY).ring().orElseThrow().count());
    }

    @Test
    void theReefSpittersRaftDriftsAndItsGunFiresAThreeWayFan() {
        EnemySpec spitter = spec("reef-spitter", Difficulty.MEDIUM);
        assertTrue(spitter.terrain());
        assertEquals(10, spitter.drift(), "px/s along its nest's current");
        assertEquals(Layer.GROUND, spitter.layer());
        assertEquals(7, spitter.hp());
        assertEquals(14, spitter.bounty());
        var gun = spitter.gun().orElseThrow();
        assertEquals(3, gun.fan());
        assertEquals(Math.toRadians(30), gun.spreadRadians(), 1e-9);
        assertEquals(2.4 / CONTENT.difficulty().enemyFireRate().of(Difficulty.MEDIUM), gun.intervalSeconds(), 1e-9);
        assertEquals(1.0, gun.firstShotDelay());
        assertEquals(
                5, spec("reef-spitter", Difficulty.HARD).gun().orElseThrow().fan(), "hard: 5-way");
        assertTrue(spitter.submerge().isEmpty() && spitter.ring().isEmpty());
        assertEquals(0, spec("spine-turret", Difficulty.MEDIUM).drift(), "a turret on land does not drift");
    }

    @Test
    void theConvoysAlliesAreHurtOnlyBySlamsAndTheFrigateNotAtAll() {
        AlliesData.Ally cargo = CONTENT.allies().allies().get("cargo-ship");
        AlliesData.Ally frigate = CONTENT.allies().allies().get("escort-frigate");
        assertTrue(cargo.naval() && frigate.naval());
        assertEquals(Optional.of(11), cargo.firstLevel());
        assertEquals(Optional.of(11), frigate.firstLevel());

        AllySpec ship = PartERules.ally(CONTENT, "cargo-ship", Optional.of(new LevelData.AllyChange(3)));
        assertEquals(1, ship.slams());
        assertEquals(3, ship.hp(), "the level's easy HP for a unit that can be damaged");
        assertTrue(ship.damageable());
        assertFalse(ship.objectiveAimed() || ship.bullets() || ship.contact() || ship.clawsPerSecond() > 0);

        AllySpec ruyter = PartERules.ally(CONTENT, "escort-frigate", Optional.of(new LevelData.AllyChange(3)));
        assertFalse(ruyter.damageable(), "its damaged_by names nothing");
        assertEquals(1, ruyter.hp(), "the level's HP does not apply");
        assertEquals(2, ruyter.flakSeconds());
        assertEquals(0, ship.flakSeconds());
    }

    @Test
    void theUnitKeysAreChecked() {
        assertProblem(replace(JELLY, "layer: ground     # while", "layer: air        # while"), "submerges");
        assertProblem(replace(JELLY, "    within: 96 ", "    within: -5 "), "within");
        assertProblem(replace(SPITTER, "    count: 3\n", "    count: 3\n    within: 50\n"), "only a ring fires within");
        assertProblem(replace(JELLY, "  drift: {speed: 15}", "  terrain: {}"), "drift");
        assertProblem(replace(JELLY, "  swap: 0.6 ", "  swap: 7 "), "swap");
        assertProblem(replace(JELLY, "  start: 0.5 ", "  start: 1.5 "), "start");
        assertProblem(
                replace(
                        SPITTER,
                        "difficulty:\n  hard: {fan_count: 5}",
                        "difficulty:\n  hard: {attacks: {fan: {within: 9}}}"),
                "within");
        assertProblem(replace(ALLIES, "    slams: 1 ", "    slams: 1\n    contact: true "), "only by slams");
        assertProblem(replace(ALLIES, "  follows: stations       # its", "  follows: road           # its"), "slams");
        assertProblem(replace(ALLIES, "  flak: {every: 2.0} ", "  flak: {every: 0} "), "every");
    }

    // --- Level keys, on Level 05 with lanes for its boss ----------------------------------------------

    private static final String LAST_WAVE =
            "  - {t: 140.5, formation: stream, enemy: skitter, count: 4, from: front, edge: right, interval: 1.5, notes: Around the nest heart}";
    private static final String FIELD =
            "\n  - {t: 141, formation: field, enemy: driftjelly, count: 6, from: front, x: 240, size: [300, 160], current: 10}";
    private static final String TURRETS = "  - target: rim turrets\n    section: 2\n    enemy: spine-turret\n";
    private static final String SECONDARY =
            "  secondary: {kill_all: [polyp-mortar, spine-turret], label: NEST, credits: 40}   # \"Scorched crater\"";
    private static final String AFLOAT = "  secondary: {afloat: true, label: CONVOY, credits: 100}";
    private static final String OBJECTIVES = "objectives:\n";
    private static final String CONVOY = String.join(
            "\n",
            "convoy:",
            "  glide: 3",
            "  lane_y: 450",
            "  easy: {hp: 3}",
            "  units:",
            "    - {ally: cargo-ship, name: Halvorsen, station: [130, 380], lane: 1}",
            "    - {ally: cargo-ship, name: Mbeki, station: [240, 350], lane: 2}",
            "    - {ally: cargo-ship, name: Saint-Laurent, station: [350, 380], lane: 4}",
            "    - {ally: escort-frigate, name: Ruyter, station: [240, 480], leaves: true}",
            "");
    private static final String RADIO = "radio:  # trigger";
    private static final String ALLY_CUES = "radio:\n"
            + "  - {event: ally-hit, speaker: Okafor, line: \"The {ally} is hit!\"}\n"
            + "  - {event: ally-lost, speaker: Okafor, line: \"We've lost the {ally}.\"}  # trigger";
    /** Level 05 as it is: its Gorgon Frigate has no lanes. */
    private static final String PAR = "without lanes";
    /** The Harbour Kraken's lanes as its data file writes them. */
    private static final String LANES =
            String.join("\n", "    count: 4", "    width: 120", "    arms: {left arm: [1, 2], right arm: [3, 4]}");

    /**
     * Level 05 with the edits; {@code level} edits the level's text. With {@code lanes} other than
     * {@link #PAR} the Harbour Kraken is its boss (its lanes written as {@code lanes}), arriving at its
     * arena's start with the arena at speed 0, and the frigate's phase cue goes.
     */
    private static List<DataFile> files(java.util.function.UnaryOperator<String> level, String lanes) {
        boolean kraken = !lanes.equals(PAR);
        return DesignTree.dataFiles().stream()
                .map(f -> switch (f.path()) {
                    case LEVEL_05 -> new DataFile(f.path(), level.apply(kraken ? kraken(f.text()) : f.text()));
                    case KRAKEN -> kraken ? new DataFile(f.path(), edit(f.text(), LANES, lanes)) : f;
                    default -> f;
                })
                .toList();
    }

    /** Level 05 with the Harbour Kraken as its boss over an arena of speed 0. */
    private static String kraken(String text) {
        String arena = edit(text, "    end: 205\n    speed: 30\n", "    end: 205\n    speed: 0\n");
        String boss = edit(arena, "  enemy: gorgon-frigate\n  t: 150", "  enemy: harbour-kraken\n  t: 150");
        return boss.replaceFirst("(?m)^  - \\{event: boss-phase, phase: Core,.*\\n", "");
    }

    /** The level keys of part E on Level 05: a jelly field, a raft nest with a current, the convoy and its secondary. */
    private static String naval(String text) {
        return edit(
                edit(
                        edit(
                                edit(
                                        edit(text, LAST_WAVE, LAST_WAVE + FIELD),
                                        TURRETS,
                                        TURRETS.replace("spine-turret", "reef-spitter") + "    current: 30\n"),
                                SECONDARY,
                                AFLOAT),
                        OBJECTIVES,
                        CONVOY + OBJECTIVES),
                RADIO,
                ALLY_CUES);
    }

    private static String edit(String text, String what, String with) {
        assertTrue(text.contains(what), "the data has " + what);
        return text.replace(what, with);
    }

    @Test
    void theLevelKeysLoadAndMapForTheSimulation() {
        Content content = ContentLoader.load(files(PartEUnitsDataTest::naval, LANES));
        LevelData level = content.level(LEVEL_05_KEY);
        assertEquals(
                4,
                content.enemy("harbour-kraken")
                        .boss()
                        .orElseThrow()
                        .lanes()
                        .orElseThrow()
                        .count());

        LevelScript medium = SimSpecs.level(content, LEVEL_05_KEY, Difficulty.MEDIUM);
        WaveSpec field = medium.waves().stream()
                .filter(w -> w.formation() == WaveSpec.Formation.FIELD)
                .findFirst()
                .orElseThrow();
        WaveSpec.Field area = field.field().orElseThrow();
        assertEquals(240, area.x());
        assertEquals(300, area.width());
        assertEquals(160, area.height());
        assertEquals(42, area.spacing(), 1e-9, "1.5 × the 28 px hit box");
        assertEquals(Math.toRadians(10), area.currentRadians(), 1e-12);
        assertEquals("driftjelly", field.enemy().slug());

        assertTrue(medium.groundUnits().stream()
                .filter(unit -> unit.enemy().slug().equals("reef-spitter"))
                .allMatch(unit -> Math.abs(unit.currentRadians() - Math.toRadians(30)) < 1e-12));

        LevelScript.Naval convoy = medium.convoy().orElseThrow();
        assertEquals(4, convoy.units().size());
        assertEquals(3, convoy.glideSeconds());
        assertEquals(PlayField.HEIGHT - 450, convoy.laneY());
        assertEquals(4, convoy.lanes());
        assertEquals(120, convoy.laneWidth());
        assertEquals(60, convoy.laneX(1));
        assertEquals(3, convoy.damageable());
        LevelScript.NavalUnit halvorsen = convoy.units().getFirst();
        assertEquals("Halvorsen", halvorsen.name());
        assertEquals(130, halvorsen.x());
        assertEquals(PlayField.HEIGHT - 380, halvorsen.y());
        assertEquals(1, halvorsen.lane());
        assertEquals(4, halvorsen.ally().hp(), "the ally's own HP at medium");
        assertTrue(convoy.units().getLast().leaves());
        assertEquals(
                3,
                SimSpecs.level(content, LEVEL_05_KEY, Difficulty.EASY)
                        .convoy()
                        .orElseThrow()
                        .units()
                        .getFirst()
                        .ally()
                        .hp(),
                "easy: three slams");

        assertTrue(medium.secondary().afloat());
        assertEquals(100, medium.secondary().credits());
        assertEquals("CONVOY", medium.secondary().label());
        assertTrue(medium.radio().stream().anyMatch(cue -> cue.trigger() == LevelScript.CueTrigger.ALLY_HIT));
        assertTrue(level.convoy().isPresent());
        assertTrue(medium.escort().isEmpty());
    }

    @Test
    void aFieldDriftsAndHoldsItsUnitsOnEveryDifficulty() {
        assertProblem(
                files(text -> naval(text).replace("enemy: driftjelly, count: 6", "enemy: skitter, count: 6"), LANES),
                "drift");
        // 250 × 80 px holds 5 × 1 = 5 units 42 px apart, not 6.
        List<DataFile> tight = files(text -> naval(text).replace("size: [300, 160]", "size: [250, 80]"), LANES);
        assertProblem(tight, "6 units on medium do not fit");
        assertProblem(
                files(text -> naval(text).replace("x: 240, size", "x: 100, size"), LANES), "beyond the play field");
        assertProblem(
                files(
                        text -> edit(text, LAST_WAVE, LAST_WAVE.replace("interval: 1.5", "interval: 1.5, x: 200")),
                        LANES),
                "only a field wave");
        assertProblem(files(text -> naval(text).replace(", x: 240, size: [300, 160]", ""), LANES), "x and size");
    }

    @Test
    void aNestsCurrentMovesOnlyWhatDrifts() {
        assertProblem(files(text -> edit(text, TURRETS, TURRETS + "    current: 30\n"), LANES), "do not drift");
    }

    @Test
    void theConvoyIsChecked() {
        assertProblem(files(text -> naval(text).replace("lane: 4}", "lane: 5}"), LANES), "4 lanes, not 5");
        assertProblem(files(PartEUnitsDataTest::naval, PAR), "boss has lanes");
        assertProblem(
                files(text -> naval(text).replace("station: [350, 380]", "station: [250, 360]"), LANES), "overlaps");
        assertProblem(
                files(text -> naval(text).replace("station: [130, 380]", "station: [10, 380]"), LANES),
                "leaves the play field");
        assertProblem(files(text -> naval(text).replace("name: Mbeki", "name: Halvorsen"), LANES), "names another");
        assertProblem(files(text -> naval(text).replace("lane: 2}", "lane: 1}"), LANES), "two units in one lane");
        assertProblem(
                files(text -> naval(text).replace("ally: escort-frigate", "ally: civilian-crawler"), LANES),
                "does not follow stations");
        assertProblem(files(text -> naval(text).replace(", leaves: true}", "}"), LANES), "lane or leaves");
        assertProblem(files(text -> naval(text).replace("  lane_y: 450\n", ""), LANES), "lane_y");
        assertProblem(files(text -> naval(text).replace(CONVOY, ""), LANES), "an afloat objective needs a convoy");
        assertProblem(files(text -> edit(text, RADIO, ALLY_CUES), LANES), "need an escort objective or a naval convoy");
        assertProblem(
                files(text -> naval(text).replace(AFLOAT, "  secondary: {afloat: true, credits: 100}"), LANES),
                "label");
    }

    @Test
    void theBossesLanesAreOwnedByItsParts() {
        assertProblem(files(text -> text, LANES.replace("left arm", "left head")), "left head");
        assertProblem(files(text -> text, LANES.replace("[3, 4]", "[3, 5]")), "lane 5");
        assertProblem(files(text -> text, LANES.replace("width: 120", "width: 130")), "play field");
    }

    private static void assertProblem(List<DataFile> files, String message) {
        var e = assertThrows(Exception.class, () -> ContentLoader.load(files));
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
