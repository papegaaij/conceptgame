package vanguard.content.campaign;

import vanguard.sim.WingmanSpec;

/**
 * The escort slot in the campaign (design/player/wingmen): whether Rook is hired, his side and his
 * armour. His fitted gun is the {@link LoadoutSlot#ESCORT} slot of the {@link Gear}'s loadout and
 * his other guns are its {@link ItemKind#ESCORT} inventory, so the hangar's transactions treat them
 * like the player's items; the save keeps all of it in its {@code escort} field.
 *
 * @param hired Rook has joined (from the hangar visit before Level 08)
 * @param side the player's side he flies on (a hangar setting)
 * @param armour his current armour (repair is not automatic); 0 after an ejection: grounded
 */
public record Escort(boolean hired, WingmanSpec.Side side, double armour) {
    public Escort {
        if (side == null || !(armour >= 0)) {
            throw new IllegalArgumentException("invalid escort: side " + side + ", armour " + armour);
        }
    }

    public Escort withArmour(double changed) {
        return new Escort(hired, side, changed);
    }

    public Escort withSide(WingmanSpec.Side changed) {
        return new Escort(hired, changed, armour);
    }
}
