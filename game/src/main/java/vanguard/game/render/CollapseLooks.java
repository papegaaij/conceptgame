package vanguard.game.render;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas.AtlasRegion;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Array;
import java.util.SplittableRandom;
import vanguard.content.BackdropData;
import vanguard.content.LevelData;
import vanguard.sim.Sortie;

/**
 * Level 09's collapse as drawn (M5 part C, user decision D5 = a; round 31's look c, approved by the
 * user on 2026-10-07; design/campaign/act-2-homefront/level-09-arcology-fall, Collapse set piece, and
 * the collapse-r31-c entry of its concept/prompts.md, whose numbers this class follows). Once its
 * groups are cleared the arcology leans slowly to the right over the warning (4° at its end, eased in
 * w^1.6, bending from its foot, a small shudder), debris and dust wisps trickling off its walls, its
 * cast shadow (half the kit's length) on the ground under it; then it drops straight down into
 * itself (its height eased 0.25 p + 0.75 p², the lean relaxing to 0.65 of its end, the top storeys
 * crushed, the roof greying with dust, the shadow shrinking toward the foot, chunks spat out); dust
 * boils out of its base 0.35 s before the impact; at the impact (warning + drop) it is gone, the
 * rubble heap fades in at its foot, a radial dust blast of puff sprites rolls out (its front the
 * simulation's kill ring: the collapse's {@code blast}), a billow rises over the heap and chunks are
 * thrown out; the dust settles over a few seconds while the {@code heavy} dust peak ramps out over
 * the collapse's {@code dust.seconds}.
 *
 * <p>The tower itself is drawn by {@link TowerProjection} (bent, see {@link Backdrop#bendTower});
 * this class works out the {@link View} each frame and draws the rest through its {@link Fall}: the
 * shadow, the heap, the landing puffs and the base dust between the ground pieces and the towers
 * ({@link Backdrop#underTowers}), the lifted dust and debris over the ground units. The particles are
 * seeded once and placed relative to the tower's foot on the ground, so they scroll with it; nothing
 * is allocated per frame.
 */
final class CollapseLooks {
    private static final float X0 = PixelScreen.PLAY_FIELD_X;
    /** The lean at the warning's end, radians, to the right. */
    static final double LEAN = Math.toRadians(4);
    /** The lean's ease over the warning: w^1.6, slow at first, then faster. */
    static final double LEAN_EASE = 1.6;
    /** How much of the lean relaxes through the drop (to 0.65 of it at the impact). */
    static final double LEAN_RELAX = 0.35;
    /** The crushed storeys at the top of the walls through the drop, a share of the full height (28 of 400 px). */
    static final double CRUSH = 28 / 400.0;
    /** How grey the roof is with dust at the impact. */
    static final double ROOF_DUST = 0.5;
    /** The base dust starts this long before the impact, s; the heap fades in over 0.2 s from 0.1 s before it. */
    static final double BASE_DUST_LEAD = 0.35;
    /** The dust peak is full this long after the impact, s. */
    static final double PEAK_FULL = 0.3;
    /** The cast shadow's darkness (the kit's shadows at half their length are 40 %). */
    static final float SHADOW_OPACITY = 0.4f;
    /**
     * The cast shadow of a point z px up: the kit's drop_shadow rule (key light (−0.55, 0.6, 0.75))
     * at half its length (the user, 2026-10-07): 0.5 z ÷ 0.921 px along (0.676, −0.737), right and down.
     */
    static final double SHADOW_X = 0.5 * 0.676 / 0.921;

    static final double SHADOW_Y = -0.5 * 0.737 / 0.921;
    /** The shadow's soft edge: this many layers, each 1 px further out. */
    private static final int SHADOW_LAYERS = 4;
    /** Gravity for the debris at the concept's scale, px/s² (a 400 px fall in about 1.25 s). */
    static final double GRAVITY = 520;
    /** The puff sprites' size, px. */
    static final int PUFF = 128;
    /** The shudder's new value this many times a second. */
    private static final double SHUDDER_RATE = 20;
    /** The specks' colour (150, 145, 150) times their tone. */
    private static final float SPECK_R = 150 / 255f;

