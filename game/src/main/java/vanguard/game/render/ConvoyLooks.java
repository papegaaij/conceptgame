package vanguard.game.render;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas.AtlasRegion;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.JsonValue;
import java.util.Arrays;
import java.util.Optional;
import java.util.function.Function;
import vanguard.content.LevelData;
import vanguard.sim.Ally;
import vanguard.sim.LevelScript;
import vanguard.sim.PlayField;
import vanguard.sim.Road;
import vanguard.sim.Sortie;

/**
 * A level's road and convoy (design/campaign, Level 04; design/allies, civilian crawler): the road
 * as a ribbon on the ground layer between its tiles and its set pieces, textured with its backdrop
 * image ({@code road.texture}, Level 04's {@code road-texture}) or a flat placeholder colour without
 * one, and the convoy's units on it. A unit shows the nearest of its rendered
 * headings to the road's direction, its wheels turning with the ground it covers, a white flash on
 * a hit and smoke below its smoke share; a wreck shows its heading's wreck frame, burning and
 * smoking (the engine flame and a darkened small explosion stand in for fire and smoke effects).
 *
 * <p>M5 part E, a naval convoy (Level 11; design/allies, convoy cargo ship and escort frigate; the
 * sprites of tools/art/convoy_ships.py registered by assets/pivots/{@code <ally>}.json): each hull
 * bow up with its wake under it (fading out while the scroll halts, back when it resumes) and its foam
 * collar over its waterline; a cargo ship after its first slam listing in its damaged frame with its
 * deck fire (additive) and smoke puffs from the pivots; a hit flashing it white; a sunk one playing its
 * sinking steps in its foam ring where it went down (scrolling with the sea), then gone. The frigate's
 * bow gun flashes with each flak burst (the burst itself is an effect on low-air).
 */
final class ConvoyLooks {
    private static final float X0 = PixelScreen.PLAY_FIELD_X;
    /** Ground covered per wheel frame, px: the tread moves on a frame every step at 120 px/s. */
    private static final double PHASE_PX = 2;

    private static final int HIT_FLASH_TICKS = 2;
    private static final int SMOKE_FRAME_TICKS = 6;
    private static final int FIRE_FRAME_TICKS = 3;
    /** The road's placeholder colour: packed regolith, a shade darker than the mare. */
    private static final Color ROAD = new Color(0.36f, 0.35f, 0.33f, 1);

    private static final Color SMOKE = new Color(0.16f, 0.16f, 0.16f, 0.55f);
    private static final Color FIRE = new Color(1f, 0.62f, 0.25f, 1);

    private final Optional<Road> road;
    private final TextureRegion pixel;
    /** The road's texture, a strip as wide as the road repeating along it; null for the flat colour. */
    private final AtlasRegion texture;

    private final Array<AtlasRegion> frames;
    private final Array<AtlasRegion> wrecks;
    private final Array<AtlasRegion> smoke;
    private final Array<AtlasRegion> fire;
    private final FlashShader flash;
    private final int headings;
    private final double step;
    /** M5 part E: per naval unit its looks; empty without a naval convoy. */
    private final Ship[] ships;
    /** The scroll speed the wakes are drawn full at (the level's first section's). */
    private final double cruise;
    /** Per naval unit, the step of its last flak burst. */
    private long[] flakTick;

    private final Array<AtlasRegion> shipSmoke;

    /** A wake streams a frame every 3 steps (20 fps), a collar flickers a frame every 9, a hull sinks a step every 18 (3 s). */
    static final int WAKE_FRAME_TICKS = 3;

    static final int COLLAR_FRAME_TICKS = 9;
    static final int SINK_FRAME_TICKS = 18;
    private static final int FIRE_TICKS = 6;
    private static final int MUZZLE_FRAME_TICKS = 2;
    private static final int SHIP_SMOKE_TICKS = 6;
    /** A damaged hull's smoke: this many puffs drifting down-wind behind its pivot, this far apart. */
    private static final int PUFFS = 3;

    private static final float PUFF_STEP = 9;

