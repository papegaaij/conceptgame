package vanguard.game.render;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import vanguard.content.Content;
import vanguard.content.ContentLoader;
import vanguard.content.Difficulty;
import vanguard.content.LevelData;
import vanguard.content.SimSpecs;
import vanguard.sim.LevelScript;
import vanguard.sim.SimStep;

/**
 * M5 part H's looks (design/campaign Level 14; design/enemies/bosses/siege-spire): the Spire's sprites
 * and pivots as the looks read them, every root's pod on its part of the data, the pods' headings, the
 * crown, mortar, maw and lift frames, the free spire's turn, the death frames over the 3 s chain; the
 * canopy's veins, tear and the low sun's beams on the timeline of tools/art/backdrop_l14.py; the root
 * bursts' cracks, dust and root over the telegraph and the eruption, the growths grown in from 0.35;
 * the armoured car's creep cleared hit by hit; the CDF supply drone's patch (step H9).
 */
class Level14LooksTest {
    private static final Path ASSETS = Path.of(System.getProperty("vanguard.assetsDir", "../assets"));
    private static final Content CONTENT = ContentLoader.fromClasspath();
    private static final String KEY = CONTENT.levelKey(14).orElseThrow();
    private static final LevelScript SCRIPT = SimSpecs.level(CONTENT, KEY, Difficulty.MEDIUM);

    @Test
    void theSpiresSpritesAreWhatTheLooksRead() {
        assertFrames("siege-spire", 3);
        assertFrames("siege-spire-root", 12);
        assertFrames("siege-spire-pod", SpireLooks.POD_HEADINGS);
        assertFrames("siege-spire-mortar", 3);
        assertFrames("siege-spire-husk", 2);
        assertFrames("siege-spire-lift", 6);
        assertFrames("siege-spire-free", 16);
        assertFrames("siege-spire-emitter", 4);
        assertFrames("siege-spire-maw-glow", 4);
        assertFrames("siege-spire-debris", 8);
        assertFrames("siege-spire-death", SpireLooks.DEATH_FRAMES);
        assertTrue(Files.isRegularFile(ASSETS.resolve("sprites/siege-spire-stump.png")));
        for (int k = 0; k < SpireLooks.TEAR_TIMES.length; k++) {
            assertTrue(Files.isRegularFile(ASSETS.resolve("backdrop/level-14/canopy-tear_" + k + ".png")), "tear " + k);
        }
        assertTrue(Files.isRegularFile(ASSETS.resolve("backdrop/level-14/daylight-beams_0.png")));
        assertTrue(Files.isRegularFile(ASSETS.resolve("backdrop/level-14/daylight-beams_1.png")));
    }

    @Test
    void everyRootsPodSitsOnItsPartOfTheData() {
        LevelScript.SetPieceSpec boss = SCRIPT.setPieces().stream()
                .filter(LevelScript.SetPieceSpec::isBoss)
                .findFirst()
                .orElseThrow();
        assertEquals(SpireLooks.SLUG, boss.slug());
        JsonValue pivots = pivots();
        JsonValue roots = pivots.get("roots");
        assertEquals(6, roots.size);
        Set<Integer> parts = new HashSet<>();
        for (int r = 0; r < roots.size; r++) {
            JsonValue root = roots.get(r);
            // The pivots are y down, the data y up.
            int part = SpireLooks.nearestPart(
                    boss.parts(), root.get("pod").getFloat(0), -root.get("pod").getFloat(1));
            assertTrue(part >= 0, "root " + r + "'s pod has its part");
            LevelScript.PartSpec spec = boss.parts().get(part);
            assertEquals(root.get("pod").getFloat(0), spec.dx(), 0.5, "root " + r + " x");
            assertEquals(-root.get("pod").getFloat(1), spec.dy(), 0.5, "root " + r + " y (up)");
            assertEquals(root.getString("kind").equals("mortar"), spec.name().startsWith("mortar"), spec.name());
            parts.add(part);
        }
        assertEquals(6, parts.size(), "a part a root");
        assertEquals(16, pivots.get("free").get("emitters").size, "emitter points per heading");
        assertEquals(6, pivots.get("free").get("emitters").get(0).size, "six emitters");
    }