    private static final float SPECK_G = 145 / 255f;
    private static final float SPECK_B = 150 / 255f;

    /** Where the collapse stands this frame; one instance, refilled by {@link #update}. */
    static final class View {
        /** Whether the collapse has started in this attempt (its warning and on). */
        boolean started;
        /** Real seconds since the warning started, at the render time. */
        double seconds;
        /** The warning (the lean), the drop and the blast, s; the impact at warning + drop. */
        double warning;

        double drop;
        double blast;
        /** The blast's front: from this radius to that, px round the foot's centre (the kill ring). */
        double blastFrom;

        double blastTo;
        /** The falling tower's index in the backdrop's towers; -1 when none lies in the band. */
        int tower = -1;
        /** The tower's footprint's centre, play-field x and y (up), at the render time; its half sizes. */
        double footX;

        double footY;
        double halfWidth;
        double halfHeight;
        /** The tower's full height, px (its camera units times {@link TowerProjection#UNIT}). */
        double height;
        /** The lean now, radians; the height left, a share of the full one. */
        double lean;

        double share = 1;

        /** Seconds from the warning's start to the impact. */
        double impact() {
            return warning + drop;
        }

        /** Seconds since the impact; negative before it. */
        double sinceImpact() {
            return seconds - impact();
        }
    }

    /** The look of the fall: what it draws under the towers and over the ground units. */
    interface Fall {
        /** On the ground layer, over its pieces and under the towers (the shadow, the heap, the base dust). */
        void drawUnder(SpriteBatch batch, View view);

        /** Over the ground units, under the flyers (the dust and the debris thrown up by the fall). */
        void drawOver(SpriteBatch batch, View view);
    }

    private final LevelData.Collapse collapse;
    private final Backdrop backdrop;
    private final View view = new View();
    private final Fall fall;

    /**
     * @param folder the level's backdrop atlas folder ({@code level-09/})
     */
    CollapseLooks(Sprites sprites, LevelData.Collapse collapse, Backdrop backdrop, String folder) {
        this.collapse = collapse;
        this.backdrop = backdrop;
        String heapName = folder + collapse.rubble();
        AtlasRegion heap =
                sprites.hasBackdrop(heapName) ? sprites.backdrop(heapName, 1).first() : null;
        Array<AtlasRegion> puffs = sprites.has("collapse-puff") ? sprites.frames("collapse-puff") : null;
        fall = new DustBlast(sprites.pixel, heap, puffs, collapse);
        backdrop.underTowers(this::drawUnder);
    }

