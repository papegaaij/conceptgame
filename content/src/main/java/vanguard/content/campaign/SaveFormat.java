package vanguard.content.campaign;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.json.JsonMapper;

/**
 * The save file format (design/systems/saves): a {@link SaveGame} as a JSON document with a
 * {@code version} field. A save of this game's version is read as it is; older versions would be
 * migrated here (version 1 is the first, so there are none yet); a newer or unknown version, a
 * missing or unknown field and an invalid value are rejected, so a save is never half read.
 */
public final class SaveFormat {
    /** The version this game writes. */
    public static final int VERSION = 1;

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
        try {
            return MAPPER.treeToValue(tree, SaveGame.class);
        } catch (JacksonException e) {
            throw new SaveException("invalid save: " + e.getOriginalMessage());
        }
    }
}
