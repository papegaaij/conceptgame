package vanguard.sim;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * The {@code pack} formation (design/enemies, formation vocabulary; M5 part C): 3–5 walkers entering
 * together, each on its own ground path, 0.25 s apart.
 */
class PackFormationTest {
    private static final List<List<WaveSpec.At>> PATHS = List.of(
            List.of(new WaveSpec.At(-40, 200), new WaveSpec.At(520, 260)),
            List.of(new WaveSpec.At(-40, 240), new WaveSpec.At(520, 300)),
            List.of(new WaveSpec.At(520, 220), new WaveSpec.At(-40, 280)),
            List.of(new WaveSpec.At(520, 260), new WaveSpec.At(-40, 320)),
            List.of(new WaveSpec.At(-40, 280), new WaveSpec.At(520, 340)));

    private static List<Spawn> plan(int count, List<List<WaveSpec.At>> paths) {
        List<Spawn> spawns = new ArrayList<>();
        Formations.plan(
                PartCSpecs.walkers(10, WaveSpec.Formation.PACK, PartCSpecs.ravager(0.3, 3.0), count, paths),
                0,
                new SplitMix64(1),
                spawns);
        return spawns;
    }

    @Test
    void eachUnitWalksItsOwnPathAQuarterSecondApart() {
        List<Spawn> spawns = plan(4, PATHS);

        assertEquals(4, spawns.size());
        for (int i = 0; i < spawns.size(); i++) {
            Spawn spawn = spawns.get(i);
            assertEquals(SimStep.ticks(10 + i * 0.25), spawn.tick(), "unit " + i);
            WalkPath path = spawn.walk().orElseThrow();
            assertEquals(PATHS.get(i).getFirst().x(), path.x(0), 1e-9, "unit " + i + " on path " + i);
            assertEquals(PlayField.HEIGHT - PATHS.get(i).getFirst().depth(), path.y(0), 1e-9);
        }
    }

    @Test
    void aPackNeedsAPathPerUnit() {
        assertThrows(IllegalArgumentException.class, () -> plan(5, PATHS.subList(0, 4)));
    }

    @Test
    void onlyWalkersFormAPack() {
        WaveSpec wave = PartCSpecs.walkers(0, WaveSpec.Formation.PACK, TestSpecs.SKITTER, 3, List.of());
        assertThrows(
                IllegalArgumentException.class, () -> Formations.plan(wave, 0, new SplitMix64(1), new ArrayList<>()));
    }
}