    /**
     * Works out the collapse at the render time: {@code lag} seconds before the sortie's step, the
     * ground scrolled to {@code scroll}; bends the falling tower until the impact, hides it from then
     * on, and sets the backdrop's dust peak.
     */
    void update(Sortie sortie, double scroll, double lag) {
        View v = view;
        v.started = sortie.collapseStarted();
        if (!v.started) {
            v.tower = -1;
            backdrop.hideTower(-1);
            backdrop.bendTower(-1, 0, 0, 0, 1, 0, 0);
            backdrop.dustPeak(0);
            return;
        }
        double seconds = Math.max(0, sortie.collapseSeconds() - lag);
        v.seconds = seconds;
        v.warning = collapse.warning();
        v.drop = collapse.drop();
        v.blast = collapse.blast().seconds();
        v.blastFrom = collapse.blast().from();
        v.blastTo = collapse.blast().to();
        double ground = sortie.collapseFootY() + sortie.groundScroll();
        if (v.tower < 0) {
            double reach = (sortie.collapseBandTop() - sortie.collapseBandBottom()) / 2;
            v.tower = backdrop.towerNear(ground, reach);
        }
        if (v.tower >= 0) {
            TowerProjection.Tower tower = backdrop.tower(v.tower);
            v.footX = tower.x();
            v.footY = tower.centre() - scroll;
            v.halfWidth = tower.footWidth() / 2.0;
            v.halfHeight = tower.footHeight() / 2.0;
            v.height = tower.height() * TowerProjection.UNIT;
        } else {
            v.footX = sortie.collapseFootX();
            v.footY = ground - scroll;
            v.halfWidth = 100;
            v.halfHeight = (sortie.collapseBandTop() - sortie.collapseBandBottom()) / 2;
            v.height = 400;
        }
        v.lean = lean(seconds, v.warning, v.drop);
        v.share = share(seconds, v.warning, v.drop);
        if (v.tower >= 0 && seconds < v.impact()) {
            double shake = shudder(seconds, v.warning, v.drop);
            long step = (long) Math.floor(seconds * SHUDDER_RATE);
            double drop = dropShare(seconds, v.warning, v.drop);
            backdrop.hideTower(-1);
            backdrop.bendTower(
                    v.tower,
                    v.lean,
                    (2 * hash(step) - 1) * shake,
                    (2 * hash(step + 7001) - 1) * shake * 0.5,
                    v.share,
                    drop > 0 ? CRUSH * Math.min(1, 3 * drop) : 0,
                    (float) (ROOF_DUST * Math.pow(drop, 1.5)));
        } else {
            backdrop.bendTower(-1, 0, 0, 0, 1, 0, 0);
            backdrop.hideTower(v.tower);
        }
        backdrop.dustPeak(dustAt(seconds, collapse));
    }

    /** The lean {@code seconds} after the warning started, radians: eased in over the warning, relaxing through the drop. */
    static double lean(double seconds, double warning, double drop) {
        if (seconds < warning) {
            return LEAN * Math.pow(seconds / warning, LEAN_EASE);
        }
        return LEAN * (1 - LEAN_RELAX * dropShare(seconds, warning, drop));
    }

    /** The drop's share gone {@code seconds} after the warning started, 0 before it to 1 at the impact. */
    static double dropShare(double seconds, double warning, double drop) {
        return Math.clamp((seconds - warning) / drop, 0, 1);
    }

    /** The tower's height left, a share of the full one: 1 until the drop, then eased 0.25 p + 0.75 p² down to 0. */
    static double share(double seconds, double warning, double drop) {
        double p = dropShare(seconds, warning, drop);
        return 1 - (0.25 * p + 0.75 * p * p);
    }

    /** The shudder at the roof, px: 0.3 growing to 0.8 over the warning, back to 0 through the drop. */
    static double shudder(double seconds, double warning, double drop) {
        if (seconds < warning) {
            return 0.3 + 0.5 * seconds / warning;
        }
        return 0.8 * (1 - dropShare(seconds, warning, drop));
    }

    /**
     * The dust peak's weight {@code seconds} (real time) after the collapse's warning started: in
     * from the base dust's start, 0.35 s before the impact, full 0.3 s after it, then ramping out over
     * {@code dust.seconds} (a smoothstep).
     */
    static float dustAt(double seconds, LevelData.Collapse collapse) {
        double impact = collapse.impact();
        double start = impact - BASE_DUST_LEAD;
        double full = impact + PEAK_FULL;
        if (seconds < start) {
            return 0;
        }
        if (seconds < full) {
            return (float) ((seconds - start) / (full - start));
        }
        double s = Math.min(1, (seconds - full) / collapse.dust().seconds());
        return (float) (1 - s * s * (3 - 2 * s));
    }

    /** Between the ground pieces and the towers: the fall's shadow, heap and base dust. */
    void drawUnder(SpriteBatch batch) {
        if (view.started) {
            fall.drawUnder(batch, view);
        }
    }

    /** Over the ground units, under the flyers: the fall's dust. */
    void drawOver(SpriteBatch batch) {
        if (view.started) {
            fall.drawOver(batch, view);
        }
    }

    /** A value in [0, 1) from {@code i}, the same every frame. */
    static double hash(long i) {
        long h = (i + 1) * 0x9E3779B97F4A7C15L;
        h ^= h >>> 31;
        h *= 0xBF58476D1CE4E5B9L;
        h ^= h >>> 29;
        return (h >>> 11) * 0x1.0p-53;
    }

