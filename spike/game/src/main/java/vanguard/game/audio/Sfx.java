package vanguard.game.audio;

/** The round-08 concept sound effects the spike uses, by file name under {@code sfx/}. */
public enum Sfx {
    PLAYER_SHOT("player-shot-r02-a"),
    ENEMY_SHOT("enemy-shot-small-r08-a"),
    HIT("hit-metal-r08-a"),
    KILL("explosion-small-r03-a"),
    PLAYER_HIT("player-shield-hit-r08-a"),
    EXPLOSION_MEDIUM("explosion-medium-r03-b"),
    EXPLOSION_LARGE("explosion-large-r03-a"),
    VULCAN("shot-vulcan-r03-b"),
    LASER("shot-laser-r03-b"),
    MISSILE("shot-missile-r03-a"),
    TESLA("shot-tesla-r03-a"),
    PICKUP("pickup-r01-a"),
    ENEMY_MISSILE("enemy-missile-r08-a"),
    ENEMY_SHOT_HEAVY("enemy-shot-heavy-r08-a"),
    HIT_ORGANIC("hit-organic-r08-a"),
    ARMOUR_HIT("player-armour-hit-r08-a");

    private final String fileName;

    Sfx(String fileName) {
        this.fileName = fileName;
    }

    public String path() {
        return "sfx/" + fileName + ".ogg";
    }
}
