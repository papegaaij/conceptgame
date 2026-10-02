package vanguard.content;

import com.fasterxml.jackson.annotation.JsonFormat;

/** A width and height in px, written {@code [width, height]}. */
@JsonFormat(shape = JsonFormat.Shape.ARRAY)
public record Size(double width, double height) {
    public Size {
        Check.positive("width", width);
        Check.positive("height", height);
    }
}