    /** 1 − (1 − t)², t clamped to [0, 1]. */
    static double easeOut(double t) {
        double c = Math.clamp(t, 0, 1);
        return 1 - (1 - c) * (1 - c);
    }

    /** A puff's opacity {@code t} s into its {@code life}: in over {@code fadeIn}, out as (1 − t ÷ life)^1.25. */
    static double fade(double t, double life, double peak, double fadeIn) {
        if (t < 0 || t > life) {
            return 0;
        }
        return peak * Math.min(1, t / fadeIn) * Math.pow(1 - t / life, 1.25);
    }

    /** From a footprint's centre to its edge along {@code angle}, px. */
    static double reach(double angle, double halfWidth, double halfHeight) {
        double c = Math.abs(Math.cos(angle));
        double s = Math.abs(Math.sin(angle));
        return Math.min(halfWidth / Math.max(c, 1e-6), halfHeight / Math.max(s, 1e-6));
    }

    /** The camera scale of a point {@code z} px up (the towers' camera: 6 units of {@link TowerProjection#UNIT} px). */
    static double scaleAt(double z) {
        return BackdropData.Tower.scaleAt(z / TowerProjection.UNIT);
    }

    /**
     * Round 31's look c (2026-10-07): the cast shadow, the heap, the dust (puff sprites as particles:
     * the trickle off the walls, the wisps, the base dust, the blast's ring and the billow) and the
     * debris specks (the trickle, the chunks out of the crushing floors, the ones thrown at the impact).
     * Particles are relative to the foot's centre on the ground; their numbers, sizes and times are
     * the concept's.
     */
    static final class DustBlast implements Fall {
        /** A particle: its start, place (px from the foot's centre, y up) and look. */
        static final class Particle {
            double t0;
            double x;
            double y;
            double outX;
            double outY;
            double angle;
            double r0;
            double distance;
            double drift;
            double speed;
            double up;
            double fraction;
            double s0;
            double s1;
            double life;
            double peak;
            double lag;
            double z1;
            float tone;
            int puff;
        }

        private final TextureRegion pixel;
        private final AtlasRegion heap;
        private final Array<AtlasRegion> puffs;
        private final float[] polygon = new float[20];
        private final float[] hexagon = new float[12];
        private final Particle[] trickle;
        private final Particle[] wisps;
        private final Particle[] chunks;
        private final Particle[] base;
        private final Particle[] ring;
        private final Particle[] billow;
        private final Particle[] thrown;