    @Test
    void aTurretPodFacesTheShipAtTheNearestOf32Headings() {
        assertEquals(0, SpireLooks.podHeading(0, -100), "the ship straight below");
        assertEquals(8, SpireLooks.podHeading(-100, 0), "left: a quarter turn clockwise from down");
        assertEquals(16, SpireLooks.podHeading(0, 100), "up");
        assertEquals(24, SpireLooks.podHeading(100, 0), "right");
        assertEquals(1, SpireLooks.podHeading(-Math.tan(Math.toRadians(10)) * 100, -100), "10° rounds to one step");
    }

    @Test
    void theCrownOpensTheMortarsLobAndTheMawFlares() {
        assertEquals(0, SpireLooks.crownFrame(-0.1, 3), "shut before the maw is exposed");
        assertEquals(1, SpireLooks.crownFrame(0, 3), "half open as it starts");
        assertEquals(1, SpireLooks.crownFrame(SpireLooks.CROWN_OPEN_SECONDS * 0.49, 3));
        assertEquals(2, SpireLooks.crownFrame(SpireLooks.CROWN_OPEN_SECONDS * 0.5, 3));
        assertEquals(2, SpireLooks.crownFrame(30, 3), "open from then on");
        assertEquals(2, SpireLooks.mortarFrame(0, 3), "open as it lobs");
        assertEquals(1, SpireLooks.mortarFrame(SpireLooks.MORTAR_OPEN_TICKS, 3), "then closing");
        assertEquals(0, SpireLooks.mortarFrame(2L * SpireLooks.MORTAR_OPEN_TICKS, 3), "shut");
        assertEquals(0, SpireLooks.mortarFrame(Long.MAX_VALUE / 4, 3), "shut without a lob");
        assertEquals(0, SpireLooks.mawFlareFrame(0, 4));
        assertEquals(3, SpireLooks.mawFlareFrame(4L * SpireLooks.MAW_GLOW_FRAME_TICKS - 1, 4));
        assertEquals(-1, SpireLooks.mawFlareFrame(4L * SpireLooks.MAW_GLOW_FRAME_TICKS, 4), "played once");
        assertEquals(-1, SpireLooks.mawFlareFrame(-5, 4));
    }

    @Test
    void theSpireTearsLooseTurnsSlowlyAndBreaksOverItsChain() {
        assertEquals(0, SpireLooks.liftFrame(0, 6));
        assertEquals(3, SpireLooks.liftFrame(0.5, 6));
        assertEquals(5, SpireLooks.liftFrame(1, 6), "the last at the station");
        assertEquals(0, SpireLooks.freeHeading(0, 16));
        assertEquals(1, SpireLooks.freeHeading(SimStep.ticks(1 / SpireLooks.FREE_TURN_FPS), 16));
        assertEquals(0, SpireLooks.freeHeading(SimStep.ticks(16 / SpireLooks.FREE_TURN_FPS), 16), "it loops");
        int chain = SimStep.ticks(3);
        assertEquals(0, SpireLooks.deathFrame(0, chain, 8));
        assertEquals(3, SpireLooks.deathFrame(SimStep.ticks(1.49), chain, 8), "cracking open for 1.5 s");
        assertEquals(4, SpireLooks.deathFrame(SimStep.ticks(1.5), chain, 8), "then breaking apart");
        assertEquals(7, SpireLooks.deathFrame(chain - 1, chain, 8));
        assertEquals(-1, SpireLooks.deathFrame(chain, chain, 8), "nothing left after the chain");
        assertTrue(SpireLooks.shockRadius(0) < SpireLooks.shockRadius(0.5));
        assertEquals(SpireLooks.SHOCK_RADIUS, SpireLooks.shockRadius(SpireLooks.SHOCK_SECONDS), 1e-9);
    }

