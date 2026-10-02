package vanguard.game.audio;

/**
 * The volume groups of the Audio tab (design/ui/options). The menu sounds play on the effects bus:
 * the Audio tab has no interface volume.
 */
public enum Bus {
    MUSIC,
    EFFECTS,
    /** The radio blips: squelch and typing. */
    RADIO
}
