package vanguard.game.hangar;

import com.badlogic.gdx.Files;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.Disposable;

/**
 * The hangar's backdrop (design/ui/hangar, chosen hangar-r07-b): the tactical display behind the
 * glass panels, rendered by tools/art/ui_scenes.py (a cyan grid over the dark Earth seen from orbit,
 * its limb across the lower part, orange range rings round the operation's area and a dashed
 * approach route). One map for Act 1, the same for every level until the later acts' settings get
 * theirs.
 */
public final class TacticalMap implements Disposable {
    private final Texture texture;

    public TacticalMap(Files files) {
        texture = new Texture(files.internal("ui/hangar-map.png"));
    }

    public void draw(SpriteBatch batch) {
        batch.draw(texture, 0, 0);
    }

    @Override
    public void dispose() {
        texture.dispose();
    }
}
