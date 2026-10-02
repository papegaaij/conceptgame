package vanguard.content.campaign;

/** A save file that cannot be read: malformed, of an unknown format version, or with invalid values. */
public final class SaveException extends Exception {
    private static final long serialVersionUID = 1L;

    public SaveException(String message) {
        super(message);
    }
}
