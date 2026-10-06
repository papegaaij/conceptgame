package vanguard.game.render;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import java.util.List;
import vanguard.content.Content;
import vanguard.content.EnemyData;
import vanguard.content.SystemsData;
import vanguard.content.Tier;
import vanguard.sim.Chain;
import vanguard.sim.Enemy;
import vanguard.sim.EnemySpec;
import vanguard.sim.Hitbox;
import vanguard.sim.LevelScript;
import vanguard.sim.SetPiece;
import vanguard.sim.SimStep;
import vanguard.sim.Sortie;

/**
 * The Targeting computer's marks (design/player/systems, Targeting computer): a thin HP bar under
 * every damaged unit that is not {@code tiny}, from its first hit until {@code bar_seconds} after
 * its last, then fading out over {@code bar_fade} s; and pulsing lime corner brackets round every
 * weak point (a part with a damage multiplier) of a boss, mid-boss or set piece while that part can
 * take damage, and round a {@code large} or bigger chain's original head when it is a weak point
 * (the Coilwyrm's ×2) while it lives. A segment chain (the Coilwyrm) shows one bar under its foremost living member, its
 * living members' HP summed. Bosses, mid-bosses and set pieces get no bar (the boss bar and the
 * brackets cover them), nor do ground objects, which have no stat block. Drawn over the units and
 * under the bullets, from the simulation's state every frame, so it allocates nothing. Geometry in
 * play-field pixels, y up.
 */
public final class TargetingOverlay {
    /** The bar: 2 px tall, 4 px under the unit's hit box, its hit box's width within 12..48 px. */
    static final float BAR_HEIGHT = 2;

    static final float BAR_GAP = 4;
    static final float BAR_MIN = 12;
    static final float BAR_MAX = 48;
    /** Below this share of its HP the fill turns amber. */
    static final double LOW = 0.3;
    /** The brackets: 2 px thick, each arm a quarter of the box's side (at least 4 px), 2 px outside it. */
    static final float BRACKET = 2;

    static final float BRACKET_MIN_ARM = 4;
    static final float BRACKET_OUTSET = 2;
    /** They pulse twice a second between 60 % and full opacity. */
    static final double PULSE_HZ = 2;

    static final float PULSE_LOW = 0.6f;

    private static final float X0 = PixelScreen.PLAY_FIELD_X;
    private static final Color FILL = HudKit.READOUT;
    private static final Color LOW_FILL = HudKit.AMBER;
    private static final Color TROUGH = HudKit.LCD;
    private static final Color LIME = Color.valueOf("B4FF3C");

    private final TextureRegion pixel;
    private final double barSeconds;
    private final double fadeSeconds;
    /** Per enemy kind ({@link Enemy#kind()}): whether it shows a bar, i.e. its stat block's tier is not tiny. */
    private final boolean[] barred;
    /** Per enemy kind: whether its stat block's tier is {@code large} or bigger (a chain head's weak point is bracketed). */
    private final boolean[] large;

    private final List<EnemySpec> kinds;
    private final Color colour = new Color();

    /**
     * @param kinds the level's enemy kinds ({@link Sortie#enemyKinds()})
     * @param numbers the Targeting computer's numbers (design/player/systems/data.yaml)
     */
    public TargetingOverlay(
            TextureRegion pixel, Content content, List<EnemySpec> kinds, SystemsData.Targeting numbers) {
        this.pixel = pixel;
        this.kinds = kinds;
        barSeconds = numbers.barSeconds();
        fadeSeconds = numbers.barFade();
        barred = new boolean[kinds.size()];
        large = new boolean[kinds.size()];
        for (int k = 0; k < barred.length; k++) {
            EnemyData data = content.enemies().get(kinds.get(k).slug());
            barred[k] = data != null && data.tier() != Tier.TINY;
            large[k] = data != null && data.tier().compareTo(Tier.LARGE) >= 0;
        }
    }

