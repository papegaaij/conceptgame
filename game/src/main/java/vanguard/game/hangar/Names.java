package vanguard.game.hangar;

import java.util.Locale;
import vanguard.content.campaign.LoadoutSlot;

/** How the hangar writes item and slot names and numbers in the bitmap fonts' upper case. */
public final class Names {
    private Names() {}

    /** An item's name in upper case: {@code Mk II "Arc"} is {@code MK II "ARC"}. */
    public static String of(String name) {
        return name.toUpperCase(Locale.ROOT);
    }

    /**
     * A part's short name for the module tiles and the schematic's callouts: the model without its
     * nickname, {@code COMP I} for Composite I, {@code MICRO-MSL} for the Micro-missile Pod.
     */
    public static String tile(String name) {
        String model =
                name.contains("\"") ? name.substring(0, name.indexOf('"')).strip() : name;
        return of(model)
                .replace("COMPOSITE", "COMP")
                .replace("STANDARD", "STD")
                .replace("SENSOR SUITE", "SENSOR")
                .replace("PICKUP MAGNET", "MAGNET")
                .replace("MICRO-MISSILE", "MICRO-MSL")
                .replace(" POD", "");
    }

    /** Power in MW with at most one decimal: {@code 4.5 MW}, {@code 8 MW}. */
    public static String megawatts(double mw) {
        return number(mw) + " MW";
    }

    /** A number with at most one decimal. */
    public static String number(double value) {
        double rounded = Math.round(value * 10) / 10.0;
        return rounded == Math.rint(rounded)
                ? Long.toString((long) rounded)
                : String.format(Locale.ROOT, "%.1f", rounded);
    }

    /** Credits grouped by thousands with a space: {@code CR 12 450}. */
    public static String credits(long value) {
        return "CR " + grouped(value);
    }

    /** A number grouped by thousands with a space: {@code 12 450}. */
    public static String grouped(long value) {
        return String.format(Locale.ROOT, "%,d", value).replace(',', ' ');
    }

    /** The slot's name on the schematic and in the shop's header. */
    public static String slot(LoadoutSlot slot) {
        return switch (slot) {
            case FRONT -> "FRONT";
            case REAR -> "REAR";
            case LEFT_WING -> "L WING";
            case RIGHT_WING -> "R WING";
            case GENERATOR -> "GENERATOR";
            case SHIELD -> "SHIELD";
            case ARMOUR -> "ARMOUR";
            case ENGINE -> "ENGINE";
            case SPECIAL -> "SPECIAL";
            case UTILITY_1 -> "UTILITY BAY 1";
            case UTILITY_2 -> "UTILITY BAY 2";
            case UTILITY_3 -> "UTILITY BAY 3";
        };
    }

    /** The slot's three-letter code on its module tile. */
    public static String code(LoadoutSlot slot) {
        return switch (slot) {
            case GENERATOR -> "GEN";
            case SHIELD -> "SHD";
            case ARMOUR -> "ARM";
            case ENGINE -> "ENG";
            case SPECIAL -> "SPC";
            case UTILITY_1, UTILITY_2, UTILITY_3 -> "UTL";
            case FRONT, REAR, LEFT_WING, RIGHT_WING -> slot(slot);
        };
    }
}
