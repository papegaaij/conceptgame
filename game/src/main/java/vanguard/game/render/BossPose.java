package vanguard.game.render;

import vanguard.sim.BossSpec;
import vanguard.sim.LevelScript;
import vanguard.sim.SetPiece;

/**
 * What the renderer reads of a hull boss's state (design/enemies/bosses/brood-carrier, D1 of M4
 * part G), from the simulation's pose state: its pose from nose-down through the pre-rendered turn
 * frames to broadside, which parts are fire-only turrets and bay sacs, whether a part is open (a sac
 * in its window, the core once exposed), and how long its chained death runs ({@code death_seconds};
 * without it an act boss's 3 s, design/enemies/bosses: {@code huge} death, and a mid-boss none: its
 * parts burst {@value #LEGACY_CHAIN_STEP_TICKS} steps apart in list order, as Level 05's frigate
 * always has).
 */
final class BossPose {
    /** An act boss's chained death, tail to head (design/enemies/bosses/brood-carrier, Death). */
    static final double ACT_BOSS_CHAIN_SECONDS = 3;
    /** A mid-boss's parts burst this many steps apart in list order (the Gorgon Frigate's death). */
    static final int LEGACY_CHAIN_STEP_TICKS = 6;

    private BossPose() {}

    /**
     * Where the hull is in its turn: 0 nose-down (phase 1, on {@code high-air}), 1 broadside (phases
     * 2–3), between them the turn's frames.
     */
    static double turn(SetPiece piece, float alpha) {
        int poses = piece.boss().map(boss -> boss.poses().size()).orElse(0);
        if (poses < 2) {
            return 0;
        }
        double share = piece.motion() == SetPiece.Motion.TURN ? piece.turnShare(alpha) : 0;
        double from = share > 0 ? piece.turnFromPose() : piece.pose();
        double to = share > 0 ? piece.turnToPose() : piece.pose();
        return Math.clamp((from + (to - from) * share) / (poses - 1), 0, 1);
    }

    /** The turn's direction: +1 swings the head (nose-down at the bottom) to the right of the screen. */
    static int turnDirection(SetPiece piece) {
        // The design's broadside has its head to the right (BC, the turn).
        return 1;
    }

    /** Whether part {@code p} is a fire-only turret (no HP, never wrecked, aimed at the ship). */
    static boolean turret(SetPiece piece, int p) {
        return piece.partArmoured(p);
    }

    /** Whether part {@code p} is a bay sac: neither the vital core nor a turret. */
    static boolean sac(SetPiece piece, int p) {
        return !piece.spec().parts().get(p).vital() && !turret(piece, p);
    }

    /**
     * Whether part {@code p} is open: a sac in its window (its window holds it open, also while the
     * boss moves), any other part while it takes damage (the core once exposed).
     */
    static boolean open(SetPiece piece, int p) {
        if (piece.partWrecked(p)) {
            return false;
        }
        return piece.partWindowed(p) ? piece.partOpen(p) : !piece.partShielded(p);
    }

    /** The vital part (the core under its iris), or -1. */
    static int core(SetPiece piece) {
        for (int p = 0; p < piece.partCount(); p++) {
            if (piece.spec().parts().get(p).vital()) {
                return p;
            }
        }
        return -1;
    }

    /** Its chained death's length, s; NaN for a mid-boss's per-part bursts in list order. */
    static double chainSeconds(SetPiece piece) {
        BossSpec boss = piece.boss().orElseThrow();
        if (boss.deathSeconds() > 0) {
            return boss.deathSeconds();
        }
        return boss.midBoss() ? Double.NaN : ACT_BOSS_CHAIN_SECONDS;
    }

    /** Whether it is an act boss: the screen flash at its death (mid-bosses have none). */
    static boolean actBoss(SetPiece piece) {
        return piece.boss().map(boss -> !boss.midBoss()).orElse(false);
    }

    /** The turn as an angle, radians counter-clockwise (y up): 0 nose-down, ±pi/2 broadside. */
    static double angle(SetPiece piece, double turn) {
        return turnDirection(piece) * turn * Math.PI / 2;
    }

    /** Part {@code p}'s data offset (its nose-down offset) turned by {@code angle}: x right. */
    static double turnedX(LevelScript.PartSpec part, double angle) {
        return part.dx() * Math.cos(angle) - part.dy() * Math.sin(angle);
    }

    /** Part {@code p}'s data offset turned by {@code angle}: y up. */
    static double turnedY(LevelScript.PartSpec part, double angle) {
        return part.dx() * Math.sin(angle) + part.dy() * Math.cos(angle);
    }
}
