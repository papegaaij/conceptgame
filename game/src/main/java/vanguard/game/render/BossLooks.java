package vanguard.game.render;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas.AtlasRegion;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.JsonValue;
import java.util.ArrayList;
import java.util.List;
import vanguard.sim.BossSpec;
import vanguard.sim.LevelScript;
import vanguard.sim.SetPiece;

/**
 * A boss in its production sprites (the Gorgon Frigate, tools/art/gorgon_frigate.py, pivots in
 * assets/pivots/{@code <slug>.json}): the neck pieces under the bell, each at the nearest of the
 * pre-rendered headings of its chain piece (nothing lit is turned at runtime); the bell in its
 * crown's stage, closed until the phase that exposes the core and opening over its first second;
 * the core's lime glow pulsing (additive) once exposed; the heads at their chains' tips, a
 * wrecked one as its torn stump, a hit part flashing white; and the boss bar with its name at the
 * top of the play field (design/ui/hud: shorter for a mid-boss). At its death the wreck where it
 * died (crown open, core dark) until its {@link SetPieceDeath break-up} replaces it by the chunks.
 * A boss without the bell (the Brood Carrier) is drawn by its {@link HullBossLooks}: its hull in its
 * pose with the sacs, the iris and the turrets, its shadow off the plane. M5 part E: an arena boss
 * (the Harbour Kraken) is drawn by {@link KrakenLooks}; only its bar is drawn here.
 */
final class BossLooks {
    private static final float X0 = PixelScreen.PLAY_FIELD_X;
    /** A part flashes white this many steps after a hit (as the set pieces). */
    private static final int HIT_FLASH_TICKS = 2;
    /** The crown opens over this long when the core is exposed (the simulation's crown opening). */
    private static final double CROWN_OPEN_SECONDS = 1;
    /** The core glow's pulse period, s. */
    private static final double PULSE_SECONDS = 0.8;

    private final FlashShader flash;
    /** The boss bar, on its production plate once it exists. */
    private final BossBar bar;
    /** The bell per crown stage, closed first; null without a boss in the level. */
    private final Array<AtlasRegion> bells;
    /** The neck pieces' heading sets, from the bell to the head. */
    private final List<Array<AtlasRegion>> necks;

    private final Array<AtlasRegion> heads;
    private final Array<AtlasRegion> stumps;
    private final AtlasRegion coreGlow;
    /** The bell centre in its sprite, px from the top left. */
    private final int originX;

    private final int originY;
    /** The heading sets: this many steps per turn, the first frame's step (clockwise from down). */
    private final int steps;

    private final int firstStep;
    /** The break-up at its death (tools/art/gorgon_frigate_death.py); null for none. */
    final SetPieceDeath death;
    /** A boss with one turning hull instead of the bell and necks; null for the frigate's kind. */
    final HullBossLooks hull;
    /** Level seconds when the core was first seen exposed; NaN while it is covered. */
    private double openedAt = Double.NaN;

    /**
     * @param spec the level's boss, or null when it has none (only the bar is then drawable)
     * @param pivots the boss's pivot file, or null
     */
    BossLooks(Sprites sprites, FlashShader flash, LevelScript.SetPieceSpec spec, JsonValue pivots) {
        this.flash = flash;
        this.bar = new BossBar(sprites);
        String slug = spec == null ? null : spec.slug();
        if (spec != null && spec.boss().flatMap(BossSpec::arena).isPresent()) {
            // M5 part E: an arena boss (the Harbour Kraken) is drawn by KrakenLooks; only its bar here.
            bells = heads = stumps = null;
            necks = null;
            coreGlow = null;
            originX = originY = steps = firstStep = 0;
            hull = null;
            death = null;
            return;
        }
        if (slug == null || !sprites.has(slug + "-bell")) {
            bells = heads = stumps = null;
            necks = null;
            coreGlow = null;
            originX = originY = steps = firstStep = 0;
            hull = slug == null ? null : new HullBossLooks(sprites, flash, spec, pivots);
            death = slug == null ? null : SetPieceDeath.of(sprites, pivots);
            return;
        }
        hull = null;
        bells = sprites.frames(slug + "-bell");
        List<Array<AtlasRegion>> pieces = new ArrayList<>();
        while (sprites.has(slug + "-neck-" + (pieces.size() + 1))) {
            pieces.add(sprites.frames(slug + "-neck-" + (pieces.size() + 1)));
        }
        necks = List.copyOf(pieces);
        heads = sprites.frames(slug + "-head");
        stumps = sprites.frames(slug + "-stump");
        coreGlow = sprites.region(slug + "-core-glow");
        int[] origin = pivots.get("bell").get("origin").asIntArray();
        originX = origin[0];
        originY = origin[1];
        steps = pivots.get("headings").getInt("steps");
        firstStep = pivots.get("headings").getInt("first");
        death = SetPieceDeath.of(sprites, pivots);
    }

    /**
     * The boss at its interpolated place: necks, bell with its crown, core glow, heads (a hull boss:
     * its hull and parts, the turrets toward the ship at ({@code shipX}, {@code shipY})).
     */
    void draw(
            SpriteBatch batch,
            SetPiece piece,
            float alpha,
            double seconds,
            float whiteFlash,
            double shipX,
            double shipY) {
        if (hull != null) {
            hull.draw(batch, piece, alpha, seconds, whiteFlash, shipX, shipY);
            return;
        }
        drawAt(batch, piece, piece.renderX(alpha), piece.renderY(alpha), seconds, whiteFlash, false);
    }

    /** A hull boss's shadow while it flies above the play plane; nothing for the frigate's kind. */
    void drawShadow(SpriteBatch batch, SetPiece piece, float alpha) {
        if (hull != null) {
            hull.drawShadow(batch, piece, alpha);
        }
    }

