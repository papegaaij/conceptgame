package vanguard.content;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.function.UnaryOperator;
import org.junit.jupiter.api.Test;
import vanguard.sim.LevelScript;
import vanguard.sim.PickupType;
import vanguard.sim.WaveSpec;

/**
 * M5 part F's level keys (design/tech/architecture, Data file schemas), on Level 12's own data: the
 * {@code weather} block (gusts with their easy/hard push and telegraph and hard's extra gusts, the
 * wall's drift by section, lightning, occlusion, rain), a circle's {@code clockwise}, a whirl cluster
 * released just outside a side edge, a chasers' rear stream and swarm, the {@code sondes} with the
 * {@code collect: sondes} secondary, a {@code low-air} trigger's {@code drift}, the {@code drop_factor},
 * a pickup placed {@code at} a time and x, the event prompt and the radio's {@code first-latch}, the
 * music's {@code section_db}; loaded strictly, checked and mapped for the simulation.
 */
class PartFLevelKeysTest {
    private static final String KEY = Level12Test.LEVEL;
    private static final String PATH = "campaign/" + KEY + "/data.yaml";
    private static final Content CONTENT = ContentLoader.fromClasspath();

    private static String replace(String text, String what, String with) {
        assertTrue(text.contains(what), "Level 12's data has " + what);
        return text.replace(what, with);
    }

    private static List<DataFile> files(UnaryOperator<String> change) {
        return DesignTree.dataFiles().stream()
                .map(f -> f.path().equals(PATH) ? new DataFile(f.path(), change.apply(f.text())) : f)
                .toList();
    }

    private static void assertProblem(UnaryOperator<String> change, String message) {
        Exception e = assertThrows(Exception.class, () -> ContentLoader.load(files(change)));
        assertTrue(e.getMessage().contains(message), e.getMessage());
    }

    @Test
    void theWeatherIsTheDifficultysWind() {
        for (Difficulty difficulty : Difficulty.values()) {
            LevelScript.Weather weather =
                    SimSpecs.level(CONTENT, KEY, difficulty).weather().orElseThrow();
            boolean hard = difficulty == Difficulty.HARD;
            boolean easy = difficulty == Difficulty.EASY;
            assertEquals(easy ? 25 : hard ? 55 : 40, weather.push(), difficulty.name());
            assertEquals(easy ? 1.5 : 1.0, weather.telegraphSeconds(), difficulty.name());
            assertEquals(2.5, weather.gustSeconds());
            assertEquals(0.25, weather.rampSeconds());
            List<Double> gusts =
                    weather.gusts().stream().map(LevelScript.Gust::t).toList();
            assertEquals(
                    hard
                            ? List.of(16.0, 34.0, 52.0, 72.0, 90.0, 118.0, 126.0, 132.0, 138.0, 144.0, 182.0)
                            : List.of(16.0, 52.0, 90.0, 118.0, 126.0, 132.0, 138.0, 144.0, 182.0),
                    gusts,
                    "hard's extra gusts in time order");
            assertTrue(weather.gusts().stream().allMatch(LevelScript.Gust::fromLeft), "the storm's wind");
            assertEquals(
                    easy ? List.of() : List.of(new LevelScript.Drift(125, 150, 20, true)),
                    weather.drifts(),
                    "the storm wall's drift, none on easy");
            assertEquals(
                    List.of(
                            78.0, 87.0, 93.0, 99.5, 103.5, 111.0, 117.0, 123.0, 129.0, 135.0, 141.0, 147.0, 180.0,
                            189.0),
                    weather.lightning());
        }
        LevelData.Weather data = CONTENT.level(KEY).weather().orElseThrow();
        assertTrue(data.occludes(), "F1 = a");
        assertEquals(0.5, data.lightning().orElseThrow().reveal());
        assertEquals(0, data.rainAt(5));
        assertEquals(0.4, data.rainAt(60), 1e-12);
        assertEquals(1, data.rainAt(140));
        assertEquals(0, data.rainAt(160), "no rain in the eye");
        assertEquals(0.7, data.rainAt(127.5), 1e-12, "linear between the keys");
    }

    @Test
    void theWavesKeysFly() {
        LevelScript level = SimSpecs.level(CONTENT, KEY, Difficulty.MEDIUM);
        WaveSpec circle = level.waves().stream()
                .filter(wave -> wave.formation() == WaveSpec.Formation.CIRCLE)
                .findFirst()
                .orElseThrow();
        assertTrue(circle.clockwise(), "the eye's circle turns with the storm");
        assertTrue(level.waves().stream()
                .filter(wave -> wave.formation() != WaveSpec.Formation.CIRCLE)
                .noneMatch(WaveSpec::clockwise));
        List<WaveSpec> windborne = level.waves().stream()
                .filter(wave ->
                        wave.formation() == WaveSpec.Formation.WHIRL_CLUSTER && wave.entry() == WaveSpec.Entry.SIDES)
                .toList();
        assertEquals(List.of(46.0, 140.0), windborne.stream().map(WaveSpec::t).toList());
        assertTrue(windborne.stream().allMatch(wave -> wave.at().orElseThrow().x() < 0), "outside the left edge");
        assertTrue(level.waves().stream()
                .filter(wave -> wave.entry() == WaveSpec.Entry.REAR)
                .allMatch(wave -> wave.formation() == WaveSpec.Formation.REAR_AMBUSH
                        || wave.enemy().chase().isPresent()));
    }