    /** The bars under the damaged units, then the brackets on the open weak points. */
    public void draw(SpriteBatch batch, Sortie sortie, float alpha) {
        for (int i = 0; i < sortie.enemyCount(); i++) {
            Enemy enemy = sortie.enemy(i);
            if (enemy.chain() != null || enemy.kind() >= barred.length || !barred[enemy.kind()]) {
                continue;
            }
            bar(batch, enemy, enemy.hp(), enemy.spec().hp(), enemy.ticksSinceHit(), alpha);
        }
        for (int c = 0; c < sortie.chainCount(); c++) {
            chainBar(batch, sortie.chain(c), alpha);
        }
        float pulse = pulse(sortie.tick() + alpha);
        for (int k = 0; k < sortie.setPieceCount(); k++) {
            brackets(batch, sortie.setPiece(k), alpha, pulse);
        }
        for (int c = 0; c < sortie.chainCount(); c++) {
            headBrackets(batch, sortie.chain(c), alpha, pulse);
        }
        batch.setColor(Color.WHITE);
    }

    /** A chain's one bar: under its foremost living member, the living members' HP over the chain's full HP. */
    private void chainBar(SpriteBatch batch, Chain chain, float alpha) {
        Enemy front = null;
        double hp = 0;
        double full = 0;
        int since = Integer.MAX_VALUE;
        EnemySpec.ChainSpec spec = chain.spec();
        for (int i = 0; i < chain.size(); i++) {
            Enemy member = chain.member(i);
            if (member == null) {
                full += i == 0
                        ? headHp(spec)
                        : i == spec.members() - 1
                                ? spec.tail().hp()
                                : spec.segment().hp();
                continue;
            }
            full += member.spec().hp();
            if (Chain.doomed(member)) {
                continue;
            }
            hp += Math.max(0, member.hp());
            since = Math.min(since, member.ticksSinceHit());
            if (front == null) {
                front = member;
            }
        }
        if (front != null && barred(front, spec)) {
            bar(batch, front, hp, full, since, alpha);
        }
    }

    /** Whether a chain shows a bar: its head's stat block is not tiny. */
    private boolean barred(Enemy front, EnemySpec.ChainSpec spec) {
        for (int k = 0; k < kinds.size(); k++) {
            if (heads(kinds.get(k), spec)) {
                return barred[k];
            }
        }
        return front.kind() < barred.length && barred[front.kind()];
    }

    /** The full HP of a chain's original head (its wave's unit); the segment's when it is not among the kinds. */
    private double headHp(EnemySpec.ChainSpec spec) {
        for (int k = 0; k < kinds.size(); k++) {
            if (heads(kinds.get(k), spec)) {
                return kinds.get(k).hp();
            }
        }
        return spec.segment().hp();
    }

    /** Whether {@code kind} is the head of chains of {@code spec}. */
    private static boolean heads(EnemySpec kind, EnemySpec.ChainSpec spec) {
        return kind.chain().isPresent() && kind.chain().get() == spec;
    }

    private void bar(SpriteBatch batch, Enemy enemy, double hp, double full, int since, float alpha) {
        float opacity = opacity(since, alpha);
        if (opacity <= 0 || hp <= 0 || hp >= full) {
            return;
        }
        Hitbox box = enemy.hitbox();
        float width = Math.clamp((float) box.width(), BAR_MIN, BAR_MAX);
        float x = Math.round(X0 + enemy.renderX(alpha) - width / 2);
        float y = Math.round(enemy.renderY(alpha) - box.height() / 2 - BAR_GAP - BAR_HEIGHT);
        double share = hp / full;
        batch.setColor(colour.set(TROUGH).mul(1, 1, 1, opacity * 0.85f));
        batch.draw(pixel, x - 1, y - 1, width + 2, BAR_HEIGHT + 2);
        batch.setColor(colour.set(share < LOW ? LOW_FILL : FILL).mul(1, 1, 1, opacity));
        batch.draw(pixel, x, y, Math.max(1, Math.round(width * (float) share)), BAR_HEIGHT);
    }

