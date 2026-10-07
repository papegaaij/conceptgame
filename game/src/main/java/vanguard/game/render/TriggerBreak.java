package vanguard.game.render;

import java.util.Set;
import vanguard.sim.LevelScript;

/**
 * Which triggers break like a destructible on the hit that spends them (design/tech/architecture,
 * ground targets): those whose look's spent frame is a wreck, Level 08's billboard (intact, hit,
 * toppled). Its topple plays a destructible's break: the small explosion where it stands, the
 * small blast and the crumble of its size; its toppled frame stays as the wreck. The other triggers
 * are opened or switched off rather than destroyed and stay quiet: Level 06's survey cache is shot
 * open, its terminal releases its core, the beacon, the lifeboat lights and the sled's clamp go dark.
 */
public final class TriggerBreak {
    /** The trigger looks whose spent frame is a wreck. */
    private static final Set<String> WRECKED_WHEN_SPENT = Set.of("billboard");

    private TriggerBreak() {}

    /** Whether {@code spec} is a trigger that breaks apart on the hit that spends it. */
    public static boolean breaks(LevelScript.GroundObjectSpec spec) {
        return spec.trigger() && WRECKED_WHEN_SPENT.contains(spec.look());
    }
}
