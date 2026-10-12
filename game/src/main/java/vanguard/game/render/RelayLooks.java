package vanguard.game.render;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas.AtlasRegion;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.JsonValue;
import vanguard.sim.Ally;
import vanguard.sim.LevelScript;
import vanguard.sim.Sortie;

/**
 * M5 part G, a defend primary's ally (design/allies, Nansen Relay; design/campaign Level 13; the sprites
 * of tools/art/relay.py, variant a until round 35's pick, with assets/pivots/{@code <ally>}.json): the
 * relay standing on the ground in its state's frame (intact, damaged below its smoke share, critical
 * below {@value #CRITICAL_SHARE}, destroyed), a white flash on each hit, the shared ship smoke rising
 * from its state's smoke points and (critical) its fires flickering at the fire points; destroyed, dark
 * with its heavy smoke column rising from the column point; and once its synchronisation completes, its
 * lights coming on in sequence ({@value #SEQUENCE_FRAMES} frames at {@value #SEQUENCE_FPS} fps), then
 * their steady loop. Nothing is allocated per frame; a level without a defend draws nothing.
 */
final class RelayLooks {
    private static final float X0 = PixelScreen.PLAY_FIELD_X;
    /** Below this share of its integrity the relay is critical: its third frame, burning. */
    static final double CRITICAL_SHARE = 0.25;
    /** The state frames: intact, damaged, critical, destroyed. */
    static final int INTACT = 0;

    static final int DAMAGED = 1;
    static final int CRITICAL = 2;
    static final int DESTROYED = 3;
    /** A hit flashes it white this many steps (as the convoy's). */
    private static final int HIT_FLASH_TICKS = 2;
    /** The lights' sequence: 8 frames at 4 fps (2 s), then the steady loop's 2 frames at 2 fps. */
    static final int SEQUENCE_FRAMES = 8;

    static final double SEQUENCE_FPS = 4;
    static final double LOOP_FPS = 2;
    /** The fires flicker at 8 fps. */
    private static final double FIRE_FPS = 8;
    /**
     * The damage smoke (tools/art/relay.py's review values; G7 capture fix: a static stack of three 16 px
     * puffs did not read on the ice; now each at {@value #SMOKE_SCALE}x): from each smoke point a puff
     * every {@value #SMOKE_EVERY} s, rising {@value #SMOKE_RISE} px/s and drifting {@value #SMOKE_DRIFT}
     * px/s with the wind through its frames at {@value #SMOKE_FPS} fps, so it trails off the relay; each
     * point a little out of step with the others.
     */
    static final double SMOKE_EVERY = 0.3;

    static final double SMOKE_FPS = 4;
    static final double SMOKE_RISE = 30;
    static final double SMOKE_DRIFT = 10;
    /** The shared ship smoke is a 16 px puff: on the relay's 96 px over the white ice it is drawn at 2x. */
    static final float SMOKE_SCALE = 2;

    private static final double SMOKE_STAGGER = 0.11;
    /**
     * The destroyed relay's smoke column (tools/art/relay.py's review values): a puff every {@value
     * #PUFF_EVERY} s from the column point, rising {@value #RISE} px/s and drifting {@value #DRIFT} px/s
     * with the wind, living through its {@value #PUFF_LIFE} s of frames.
     */
    static final double PUFF_EVERY = 0.15;

    static final double RISE = 42;
    static final double DRIFT = 16;
    static final double PUFF_LIFE = 2;

    private final Array<AtlasRegion> states;
    private final Array<AtlasRegion> lights;
    private final Array<AtlasRegion> fire;
    private final Array<AtlasRegion> column;
    private final Array<AtlasRegion> smoke;
    private final FlashShader flash;
    /** The damage smoke's tint: the ship smoke as it is, the small explosion as dark smoke. */
    private final Color smokeTint;

    private static final Color DARK_SMOKE = new Color(0.16f, 0.16f, 0.16f, 0.55f);
    /** Per state its smoke and fire points (px from the frame's top left), and the column's point; null without pivots. */
    private final int[][][] smokeAt;

    private final int[][][] fireAt;
    private final int[] columnAt;
    /** The helipad's centre, px from the frame's top left (the supply drone's lift-off). */
    private final float[] helipad;
    /** The level time the synchronisation completes (its lights come on); NaN without a defend. */
    private final double online;
    /** The real time the relay was first seen destroyed in this attempt; NaN while it stands. */
    private double lostAt = Double.NaN;

