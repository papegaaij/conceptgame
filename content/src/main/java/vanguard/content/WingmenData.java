package vanguard.content;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * design/player/wingmen/data.yaml (M5 part A): Rook in the escort slot, his craft and AI, his four
 * guns (each derived from a player weapon's per-level table) and his radio barks.
 */
public record WingmenData(Rook rook, Guns guns, Barks barks) {
    /**
     * Rook's craft and AI (design/player/wingmen, Rook's AI).
     *
     * @param joins hired when the hangar opens with this level (or a later one) next
     * @param side a new campaign's side, {@code left} or {@code right}
     * @param size the craft sprite, px
     * @param muzzle his one muzzle at the nose, px from the sprite's top left
     * @param speed top speed, px/s
     * @param minDistance he never comes closer to the player's centre, px
     * @param edgeGap the gap his craft keeps to every play field edge, px
     * @param ramDamage what an air enemy he collides with takes
     * @param launchWarning the hangar warns while his armour is below this share
     * @param swapSeconds his own slot back inside the field this long before he returns to it
     * @param flankDistance px horizontally: the Wide formation and target priority 2
     * @param cone degrees, the full width of his firing cone ahead
     * @param range how far ahead he fires, px
     * @param recentHitSeconds target priority 1: an enemy the player damaged this recently
     */
    public record Rook(
            int joins,
            String side,
            double armour,
            Size size,
            Size hitbox,
            Point muzzle,
            double speed,
            double accelerationSeconds,
            double minDistance,
            double edgeGap,
            double ramDamage,
            double launchWarning,
            double glideSeconds,
            double swapSeconds,
            Formations formations,
            double flankDistance,
            double reactionSeconds,
            Dodge dodge,
            double cone,
            double range,
            double recentHitSeconds,
            Eject eject) {
        public Rook {
            Check.positive("joins", joins);
            Check.that(side.equals("left") || side.equals("right"), "side must be left or right, was " + side);
            Check.positive("armour", armour);
            Check.positive("speed", speed);
            Check.positive("acceleration_seconds", accelerationSeconds);
            Check.notNegative("min_distance", minDistance);
            Check.notNegative("edge_gap", edgeGap);
            Check.notNegative("ram_damage", ramDamage);
            Check.share("launch_warning", launchWarning);
            Check.positive("glide_seconds", glideSeconds);
            Check.notNegative("swap_seconds", swapSeconds);
            Check.notNegative("flank_distance", flankDistance);
            Check.notNegative("reaction_seconds", reactionSeconds);
            Check.that(cone > 0 && cone < 180, "cone must be between 0 and 180 degrees, was " + cone);
            Check.positive("range", range);
            Check.notNegative("recent_hit_seconds", recentHitSeconds);
        }
    }

    /** His formation slots, each {@code [x beside the player on his side, y behind the player]}, px. */
    public record Formations(Point wing, Point wide, Point trail) {}

    /**
     * His dodging: every {@code interval} s he predicts the enemy bullets {@code lookAhead} s ahead;
     * he reacts to the share {@code reacts} of those passing within {@code clearance} px (each
     * decided once) with a sidestep of {@code step} px at right angles to it. His slot keeps the
     * clearance to the air enemies' hit boxes too.
     */
    public record Dodge(double interval, double lookAhead, double clearance, double step, double reacts) {
        public Dodge {
            Check.positive("interval", interval);
            Check.positive("look_ahead", lookAhead);
            Check.positive("clearance", clearance);
            Check.positive("step", step);
            Check.share("reacts", reacts);
        }
    }

    /** The ejection: the craft's explosion class and the pod's drift to the nearer side edge, px/s. */
    public record Eject(String explosion, double podSpeed) {
        public Eject {
            Check.positive("pod_speed", podSpeed);
        }
    }

    /**
     * His guns.
     *
     * @param priceFactor of the base weapon's price and upgrade base
     * @param list the first is the starter: free, fitted when he joins
     */
    public record Guns(double priceFactor, List<Gun> list) {
        public Guns {
            Check.share("price_factor", priceFactor);
            Check.notEmpty("list", list);
            list = List.copyOf(list);
            Set<String> ids = new HashSet<>();
            for (Gun gun : list) {
                Check.that(ids.add(gun.id()), "list: the id '" + gun.id() + "' is not unique");
            }
        }

        /** The gun fitted when he joins. */
        public Gun starter() {
            return list.getFirst();
        }

        /** The gun with this id; it must exist. */
        public Gun gun(String id) {
            return list.stream()
                    .filter(gun -> gun.id().equals(id))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("no escort gun '" + id + "'"));
        }
    }

    /**
     * One of Rook's guns.
     *
     * @param id its id in the save and the {@code --escort} option
     * @param name its shop name
     * @param base the slug of the player weapon it is derived from
     * @param scale multiplies the base weapon's damage per projectile at each level
     * @param available in the escort shop from the hangar visit before this (as the items' availability)
     */
    public record Gun(String id, String name, String base, double scale, String available) {
        public Gun {
            Check.that(id.matches("[a-z0-9]+(-[a-z0-9]+)*"), "id: '" + id + "' is not a kebab-case slug");
            Check.positive("scale", scale);
        }
    }

    /**
     * His radio barks (design/player/wingmen, Radio barks).
     *
     * @param spacing s from the start of a Rook line to the next bark
     * @param triggers in priority order, the first the highest
     */
    public record Barks(String speaker, double spacing, List<Bark> triggers) {
        public Barks {
            Check.positive("spacing", spacing);
            Check.notEmpty("triggers", triggers);
            triggers = List.copyOf(triggers);
        }

        /** The bark of {@code trigger}, if there is one. */
        public Optional<Bark> bark(String trigger) {
            return triggers.stream()
                    .filter(bark -> bark.trigger().equals(trigger))
                    .findFirst();
        }
    }

    /**
     * One bark trigger with its line variants.
     *
     * @param ahead s before the wave enters (the rear wave)
     * @param below the armour share it falls below (the armour triggers)
     * @param kills the kills of a streak within {@code seconds}
     */
    public record Bark(
            String trigger,
            Expression expression,
            Optional<Boolean> shout,
            List<String> lines,
            Optional<Double> ahead,
            Optional<Double> below,
            Optional<Integer> kills,
            Optional<Double> seconds) {
        public Bark {
            Check.notEmpty("lines", lines);
            lines = List.copyOf(lines);
            below.ifPresent(share -> Check.share("below", share));
        }

        public boolean shouted() {
            return shout.orElse(false);
        }
    }

    /** The bark trigger whose share is Rook's low-armour line. */
    public static final String ROOK_ARMOUR = "rook-armour";
}
