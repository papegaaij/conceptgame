package vanguard.sim;

/**
 * The deterministic game state, advanced in fixed 60 Hz steps by {@link #step(int)}. All entities
 * live in pools allocated up front, so stepping does not allocate. Transcendental functions use
 * {@link Trig} and {@link StrictMath}, so the same seed and commands give the same
 * {@link #stateHash()} on every platform.
 */
public final class World {
    public static final int WIDTH = 480;
    public static final int HEIGHT = 540;
    public static final double STEP_SECONDS = 1.0 / 60;
    /** Ground layer scroll per step (a "normal" level: 160 px/s). */
    public static final double GROUND_SCROLL = 160 * STEP_SECONDS;

    private static final double PLAYER_BULLET_SPEED = 600 * STEP_SECONDS;
    private static final double ENEMY_BULLET_SPEED = 120 * STEP_SECONDS;
    private static final double FAN_STEP = 0.10;
    /** New enemies enter just above the top edge, so nearly all of them are on screen. */
    private static final double SPAWN_BAND = 60;
    private static final int ENEMY_BULLET_DAMAGE = 6;
    private static final int BULLET_CAPACITY = 1024;
    private static final int EVENT_CAPACITY = 512;

    private final SimConfig config;
    private final SplitMix64 rng;
    private final Player player = new Player();
    private final Pool<Enemy> enemies;
    private final Pool<Bullet> bullets = new Pool<>(BULLET_CAPACITY, Bullet::new, Bullet[]::new);
    private final SimEvents events = new SimEvents(EVENT_CAPACITY);
    private long tick;
    private long score;
    private double groundScroll;

    public World(SimConfig config) {
        this.config = config;
        this.rng = new SplitMix64(config.seed());
        this.enemies = new Pool<>(config.enemyCount(), Enemy::new, Enemy[]::new);
        while (enemies.size() < config.enemyCount()) {
            spawnEnemy(rng.range(0, HEIGHT));
        }
    }

    /** Advances the game by one step with the given {@link Command} set. */
    public void step(int commands) {
        tick++;
        events.clear();
        rememberPositions();
        groundScroll += GROUND_SCROLL;
        player.move(commands);
        firePlayer(commands);
        updateEnemies();
        moveBullets();
        resolveHits();
        while (enemies.size() < config.enemyCount()) {
            spawnEnemy(HEIGHT + rng.range(10, SPAWN_BAND));
        }
    }

    private void rememberPositions() {
        player.rememberPosition();
        for (int i = 0; i < enemies.size(); i++) {
            enemies.get(i).rememberPosition();
        }
        for (int i = 0; i < bullets.size(); i++) {
            bullets.get(i).rememberPosition();
        }
    }

    private void firePlayer(int commands) {
        if (player.fireCooldown > 0) {
            player.fireCooldown--;
            return;
        }
        if (!Command.FIRE.in(commands)) {
            return;
        }
        int count = config.volleySize();
        for (int i = 0; i < count; i++) {
            double angle = StrictMath.PI / 2 + (i - (count - 1) / 2.0) * FAN_STEP;
            fire(Faction.PLAYER, player.x, player.y + 12, angle, PLAYER_BULLET_SPEED, 1);
        }
        player.fireCooldown = config.volleyTicks() - 1;
        events.add(SimEvents.Type.PLAYER_SHOT, player.x, player.y);
    }

    private void updateEnemies() {
        for (int i = enemies.size() - 1; i >= 0; i--) {
            Enemy enemy = enemies.get(i);
            enemy.move(GROUND_SCROLL);
            if (enemy.belowScreen()) {
                enemies.free(i);
            } else if (enemy.onScreen() && enemy.layer().hittable() && --enemy.fireCooldown <= 0) {
                double angle = StrictMath.atan2(player.y - enemy.y, player.x - enemy.x);
                fire(Faction.ENEMY, enemy.x, enemy.y, angle, ENEMY_BULLET_SPEED, ENEMY_BULLET_DAMAGE);
                enemy.fireCooldown = config.enemyFireTicks() / 2 + rng.nextInt(config.enemyFireTicks());
                events.add(SimEvents.Type.ENEMY_SHOT, enemy.x, enemy.y);
            }
        }
    }

