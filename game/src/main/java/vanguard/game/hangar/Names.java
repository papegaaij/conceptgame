package vanguard.game.hangar;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import vanguard.content.campaign.LoadoutSlot;

/** How the hangar writes item and slot names and numbers in the bitmap fonts' upper case. */
public final class Names {
    private Names() {}

    /** An item's name in upper case: {@code Mk II "Arc"} is {@code MK II "ARC"}. */
    public static String of(String name) {
        return name.toUpperCase(Locale.ROOT);
    }

    /** The most characters a module tile's name shows: 48 px of tile in the 8 px label font. */
    public static final int TILE_CHARS = 6;

    /** The tiles' abbreviations, longest first so that a name is not shortened twice. */
    private static final Map<String, String> TILE_WORDS = tileWords();

    /**
     * A weapon's or Rook's gun's short name for the schematic's callouts: the model without its
     * nickname, {@code MICRO-MSL} for the Micro-missile Pod.
     */
    public static String callout(String name) {
        return of(model(name)).replace("MICRO-MISSILE", "MICRO-MSL").replace(" POD", "");
    }

    /**
     * A core part's, special's or utility module's name on its module tile, at most {@link
     * #TILE_CHARS} characters: the model without its nickname ({@code MK II} for Mk II "Arc"),
     * with the tiles' abbreviations ({@code CMP I}, {@code STRIKE}, {@code S-BOMB}, {@code
     * FLARES}, {@code TARGET}, {@code SALVGE}); a longer name loses its spaces and is cut.
     */
    public static String tile(String name) {
        String text = abbreviated(name);
        if (text.length() <= TILE_CHARS) {
            return text;
        }
        String joined = text.replace(" ", "");
        return joined.substring(0, Math.min(TILE_CHARS, joined.length()));
    }

    /** The tile name before the fallback cut: the model with the tiles' abbreviations. */
    static String abbreviated(String name) {
        String text = of(model(name));
        for (Map.Entry<String, String> word : TILE_WORDS.entrySet()) {
            text = text.replace(word.getKey(), word.getValue());
        }
        return text;
    }

    private static Map<String, String> tileWords() {
        Map<String, String> words = new LinkedHashMap<>();
        words.put("TARGETING COMPUTER", "TARGET");
        words.put("SALVAGE SCANNER", "SALVGE");
        words.put("PICKUP MAGNET", "MAGNET");
        words.put("SENSOR SUITE", "SENSOR");
        words.put("DECOY FLARES", "FLARES");
        words.put("SMART BOMB", "S-BOMB");
        words.put("COMPOSITE", "CMP");
        words.put("AIRSTRIKE", "STRIKE");
        words.put("STANDARD", "STD");
        return words;
    }

    /** The model without its nickname: {@code Mk II} for {@code Mk II "Arc"}. */
    private static String model(String name) {
        return name.contains("\"") ? name.substring(0, name.indexOf('"')).strip() : name;
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
            case ESCORT -> "ESCORT";
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
            case ESCORT -> "ESC";
            case FRONT, REAR, LEFT_WING, RIGHT_WING -> slot(slot);
        };
    }
}
