package vanguard.game.render;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import vanguard.content.Content;
import vanguard.content.ContentLoader;
import vanguard.content.EnemyBasisData;

class BulletLooksTest {
    private static double damage(Content content, String bullet) {
        return content.enemyBasis().bulletDamage().stream()
                .filter(b -> b.bullet().equals(bullet))
                .mapToDouble(EnemyBasisData.BulletDamage::damage)
                .findFirst()
                .orElseThrow();
    }

    @Test
    void theLargeOrbIsDrawnFromTheMediumClassesDamage() {
        Content content = ContentLoader.fromClasspath();
        assertEquals(damage(content, "medium"), BulletLooks.MEDIUM_DAMAGE, "design/enemies/data.yaml");
        assertTrue(damage(content, "small") < BulletLooks.MEDIUM_DAMAGE, "a small bullet stays the small orb");
    }
}