    /**
     * A bar's opacity {@code since} steps after its unit's last hit ({@code alpha} of a step later):
     * full until the bar's time is up, then fading to nothing; 0 before the first hit.
     */
    float opacity(int since, float alpha) {
        if (since == Integer.MAX_VALUE) {
            return 0;
        }
        double seconds = (since + alpha) * SimStep.SECONDS;
        if (seconds <= barSeconds) {
            return 1;
        }
        return fadeSeconds <= 0 ? 0 : (float) Math.max(0, 1 - (seconds - barSeconds) / fadeSeconds);
    }

    /** The brackets' opacity at {@code ticks} steps: a sine between {@link #PULSE_LOW} and full, {@link #PULSE_HZ} a second. */
    static float pulse(double ticks) {
        double phase = ticks * SimStep.SECONDS * PULSE_HZ * 2 * Math.PI;
        return (float) (PULSE_LOW + (1 - PULSE_LOW) * (0.5 + 0.5 * Math.sin(phase)));
    }

    /** The open weak points of a boss, mid-boss or set piece on the screen. */
    private void brackets(SpriteBatch batch, SetPiece piece, float alpha, float pulse) {
        if (!piece.present() || piece.destroyed()) {
            return;
        }
        double x = piece.renderX(alpha);
        double y = piece.renderY(alpha);
        double scale = piece.boss().isPresent() ? piece.scale() : 1;
        batch.setColor(colour.set(LIME).mul(1, 1, 1, pulse));
        for (int p = 0; p < piece.partCount(); p++) {
            LevelScript.PartSpec part = piece.spec().parts().get(p);
            if (part.multiplier() == 1 || piece.partWrecked(p) || piece.partArmoured(p) || piece.partShielded(p)) {
                continue;
            }
            corners(
                    batch,
                    X0 + x + piece.partOffsetX(p),
                    y + piece.partOffsetY(p),
                    part.box().width() * scale,
                    part.box().height() * scale);
        }
    }

    /**
     * The brackets round a chain's original head when it is a weak point (its chain's head
     * multiplier is not 1) of a {@code large} or bigger unit and alive; a regrown head has none.
     */
    private void headBrackets(SpriteBatch batch, Chain chain, float alpha, float pulse) {
        Enemy head = chain.size() > 0 ? chain.member(0) : null;
        if (head == null
                || Chain.doomed(head)
                || head.spec().chain().isEmpty()
                || head.spec().chain().get().headMultiplier() == 1
                || head.kind() >= large.length
                || !large[head.kind()]) {
            return;
        }
        batch.setColor(colour.set(LIME).mul(1, 1, 1, pulse));
        Hitbox box = head.hitbox();
        corners(batch, X0 + head.renderX(alpha), head.renderY(alpha), box.width(), box.height());
    }

    /** Four corner brackets round a box of {@code w} × {@code h} centred on ({@code cx}, {@code cy}) on the screen. */
    private void corners(SpriteBatch batch, double cx, double cy, double w, double h) {
        float left = Math.round(cx - w / 2 - BRACKET_OUTSET);
        float right = Math.round(cx + w / 2 + BRACKET_OUTSET);
        float bottom = Math.round(cy - h / 2 - BRACKET_OUTSET);
        float top = Math.round(cy + h / 2 + BRACKET_OUTSET);
        float armX = Math.max(BRACKET_MIN_ARM, Math.round((right - left) / 4));
        float armY = Math.max(BRACKET_MIN_ARM, Math.round((top - bottom) / 4));
        // Each corner: a horizontal and a vertical stroke meeting at the corner, inside the outline.
        batch.draw(pixel, left, top - BRACKET, armX, BRACKET);
        batch.draw(pixel, left, top - armY, BRACKET, armY);
        batch.draw(pixel, right - armX, top - BRACKET, armX, BRACKET);
        batch.draw(pixel, right - BRACKET, top - armY, BRACKET, armY);
        batch.draw(pixel, left, bottom, armX, BRACKET);
        batch.draw(pixel, left, bottom, BRACKET, armY);
        batch.draw(pixel, right - armX, bottom, armX, BRACKET);
        batch.draw(pixel, right - BRACKET, bottom, BRACKET, armY);
    }
}