    /**
     * The destroyed boss where it died, until its break-up's swap: the necks as they were (gone
     * limp), the bell with its crown open over the burst core (no glow), the stumps (a hull boss:
     * its hull with the sacs burst, at {@code scale}).
     */
    void drawWreck(SpriteBatch batch, SetPiece piece, double x, double y, float scale) {
        if (hull != null) {
            Color tint = batch.getColor();
            hull.drawWreck(batch, piece, x, y, scale, tint.r, tint.a);
            return;
        }
        drawAt(batch, piece, x, y, 0, 0, true);
    }

    private void drawAt(
            SpriteBatch batch, SetPiece piece, double x, double y, double seconds, float whiteFlash, boolean wreck) {
        var boss = piece.boss().orElseThrow();
        var parts = piece.spec().parts();
        int i = 0;
        for (int c = 0; c < boss.chains().size(); c++) {
            BossSpec.Chain chain = boss.chains().get(c);
            double rest = rest(chain, parts.get(chain.part()));
            for (int k = 0; k < chain.segments(); k++, i++) {
                Array<AtlasRegion> set = necks.get(Math.min(k, necks.size() - 1));
                drawCentred(
                        batch,
                        set.get(headingFrame(rest + piece.chainAngle(c, k), set.size)),
                        x + piece.segmentOffsetX(i),
                        y + piece.segmentOffsetY(i));
            }
        }
        int core = coreOf(piece);
        boolean exposed =
                !wreck && core >= 0 && piece.settled() && !piece.partShielded(core) && !piece.partWrecked(core);
        if (!exposed) {
            openedAt = Double.NaN;
        } else if (Double.isNaN(openedAt)) {
            openedAt = seconds;
        }
        double opening = exposed ? Math.min(1, (seconds - openedAt) / CROWN_OPEN_SECONDS) : 0;
        int stage =
                wreck ? bells.size - 1 : exposed ? Math.min(bells.size - 1, 1 + (int) (opening * (bells.size - 1))) : 0;
        AtlasRegion bell = bells.get(stage);
        batch.draw(bell, Math.round(X0 + x - originX), Math.round(y - (bell.getRegionHeight() - originY)));
        if (exposed) {
            boolean hit = piece.partTicksSinceHit(core) < HIT_FLASH_TICKS;
            float pulse = (float) (0.75 + 0.25 * Math.sin(2 * Math.PI * seconds / PULSE_SECONDS));
            float level = hit ? 1 : (float) opening * pulse;
            batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE);
            batch.setColor(level, level, level, 1);
            drawCentred(batch, coreGlow, x + piece.partOffsetX(core), y + piece.partOffsetY(core));
            if (hit) {
                batch.setColor(whiteFlash, whiteFlash, whiteFlash, 1);
                drawCentred(batch, coreGlow, x + piece.partOffsetX(core), y + piece.partOffsetY(core));
            }
            batch.setColor(Color.WHITE);
            batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        }
        for (int p = 0; p < piece.partCount(); p++) {
            int c = chainOf(piece, p);
            if (c < 0) {
                continue;
            }
            BossSpec.Chain chain = boss.chains().get(c);
            int frame = headingFrame(rest(chain, parts.get(p)) + piece.chainAngle(c, chain.segments()), heads.size);
            double px = x + piece.partOffsetX(p);
            double py = y + piece.partOffsetY(p);
            if (wreck || piece.partWrecked(p)) {
                drawCentred(batch, stumps.get(frame), px, py);
            } else if (piece.partTicksSinceHit(p) < HIT_FLASH_TICKS) {
                flash.draw(batch, heads.get(frame), Math.round(X0 + px), Math.round(py), Color.WHITE, whiteFlash);
            } else {
                drawCentred(batch, heads.get(frame), px, py);
            }
        }
    }

    /** A chain's rest direction from its anchor to its part, radians counter-clockwise (y up). */
    private static double rest(BossSpec.Chain chain, LevelScript.PartSpec end) {
        return Math.atan2(end.dy() - chain.fromDy(), end.dx() - chain.fromDx());
    }

    /**
     * The frame of a heading set for a piece pointing at {@code angle} (radians counter-clockwise,
     * y up): the nearest step clockwise from straight down, clamped to the set's range.
     */
    private int headingFrame(double angle, int count) {
        double clockwise = -Math.PI / 2 - angle;
        clockwise -= 2 * Math.PI * Math.floor((clockwise + Math.PI) / (2 * Math.PI));
        int step = (int) Math.round(clockwise / (2 * Math.PI / steps));
        return Math.clamp(step - firstStep, 0, count - 1);
    }

    private static int coreOf(SetPiece piece) {
        for (int p = 0; p < piece.partCount(); p++) {
            if (piece.spec().parts().get(p).vital()) {
                return p;
            }
        }
        return -1;
    }

    private static int chainOf(SetPiece piece, int part) {
        var boss = piece.boss().orElseThrow();
        for (int c = 0; c < boss.chains().size(); c++) {
            if (boss.chains().get(c).part() == part) {
                return c;
            }
        }
        return -1;
    }

    /**
     * The boss bar at the top of the play field while a boss is present: its name over a bar of
     * its parts' remaining HP, shorter for a mid-boss.
     */
    void drawBar(SpriteBatch batch, BitmapFont font, SetPiece piece) {
        var boss = piece.boss().orElseThrow();
        bar.draw(batch, font, boss.barName(), boss.midBoss(), piece.barShare());
    }

    private static void drawCentred(SpriteBatch batch, TextureRegion region, double x, double y) {
        batch.draw(
                region,
                Math.round(X0 + x - region.getRegionWidth() / 2.0),
                Math.round(y - region.getRegionHeight() / 2.0));
    }
}
