package vanguard.game.render;

import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas.AtlasRegion;
import com.badlogic.gdx.utils.Array;
import java.util.List;
import vanguard.content.LevelData;
import vanguard.sim.Sortie;

/**
 * M5 part G, the supply drones (design/allies, CDF supply drone; user decision G6 = a; the sprites of
 * tools/art/l13_props.py): presentation only, nobody can hit them. Each lifts off the defended ally's
 * helipad {@value #ARC_SECONDS} s before its drop, flies a slow arc out to one side and back over the
 * relay to the drop's point, carrying the armour patch in its bay, and releases it there as the
 * simulation places the pickup ({@code PICKUP_PLACED}); with its clamps open it then flies the other
 * half of the loop back down to the helipad. The drones alternate sides. Without a defended ally
 * (Level 14, step H9) the drone flies in from below the bottom edge under its point and leaves the
 * same way. Its rotors blur at
 * {@value #ROTOR_FPS} fps, its navigation lights blink (the belly strobe {@value #STROBE_SECONDS} s
 * every {@value #STROBE_EVERY} s). All on the level clock, so a pause or {@code --debug-speed} keeps
 * in step; nothing is allocated per frame.
 */
public final class DroneLooks {
    private static final float X0 = PixelScreen.PLAY_FIELD_X;
    /** The arc from the helipad to the drop's point, and the flight back, s (the drop's event comes at its end). */
    public static final double ARC_SECONDS = 4;
    /** How far out to its side the arc swings, px, and how far above the higher end it bulges. */
    static final float SWING = 110;

    static final float BULGE = 30;
    /** Without a helipad, how far below the bottom edge the drone's flight starts and ends, px (its 40 px sprite off the screen). */
    static final float BELOW_EDGE = 24;

    static final double ROTOR_FPS = 20;
    static final double STROBE_EVERY = 0.8;
    static final double STROBE_SECONDS = 0.1;
    /** The patch in the bay is the pickup's sprite at this scale. */
    static final float PATCH_SCALE = 0.75f;

    private final List<LevelData.Drop> drops;
    private final Array<AtlasRegion> closed;
    private final Array<AtlasRegion> open;
    private final Array<AtlasRegion> lights;
    private final AtlasRegion patch;
    private final float[] at = new float[2];

    private DroneLooks(
            List<LevelData.Drop> drops,
            Array<AtlasRegion> closed,
            Array<AtlasRegion> open,
            Array<AtlasRegion> lights,
            AtlasRegion patch) {
        this.drops = drops;
        this.closed = closed;
        this.open = open;
        this.lights = lights;
        this.patch = patch;
    }

    /** The level's drones, or null for a level without drops carried by a drone (or without its sprites). */
    static DroneLooks of(Sprites sprites, LevelData level) {
        List<LevelData.Drop> drops = level.released().stream()
                .filter(drop -> drop.sprite().filter(sprites::has).isPresent())
                .toList();
        if (drops.isEmpty()) {
            return null;
        }
        String slug = drops.getFirst().sprite().orElseThrow();
        return new DroneLooks(
                drops,
                sprites.frames(slug),
                sprites.has(slug + "-open") ? sprites.frames(slug + "-open") : sprites.frames(slug),
                sprites.has(slug + "-lights") ? sprites.frames(slug + "-lights") : new Array<>(0),
                sprites.armourPatch.first());
    }

    /**
     * Where drone {@code index} is {@code since} s after its drop (negative before it), from the helipad
     * at ({@code padX}, {@code padY}) to the drop's point ({@code dropX}, {@code dropY}) and back; y up.
     * The way out is a quadratic curve swinging {@link #SWING} px to its side, the way back swings to the
     * other side. Returns false outside the flight.
     */
    static boolean position(double since, int index, float padX, float padY, double dropX, double dropY, float[] out) {
        if (!(since >= -ARC_SECONDS) || since > ARC_SECONDS) {
            return false;
        }
        float side = index % 2 == 0 ? 1 : -1;
        boolean outbound = since <= 0;
        double u = outbound ? (since + ARC_SECONDS) / ARC_SECONDS : since / ARC_SECONDS;
        // Ease in and out along the curve: a slow lift, a slow arrival.
        u = u * u * (3 - 2 * u);
        double fromX = outbound ? padX : dropX;
        double fromY = outbound ? padY : dropY;
        double toX = outbound ? dropX : padX;
        double toY = outbound ? dropY : padY;
        double controlX = (padX + dropX) / 2 + (outbound ? side : -side) * SWING;
        double controlY = Math.max(padY, dropY) + BULGE;
        double a = (1 - u) * (1 - u);
        double b = 2 * (1 - u) * u;
        double c = u * u;
        out[0] = (float) (a * fromX + b * controlX + c * toX);
        out[1] = (float) (a * fromY + b * controlY + c * toY);
        return true;
    }

    /** The navigation lights' frame at real {@code seconds}: the strobe's frame 0 for its flash, else frame 1. */
    static int lightsFrame(double seconds, int frames) {
        if (frames < 2) {
            return frames - 1;
        }
        double into = seconds - Math.floor(seconds / STROBE_EVERY) * STROBE_EVERY;
        return into < STROBE_SECONDS ? 0 : 1;
    }

    /**
     * The drones in flight at level time {@code seconds}, while the defended ally stands (its helipad from
     * {@code relay}; without one each comes in from {@link #BELOW_EDGE} px below the bottom edge under its
     * point).
     */
    void draw(SpriteBatch batch, Sortie sortie, RelayLooks relay, float alpha, double seconds) {
        boolean standing = sortie.allyCount() > 0 && sortie.ally(0).alive();
        if (relay != null && !standing) {
            return;
        }
        for (int i = 0; i < drops.size(); i++) {
            LevelData.Drop drop = drops.get(i);
            double since = seconds - drop.t();
            float padX;
            float padY;
            if (relay != null) {
                padX = relay.helipadX(sortie.ally(0).renderX(alpha));
                padY = relay.helipadY(sortie.ally(0).renderY(alpha));
            } else {
                padX = (float) drop.x();
                padY = -BELOW_EDGE;
            }
            if (!position(since, i, padX, padY, drop.x(), drop.y(), at)) {
                continue;
            }
            boolean carrying = since < 0;
            Array<AtlasRegion> body = carrying ? closed : open;
            AtlasRegion frame =
                    body.get((int) Math.floorMod((long) Math.floor(seconds * ROTOR_FPS) + i, (long) body.size));
            float x = Math.round(X0 + at[0] - frame.getRegionWidth() / 2f);
            float y = Math.round(at[1] - frame.getRegionHeight() / 2f);
            batch.draw(frame, x, y);
            if (carrying) {
                float w = patch.getRegionWidth() * PATCH_SCALE;
                float h = patch.getRegionHeight() * PATCH_SCALE;
                batch.draw(patch, Math.round(X0 + at[0] - w / 2), Math.round(at[1] - h / 2), w, h);
            }
            int light = lightsFrame(seconds, lights.size);
            if (light >= 0) {
                batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE);
                batch.draw(lights.get(light), x, y);
                batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
            }
        }
    }

    /** The drops' times, for the launch sound ({@code FlightSounds}). */
    List<LevelData.Drop> drops() {
        return drops;
    }
}