        DustBlast(TextureRegion pixel, AtlasRegion heap, Array<AtlasRegion> puffs, LevelData.Collapse collapse) {
            this.pixel = pixel;
            this.heap = heap;
            this.puffs = puffs;
            double warning = collapse.warning();
            double impact = collapse.impact();
            double fall = impact - collapse.drop();
            double reachSpan = collapse.blast().to() - collapse.blast().from();
            SplittableRandom rng = new SplittableRandom(3161);
            trickle = new Particle[46];
            for (int i = 0; i < trickle.length; i++) {
                Particle p = trickle[i] = new Particle();
                p.t0 = uniform(rng, 0.15, impact - 0.15);
                side(p, rng.nextInt(4), uniform(rng, -1, 1), 2);
                p.fraction = uniform(rng, 0.35, 0.98);
                p.speed = uniform(rng, 8, 30);
                p.puff = rng.nextInt(8);
                p.s0 = uniform(rng, 0.08, 0.14);
            }
            wisps = new Particle[16];
            for (int i = 0; i < wisps.length; i++) {
                Particle p = wisps[i] = new Particle();
                p.t0 = uniform(rng, 0.3, warning + 0.6);
                side(p, 1 + rng.nextInt(3), uniform(rng, -0.9, 0.9), 3);
                p.fraction = uniform(rng, 0.45, 0.95);
                p.puff = rng.nextInt(8);
                p.s0 = uniform(rng, 0.08, 0.15);
            }
            chunks = new Particle[60];
            for (int i = 0; i < chunks.length; i++) {
                Particle p = chunks[i] = new Particle();
                p.t0 = uniform(rng, fall + 0.15, impact - 0.05);
                p.angle = uniform(rng, 0, 2 * Math.PI);
                p.speed = uniform(rng, 40, 130);
                p.tone = (float) uniform(rng, 0.25, 0.55);
            }
            base = new Particle[44];
            for (int i = 0; i < base.length; i++) {
                Particle p = base[i] = new Particle();
                p.t0 = uniform(rng, impact - BASE_DUST_LEAD, impact);
                p.angle = uniform(rng, 0, 2 * Math.PI);
                p.fraction = uniform(rng, 0.6, 1.0);
                p.distance = uniform(rng, 30, 150);
                p.s0 = uniform(rng, 0.25, 0.4);
                p.s1 = uniform(rng, 0.8, 1.2);
                p.life = uniform(rng, 3.0, 4.5);
                p.peak = uniform(rng, 0.55, 0.75);
                p.puff = rng.nextInt(8);
            }
            ring = new Particle[200];
            for (int i = 0; i < ring.length; i++) {
                Particle p = ring[i] = new Particle();
                boolean front = i < 130;
                int n = front ? 130 : 70;
                double angle = ((i % n) + uniform(rng, -0.45, 0.45)) * 2 * Math.PI / n;
                double lobes = 1
                        + 0.16 * Math.sin(3 * angle + 0.7)
                        + 0.1 * Math.sin(5 * angle + 2.1)
                        + 0.06 * Math.sin(9 * angle + 4.0);
                p.angle = angle;
                p.fraction = uniform(rng, 0.7, 1.0);
                p.distance = (front ? uniform(rng, 0.62, 1.05) : uniform(rng, 0.2, 0.6)) * lobes * reachSpan;
                p.drift = uniform(rng, 20, 70);
                p.s0 = uniform(rng, 0.35, 0.6);
                p.s1 = uniform(rng, 0.9, 1.7);
                p.life = uniform(rng, 2.6, 4.6);
                p.peak = front ? uniform(rng, 0.5, 0.7) : uniform(rng, 0.35, 0.5);
                p.puff = rng.nextInt(8);
                p.lag = uniform(rng, 0, 0.22);
            }
            billow = new Particle[26];
            for (int i = 0; i < billow.length; i++) {
                Particle p = billow[i] = new Particle();
                double angle = uniform(rng, 0, 2 * Math.PI);
                double rr = Math.sqrt(rng.nextDouble())
                        * (1 + 0.25 * Math.sin(3 * angle + 1.3) + uniform(rng, -0.15, 0.15));
                p.x = Math.cos(angle) * rr * 1.1;
                p.y = Math.sin(angle) * rr * 1.1;
                p.z1 = uniform(rng, 60, 150);
                p.s0 = uniform(rng, 0.8, 1.1);
                p.s1 = uniform(rng, 1.7, 2.4);
                p.life = uniform(rng, 3.6, 5.8);
                p.peak = uniform(rng, 0.55, 0.7);
                p.puff = rng.nextInt(8);
                p.lag = uniform(rng, -0.1, 0.05);
            }
            thrown = new Particle[40];
            for (int i = 0; i < thrown.length; i++) {
                Particle p = thrown[i] = new Particle();
                p.angle = uniform(rng, 0, 2 * Math.PI);
                p.speed = uniform(rng, 150, 330);
                p.up = uniform(rng, 60, 160);
                p.tone = (float) uniform(rng, 0.25, 0.5);
            }
        }

        private static double uniform(SplittableRandom rng, double from, double to) {
            return from + (to - from) * rng.nextDouble();
        }

