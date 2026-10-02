package vanguard.content;

import com.fasterxml.jackson.annotation.JsonFormat;

/** A position in px, written {@code [x, y]}. */
@JsonFormat(shape = JsonFormat.Shape.ARRAY)
public record Point(double x, double y) {}
