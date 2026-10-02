package vanguard.content;

import com.fasterxml.jackson.annotation.JsonFormat;

/** A rectangle in a sprite, px from its top left, written {@code [x, y, width, height]}. */
@JsonFormat(shape = JsonFormat.Shape.ARRAY)
public record Box(double x, double y, double width, double height) {
    public Box {
        Check.notNegative("x", x);
        Check.notNegative("y", y);
        Check.positive("width", width);
        Check.positive("height", height);
    }
}
