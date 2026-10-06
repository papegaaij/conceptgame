package vanguard.sim;

/**
 * A wingman in the escort slot (design/player/wingmen: Rook), built by
 * {@code vanguard.content.SimSpecs} from design/player/wingmen/data.yaml: his side, his armour at
 * the level start, his craft, his AI and his fitted gun.
 *
 * @param side the player's side he flies on (a hangar setting)
 * @param armour his armour at the level start (a retry starts him with it again)
 * @param craft his craft's numbers
 * @param ai his formations and reactions
 * @param gun his fitted gun at its level, its muzzles around his centre (no overdrive)
 */
public record WingmanSpec(Side side, double armour, Craft craft, Ai ai, WeaponSpec gun) {
    public WingmanSpec {
        if (!(armour > 0 && armour <= craft.maxArmour())) {
            throw new IllegalArgumentException(
                    "a wingman flies with armour in (0, " + craft.maxArmour() + "], not " + armour);
        }
    }

    /** The player's side a wingman flies on. */
    public enum Side {
        LEFT,
        RIGHT;

        /** -1 on the left, 1 on the right: a formation offset's x times this is px to the right of the player. */
        public int sign() {
            return this == LEFT ? -1 : 1;
        }

        public Side other() {
            return this == LEFT ? RIGHT : LEFT;
        }
    }

    /**
     * His craft.
     *
     * @param size the edge of the square craft sprite, px
     * @param maxArmour his full armour (no shield)
     * @param hitbox on the {@code air} layer
     * @param speed top speed, px/s
     * @param accelerationSeconds standing still to full speed
     * @param minDistance he never comes closer to the player's centre, px
     * @param edgeGap the gap his craft keeps to every play field edge, px
     * @param ramDamage what an air enemy he collides with takes (he takes its full contact damage)
     * @param lowArmour his low-armour line is set off once per attempt below this share
     * @param podSpeed the eject pod's drift towards the nearer side edge, px/s
     * @param bankStepTicks steps per banking frame change (the player's)
     */
    public record Craft(
            double size,
            double maxArmour,
            Hitbox hitbox,
            double speed,
            double accelerationSeconds,
            double minDistance,
            double edgeGap,
            double ramDamage,
            double lowArmour,
            double podSpeed,
            int bankStepTicks) {
        /** How close his centre may come to a play field edge: half the craft plus the gap. */
        public double edgeLimit() {
            return size / 2 + edgeGap;
        }
    }

    /**
     * A formation slot around the player: {@code x} px beside him on the wingman's side, {@code y}
     * px behind him (down the screen).
     */
    public record Offset(double x, double y) {}

    /**
     * His AI (design/player/wingmen, Rook's AI).
     *
     * @param wing the default slot
     * @param wide while a sides wave is active or an enemy is within {@code flankDistance} of him
     * @param trail while a rear wave is active (it wins over wide)
     * @param glideSeconds to a new slot
     * @param swapSeconds his own slot back inside the field this long before he leaves the mirrored one
     * @param flankDistance px: an enemy this close to him is a flank threat (the wide slot, target priority 2)
     * @param reactionSeconds the delay of target switches, formation changes, side swaps and dodges
     * @param dodgeInterval s between his bullet predictions
     * @param lookAhead s ahead he predicts the bullets
     * @param clearance a bullet passing this close is dodged, px; his slot keeps this gap to the
     *     air enemies' hit boxes too
     * @param dodgeStep his sidestep at right angles to the bullet, px
     * @param reacts the share of the bullets he predicts that he reacts to (each decided once, by his
     *     generator); the rest he misses
     * @param coneHalfAngle he fires at targets within this angle either side of straight ahead, radians
     * @param range how far ahead he fires, px
     * @param recentHitSeconds an enemy the player damaged this recently is his first target
     */
    public record Ai(
            Offset wing,
            Offset wide,
            Offset trail,
            double glideSeconds,
            double swapSeconds,
            double flankDistance,
            double reactionSeconds,
            double dodgeInterval,
            double lookAhead,
            double clearance,
            double dodgeStep,
            double reacts,
            double coneHalfAngle,
            double range,
            double recentHitSeconds) {
        public Ai {
            if (!(reacts >= 0 && reacts <= 1)) {
                throw new IllegalArgumentException(
                        "a wingman reacts to a share in [0, 1] of the bullets, not " + reacts);
            }
        }
    }
}
