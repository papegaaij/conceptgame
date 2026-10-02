package vanguard.content;

import java.util.List;
import java.util.Map;

/**
 * design/enemies/data.yaml: the balancing basis and the formation vocabulary all enemies share.
 *
 * @param referenceDps the single-target player DPS the stat blocks assume, per level
 * @param bulletDamage damage to the player per bullet class
 * @param contactDamage damage of touching an enemy, per size tier
 * @param formations the formation names with their descriptions
 */
public record EnemyBasisData(
        Map<Integer, Double> referenceDps,
        List<BulletDamage> bulletDamage,
        Map<Tier, Double> contactDamage,
        Map<String, String> formations) {
    public EnemyBasisData {
        referenceDps = Map.copyOf(referenceDps);
        Check.that(contactDamage.size() == Tier.values().length, "contact_damage: one value per size tier");
        contactDamage = Map.copyOf(contactDamage);
        formations = Map.copyOf(formations);
    }

    /** Damage of one bullet class. */
    public record BulletDamage(String bullet, double damage) {
        public BulletDamage {
            Check.positive("damage", damage);
        }
    }

    /** Whether {@code bullet} names a bullet class. */
    boolean knowsBullet(String bullet) {
        return bulletDamage.stream().anyMatch(b -> b.bullet().equals(bullet));
    }
}
