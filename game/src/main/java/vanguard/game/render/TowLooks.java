package vanguard.game.render;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas.AtlasRegion;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Array;
import vanguard.sim.Hitbox;
import vanguard.sim.Sortie;
import vanguard.sim.Tow;

/**
 * Level 07's lifeboat tow (design/campaign/act-1-first-contact/level-07-brood-carrier, Secrets): a
 * friendly CDF lifeboat drifting down the screen on the air layer with a cargo pod on an amber cable.
 * The cable shows intact while it holds the pod, then as a cut stub hanging from the boat; the loose
 * pod falls away from the boat on its own, tumbling ({@link #TUMBLE}), until it leaves the screen,
 * and the hidden crate (a pickup) falls out of it on the way down ({@link Tow}). A hit on the cable
 * sparks (the level screen's glance on its hit event).
 *
 * <p>Sprites (concept round 25, production after the choice): {@code lifeboat}, {@code lifeboat-pod},
 * {@code lifeboat-cable} (intact, cut), each centred on its place (the cable on its hit box). Without
 * them a placeholder in flat shapes of the tow's boxes: a pale hull with a CDF blue band, an olive pod
 * and the amber cable.
 */
final class TowLooks {
    private static final float X0 = PixelScreen.PLAY_FIELD_X;
    private static final Color HULL = Color.valueOf("B8BEC4");
    private static final Color BAND = Color.valueOf("2A5CB0");
    private static final Color POD = Color.valueOf("6E6A3A");
    private static final Color CABLE = Color.valueOf("FFB020");
    private static final float CABLE_WIDTH = 3;
    /** The cut cable's stub: this share of the cable, hanging from the boat. */
    private static final float STUB = 0.3f;
    /** How fast a loose pod tumbles, degrees per second (clockwise on the screen). */
    static final float TUMBLE = -150;

    private final TextureRegion pixel;
    private final AtlasRegion boat;
    private final AtlasRegion pod;
    private final Array<AtlasRegion> cable;

    TowLooks(Sprites sprites) {
        pixel = sprites.pixel;
        boat = sprites.has("lifeboat") ? sprites.region("lifeboat") : null;
        pod = sprites.has("lifeboat-pod") ? sprites.region("lifeboat-pod") : null;
        cable = sprites.has("lifeboat-cable") ? sprites.frames("lifeboat-cable") : null;
    }

    /** The tows on the screen: cable, pod, then the boat over the cable's end. */
    void draw(SpriteBatch batch, Sortie sortie, float alpha) {
        for (int i = 0; i < sortie.towCount(); i++) {
            Tow tow = sortie.tow(i);
            if (!tow.present()) {
                continue;
            }
            double x = tow.renderX(alpha);
            double y = tow.renderY(alpha);
            var spec = tow.spec();
            drawCable(batch, x, y, spec.podDx(), spec.podDy(), tow.holding());
            float tumble = (float) (TUMBLE * tow.looseSeconds(alpha));
            drawBox(batch, pod, spec.pod(), tow.podRenderX(alpha), tow.podRenderY(alpha), POD, tumble);
            drawBox(batch, boat, spec.boat(), x, y, HULL, 0);
            if (boat == null) {
                Hitbox box = spec.boat();
                batch.setColor(BAND);
                batch.draw(pixel, Math.round(X0 + x - box.width() / 2), Math.round(y - 2), (float) box.width(), 4);
            }
        }
        batch.setColor(Color.WHITE);
    }

    private void drawCable(SpriteBatch batch, double x, double y, double dx, double dy, boolean holding) {
        if (cable != null) {
            AtlasRegion frame = cable.get(holding ? 0 : Math.min(1, cable.size - 1));
            batch.setColor(Color.WHITE);
            batch.draw(
                    frame,
                    Math.round(X0 + x + dx / 2 - frame.getRegionWidth() / 2.0),
                    Math.round(y + dy / 2 - frame.getRegionHeight() / 2.0));
            return;
        }
        float length = (float) Math.hypot(dx, dy) * (holding ? 1 : STUB);
        float degrees = (float) Math.toDegrees(Math.atan2(dy, dx));
        batch.setColor(CABLE);
        batch.draw(
                pixel,
                (float) (X0 + x),
                (float) (y - CABLE_WIDTH / 2),
                0,
                CABLE_WIDTH / 2,
                length,
                CABLE_WIDTH,
                1,
                1,
                degrees);
    }

    /** A sprite (or a flat box of {@code colour}) centred on (x, y), turned by {@code degrees} (counter-clockwise). */
    private void drawBox(
            SpriteBatch batch, AtlasRegion sprite, Hitbox box, double x, double y, Color colour, float degrees) {
        TextureRegion region = sprite != null ? sprite : pixel;
        float width = sprite != null ? sprite.getRegionWidth() : (float) box.width();
        float height = sprite != null ? sprite.getRegionHeight() : (float) box.height();
        batch.setColor(sprite != null ? Color.WHITE : colour);
        if (degrees == 0) {
            batch.draw(region, Math.round(X0 + x - width / 2.0), Math.round(y - height / 2.0), width, height);
            return;
        }
        batch.draw(
                region,
                (float) (X0 + x - width / 2.0),
                (float) (y - height / 2.0),
                width / 2,
                height / 2,
                width,
                height,
                1,
                1,
                degrees);
    }
}
