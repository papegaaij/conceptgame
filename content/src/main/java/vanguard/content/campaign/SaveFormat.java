package vanguard.content.campaign;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

/**
 * The save file format (design/systems/saves): a {@link SaveGame} as a JSON document with a
 * {@code version} field. A save of this game's version is read as it is; an older version is
 * migrated first; a newer or unknown version, a missing or unknown field and an invalid value are
 * rejected, so a save is never half read.
 *
 * <p>Versions: 1 (M3 part B1); 2 (M4 part G) adds {@code stats.levels}, each won level's banked
 * credits and kills for the act summary. A version 1 save loads with none recorded.
 */
public final class SaveFormat {
    /** The version this game writes. */
    public static final int VERSION = 2;

    private static final JsonMapper MAPPER = JsonMapper.builder()
            .enable(SerializationFeature.INDENT_OUTPUT)
            .enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .enable(DeserializationFeature.FAIL_ON_MISSING_CREATOR_PROPERTIES)
            .enable(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES)
            .disable(DeserializationFeature.ACCEPT_FLOAT_AS_INT)
            .build();

    private SaveFormat() {}

    public static String write(SaveGame save) {
        return MAPPER.writeValueAsString(save);
    }

    public static SaveGame read(String json) throws SaveException {
        JsonNode tree;
        try {
            tree = MAPPER.readTree(json);
        } catch (JacksonException e) {
            throw new SaveException("not a save file: " + e.getOriginalMessage());
        }
        JsonNode version = tree == null ? null : tree.get("version");
        if (version == null || !version.isInt()) {
            throw new SaveException("no format version");
        }
        if (version.intValue() > VERSION) {
            throw new SaveException("saved by a newer version of the game (format " + version.intValue() + ")");
        }
        if (version.intValue() < 1) {
            throw new SaveException("unknown format version " + version.intValue());
        }
        if (version.intValue() == 1) {
            migrateFrom1(tree);
        }
        try {
            return MAPPER.treeToValue(tree, SaveGame.class);
        } catch (JacksonException e) {
            throw new SaveException("invalid save: " + e.getOriginalMessage());
        }
    }

    /** Version 1 to 2: no level recorded in the stats yet. */
    private static void migrateFrom1(JsonNode tree) throws SaveException {
        if (!(tree instanceof ObjectNode save) || !(save.get("stats") instanceof ObjectNode stats)) {
            throw new SaveException("invalid save: no stats");
        }
        if (stats.has("levels")) {
            throw new SaveException("invalid save: a version 1 save has no stats.levels");
        }
        stats.putObject("levels");
        save.put("version", VERSION);
    }
}