    /**
     * A naval unit's looks: its hull frames (afloat, damaged), wake, collar, deck fire, sinking steps
     * and bow gun flash, with the pivots' offsets of each overlay's top left from the hull's (y down).
     */
    record Ship(
            AtlasRegion hull,
            AtlasRegion damaged,
            Array<AtlasRegion> wake,
            Array<AtlasRegion> collar,
            Array<AtlasRegion> fire,
            Array<AtlasRegion> sink,
            Array<AtlasRegion> muzzle,
            int[] wakeAt,
            int[] collarAt,
            int[] sinkAt,
            int[] fireAt,
            int[] smokeAt,
            int[] muzzleAt) {
        private static final Array<AtlasRegion> NONE = new Array<>(0);

        static Ship of(Sprites sprites, String slug, JsonValue pivots) {
            Array<AtlasRegion> hull = sprites.has(slug) ? sprites.frames(slug) : NONE;
            if (hull.isEmpty()) {
                return null;
            }
            return new Ship(
                    hull.first(),
                    sprites.has(slug + "-damaged")
                            ? sprites.frames(slug + "-damaged").first()
                            : hull.first(),
                    optional(sprites, slug + "-wake"),
                    optional(sprites, slug + "-collar"),
                    optional(sprites, slug + "-fire"),
                    optional(sprites, slug + "-sink"),
                    optional(sprites, slug + "-muzzle"),
                    point(pivots, "wake"),
                    point(pivots, "collar"),
                    point(pivots, "sink"),
                    point(pivots, "fire"),
                    point(pivots, "smoke"),
                    point(pivots, "muzzle"));
        }

        private static Array<AtlasRegion> optional(Sprites sprites, String name) {
            return sprites.has(name) ? sprites.frames(name) : NONE;
        }

        private static int[] point(JsonValue pivots, String name) {
            return pivots != null && pivots.has(name) ? pivots.get(name).asIntArray() : null;
        }
    }

    ConvoyLooks(
            Sprites sprites,
            FlashShader flash,
            LevelData level,
            LevelScript script,
            String levelKey,
            Function<String, JsonValue> pivots) {
        this.flash = flash;
        ships = script.convoy()
                .map(naval -> naval.units().stream()
                        .map(unit -> Ship.of(
                                sprites,
                                unit.ally().slug(),
                                pivots.apply(unit.ally().slug())))
                        .toArray(Ship[]::new))
                .orElse(new Ship[0]);
        flakTick = new long[ships.length];
        Arrays.fill(flakTick, Long.MIN_VALUE);
        cruise = script.sections().isEmpty()
                ? 1
                : Math.max(1, script.sections().getFirst().speed());
        shipSmoke = sprites.has("ship-smoke") ? sprites.frames("ship-smoke") : null;
        road = script.road();
        pixel = sprites.pixel;
        String image = level.road()
                .flatMap(LevelData.Road::texture)
                .map(id -> Backdrop.folder(levelKey) + id)
                .orElse("");
        texture = !image.isEmpty() && sprites.hasBackdrop(image)
                ? sprites.backdrop(image, 1).first()
                : null;
        // M5 part D: an air escort's units are drawn by ShuttleLooks.
        String slug = script.escort()
                .filter(escort -> escort.air().isEmpty())
                .map(escort -> escort.ally().slug())
                .orElse("");
        boolean drawn = !slug.isEmpty() && sprites.has(slug) && sprites.has(slug + "-wreck");
        frames = drawn ? sprites.frames(slug) : null;
        wrecks = drawn ? sprites.frames(slug + "-wreck") : null;
        smoke = sprites.explosionSmall;
        fire = sprites.has("engine-flame") ? sprites.frames("engine-flame") : null;
        headings = drawn ? wrecks.size : 1;
        double most =
                script.escort().map(escort -> escort.ally().maxHeadingDegrees()).orElse(0.0);
        step = headings > 1 ? 2 * most / (headings - 1) : 1;
    }

    /**
     * The road at the ground's scroll {@code scroll} (whole px, as the tiles): row by row, each
     * centred on the road's x at that height and widened by the road's bend so its width across
     * holds. The texture's row is the arc length along the road from its first point, repeating,
     * its bottom row first (tools/art/backdrop_l04.py, the road texture's contract). It ends at its
     * last point (Level 04: 40 px inside the terminal hangar's door).
     */
    void drawRoad(SpriteBatch batch, long scroll) {
        if (road.isEmpty()) {
            return;
        }
        Road curve = road.get();
        int rows = texture == null ? 2 : 1;
        if (texture == null) {
            batch.setColor(ROAD);
        }
        for (int y = 0; y < PlayField.HEIGHT; y += rows) {
            double along = scroll + y + rows / 2.0 - PlayField.HEIGHT / 2.0;
            if (along > curve.end()) {
                break;
            }
            double slope = curve.slope(along);
            float width = (float) (curve.width() * Math.sqrt(1 + slope * slope));
            float left = (float) (X0 + curve.x(along) - width / 2);
            if (texture == null) {
                batch.draw(pixel, left, y, width, rows);
            } else {
                int height = texture.getRegionHeight();
                int row = height - 1 - (int) Math.floorMod((long) Math.floor(curve.arc(along)), (long) height);
                batch.draw(
                        texture.getTexture(),
                        left,
                        y,
                        width,
                        rows,
                        texture.getRegionX(),
                        texture.getRegionY() + row,
                        texture.getRegionWidth(),
                        rows,
                        false,
                        false);
            }
        }
        batch.setColor(Color.WHITE);
    }

