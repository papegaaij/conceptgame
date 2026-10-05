package vanguard.game.screen;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Optional;
import org.junit.jupiter.api.Test;
import vanguard.content.campaign.Hangar;
import vanguard.game.audio.Sfx;
import vanguard.game.hangar.HangarState.Command;
import vanguard.game.hangar.HangarState.Focus;

/** The hangar's sound for a confirmation that was made (design/audio/sfx, UI and radio). */
class HangarSoundsTest {
    @Test
    void aShopActionPlaysItsOwnSound() {
        assertEquals(Sfx.SHOP_BUY, HangarScreen.doneSound(Optional.of(Hangar.Action.BUY), Focus.SHOP, Command.UNDO));
        assertEquals(
                Sfx.SHOP_BUY, HangarScreen.doneSound(Optional.of(Hangar.Action.BUY_CHARGE), Focus.SHOP, Command.UNDO));
        assertEquals(Sfx.UPGRADE, HangarScreen.doneSound(Optional.of(Hangar.Action.UPGRADE), Focus.SHOP, Command.UNDO));
        assertEquals(Sfx.EQUIP, HangarScreen.doneSound(Optional.of(Hangar.Action.FIT), Focus.SHOP, Command.UNDO));
        assertEquals(Sfx.EQUIP, HangarScreen.doneSound(Optional.of(Hangar.Action.UNFIT), Focus.SHOP, Command.UNDO));
    }

    @Test
    void aRepairBuysAndAnUndoRefunds() {
        assertEquals(Sfx.SHOP_BUY, HangarScreen.doneSound(Optional.empty(), Focus.REPAIR, Command.REPAIR));
        assertEquals(Sfx.SHOP_SELL, HangarScreen.doneSound(Optional.empty(), Focus.COMMANDS, Command.UNDO));
        assertEquals(Sfx.MENU_CONFIRM, HangarScreen.doneSound(Optional.empty(), Focus.COMMANDS, Command.SAVE));
    }
}