        /**
         * A wall particle's side: 0 or 3 east, 1 north, 2 south (east twice as likely, as the concept
         * picks), {@code along} it from −1 to 1, {@code gap} px out; x, y as shares of the half sizes
         * plus the gap.
         */
        private static void side(Particle p, int side, double along, double gap) {
            switch (side) {
                case 1 -> {
                    p.x = along;
                    p.y = 1;
                    p.outY = 1;
                }
                case 2 -> {
                    p.x = along;
                    p.y = -1;
                    p.outY = -1;
                }
                default -> {
                    p.x = 1;
                    p.y = along;
                    p.outX = 1;
                }
            }
            p.r0 = gap;
        }

        /** A wall particle's place on the ground, px from the foot's centre. */
        private static double wallX(Particle p, View v) {
            return p.x * v.halfWidth + p.outX * p.r0;
        }

        private static double wallY(Particle p, View v) {
            return p.y * v.halfHeight + p.outY * p.r0;
        }

        @Override
        public void drawUnder(SpriteBatch batch, View v) {
            under(batch, v);
            batch.setColor(Color.WHITE);
        }

        @Override
        public void drawOver(SpriteBatch batch, View v) {
            over(batch, v);
            batch.setColor(Color.WHITE);
        }

        private void under(SpriteBatch batch, View v) {
            double t = v.sinceImpact();
            if (t < 0) {
                drawShadow(batch, v);
            }
            if (heap != null && t >= -0.1) {
                float alpha = (float) Math.min(1, (t + 0.1) / 0.2);
                batch.setColor(1, 1, 1, alpha);
                batch.draw(heap, X0 + (float) Math.round(v.footX - heap.getRegionWidth() / 2.0), (float)
                        Math.round(v.footY - heap.getRegionHeight() / 2.0));
            }
            if (puffs == null) {
                return;
            }
            // The trickling debris landing: a small puff where each speck hits the ground.
            for (Particle p : trickle) {
                double fallTime = Math.sqrt(2 * p.fraction * v.height / GRAVITY);
                double since = v.seconds - p.t0 - fallTime;
                if (since >= 0 && since < 1.6) {
                    double gx = wallX(p, v) + p.outX * p.speed * fallTime;
                    double gy = wallY(p, v) + p.outY * p.speed * fallTime;
                    stamp(
                            batch,
                            p.puff,
                            v.footX + gx,
                            v.footY + gy,
                            p.s0 * (1 + 1.4 * easeOut(since / 1.6)),
                            fade(since, 1.6, 0.4, 0.08));
                }
            }
            // The base dust boiling out of the foot from just before the impact (under the walls).
            for (Particle p : base) {
                double since = v.seconds - p.t0;
                if (since < 0) {
                    continue;
                }
                double r = reach(p.angle, v.halfWidth, v.halfHeight) * p.fraction
                        + p.distance * (1 - Math.exp(-since / 0.7));
                double scale = p.s0 + (p.s1 - p.s0) * easeOut(since / 2.0);
                stamp(
                        batch,
                        p.puff,
                        v.footX + r * Math.cos(p.angle),
                        v.footY + r * Math.sin(p.angle),
                        scale,
                        fade(since, p.life, p.peak, 0.15));
            }
        }