    /**
     * The convoy's units on the road, wrecks burning where they stopped; a unit past the road's end
     * has driven into the terminal (under the overhead hangar, wholly past its door) and is no longer drawn.
     */
    void drawConvoy(SpriteBatch batch, Sortie sortie, float alpha, float whiteFlash) {
        if (ships.length > 0) {
            drawShips(batch, sortie, alpha, whiteFlash);
            return;
        }
        if (frames == null) {
            return;
        }
        long tick = sortie.tick();
        for (int k = 0; k < sortie.allyCount(); k++) {
            Ally ally = sortie.ally(k);
            if (ally.state() == Ally.State.WAITING) {
                continue;
            }
            double x = ally.renderX(alpha);
            double y = ally.renderY(alpha);
            if (ally.alive() && pastEnd(sortie, y)) {
                continue;
            }
            int heading = heading(ally.headingDegrees());
            if (!ally.alive()) {
                draw(batch, wrecks.get(heading), x, y);
                drawSmoke(batch, x, y + 6, tick + k * 5L);
                drawFire(batch, x, y, tick + k * 3L);
                continue;
            }
            int perHeading = frames.size / headings;
            int phase = (int) (Math.floor(ally.travelled(alpha) / PHASE_PX) % perHeading);
            AtlasRegion frame = frames.get(heading * perHeading + phase);
            if (ally.ticksSinceHit() < HIT_FLASH_TICKS) {
                flash.draw(batch, frame, Math.round(X0 + x), Math.round(y), Color.WHITE, whiteFlash);
            } else {
                draw(batch, frame, x, y);
            }
            if (ally.hpShare() < sortie.script().escort().orElseThrow().ally().smokeBelow()) {
                drawSmoke(batch, x, y + 12, tick + k * 5L);
            }
        }
    }

    /** The level restarts: the frigate's gun flashes are forgotten. */
    void reset() {
        Arrays.fill(flakTick, Long.MIN_VALUE);
    }

    /** M5 part E: naval unit {@code k} fired a flak burst at step {@code tick}. */
    void flak(int k, long tick) {
        if (k >= 0 && k < flakTick.length) {
            flakTick[k] = tick;
        }
    }

