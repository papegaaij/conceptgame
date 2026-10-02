package vanguard.content;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Locale;

/**
 * The parallax layers a backdrop draws on (design/art-direction, Parallax layer model), back to
 * front; the play plane ({@code air}) lies between {@link #LOW_AIR} and {@link #HIGH_AIR}.
 */
public enum BackdropLayer {
    @JsonProperty("deep")
    DEEP,
    @JsonProperty("far")
    FAR,
    @JsonProperty("ground")
    GROUND,
    @JsonProperty("low-air")
    LOW_AIR,
    @JsonProperty("high-air")
    HIGH_AIR;

    /** The name in the documents and data files. */
    public String key() {
        return name().toLowerCase(Locale.ROOT).replace('_', '-');
    }
}