        private void over(SpriteBatch batch, View v) {
            double t = v.sinceImpact();
            if (puffs != null) {
                // Dust wisps off the walls in the lean, drifting out and sinking.
                for (Particle p : wisps) {
                    double since = v.seconds - p.t0;
                    if (since < 0 || since >= 1.4) {
                        continue;
                    }
                    double z = p.fraction * v.height - 60 * since * since;
                    double gx = wallX(p, v)
                            + p.outX * 14 * since
                            + TowerProjection.leanOffset(z / v.height, v.height, v.lean);
                    double gy = wallY(p, v) + p.outY * 14 * since;
                    stampLifted(
                            batch,
                            p.puff,
                            v.footX + gx,
                            v.footY + gy,
                            z,
                            p.s0 * (1 + 1.4 * since),
                            fade(since, 1.4, 0.35, 0.1));
                }
            }
            // The trickling debris, falling off the walls.
            for (Particle p : trickle) {
                double fallTime = Math.sqrt(2 * p.fraction * v.height / GRAVITY);
                double since = v.seconds - p.t0;
                if (since < 0 || since >= fallTime) {
                    continue;
                }
                double z0 = p.fraction * v.height * share(p.t0, v.warning, v.drop);
                double z = z0 - 0.5 * GRAVITY * since * since;
                if (z <= 0) {
                    continue;
                }
                double lean = lean(p.t0, v.warning, v.drop);
                double gx = wallX(p, v)
                        + p.outX * p.speed * since
                        + TowerProjection.leanOffset(z0 / v.height, v.height, lean);
                double gy = wallY(p, v) + p.outY * p.speed * since;
                speckLifted(batch, v.footX + gx, v.footY + gy, z, 0.35f);
            }
            // Chunks spat out of the crushing floors through the drop.
            for (Particle p : chunks) {
                double since = v.seconds - p.t0;
                double z0 = v.height * share(p.t0, v.warning, v.drop);
                if (since < 0 || z0 <= 2) {
                    continue;
                }
                double z = z0 - 0.5 * GRAVITY * since * since;
                if (z <= 0) {
                    continue;
                }
                double r = reach(p.angle, v.halfWidth, v.halfHeight) + 3 + p.speed * since;
                double lean = lean(p.t0, v.warning, v.drop);
                speckLifted(
                        batch,
                        v.footX + r * Math.cos(p.angle) + TowerProjection.leanOffset(z0 / v.height, v.height, lean),
                        v.footY + r * Math.sin(p.angle),
                        z,
                        p.tone);
            }
            if (t < -0.1) {
                return;
            }
            // The chunks thrown out at the impact.
            for (Particle p : thrown) {
                double z = p.up * t - 0.5 * GRAVITY * t * t;
                if (t < 0 || (z <= 0 && t > 0.05)) {
                    continue;
                }
                double r = reach(p.angle, v.halfWidth, v.halfHeight) + p.speed * t;
                speck(
                        batch,
                        v.footX + r * Math.cos(p.angle),
                        v.footY + r * Math.sin(p.angle) + Math.max(0, z) * 0.15,
                        p.tone);
            }
            if (puffs == null) {
                return;
            }
            // The blast's ring rolling out in every direction: its front the kill ring, then drifting on.
            for (Particle p : ring) {
                double since = t - p.lag;
                if (since < 0 || since > p.life) {
                    continue;
                }
                double eased = since < v.blast
                        ? easeOut(since / v.blast)
                        : 1 + (1 - Math.exp(-(since - v.blast) / 1.6)) * p.drift / p.distance;
                double r = reach(p.angle, v.halfWidth, v.halfHeight) * p.fraction + p.distance * eased;
                double scale = p.s0 + (p.s1 - p.s0) * easeOut(since / 2.4);
                double z = 10 + 30 * easeOut(since / 3.0);
                stampLifted(
                        batch,
                        p.puff,
                        v.footX + r * Math.cos(p.angle),
                        v.footY + r * Math.sin(p.angle),
                        z,
                        scale,
                        fade(since, p.life, p.peak, 0.08));
            }
            // The billow rising over the heap, spreading as it rises.
            for (Particle p : billow) {
                double since = t - p.lag;
                if (since < 0 || since > p.life) {
                    continue;
                }
                double z = 20 + (p.z1 - 20) * easeOut(since / 4.0);
                double scale = p.s0 + (p.s1 - p.s0) * easeOut(since / 3.0);
                double spread = 1 + 0.25 * easeOut(since / 3.0);
                stampLifted(
                        batch,
                        p.puff,
                        v.footX + p.x * v.halfWidth * spread,
                        v.footY + p.y * v.halfHeight * spread,
                        z,
                        scale,
                        fade(since, p.life, p.peak, 0.1));
            }
        }