    private RelayLooks(
            Array<AtlasRegion> states,
            Array<AtlasRegion> lights,
            Array<AtlasRegion> fire,
            Array<AtlasRegion> column,
            Array<AtlasRegion> smoke,
            FlashShader flash,
            JsonValue pivots,
            double online) {
        this.states = states;
        this.lights = lights;
        this.fire = fire;
        this.column = column;
        this.smoke = smoke;
        this.flash = flash;
        this.online = online;
        smokeTint = smoke.size > 0 && smoke.first().name.startsWith("ship-smoke") ? Color.WHITE : DARK_SMOKE;
        smokeAt = points(pivots, "smoke");
        fireAt = points(pivots, "fire");
        columnAt = pivots != null && pivots.has("column") ? pivots.get("column").asIntArray() : null;
        helipad =
                pivots != null && pivots.has("helipad") ? pivots.get("helipad").asFloatArray() : null;
    }

    /** The defend's ally's looks, or null for a level without a defend or without the ally's sprites. */
    static RelayLooks of(
            Sprites sprites,
            FlashShader flash,
            LevelScript script,
            java.util.function.Function<String, JsonValue> pivots) {
        if (script.defend().isEmpty()) {
            return null;
        }
        LevelScript.Defend defend = script.defend().get();
        String slug = defend.ally().slug();
        if (!sprites.has(slug)) {
            return null;
        }
        return new RelayLooks(
                sprites.frames(slug),
                optional(sprites, slug + "-lights"),
                optional(sprites, slug + "-fire"),
                optional(sprites, slug + "-smoke"),
                // The shared ship smoke where the level's atlas has it, else the small explosion darkened
                // (as a burning crawler's, ConvoyLooks).
                sprites.has("ship-smoke") ? sprites.frames("ship-smoke") : sprites.explosionSmall,
                flash,
                pivots.apply(slug),
                defend.end());
    }

    private static Array<AtlasRegion> optional(Sprites sprites, String name) {
        return sprites.has(name) ? sprites.frames(name) : new Array<>(0);
    }

    private static int[][][] points(JsonValue pivots, String name) {
        if (pivots == null || !pivots.has(name)) {
            return null;
        }
        JsonValue states = pivots.get(name);
        int[][][] out = new int[states.size][][];
        for (int s = 0; s < states.size; s++) {
            JsonValue state = states.get(s);
            out[s] = new int[state.size][];
            for (int p = 0; p < state.size; p++) {
                out[s][p] = state.get(p).asIntArray();
            }
        }
        return out;
    }

    /** The state frame for an ally at {@code share} of its integrity: destroyed, critical, damaged below {@code smokeBelow}, intact. */
    static int state(boolean lost, double share, double smokeBelow) {
        if (lost) {
            return DESTROYED;
        }
        return share < CRITICAL_SHARE ? CRITICAL : share < smokeBelow ? DAMAGED : INTACT;
    }

    /**
     * The lights' frame {@code since} s after the synchronisation completed: the sequence's frames, then
     * the steady loop's two; -1 before it.
     */
    static int lightsFrame(double since, int frames) {
        if (!(since >= 0) || frames == 0) {
            return -1;
        }
        int step = (int) Math.floor(since * SEQUENCE_FPS);
        if (step < SEQUENCE_FRAMES || frames <= SEQUENCE_FRAMES) {
            return Math.min(step, Math.min(SEQUENCE_FRAMES, frames) - 1);
        }
        double loop = since - SEQUENCE_FRAMES / SEQUENCE_FPS;
        int loopFrames = frames - SEQUENCE_FRAMES;
        return SEQUENCE_FRAMES + (int) Math.floorMod((long) Math.floor(loop * LOOP_FPS), (long) loopFrames);
    }

    /** The level restarts: the relay stands again. */
    void reset() {
        lostAt = Double.NaN;
    }

    /** The helipad's centre on the screen for the ally at (x, y) (y up): the supply drone's lift-off; the centre without pivots. */
    float helipadX(double x) {
        return helipad == null ? (float) x : (float) (x - states.first().getRegionWidth() / 2.0 + helipad[0]);
    }

    float helipadY(double y) {
        return helipad == null ? (float) y : (float) (y + states.first().getRegionHeight() / 2.0 - helipad[1]);
    }

