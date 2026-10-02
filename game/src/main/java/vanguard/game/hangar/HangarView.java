package vanguard.game.hangar;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.Disposable;
import java.util.Optional;
import vanguard.content.BriefingPage;
import vanguard.content.campaign.Intel;
import vanguard.game.render.Sprites;
import vanguard.game.ui.Glass;

/** The hangar's panels over its tactical map: the shop (left), the schematic (centre) and the intel (right). */
public final class HangarView implements Disposable {
    private final TacticalMap map = new TacticalMap();
    private final ShopPanel shop;
    private final LoadoutPanel loadout;
    private final IntelPanel intel;

    public HangarView(Glass glass, Sprites sprites) {
        shop = new ShopPanel(glass);
        loadout = new LoadoutPanel(glass, sprites.ship.get(sprites.ship.size / 2));
        intel = new IntelPanel(glass, sprites);
    }

    /**
     * @param levelKey the next level, if it is built
     * @param teaser its hangar teaser
     */
    public void draw(
            SpriteBatch batch,
            HangarState state,
            Optional<Intel> next,
            Optional<BriefingPage> teaser,
            Optional<String> levelKey) {
        map.draw(batch);
        shop.draw(batch, state, next.map(Intel::markedTraits).orElse(java.util.List.of()));
        loadout.draw(batch, state);
        intel.draw(batch, state.hangar().campaign().nextLevel(), next, teaser, levelKey.orElse(""));
    }

    @Override
    public void dispose() {
        map.dispose();
    }
}
