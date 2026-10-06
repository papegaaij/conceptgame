package vanguard.game.render;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import java.util.List;
import java.util.Locale;
import vanguard.content.campaign.Flight;
import vanguard.game.level.PromptTexts;
import vanguard.game.level.RadioQueue;
import vanguard.game.ui.Fonts;
import vanguard.sim.Armament;
import vanguard.sim.Sortie;

/** The level HUD in the two side panels (design/ui/hud): mission on the left, ship on the right. */
public final class Hud {
    private final MissionPanel mission;
    private final ShipPanel ship;
    private final Flight flight;
    private final double regenBonus;
    /** The flown weapons by {@link Armament.Slot}. */
    private final Flight.Weapon[] weapons = new Flight.Weapon[Armament.Slot.values().length];

    /**
     * @param number the level number
     * @param name the level's name
     * @param launchBalance the credits the player launched with
     * @param flight what the sortie flies
     * @param regenBonus the shield regen bonus of the flight's spare power
     * @param overdriveLength how long an overdrive lasts, s
     */
    public Hud(
            Sprites sprites,
            Fonts fonts,
            TransmissionStatic transmissionStatic,
            int number,
            String name,
            int launchBalance,
            Flight flight,
            double regenBonus,
            double overdriveLength) {
        HudKit kit = new HudKit(sprites, fonts);
        mission = new MissionPanel(kit, sprites, transmissionStatic, number, name, launchBalance);
        ship = new ShipPanel(
                kit,
                overdriveLength,
                flight.loadout()
                        .special()
                        .map(special -> sprites.region("icons/" + iconName(special.name())))
                        .map(TextureRegion.class::cast)
                        .orElse(null),
                sprites.region("icons/escort-rook"));
        this.flight = flight;
        this.regenBonus = regenBonus;
        for (Flight.Weapon weapon : flight.weapons()) {
            weapons[weapon.slot().ordinal()] = weapon;
        }
    }

    /** The hangar icon of a special: {@code special-} and its name in lower case, hyphenated (tools/art/icons.py). */
    static String iconName(String special) {
        return "special-" + special.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "-");
    }

    /** The special button was denied: the special's row flashes. */
    public void specialDenied() {
        ship.specialDenied();
    }

    /** @param prompts the control prompts to show */
    public void draw(SpriteBatch batch, Sortie sortie, RadioQueue radio, List<PromptTexts.Text> prompts) {
        mission.draw(batch, sortie, radio, prompts);
        ship.draw(batch, sortie, weapons, flight.sparePower(), regenBonus, flight.notFlown());
        batch.setColor(1, 1, 1, 1);
    }
}