    /**
     * The relay in its state with its hit flash, damage smoke and fires, and its lights once online.
     *
     * @param seconds the level clock (interpolated)
     * @param real the real clock (the column of a destroyed relay rises on it)
     */
    void draw(SpriteBatch batch, Sortie sortie, float alpha, float whiteFlash, double seconds, double real) {
        if (sortie.allyCount() == 0) {
            return;
        }
        Ally ally = sortie.ally(0);
        if (ally.state() == Ally.State.WAITING) {
            return;
        }
        double x = ally.renderX(alpha);
        double y = ally.renderY(alpha);
        int state = Math.min(
                states.size - 1,
                state(ally.lost(), ally.hpShare(), sortie.allySpec(0).smokeBelow()));
        AtlasRegion frame = states.get(state);
        float left = (float) (x - frame.getRegionWidth() / 2.0);
        float top = (float) (y + frame.getRegionHeight() / 2.0);
        if (!ally.lost() && ally.ticksSinceHit() < HIT_FLASH_TICKS) {
            flash.draw(batch, frame, Math.round(X0 + x), Math.round(y), Color.WHITE, whiteFlash);
        } else {
            batch.draw(frame, Math.round(X0 + left), Math.round(top - frame.getRegionHeight()));
        }
        if (state != INTACT && state != DESTROYED && smokeAt != null && state < smokeAt.length && smoke.size > 0) {
            batch.setColor(smokeTint);
            double life = smoke.size / SMOKE_FPS;
            int puffs = (int) Math.ceil(life / SMOKE_EVERY);
            for (int p = 0; p < smokeAt[state].length; p++) {
                int[] at = smokeAt[state][p];
                double clock = real + p * SMOKE_STAGGER;
                // The oldest first, so the newest puff sits over them at the smoke point.
                for (int k = puffs; k >= 0; k--) {
                    double age = clock - (Math.floor(clock / SMOKE_EVERY) - k) * SMOKE_EVERY;
                    if (age < 0 || age >= life) {
                        continue;
                    }
                    AtlasRegion puff = smoke.get(Math.min(smoke.size - 1, (int) (age * SMOKE_FPS)));
                    float w = puff.getRegionWidth() * SMOKE_SCALE;
                    float h = puff.getRegionHeight() * SMOKE_SCALE;
                    batch.draw(
                            puff,
                            Math.round(X0 + left + at[0] + SMOKE_DRIFT * age - w / 2),
                            Math.round(top - at[1] + SMOKE_RISE * age - h / 2),
                            w,
                            h);
                }
            }
            batch.setColor(Color.WHITE);
        }
        batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE);
        if (fireAt != null && state < fireAt.length && fire.size > 0) {
            for (int p = 0; p < fireAt[state].length; p++) {
                int[] at = fireAt[state][p];
                AtlasRegion flame =
                        fire.get((int) Math.floorMod((long) Math.floor(real * FIRE_FPS) + p, (long) fire.size));
                batch.draw(
                        flame,
                        Math.round(X0 + left + at[0] - flame.getRegionWidth() / 2f),
                        Math.round(top - at[1] - flame.getRegionHeight() / 2f));
            }
        }
        int light = ally.lost() ? -1 : lightsFrame(seconds - online, lights.size);
        if (light >= 0) {
            batch.draw(lights.get(light), Math.round(X0 + left), Math.round(top - frame.getRegionHeight()));
        }
        batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        if (ally.lost() && Double.isNaN(lostAt)) {
            lostAt = real;
        } else if (!ally.lost()) {
            lostAt = Double.NaN;
        }
    }

    /**
     * A destroyed relay's smoke column over the ground units: a puff every {@value #PUFF_EVERY} s from
     * the column point since it fell, each rising and drifting through its frames.
     */
    void drawColumn(SpriteBatch batch, Sortie sortie, float alpha, double real) {
        if (Double.isNaN(lostAt) || columnAt == null || column.size == 0 || sortie.allyCount() == 0) {
            return;
        }
        Ally ally = sortie.ally(0);
        AtlasRegion body = states.first();
        float baseX = (float) (ally.renderX(alpha) - body.getRegionWidth() / 2.0 + columnAt[0]);
        float baseY = (float) (ally.renderY(alpha) + body.getRegionHeight() / 2.0 - columnAt[1]);
        double since = real - lostAt;
        int puffs = (int) Math.ceil(PUFF_LIFE / PUFF_EVERY);
        // The oldest first, so the newest puff sits over them at the foot of the column.
        for (int k = puffs; k >= 0; k--) {
            double emitted = Math.floor(since / PUFF_EVERY) - k;
            double age = since - emitted * PUFF_EVERY;
            if (emitted < 0 || age < 0 || age >= PUFF_LIFE) {
                continue;
            }
            TextureRegion puff = column.get(Math.min(column.size - 1, (int) (age / PUFF_LIFE * column.size)));
            batch.draw(
                    puff,
                    Math.round(X0 + baseX + DRIFT * age - puff.getRegionWidth() / 2f),
                    Math.round(baseY + RISE * age - puff.getRegionHeight() / 2f));
        }
    }
}