    private void fire(Faction faction, double x, double y, double angle, double speed, int damage) {
        Bullet bullet = bullets.obtain();
        if (bullet != null) {
            bullet.fire(faction, x, y, Trig.cos(angle) * speed, Trig.sin(angle) * speed, damage);
        }
    }

    private void moveBullets() {
        for (int i = bullets.size() - 1; i >= 0; i--) {
            Bullet bullet = bullets.get(i);
            bullet.move();
            if (bullet.offScreen()) {
                bullets.free(i);
            }
        }
    }

    private void resolveHits() {
        for (int i = bullets.size() - 1; i >= 0; i--) {
            Bullet bullet = bullets.get(i);
            boolean hit = bullet.faction() == Faction.PLAYER ? hitEnemy(bullet) : hitPlayer(bullet);
            if (hit) {
                bullets.free(i);
            }
        }
    }

    private boolean hitEnemy(Bullet bullet) {
        for (int j = enemies.size() - 1; j >= 0; j--) {
            Enemy enemy = enemies.get(j);
            if (enemy.layer().hittable() && enemy.onScreen() && bullet.overlaps(enemy)) {
                enemy.hp -= bullet.damage;
                events.add(SimEvents.Type.HIT, bullet.x, bullet.y);
                if (enemy.hp <= 0) {
                    score += 100L * (enemy.variant() + 1);
                    events.add(SimEvents.Type.KILL, enemy.x, enemy.y);
                    enemies.free(j);
                }
                return true;
            }
        }
        return false;
    }

    private boolean hitPlayer(Bullet bullet) {
        if (!bullet.overlaps(player)) {
            return false;
        }
        player.takeDamage(bullet.damage);
        events.add(SimEvents.Type.PLAYER_HIT, bullet.x, bullet.y);
        return true;
    }

    private void spawnEnemy(double y) {
        Enemy enemy = enemies.obtain();
        int roll = rng.nextInt(20);
        Layer layer = roll < 5 ? Layer.GROUND : roll < 7 ? Layer.SUB : roll < 11 ? Layer.LOW_AIR : Layer.AIR;
        enemy.spawn(layer, rng.nextInt(3), rng.range(40, WIDTH - 40), y, rng);
    }

    /** A hash over the complete game state; equal hashes mean equal replays. */
    public long stateHash() {
        StateHash hash = new StateHash()
                .add(tick).add(score).add(rng.state()).add(groundScroll)
                .add(player.x).add(player.y).add(player.shield()).add(player.armour())
                .add(player.retries()).add(player.fireCooldown)
                .add(enemies.size()).add(bullets.size());
        for (int i = 0; i < enemies.size(); i++) {
            Enemy e = enemies.get(i);
            hash.add(e.layer().ordinal()).add(e.variant()).add(e.x).add(e.y).add(e.hp).add(e.fireCooldown);
        }
        for (int i = 0; i < bullets.size(); i++) {
            Bullet b = bullets.get(i);
            hash.add(b.faction().ordinal()).add(b.x).add(b.y).add(b.vx()).add(b.vy()).add(b.damage);
        }
        return hash.value();
    }

    public Player player() {
        return player;
    }

    public int enemyCount() {
        return enemies.size();
    }

    public Enemy enemy(int index) {
        return enemies.get(index);
    }

    public int bulletCount() {
        return bullets.size();
    }

    public Bullet bullet(int index) {
        return bullets.get(index);
    }

    /** Events of the last step. */
    public SimEvents events() {
        return events;
    }

    public long tick() {
        return tick;
    }

    public long score() {
        return score;
    }

    /** Total distance the ground layer has scrolled, in pixels. */
    public double groundScroll() {
        return groundScroll;
    }
}
