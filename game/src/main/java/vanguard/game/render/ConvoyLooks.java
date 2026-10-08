package vanguard.game.render;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas.AtlasRegion;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Array;
import java.util.Optional;
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

    ConvoyLooks(Sprites sprites, FlashShader flash, LevelData level, LevelScript script, String levelKey) {
        this.flash = flash;
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