        /**
         * The cast shadow while the tower stands: the footprint swept up the bent tower toward the
         * shadow's direction (the hull of the footprint and the top's outline moved by the lean and
         * the shadow's offset), 40 % dark with a soft edge of a few px.
         */
        private void drawShadow(SpriteBatch batch, View v) {
            double z = v.height * v.share;
            if (z <= 0) {
                return;
            }
            double dx = TowerProjection.leanOffset(v.share, v.height, v.lean) + SHADOW_X * z;
            double dy = SHADOW_Y * z;
            float layer = (float) (1 - Math.pow(1 - SHADOW_OPACITY, 1.0 / SHADOW_LAYERS));
            float colour = Color.toFloatBits(0, 0, 0, layer);
            for (int l = SHADOW_LAYERS - 1; l >= 0; l--) {
                double x0 = v.footX - v.halfWidth - l;
                double x1 = v.footX + v.halfWidth + l;
                double y0 = v.footY - v.halfHeight - l;
                double y1 = v.footY + v.halfHeight + l;
                // The hull of the two rectangles, the second down and right of the first.
                corner(0, x0, y1);
                corner(1, x1, y1);
                corner(2, x1 + dx, y1 + dy);
                corner(3, x1 + dx, y0 + dy);
                corner(4, x0 + dx, y0 + dy);
                corner(5, x0, y0);
                quad(batch, colour, 0, 1, 2, 3);
                quad(batch, colour, 0, 3, 4, 5);
            }
        }

        private void corner(int i, double x, double y) {
            hexagon[2 * i] = X0 + (float) x;
            hexagon[2 * i + 1] = (float) y;
        }

        /** The hexagon's corners a, b, c, d (a convex quad) filled with {@code colour}. */
        private void quad(SpriteBatch batch, float colour, int a, int b, int c, int d) {
            vertex(0, a, colour);
            vertex(1, b, colour);
            vertex(2, c, colour);
            vertex(3, d, colour);
            batch.draw(pixel.getTexture(), polygon, 0, polygon.length);
        }

        /** Vertex {@code i} of the quad: the hexagon's corner {@code corner}, at the white pixel's middle. */
        private void vertex(int i, int corner, float colour) {
            int o = i * 5;
            polygon[o] = hexagon[2 * corner];
            polygon[o + 1] = hexagon[2 * corner + 1];
            polygon[o + 2] = colour;
            polygon[o + 3] = (pixel.getU() + pixel.getU2()) / 2;
            polygon[o + 4] = (pixel.getV() + pixel.getV2()) / 2;
        }

        /** Puff {@code v} centred at play-field (x, y) at {@code scale} × its 128 px, faded by {@code alpha}. */
        private void stamp(SpriteBatch batch, int v, double x, double y, double scale, double alpha) {
            float size = (float) (PUFF * scale);
            if (alpha <= 0.01 || size < 3) {
                return;
            }
            batch.setColor(1, 1, 1, (float) alpha);
            batch.draw(puffs.get(v), X0 + (float) x - size / 2, (float) y - size / 2, size, size);
        }

        /** As {@link #stamp}, lifted {@code z} px up: placed and scaled by the camera's projection. */
        private void stampLifted(SpriteBatch batch, int v, double x, double y, double z, double scale, double alpha) {
            double k = scaleAt(z);
            stamp(
                    batch,
                    v,
                    BackdropData.Tower.project(x, BackdropData.Tower.CENTRE_X, k),
                    BackdropData.Tower.project(y, BackdropData.Tower.CENTRE_Y, k),
                    scale * k,
                    alpha);
        }

        /** A 2 px debris speck at play-field (x, y). */
        private void speck(SpriteBatch batch, double x, double y, float tone) {
            batch.setColor(SPECK_R * tone, SPECK_G * tone, SPECK_B * tone, 1);
            batch.draw(pixel, X0 + (float) Math.round(x - 1), (float) Math.round(y - 1), 2, 2);
        }

        /** As {@link #speck}, lifted {@code z} px up. */
        private void speckLifted(SpriteBatch batch, double x, double y, double z, float tone) {
            double k = scaleAt(z);
            speck(
                    batch,
                    BackdropData.Tower.project(x, BackdropData.Tower.CENTRE_X, k),
                    BackdropData.Tower.project(y, BackdropData.Tower.CENTRE_Y, k),
                    tone);
        }
    }
}