    @Test
    void theCanopyWithersTearsAndLetsTheSunInOnTheBackdropsTimeline() {
        assertEquals(1, SpireLooks.veins(-1), 1e-6, "the veins while the Spire lives");
        assertEquals(1, SpireLooks.veins(2), 1e-6, "until 2 s after its death");
        float half = SpireLooks.veins(4.5);
        assertTrue(half > 0.3 && half < 0.7, "fading: " + half);
        assertEquals(0, SpireLooks.veins(7), 1e-6, "gone by 7 s");
        assertEquals(-1, SpireLooks.tearFrame(0.49));
        assertEquals(0, SpireLooks.tearFrame(0.5));
        assertEquals(0, SpireLooks.tearFrame(2.49));
        assertEquals(1, SpireLooks.tearFrame(2.5));
        assertEquals(2, SpireLooks.tearFrame(5));
        assertEquals(3, SpireLooks.tearFrame(8.49));
        assertEquals(-1, SpireLooks.tearFrame(8.5), "the membrane is gone");
        assertEquals(-1, SpireLooks.beamFrame(2.49));
        assertEquals(0, SpireLooks.beamFrame(2.5));
        assertEquals(1, SpireLooks.beamFrame(6.25));
        assertEquals(0, SpireLooks.beamStrength(2.5), 1e-6);
        assertEquals(1, SpireLooks.beamStrength(5), 1e-6);
        assertEquals(1, SpireLooks.beamStrength(30), 1e-6, "the sun stays");
    }

    @Test
    void aRootBurstCracksRisesInDustAndBreaksOut() {
        List<LevelScript.RootBurst> bursts = SCRIPT.groundUnits().stream()
                .flatMap(unit -> unit.burst().stream())
                .distinct()
                .toList();
        assertEquals(4, bursts.size(), "four bursts");
        double telegraph = bursts.getFirst().telegraphSeconds();
        assertEquals(1, telegraph, 1e-9);
        assertEquals(0, RootBurstLooks.decalFrame(0, telegraph, 8));
        assertEquals(7, RootBurstLooks.decalFrame(0.99, telegraph, 8));
        assertEquals(7, RootBurstLooks.decalFrame(20, telegraph, 8), "the cracks stay");
        assertEquals(0, RootBurstLooks.puffFrame(0, telegraph, 12));
        assertEquals(11, RootBurstLooks.puffFrame(telegraph + RootBurstLooks.ERUPT_SECONDS - 0.01, telegraph, 12));
        assertEquals(-1, RootBurstLooks.puffFrame(telegraph + RootBurstLooks.ERUPT_SECONDS, telegraph, 12), "settled");
        assertEquals(0, RootBurstLooks.rootFrame(0, 6));
        assertEquals(5, RootBurstLooks.rootFrame(RootBurstLooks.ERUPT_SECONDS, 6));
        assertEquals(5, RootBurstLooks.rootFrame(10, 6), "the crater stays");
        assertFrames("root-burst-decal", 8);
        assertFrames("root-burst-puff", 12);
        assertFrames("root-burst-root", 6);
    }

    @Test
    void aGrowthGrowsInFromAThirdOfItsSizeOutOfTheCreepsViolet() {
        assertEquals(RootBurstLooks.GROW_FROM, RootBurstLooks.growScale(0), 1e-6);
        assertEquals(1, RootBurstLooks.growScale(1), 1e-6);
        assertTrue(RootBurstLooks.growScale(0.5) > 0.6 && RootBurstLooks.growScale(0.5) < 0.7);
        assertEquals(0.42f, RootBurstLooks.growTint(0, 0), 1e-6);
        assertEquals(0.3f, RootBurstLooks.growTint(0, 1), 1e-6);
        assertEquals(0.6f, RootBurstLooks.growTint(0, 2), 1e-6);
        for (int c = 0; c < 3; c++) {
            assertEquals(1, RootBurstLooks.growTint(1, c), 1e-6, "its own colours once grown");
        }
        assertNotEquals(RootBurstLooks.growTint(0.5, 0), RootBurstLooks.growTint(0.5, 2));
    }

