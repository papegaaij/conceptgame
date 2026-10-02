package vanguard.content;

import com.fasterxml.jackson.annotation.JsonFormat;

/** A range of values, written {@code [min, max]}. */
@JsonFormat(shape = JsonFormat.Shape.ARRAY)
public record Span(double min, double max) {
    public Span {
        Check.that(min <= max, "a range must be written [min, max], was [" + min + ", " + max + "]");
    }
}
