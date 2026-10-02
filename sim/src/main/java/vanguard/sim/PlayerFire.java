package vanguard.sim;

import java.util.List;

/**
 * The ship's shots: the volleys its gun fires, their flight, and what they hit on the enemies'
 * layers and the ground layer below (design/enemies, layer rules). What a hit destroys is handed
 * to {@link Hits}, which the {@link Sortie} implements.
 */
final class PlayerFire {
    private static final int SHOT_CAPACITY = 64;

    /** What the sortie does when a shot destroys something. */
    interface Hits {
        /** The enemy at {@code index} was destroyed. */
        void enemyDestroyed(int index);

        /** The destructible ground object at {@code index} was destroyed. */
        void groundDestroyed(int index);

        /** The trigger at {@code index} took its last hit and releases its secret. */
        void triggerReleased(int index);
    }

    private final Ship ship;
    private final SimEvents events;
    private final Hits hits;
    private final Pool<Shot> shots = new Pool<>(SHOT_CAPACITY, Shot::new, Shot[]::new);

    PlayerFire(Ship ship, SimEvents events, Hits hits) {
        this.ship = ship;
        this.events = events;
        this.hits = hits;
    }

    void reset() {
        Pools.clear(shots);
    }

    /** Fires when fire is held and the gun is ready. */
    void fire(int commands) {
        if (!ship.fireGun(commands)) {
            return;
        }
        double muzzleY = ship.y() + ship.spec().muzzleOffsetY();
        List<Double> pattern = ship.gun().pattern();
        // Indexed: an iterator would allocate on every volley.
        for (int i = 0; i < pattern.size(); i++) {
            Shot shot = shots.obtain();
            if (shot != null) {
                shot.fire(ship.x() + pattern.get(i), muzzleY, ship.gun().damage());
            }
        }
        events.add(SimEvents.Type.SHOT_FIRED, ship.x(), muzzleY);
    }

    void move() {
        double step = ship.gun().boltSpeed() * SimStep.SECONDS;
        Hitbox bolt = ship.gun().bolt();
        for (int i = shots.size() - 1; i >= 0; i--) {
            Shot shot = shots.get(i);
            shot.move(step);
            if (!PlayField.overlaps(shot.x(), shot.y(), bolt)) {
                shots.free(i);
            }
        }
    }

    void hitEnemies(Pool<Enemy> enemies) {
        Hitbox bolt = ship.gun().bolt();
        for (int i = shots.size() - 1; i >= 0; i--) {
            Shot shot = shots.get(i);
            for (int j = enemies.size() - 1; j >= 0; j--) {
                Enemy enemy = enemies.get(j);
                EnemySpec spec = enemy.spec();
                if (spec.layer().hitByStandardShots()
                        && PlayField.overlaps(enemy.x(), enemy.y(), spec.hitbox())
                        && bolt.overlaps(shot.x(), shot.y(), spec.hitbox(), enemy.x(), enemy.y())) {
                    events.add(SimEvents.Type.ENEMY_HIT, shot.x(), shot.y());
                    if (enemy.damage(shot.damage())) {
                        hits.enemyDestroyed(j);
                    }
                    shots.free(i);
                    break;
                }
            }
        }
    }

    /** Shots that missed the air hit the ground layer below (design/enemies, layer rules). */
    void hitGround(Pool<GroundObject> ground) {
        Hitbox bolt = ship.gun().bolt();
        for (int i = shots.size() - 1; i >= 0; i--) {
            Shot shot = shots.get(i);
            for (int j = ground.size() - 1; j >= 0; j--) {
                GroundObject object = ground.get(j);
                LevelScript.GroundObjectSpec spec = object.spec();
                if (object.hittable() && bolt.overlaps(shot.x(), shot.y(), spec.size(), object.x(), object.y())) {
                    events.add(SimEvents.Type.GROUND_HIT, shot.x(), shot.y(), spec.trigger() ? 1 : 0);
                    shots.free(i);
                    if (spec.trigger()) {
                        if (object.countHit()) {
                            hits.triggerReleased(j);
                        }
                    } else if (object.damage(shot.damage())) {
                        hits.groundDestroyed(j);
                    }
                    break;
                }
            }
        }
    }

    Pool<Shot> shots() {
        return shots;
    }
}
