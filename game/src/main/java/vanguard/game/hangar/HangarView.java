package vanguard.game.hangar;

import com.badlogic.gdx.Files;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.Disposable;
import java.util.List;
import java.util.Optional;
import vanguard.content.BriefingPage;
import vanguard.content.Content;
import vanguard.content.campaign.Catalogue;
import vanguard.content.campaign.Intel;
import vanguard.game.render.PodPivots;
import vanguard.game.render.Sprites;
import vanguard.game.render.TestFireView;
import vanguard.game.ui.Glass;

/**
 * The hangar's panels over its tactical map: the shop (left) with its test-fire box, the schematic
 * (centre) and the intel (right).
 */
public final class HangarView implements Disposable {
    private final TacticalMap map;
    private final ShopPanel shop;
    private final LoadoutPanel loadout;
    private final IntelPanel intel;
    private final TestFirePanel testFire;

    public HangarView(Files files, Glass glass, Sprites sprites, Catalogue catalogue, Content content) {
        map = new TacticalMap(files);
        ItemIcons icons = new ItemIcons(sprites, catalogue);
        shop = new ShopPanel(glass, icons, content);
        loadout = new LoadoutPanel(glass, sprites.ship.get(sprites.ship.size / 2), icons);
        intel = new IntelPanel(glass, sprites);
        testFire = new TestFirePanel(glass, content, new TestFireView(sprites, new PodPivots(files)), sprites.pixel);
    }

    /** Runs the test fire of the selected weapon. */
    public void update(float seconds, HangarState state) {
        testFire.update(seconds, state);
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
        shop.draw(batch, state, next.map(Intel::markedTraits).orElse(List.of()));
        testFire.draw(batch);
        loadout.draw(batch, state);
        intel.draw(batch, state.hangar().campaign().nextLevel(), next, teaser, levelKey.orElse(""));
    }

    @Override
    public void dispose() {
        map.dispose();
    }
}
