package vanguard.content;

import java.util.List;

/**
 * Range checks for the data records' compact constructors. A failing check throws
 * {@link IllegalArgumentException}; the loader adds the file and the record's path to the message.
 */
final class Check {
    private Check() {}

    static double positive(String field, double value) {
        if (!(value > 0)) {
            throw new IllegalArgumentException(field + " must be > 0, was " + value);
        }
        return value;
    }

    static double notNegative(String field, double value) {
        if (!(value >= 0)) {
            throw new IllegalArgumentException(field + " must be >= 0, was " + value);
        }
        return value;
    }

    /** A share between 0 and 1. */
    static double share(String field, double value) {
        if (!(value >= 0 && value <= 1)) {
            throw new IllegalArgumentException(field + " must be between 0 and 1, was " + value);
        }
        return value;
    }

    static <T> List<T> notEmpty(String field, List<T> list) {
        if (list.isEmpty()) {
            throw new IllegalArgumentException(field + " must not be empty");
        }
        return list;
    }

    static void that(boolean condition, String message) {
        if (!condition) {
            throw new IllegalArgumentException(message);
        }
    }
}
