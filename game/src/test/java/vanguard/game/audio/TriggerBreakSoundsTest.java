package vanguard.game.audio;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Files;
import com.badlogic.gdx.backends.lwjgl3.audio.mock.MockAudio;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import vanguard.content.Content;
import vanguard.content.ContentLoader;
import vanguard.content.Difficulty;
import vanguard.content.SimSpecs;
import vanguard.game.render.EnemyLooks;
import vanguard.game.settings.AudioSettings;
import vanguard.sim.Command;
import vanguard.sim.Hitbox;
import vanguard.sim.LevelScript;
import vanguard.sim.PlayField;
import vanguard.sim.SimEvents;
import vanguard.sim.SimStep;
import vanguard.sim.Sortie;

/**
 * A trigger whose spent frame is a wreck (Level 08's billboard) breaks like a destructible of its
 * size on the hit that topples it: the small blast and, at 72×40 px, the structure's crumble. A
 * trigger that is opened rather than destroyed (Level 06's survey cache) is spent without them.
 */
class TriggerBreakSoundsTest {
    private static final Content CONTENT = ContentLoader.fromClasspath();
    private static final long NANOS_PER_STEP = 1_000_000_000L / SimStep.PER_SECOND;

    @Test
    void theBillboardsToppleBlastsAndCrumbles() {
        int[] played = spend("billboard");

        assertEquals(1, played[0], "the small blast");
        assertEquals(1, played[1], "the large crumble");
        assertEquals(0, played[2]);
    }

    @Test
    void theSurveyCacheIsShotOpenWithoutABlast() {
        int[] played = spend("survey-cache");

        assertEquals(0, played[0]);
        assertEquals(0, played[1] + played[2]);
    }

    /** Shoots a three-hit trigger with {@code look} until it is spent; the blasts, large and small crumbles playing in that step. */
    private static int[] spend(String look) {
        var trigger = new LevelScript.GroundObjectSpec(
                0,
                PlayField.WIDTH / 2.0,
                new Hitbox(72, 40),
                0,
                0,
                Optional.empty(),
                3,
                160,
                "billboard cache",
                false,
                0,
                1,
                Optional.empty(),
                look);
        var script = new LevelScript(
                8,
                2,
                0,
                List.of(new LevelScript.Section(20, 130)),
                List.of(),
                List.of(trigger),
                List.of(),
                1,
                List.of(),
                new LevelScript.Secondary(0.8, 50),
                List.of());
        String key = CONTENT.levelKey(1).orElseThrow();
        var sortie = new Sortie(
                1,
                SimSpecs.starterLoadout(CONTENT, Difficulty.MEDIUM),
                script,
                SimSpecs.rules(CONTENT, key, Difficulty.MEDIUM),
                60);
        long[] now = {0};
        var mixer = new Mixer(AudioSettings.defaults());
        var bank = new SfxBank(new MockAudio(), new Lwjgl3Files(), mixer, () -> now[0]);
        var sounds = new FlightSounds(bank, new EnemyLooks[0], sortie.armament(), List.of());
        for (int i = 0; i < SimStep.ticks(6); i++) {
            now[0] += NANOS_PER_STEP;
            sortie.step(Command.FIRE.bit());
            sounds.step();
            sounds.play(sortie.events());
            sounds.watch(sortie);
            if (sortie.events().count(SimEvents.Type.TRIGGER_SPENT) > 0) {
                return new int[] {
                    bank.playing(Sfx.EXPLOSION_SMALL_A) + bank.playing(Sfx.EXPLOSION_SMALL_C),
                    bank.playing(Sfx.CRUMBLE_LARGE),
                    bank.playing(Sfx.CRUMBLE_SMALL)
                };
            }
        }
        throw new AssertionError("the trigger was never spent");
    }
}