    @Test
    void theArmouredCarsCreepClearsHitByHit() {
        assertFrames("armoured-car", 8);
        LevelScript.GroundObjectSpec car = SCRIPT.groundObjects().stream()
                .filter(object -> object.look().equals("armoured-car"))
                .findFirst()
                .orElseThrow();
        assertEquals(6, car.hits());
        assertTrue(TriggerBreak.hitByHit(8, car.hits()));
        assertFalse(TriggerBreak.hitByHit(3, 1), "a plain trigger look");
        assertFalse(TriggerBreak.hitByHit(3, 3), "Level 08's billboard: intact, hit, toppled");
        assertEquals(0, TriggerBreak.hitFrame(6, 6, Integer.MAX_VALUE), "all six blobs");
        assertEquals(1, TriggerBreak.hitFrame(6, 5, 0));
        assertEquals(5, TriggerBreak.hitFrame(6, 1, 0), "one blob left");
        assertEquals(6, TriggerBreak.hitFrame(6, 0, 0), "cleared as it is spent");
        assertEquals(6, TriggerBreak.hitFrame(6, 0, TriggerBreak.CLEARED_TICKS - 1));
        assertEquals(7, TriggerBreak.hitFrame(6, 0, TriggerBreak.CLEARED_TICKS), "then the hatch open");
    }

    private static JsonValue pivots() {
        return new JsonReader().parse(new FileHandle(new File(ASSETS.toFile(), "pivots/siege-spire.json")));
    }

    /**
     * Step H9: the armour patch at t=100 is Level 13's supply drone's drop: it flies in from below the
     * bottom edge (no helipad here), releases the patch at its point and leaves the same way, all before
     * the arena.
     */
    @Test
    void theArmourPatchIsCarriedInByTheSupplyDrone() {
        List<LevelData.Drop> drops = CONTENT.level(KEY).released();
        assertEquals(1, drops.size());
        LevelData.Drop drop = drops.getFirst();
        assertEquals(Optional.of("supply-drone"), drop.sprite());
        assertTrue(
                drop.t() - DroneLooks.ARC_SECONDS >= 0 && drop.t() + DroneLooks.ARC_SECONDS < 108, "before the arena");
        assertFrames("supply-drone", 4);
        assertFrames("supply-drone-open", 4);
        float[] at = new float[2];
        float padY = -DroneLooks.BELOW_EDGE;
        assertTrue(DroneLooks.position(-DroneLooks.ARC_SECONDS, 0, (float) drop.x(), padY, drop.x(), drop.y(), at));
        assertTrue(at[1] + 20 < 0, "it starts off the bottom edge: " + at[1]);
        assertTrue(DroneLooks.position(0, 0, (float) drop.x(), padY, drop.x(), drop.y(), at));
        assertEquals(drop.y(), at[1], 1e-3, "at its point as it releases the patch");
        assertTrue(DroneLooks.position(DroneLooks.ARC_SECONDS, 0, (float) drop.x(), padY, drop.x(), drop.y(), at));
        assertTrue(at[1] + 20 < 0, "and leaves off the bottom edge: " + at[1]);
    }

    private static void assertFrames(String name, int count) {
        for (int i = 0; i < count; i++) {
            assertTrue(Files.isRegularFile(ASSETS.resolve("sprites/" + name + "_" + i + ".png")), name + "_" + i);
        }
        assertFalse(
                Files.isRegularFile(ASSETS.resolve("sprites/" + name + "_" + count + ".png")),
                name + ": " + count + " frames");
    }
}
