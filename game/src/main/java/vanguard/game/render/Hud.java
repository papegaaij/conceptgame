package vanguard.game.render;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import java.util.List;
import vanguard.game.level.PromptTexts;
import vanguard.game.level.RadioQueue;
import vanguard.game.ui.Fonts;
import vanguard.sim.Sortie;

/** The level HUD in the two side panels (design/ui/hud): mission on the left, ship on the right. */
public final class Hud {
    private final MissionPanel mission;
    private final ShipPanel ship;

    /**
     * @param number the level number
     * @param name the level's name
     * @param launchBalance the credits the player launched with
     */
    public Hud(Sprites sprites, Fonts fonts, int number, String name, int launchBalance) {
        HudKit kit = new HudKit(sprites.pixel, fonts);
        mission = new MissionPanel(kit, sprites, number, name, launchBalance);
        ship = new ShipPanel(kit);
    }

    /**
     * @param prompts the control prompts to show
     * @param weapon the front weapon's name
     * @param weaponLevel its upgrade level, 1..5
     * @param notFlown the fitted items the sortie leaves out until they fly (M4)
     */
    public void draw(
            SpriteBatch batch,
            Sortie sortie,
            RadioQueue radio,
            List<PromptTexts.Text> prompts,
            String weapon,
            int weaponLevel,
            List<String> notFlown) {
        mission.draw(batch, sortie, radio, prompts);
        ship.draw(batch, sortie.ship().defences(), weapon, weaponLevel, notFlown);
        batch.setColor(1, 1, 1, 1);
    }
}