    @Test
    void theSondesTheCanisterAndThePickups() {
        LevelScript level = SimSpecs.level(CONTENT, KEY, Difficulty.MEDIUM);
        LevelScript.Sondes sondes = level.sondes().orElseThrow();
        assertEquals(60, sondes.speed());
        assertEquals(36, sondes.radius());
        assertEquals(
                List.of(
                        new LevelScript.Sonde(60, 400),
                        new LevelScript.Sonde(110, 70),
                        new LevelScript.Sonde(165, 390)),
                sondes.at());
        assertTrue(level.secondary().sondes(), "collect: sondes");
        assertEquals(75, level.secondary().credits());
        assertEquals("SONDES", level.secondary().label());

        LevelScript.GroundObjectSpec canister = level.groundObjects().getFirst();
        assertEquals(1, level.groundObjects().size());
        assertTrue(canister.lowAir());
        assertEquals(-24, canister.driftX());
        assertEquals(-70, canister.driftY());
        assertEquals(3, canister.hits());
        assertEquals(130, canister.crateCredits());
        assertEquals(
                5,
                SimSpecs.level(CONTENT, KEY, Difficulty.HARD)
                        .groundObjects()
                        .getFirst()
                        .hits());

        assertEquals(1.5, level.dropFactor());
        assertEquals(List.of(new LevelScript.PlacedPickup(152, 300, PickupType.ARMOUR_PATCH)), level.placedPickups());
        assertEquals(
                2,
                level.waves().stream()
                        .mapToLong(wave -> wave.carried().stream()
                                .filter(c -> c.pickup() == PickupType.OVERDRIVE)
                                .count())
                        .sum(),
                "the two overdrives are carried");
    }

    @Test
    void theEventPromptTheRadioEventAndTheMusic() {
        LevelData data = CONTENT.level(KEY);
        LevelData.Prompt prompt = data.prompts().orElseThrow().getFirst();
        assertTrue(prompt.onEvent());
        assertEquals(LevelData.PromptEvent.FIRST_LATCH, prompt.event().orElseThrow());
        assertTrue(Double.isNaN(prompt.t()), "never due by the clock");
        assertEquals("SHAKE OFF", prompt.action());
        assertEquals(6, prompt.seconds());
        assertEquals(
                1,
                SimSpecs.level(CONTENT, KEY, Difficulty.MEDIUM).radio().stream()
                        .filter(cue -> cue.trigger() == LevelScript.CueTrigger.FIRST_LATCH)
                        .count());
        LevelData.Music music = data.music();
        assertEquals(0, music.sectionLevel(4));
        assertEquals(-6, music.sectionLevel(5));
        assertEquals(0, music.sectionLevel(6));
        assertFalse(music.full(5), "the base stem in the eye");
        assertTrue(music.full(6));
        assertEquals("earth-storm", music.ambience());
    }

    @Test
    void theNewKeysAreChecked() {
        assertProblem(
                text -> replace(
                        text,
                        "{pickup: armour patch, at: [152, 300]}",
                        "{pickup: armour patch, at: [152, 300], dropped_by: {wave: 104, unit: first}}"),
                "a pickup is dropped_by a carrier or placed at [t, x]");
        assertProblem(
                text -> replace(
                        text, "{pickup: armour patch, at: [152, 300]}", "{pickup: armour patch, at: [152, 520]}"),
                "pickups[2].at: x=520.0 is outside the play field");
        assertProblem(
                text -> replace(
                        text, "    layer: low-air\n    size: [32, 24]", "    layer: ground\n    size: [32, 24]"),
                "only a trigger on low-air drifts");
        assertProblem(text -> replace(text, "    drift: [-24, -70]", "    drift: [-24, 70]"), "drifts down the screen");
        assertProblem(
                text -> replace(
                        text,
                        "  - {t: 112, formation: stream, enemy: lamprey, count: 4, from: rear,",
                        "  - {t: 112, formation: stream, enemy: skitter, count: 4, from: rear,"),
                "only chasers stream in from the rear");
        assertProblem(
                text -> replace(
                        text,
                        "  - {t: 196, formation: V-wing, enemy: needler, count: 5, from: front}",
                        "  - {t: 196, formation: V-wing, enemy: needler, count: 5, from: front, clockwise: true}"),
                "only a circle wave turns clockwise");
        assertProblem(text -> replace(text, "at: [-16, 150]", "at: [-60, 150]"), "is outside the play field");
        assertProblem(
                text -> replace(
                        text, "  at: [[60, 400], [110, 70], [165, 390]]", "  at: [[60, 500], [110, 70], [165, 390]]"),
                "sondes.at[0]");
        assertProblem(
                text -> replace(text, "  secondary: {collect: sondes,", "  secondary: {collect: seeds,"),
                "collect: sondes, was 'seeds'");
        assertProblem(
                text -> replace(text, "    at: [78, 87, 93,", "    at: [78, 78.2, 78.4, 78.6, 87, 93,"),
                "a fourth flash within a second");
        assertProblem(
                text -> replace(text, "  drift: {section: 4,", "  drift: {section: 9,"),
                "weather.drift.section: no section 9");
        assertProblem(
                text -> replace(text, "  section_db: {5: -6, 6: 0}", "  section_db: {5: 3, 6: 0}"),
                "section_db: must not be above full level");
        assertProblem(
                text -> replace(
                        text,
                        "  - {event: first-latch, action: SHAKE OFF,",
                        "  - {t: 30, event: first-latch, action: SHAKE OFF,"),
                "a prompt has its t or its event");
        assertProblem(
                text -> replace(
                        text,
                        "    seconds: 2.5      # including the ramps",
                        "    seconds: 0.4      # including the ramps"),
                "longer than the ramps up and down");
        assertProblem(text -> replace(text, "drop_factor: 1.5 ", "drop_factor: 0 "), "drop_factor must be > 0");
    }
}