    /** M5 part E: the naval convoy's hulls, the frigate's wake first (it trails the cargo ships). */
    private void drawShips(SpriteBatch batch, Sortie sortie, float alpha, float whiteFlash) {
        long tick = sortie.tick();
        float wake = wakeStrength(sortie.groundSpeed(), cruise);
        for (int k = ships.length - 1; k >= 0; k--) {
            Ship ship = ships[k];
            if (ship == null || k >= sortie.allyCount()) {
                continue;
            }
            Ally ally = sortie.ally(k);
            double x = ally.renderX(alpha);
            double y = ally.renderY(alpha);
            float left = (float) (x - ship.hull().getRegionWidth() / 2.0);
            float top = (float) (y + ship.hull().getRegionHeight() / 2.0);
            if (ally.lost()) {
                int step = sinkStep(ally.ticksSinceLost(), ship.sink().size);
                if (step >= 0 && ship.sinkAt() != null) {
                    drawAt(batch, ship.sink().get(step), left, top, ship.sinkAt());
                }
                continue;
            }
            if (wake > 0 && !ship.wake().isEmpty() && ship.wakeAt() != null) {
                batch.setColor(1, 1, 1, wake);
                drawAt(
                        batch,
                        ship.wake().get((int) ((tick / WAKE_FRAME_TICKS + 5L * k) % ship.wake().size)),
                        left,
                        top,
                        ship.wakeAt());
                batch.setColor(Color.WHITE);
            }
            boolean damaged = ally.hitsTaken() > 0;
            AtlasRegion hull = damaged ? ship.damaged() : ship.hull();
            if (ally.ticksSinceHit() < HIT_FLASH_TICKS) {
                flash.draw(batch, hull, Math.round(X0 + x), Math.round(y), Color.WHITE, whiteFlash);
            } else {
                draw(batch, hull, x, y);
            }
            if (!ship.collar().isEmpty() && ship.collarAt() != null) {
                drawAt(
                        batch,
                        ship.collar().get((int) ((tick / COLLAR_FRAME_TICKS + k) % ship.collar().size)),
                        left,
                        top,
                        ship.collarAt());
            }
            if (damaged) {
                drawDamage(batch, ship, left, top, tick + 7L * k);
            }
            long sinceFlak = tick - flakTick[k];
            if (!ship.muzzle().isEmpty()
                    && ship.muzzleAt() != null
                    && sinceFlak >= 0
                    && sinceFlak < (long) MUZZLE_FRAME_TICKS * ship.muzzle().size) {
                batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE);
                AtlasRegion muzzle = ship.muzzle().get((int) (sinceFlak / MUZZLE_FRAME_TICKS));
                batch.draw(
                        muzzle,
                        Math.round(X0 + left + ship.muzzleAt()[0] - muzzle.getRegionWidth() / 2f),
                        Math.round(top - ship.muzzleAt()[1] - muzzle.getRegionHeight() / 2f));
                batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
            }
        }
    }

    /** A damaged hull's smoke puffs drifting from its pivot and its deck fire (additive) on its own. */
    private void drawDamage(SpriteBatch batch, Ship ship, float left, float top, long tick) {
        if (shipSmoke != null && ship.smokeAt() != null) {
            for (int p = 0; p < PUFFS; p++) {
                int frame = (int) ((tick / SHIP_SMOKE_TICKS + 2L * p) % shipSmoke.size);
                AtlasRegion puff = shipSmoke.get(frame);
                batch.draw(
                        puff,
                        Math.round(X0 + left + ship.smokeAt()[0] + p * 2 - puff.getRegionWidth() / 2f),
                        Math.round(top - ship.smokeAt()[1] - p * PUFF_STEP - puff.getRegionHeight() / 2f));
            }
        }
        if (!ship.fire().isEmpty() && ship.fireAt() != null) {
            batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE);
            AtlasRegion fire = ship.fire().get((int) (tick / FIRE_TICKS % ship.fire().size));
            batch.draw(
                    fire,
                    Math.round(X0 + left + ship.fireAt()[0] - fire.getRegionWidth() / 2f),
                    Math.round(top - ship.fireAt()[1] - fire.getRegionHeight() / 2f));
            batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        }
    }

    /** An overlay whose top left is {@code at} (px right and down) from the hull's top left at ({@code left}, {@code top}). */
    private static void drawAt(SpriteBatch batch, TextureRegion region, float left, float top, int[] at) {
        batch.draw(region, Math.round(X0 + left + at[0]), Math.round(top - at[1] - region.getRegionHeight()));
    }

    /** How strongly the wakes show at the scroll {@code speed}: full at the cruise, fading out as the scroll halts. */
    static float wakeStrength(double speed, double cruise) {
        return (float) Math.clamp(speed / cruise, 0, 1);
    }

    /** The sinking step {@code ticksSinceLost} steps after a hull went down; -1 once it has played out. */
    static int sinkStep(int ticksSinceLost, int steps) {
        int step = ticksSinceLost / SINK_FRAME_TICKS;
        return step < steps ? step : -1;
    }

    /** The heading frame for {@code degrees} clockwise from straight up: the nearest one, clamped to the set. */
    int heading(double degrees) {
        int centre = (headings - 1) / 2;
        return Math.clamp(Math.round(degrees / step) + centre, 0, headings - 1);
    }

    private void drawSmoke(SpriteBatch batch, double x, double y, long tick) {
        batch.setColor(SMOKE);
        draw(batch, smoke.get((int) (tick / SMOKE_FRAME_TICKS % smoke.size)), x, y);
        batch.setColor(Color.WHITE);
    }

    private void drawFire(SpriteBatch batch, double x, double y, long tick) {
        if (fire == null) {
            return;
        }
        batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE);
        batch.setColor(FIRE);
        draw(batch, fire.get((int) (tick / FIRE_FRAME_TICKS % fire.size)), x - 6, y + 10);
        draw(batch, fire.get((int) ((tick / FIRE_FRAME_TICKS + 3) % fire.size)), x + 5, y - 8);
        batch.setColor(Color.WHITE);
        batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
    }

    private static void draw(SpriteBatch batch, TextureRegion region, double x, double y) {
        batch.draw(
                region,
                Math.round(X0 + x - region.getRegionWidth() / 2.0),
                Math.round(y - region.getRegionHeight() / 2.0));
    }

    /** Whether a point at screen height {@code y} lies beyond the road's last point. */
    private boolean pastEnd(Sortie sortie, double y) {
        return road.isPresent()
                && sortie.groundScroll() + y - PlayField.HEIGHT / 2.0
                        > road.get().end();
    }
}
