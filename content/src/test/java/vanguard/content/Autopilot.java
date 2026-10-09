package vanguard.content;

import vanguard.sim.Command;
import vanguard.sim.Debris;
import vanguard.sim.Enemy;
import vanguard.sim.EnemyBullet;
import vanguard.sim.GroundObject;
import vanguard.sim.Layer;
import vanguard.sim.Lob;
import vanguard.sim.Mine;
import vanguard.sim.Pickup;
import vanguard.sim.PlayField;
import vanguard.sim.SetPiece;
import vanguard.sim.SimStep;
import vanguard.sim.Sortie;

/**
 * A simple pilot for headless runs of a whole level: fires all the time, lines up under the lowest
 * enemy on screen (or a ground object when the air is clear), sidesteps bullets, rammers, armed
 * spores and large debris that come close, stays under a set piece on the player's layer and
 * aims at its vital part (a boss's lowest part that takes damage; in a boss fight each move is
 * chosen by a short look-ahead over the bullets and air enemies, keeping to the field's centre
 * band), and picks up what drops when nothing threatens. At Level 05's batteries it flies low, so
 * a unit stays in its line of fire as long as possible. With a convoy (Level 04) it
 * cruises among the column, so more of the ground enemies' aimed shots (at the nearer of the ship
 * and the convoy) go for it, and it shoots the ground enemies nearest the convoy first. With a segment chain on the
 * screen (Level 06) it slips sideways to the side clear of its members and lines up under a
 * chain's head rather than its body (a cut grows a new head). Over a hardened destroy-targets unit
 * (Level 09's Hive Nodes, which only bombs and mortars crack) it flies over the unit, where its bombs
 * fall, leading it by the scroll during the bombs' fall, holding its line as over a battery; when
 * one is about to get away it calls the Airstrike (if fitted and charged) with a bomber over it. It reads each unit's current layer (a
 * pouncing Ravager is in the air for part of its leap). With an air escort (Level 10's shuttles) it
 * cruises low, below the shuttle band; it never lines up under a cloaked unit (a Wraith on
 * {@code high-air}, which only homing reaches and which cannot ram), and a decloaked Wraith holding
 * below it gets its x first, so the rear gun hits it; it sidesteps bullets coming up from below as
 * well as from above; it lines up under the ferry hatch while it is on the upper half of the screen,
 * to shoot it open. Against an arena boss with slam lanes (M5 part E, Level 11's Harbour Kraken) it
 * keeps out of every telegraphed lane until its impact, lines up under what is exposed (an arm risen
 * or awash in its lane, else the surfaced head's nearer eye, else the head's place) and dodges the
 * bullets with the boss planner, a move into a lane about to be slammed counting as a hit; played
 * {@link ArenaPlay#ARMS arm-first} it cuts the slam arm guarding the convoy's ships first, baiting
 * the slams into the lanes where they cost least. It reads the sortie's state only, so it is deterministic; the recorded replay stores its commands. Public for the campaign tests (the
 * Act 1 playthrough, {@code ActPlaythroughTest}).
 */
public final class Autopilot {
    private static final double CRUISE_Y = 110;
    private static final double DANGER = 70;
    private static final double DEAD_ZONE = 4;
    /** Its cruise height over a convoy (the column spans y = 118-526). */
    private static final double CONVOY_CRUISE = 220;
    /** How far it keeps from a lit rail (Level 05's sleds), px. */
    private static final double RAIL_CLEARANCE = 44;
    /** How close a chain member may come before it slips aside (Level 06), px. */
    private static final double CHAIN_DX = 52;
    /**
     * Its height while a battery unit is its target (Level 05): low, so a unit scrolling down stays
     * above it, in its line of fire, as long as possible.
     */
    private static final double BATTERY_Y = 45;

    /** Level 09: the bombs' fall, s (design/player/weapons/bomb-rack): it leads a node by the scroll over it. */
    private static final double BOMB_FALL = 0.5;
    /**
     * Level 09: a hardened target this close to the bottom edge, px, with more HP than {@link
     * #STRIKE_HP} gets the Airstrike, if one is fitted and charged.
     */
    private static final double STRIKE_LOW = 170;

    private static final double STRIKE_HP = 15;
    /** The Airstrike's bombers fly this far either side of the ship's x (design/player/specials). */
    private static final double STRIKE_OFFSET = 64;
    /** Two targets this far apart sideways, px, both lie under one strike's two bombers. */
    private static final double STRIKE_PAIR_MIN = 80;

    private static final double STRIKE_PAIR_MAX = 190;

    /** Level 10: how far it keeps above a holding Wraith it lines up over, px. */
    private static final double AMBUSH_ABOVE = 55;
    /** Level 10: its height over a Wraith still cloaked below it, px: clear of the highest hold point and the decloak. */
    private static final double AMBUSH_WAIT = 135;
    /** Level 10: the Airstrike goes to an ambush of at least this many Wraiths (the larger ones, later in the level). */
    private static final int STRIKE_AMBUSH = 3;
    /** Level 10: with an air escort it keeps out of the side lanes, where the Wraiths leave, px from either edge. */
    private static final double SIDE_LANE = 96;

    private static final double CHAIN_ABOVE = 120;
    private static final double CHAIN_BELOW = 70;

    private Autopilot() {}

    /**
     * M5 part E: how it plays an arena boss with slam lanes (Level 11's Harbour Kraken); no other
     * level has one, so the choice moves no other level's results.
     */
    public enum ArenaPlay {
        /**
         * Step E2c's pilot: it lines up under an arm out of the water, else under the surfaced head's
         * nearer eye, and lets the slams fall where its x sends them.
         */
        EYES,
        /**
         * Arm-first (2026-10-09, the user's convoy-bonus measurement): it cuts the slam arm guarding
         * the most ships afloat first. It baits each slam (or volley) to where it costs least by the
         * boss's own lane rules (the healthiest ship of that arm's half, a free lane), waits beside a
         * telegraphed lane and cuts the arm as it lies awash after its impact; the surfaced head's
         * eyes while its window is open between slams. Once some place keeps every slam off the ships
         * (the guarding arm severed) it baits there and plays the head; when the ships of the arm's
         * half cannot spare the slams its cut takes (hard), it plays as {@link #EYES}.
         */
        ARMS
    }

    /**
     * One step's commands, an arena boss played as step E2c's pilot ({@link ArenaPlay#EYES}): the
     * measurements the levels' tests and the balance were made with.
     */
    public static int commands(Sortie sortie) {
        return commands(sortie, ArenaPlay.EYES);
    }

    /** One step's commands, an arena boss played as {@code play} says. */
    public static int commands(Sortie sortie, ArenaPlay play) {
        double shipX = sortie.ship().x();
        double shipY = sortie.ship().y();
        int commands = Command.FIRE.bit();
        double lob = lobThreat(sortie, shipX, shipY);
        if (lob != 0) {
            return commands | (lob < 0 ? Command.LEFT.bit() : Command.RIGHT.bit());
        }
        double coil = chainThreat(sortie, shipX, shipY);
        if (coil != 0) {
            return commands | (coil < 0 ? Command.LEFT.bit() : Command.RIGHT.bit());
        }
        double hazard = hazard(sortie, shipX, shipY);
        if (hazard != 0) {
            return commands | (hazard < 0 ? Command.LEFT.bit() : Command.RIGHT.bit()) | Command.DOWN.bit();
        }
        SetPiece kraken = arenaBoss(sortie);
        if (kraken != null) {
            return commands | arenaMove(sortie, kraken, play);
        }
        SetPiece piece = descended(sortie);
        if (piece != null && piece.boss().isPresent()) {
            return commands | bossMove(sortie, vitalX(piece), Math.min(CRUISE_Y, bodyBottom(piece) - 70));
        }
        double threat = threat(sortie, shipX, shipY);
        if (threat != 0) {
            // With an air escort a threat may come from below (a Wraith's burst, a looping swarm, a Wraith
            // holding at the bottom edge): no lower then, nor below its cruise.
            int vertical = sortie.airEscort() && (shipY < CRUISE_Y || fromBelow(sortie, shipX, shipY))
                    ? 0
                    : Command.DOWN.bit();
            return commands | (threat < 0 ? Command.LEFT.bit() : Command.RIGHT.bit()) | vertical;
        }
        double targetX = shipX;
        double targetY = cruise(sortie);
        if (piece != null) {
            return commands | steer(shipX, shipY, vitalX(piece), Math.min(CRUISE_Y, bodyBottom(piece) - 70));
        }
        Enemy lowest = null;
        for (int i = 0; i < sortie.enemyCount(); i++) {
            Enemy enemy = sortie.enemy(i);
            double y = enemy.renderY(1);
            if (!enemy.cloaked()
                    && y < PlayField.HEIGHT - 10
                    && y > shipY + 40
                    && (lowest == null || y < lowest.renderY(1))) {
                lowest = enemy;
            }
        }
        Enemy behind = rearAmbusher(sortie, shipY);
        if (behind != null) {
            lowest = behind;
            targetY = holding(behind) ? behind.renderY(1) + AMBUSH_ABOVE : AMBUSH_WAIT;
            int pair = ambushPair(sortie);
            if (pair >= 0) {
                Enemy a = sortie.enemy(pair / 1024);
                Enemy b = sortie.enemy(pair % 1024);
                double strikeX = (a.renderX(1) + b.renderX(1)) / 2;
                boolean rising = a.ambushPhase() == Enemy.AmbushPhase.RISE && b.ambushPhase() == Enemy.AmbushPhase.RISE;
                if (rising && Math.abs(strikeX - shipX) <= 3 * DEAD_ZONE) {
                    return commands | Command.SPECIAL.bit();
                }
                return commands | steer(shipX, shipY, strikeX, AMBUSH_WAIT);
            }
        }
        Enemy battery = battery(sortie, shipY);
        if (battery != null) {
            lowest = battery;
            targetY = Math.min(targetY, BATTERY_Y);
        }
        Enemy node = hardenedTarget(sortie);
        if (node != null) {
            lowest = node;
            targetY = Math.max(40, node.renderY(1) - sortie.groundSpeed() * BOMB_FALL);
        }
        Enemy head = chainHead(sortie, shipY);
        if (head != null) {
            lowest = head;
        }
        Enemy walker = convoyThreat(sortie, shipY);
        if (walker != null) {
            targetX = walker.renderX(1);
        } else if (lowest != null) {
            targetX = lowest.renderX(1);
        } else {
            GroundObject ground = lowestGround(sortie, shipY);
            Pickup pickup = sortie.pickupCount() > 0 ? sortie.pickup(0) : null;
            if (pickup != null) {
                targetX = pickup.renderX();
                targetY = Math.max(40, pickup.renderY(1) - 10);
            } else if (ground != null) {
                targetX = ground.renderX();
            }
        }
        if (node != null && strikeDue(sortie, node)) {
            double strikeX = strikeX(sortie, node);
            if (Math.abs(strikeX - shipX) <= DEAD_ZONE) {
                return commands | Command.SPECIAL.bit();
            }
            return commands | steer(shipX, shipY, strikeX, targetY);
        }
        if ((lowest != null || walker != null) && behind == null && sortie.pickupCount() > 0) {
            Pickup pickup = sortie.pickup(0);
            if (Math.abs(pickup.renderX() - shipX) < 120 && pickup.renderY(1) < 260) {
                targetX = pickup.renderX();
                targetY = Math.max(40, pickup.renderY(1) - 10);
            }
        }
        GroundObject cache = sortie.airEscort() && behind == null ? secretTarget(sortie, shipY) : null;
        if (cache != null) {
            // Level 10's ferry hatch (2026-10-08: 50 HP): it lines up under it to shoot it open.
            targetX = cache.renderX();
        }
        if (sortie.airEscort()) {
            targetX = Math.clamp(targetX, SIDE_LANE, PlayField.WIDTH - SIDE_LANE);
        }
        return commands | steer(shipX, shipY, clearOfRail(sortie, targetX), targetY);
    }

    /**
     * Level 10: the rear-ambush unit (a Wraith) to line up over: the nearest sideways of those in
     * their flash or hold below the ship, else of those still cloaked below the bottom edge or
     * rising to their hold (their lane is known); null without one.
     */
    private static Enemy rearAmbusher(Sortie sortie, double shipY) {
        double shipX = sortie.ship().x();
        Enemy nearest = null;
        for (int i = 0; i < sortie.enemyCount(); i++) {
            Enemy enemy = sortie.enemy(i);
            Enemy.AmbushPhase phase = enemy.ambushPhase();
            boolean coming = phase == Enemy.AmbushPhase.GAP || phase == Enemy.AmbushPhase.RISE;
            if ((holding(enemy) || coming)
                    && enemy.renderY(1) < shipY
                    && (nearest == null
                            || (holding(enemy) && !holding(nearest))
                            || (holding(enemy) == holding(nearest)
                                    && Math.abs(enemy.renderX(1) - shipX) < Math.abs(nearest.renderX(1) - shipX)))) {
                nearest = enemy;
            }
        }
        return nearest;
    }

    /**
     * Level 10: the pair of units of a Wraith ambush of at least {@link #STRIKE_AMBUSH} to call the
     * Airstrike on, if one is fitted, charged and ready: two of its units {@link
     * #STRIKE_PAIR_MIN}–{@link #STRIKE_PAIR_MAX} px apart still cloaked below the bottom edge or
     * rising, the pair whose midpoint lies nearest the ship (each bomber then flies up one's lane;
     * the bombs burst low on the screen just as they decloak there), as {@code first × 1024 +
     * second} (their indices); -1 for none. It is called once both rise.
     */
    private static int ambushPair(Sortie sortie) {
        var special = sortie.special();
        if (!special.fitted() || !special.name().equals("Airstrike") || !special.ready() || special.charges() <= 0) {
            return -1;
        }
        int coming = 0;
        for (int i = 0; i < sortie.enemyCount(); i++) {
            if (coming(sortie.enemy(i))) {
                coming++;
            }
        }
        if (coming < STRIKE_AMBUSH) {
            return -1;
        }
        double shipX = sortie.ship().x();
        int best = -1;
        double bestDistance = Double.POSITIVE_INFINITY;
        for (int i = 0; i < sortie.enemyCount(); i++) {
            Enemy a = sortie.enemy(i);
            if (!coming(a)) {
                continue;
            }
            for (int j = 0; j < sortie.enemyCount(); j++) {
                Enemy b = sortie.enemy(j);
                double dx = b.renderX(1) - a.renderX(1);
                double distance = Math.abs((a.renderX(1) + b.renderX(1)) / 2 - shipX);
                if (coming(b) && dx >= STRIKE_PAIR_MIN && dx <= STRIKE_PAIR_MAX && distance < bestDistance) {
                    bestDistance = distance;
                    best = i * 1024 + j;
                }
            }
        }
        return best;
    }

    /** Level 10: a rear-ambush unit still cloaked below the bottom edge or rising to its hold point. */
    private static boolean coming(Enemy enemy) {
        return enemy.ambushPhase() == Enemy.AmbushPhase.GAP || enemy.ambushPhase() == Enemy.AmbushPhase.RISE;
    }

    /**
     * Level 05: the lowest unit of a destroy-targets group (a battery) on the screen above the ship,
     * which must die before it leaves; null without one.
     */
    private static Enemy battery(Sortie sortie, double shipY) {
        if (sortie.script().targets().isEmpty()) {
            return null;
        }
        Enemy lowest = null;
        for (int i = 0; i < sortie.enemyCount(); i++) {
            Enemy enemy = sortie.enemy(i);
            double y = enemy.renderY(1);
            if (enemy.groupIndex() >= 0
                    && y < PlayField.HEIGHT - 10
                    && y > shipY + 20
                    && (lowest == null || y < lowest.renderY(1))) {
                lowest = enemy;
            }
        }
        return lowest;
    }

    /**
     * Level 09: the lowest hardened unit of a destroy-targets group on the screen (a Hive Node: only
     * {@code anti-ground} deliveries crack it, so it flies over it); null without one.
     */
    private static Enemy hardenedTarget(Sortie sortie) {
        if (sortie.script().targets().isEmpty()) {
            return null;
        }
        Enemy lowest = null;
        for (int i = 0; i < sortie.enemyCount(); i++) {
            Enemy enemy = sortie.enemy(i);
            double y = enemy.renderY(1);
            if (enemy.groupIndex() >= 0
                    && enemy.spec().hardened()
                    && y < PlayField.HEIGHT - 20
                    && y > 20
                    && (lowest == null || y < lowest.renderY(1))) {
                lowest = enemy;
            }
        }
        return lowest;
    }

    /**
     * Level 09: whether to call the Airstrike on a hardened target about to get away (low on the
     * screen with HP left): only with an Airstrike fitted, charged and not already flying.
     */
    private static boolean strikeDue(Sortie sortie, Enemy node) {
        var special = sortie.special();
        return special.fitted()
                && special.name().equals("Airstrike")
                && special.ready()
                && special.charges() > 0
                && node.renderY(1) < STRIKE_LOW
                && node.hp() > STRIKE_HP;
    }

    /**
     * Where to call the strike: with another hardened target of a group on the screen
     * {@link #STRIKE_PAIR_MIN}–{@link #STRIKE_PAIR_MAX} px to its side, midway between the two (each
     * bomber then flies over one); otherwise {@link #STRIKE_OFFSET} to its side, toward the centre.
     */
    private static double strikeX(Sortie sortie, Enemy node) {
        double x = node.renderX(1);
        for (int i = 0; i < sortie.enemyCount(); i++) {
            Enemy other = sortie.enemy(i);
            double dx = Math.abs(other.renderX(1) - x);
            if (other != node
                    && other.groupIndex() >= 0
                    && other.spec().hardened()
                    && other.renderY(1) < PlayField.HEIGHT - 20
                    && dx >= STRIKE_PAIR_MIN
                    && dx <= STRIKE_PAIR_MAX) {
                return (x + other.renderX(1)) / 2;
            }
        }
        return x < PlayField.WIDTH / 2.0 ? x + STRIKE_OFFSET : x - STRIKE_OFFSET;
    }

    /** Level 06: the lowest head of a segment chain on the screen above the ship; null without one. */
    private static Enemy chainHead(Sortie sortie, double shipY) {
        Enemy lowest = null;
        for (int c = 0; c < sortie.chainCount(); c++) {
            Enemy head = sortie.chain(c).member(0);
            if (head == null) {
                continue;
            }
            double x = head.renderX(1);
            double y = head.renderY(1);
            if (x > 0
                    && x < PlayField.WIDTH
                    && y < PlayField.HEIGHT - 10
                    && y > shipY + 60
                    && (lowest == null || y < lowest.renderY(1))) {
                lowest = head;
            }
        }
        return lowest;
    }

    /**
     * Level 06: with a member of a segment chain close above, beside or below, the side whose
     * position 50 px away lies farther from every member (negative = left); 0 when none is close.
     */
    private static double chainThreat(Sortie sortie, double shipX, double shipY) {
        boolean close = false;
        for (int c = 0; c < sortie.chainCount() && !close; c++) {
            var chain = sortie.chain(c);
            for (int k = 0; k < chain.size(); k++) {
                Enemy member = chain.member(k);
                if (member != null
                        && Math.abs(member.renderX(1) - shipX) < CHAIN_DX
                        && member.renderY(1) - shipY > -CHAIN_BELOW
                        && member.renderY(1) - shipY < CHAIN_ABOVE) {
                    close = true;
                    break;
                }
            }
        }
        if (!close) {
            return 0;
        }
        if (shipX < 60) {
            return 1;
        }
        if (shipX > PlayField.WIDTH - 60) {
            return -1;
        }
        return chainClearance(sortie, shipX - 50, shipY) > chainClearance(sortie, shipX + 50, shipY) ? -1 : 1;
    }

    /** The distance from (x, y) to the nearest chain member, the vertical counted at 0.7. */
    private static double chainClearance(Sortie sortie, double x, double y) {
        double nearest = Double.POSITIVE_INFINITY;
        for (int c = 0; c < sortie.chainCount(); c++) {
            var chain = sortie.chain(c);
            for (int k = 0; k < chain.size(); k++) {
                Enemy member = chain.member(k);
                if (member != null) {
                    nearest = Math.min(nearest, Math.hypot(member.renderX(1) - x, 0.7 * (member.renderY(1) - y)));
                }
            }
        }
        return nearest;
    }

    /** Level 05: while the rail is lit (lights or a sled), a target beside the rail rather than on it. */
    private static double clearOfRail(Sortie sortie, double targetX) {
        if (sortie.sled().isEmpty() || !sortie.sled().get().lit()) {
            return targetX;
        }
        double rail = sortie.sled().get().spec().x();
        return Math.abs(targetX - rail) < RAIL_CLEARANCE ? rail - RAIL_CLEARANCE : targetX;
    }

    /**
     * Its cruise height: low, or with a convoy alive among the column, high enough that the air
     * enemies above still give it time and close enough that the ground enemies' aimed shots go for
     * it rather than the crawlers; with an air escort low, below the shuttle band.
     */
    private static double cruise(Sortie sortie) {
        if (sortie.airEscort()) {
            return CRUISE_Y;
        }
        for (int k = 0; k < sortie.allyCount(); k++) {
            if (sortie.ally(k).alive()) {
                return CONVOY_CRUISE;
            }
        }
        return CRUISE_Y;
    }

    /**
     * With a convoy, the ground enemy on the screen above the ship that is closest to a convoy unit
     * (a turret or a walker about to fire at it or claw it); null without one.
     */
    private static Enemy convoyThreat(Sortie sortie, double shipY) {
        if (sortie.allyCount() == 0) {
            return null;
        }
        Enemy best = null;
        double bestDistance = Double.POSITIVE_INFINITY;
        for (int i = 0; i < sortie.enemyCount(); i++) {
            Enemy enemy = sortie.enemy(i);
            double x = enemy.renderX(1);
            double y = enemy.renderY(1);
            if (enemy.layer() != Layer.GROUND
                    || y > PlayField.HEIGHT - 10
                    || y < shipY - 20
                    || x < 0
                    || x > PlayField.WIDTH) {
                continue;
            }
            for (int k = 0; k < sortie.allyCount(); k++) {
                if (sortie.ally(k).alive()) {
                    double distance = Math.hypot(
                            sortie.ally(k).renderX(1) - x, sortie.ally(k).renderY(1) - y);
                    if (distance < bestDistance) {
                        bestDistance = distance;
                        best = enemy;
                    }
                }
            }
        }
        return best;
    }

    private static int steer(double shipX, double shipY, double targetX, double targetY) {
        int commands = 0;
        if (targetX < shipX - DEAD_ZONE) {
            commands |= Command.LEFT.bit();
        } else if (targetX > shipX + DEAD_ZONE) {
            commands |= Command.RIGHT.bit();
        }
        if (targetY < shipY - DEAD_ZONE) {
            commands |= Command.DOWN.bit();
        } else if (targetY > shipY + DEAD_ZONE) {
            commands |= Command.UP.bit();
        }
        return commands;
    }

    /** M5 part E: its height in an arena with slam lanes, px (low: the fans have room to spread). */
    private static final double ARENA_Y = 70;
    /** M5 part E: the planner counts a lane as struck from this long before its impact, s (a margin). */
    private static final double LANE_WARNING = 0.15;
    /** M5 part E: it keeps this far beside a telegraphed lane, px. */
    private static final double LANE_GAP = 6;

    /** M5 part E: the present arena boss with slam lanes (engaged or arriving); null without one. */
    private static SetPiece arenaBoss(Sortie sortie) {
        for (int i = 0; i < sortie.setPieceCount(); i++) {
            SetPiece piece = sortie.setPiece(i);
            if (piece.present()
                    && piece.arena().isPresent()
                    && piece.arena().get().laneCount() > 0) {
                return piece;
            }
        }
        return null;
    }

    /**
     * M5 part E: the move in an arena with slam lanes: the boss planner toward what is exposed, kept
     * beside every telegraphed lane (out of the one it is in first), a move that is still in a lane
     * when it strikes counting as a hit.
     */
    private static int arenaMove(Sortie sortie, SetPiece boss, ArenaPlay play) {
        var arena = boss.arena().orElseThrow();
        double shipX = sortie.ship().x();
        double half = HULL_X + LANE_GAP;
        double wanted = play == ArenaPlay.ARMS ? armsFirstX(sortie, boss, shipX) : exposedX(boss, shipX);
        double targetX = clearOfLanes(arena, wanted, half, shipX);
        if (laneUnder(arena, arena.telegraphed(), shipX, half) > 0) {
            // Out of the lane first, toward the nearer side clear of it (the target's x is beside it).
            targetX = clearOfLanes(arena, shipX, half, shipX);
        }
        return bossMove(sortie, targetX, ARENA_Y, arena);
    }

    /**
     * What to line up under: an arm out of the water (rising or awash) in a lane not about to be
     * slammed, the nearest; else the surfaced head's nearer eye; else the head (waiting for it).
     */
    private static double exposedX(SetPiece boss, double shipX) {
        var arena = boss.arena().orElseThrow();
        double best = Double.NaN;
        for (int a = 0; a < arena.armCount(); a++) {
            int lane = arena.armLane(a);
            if (lane == 0 || boss.partWrecked(arena.armPart(a)) || boss.partLayer(arena.armPart(a)) != Layer.GROUND) {
                continue;
            }
            double x = arena.laneWidth() * (lane - 0.5);
            if ((arena.telegraphed() & (1 << lane)) == 0
                    && (Double.isNaN(best) || Math.abs(x - shipX) < Math.abs(best - shipX))) {
                best = x;
            }
        }
        if (!Double.isNaN(best)) {
            return best;
        }
        int head = arena.surfacePart() >= 0 ? arena.surfacePart() : vitalPart(boss);
        if (head < 0) {
            return shipX;
        }
        double x = boss.partX(head);
        // The eyes sit either side of the head's centre (the data's weak spots).
        double eye = EYE_DX;
        return Math.abs(shipX - (x - eye)) <= Math.abs(shipX - (x + eye)) ? x - eye : x + eye;
    }

    /**
     * M5 part E, {@link ArenaPlay#ARMS}: what to line up under. The arm to cut is the living one
     * guarding the most ships afloat (the fewer HP on a tie). While it must be cut: an arm lying awash
     * past its impact, that one first; else (between single slams) beside the lane an arm is about to
     * strike (the clamp beside the telegraphed lanes puts it there), that one first; else, while the
     * head's window is open, its eye clear of the telegraphed lanes; else the bait: where the next slam
     * costs least. Once a safe place exists it only baits and plays the head (its eyes also while it
     * rises); with a cut out of reach, as {@link ArenaPlay#EYES}.
     */
    private static double armsFirstX(Sortie sortie, SetPiece boss, double shipX) {
        var arena = boss.arena().orElseThrow();
        int cut = armToCut(sortie, boss);
        if (cut < 0) {
            return exposedX(boss, shipX);
        }
        boolean volleys = volleys(boss);
        // Once some place keeps every slam off the ships (the other arm severed and a free lane), the
        // arm need not be cut: it baits there and shoots the head.
        boolean guard = !safePlace(sortie, boss, volleys);
        if (guard && !cuttable(sortie, boss, cut)) {
            // The ships in its half are lost anyway: as the first pilot (an arm when exposed, else the head).
            return exposedX(boss, shipX);
        }
        double x = guard ? awashX(boss, cut) : Double.NaN;
        for (int a = 0; guard && a < arena.armCount() && Double.isNaN(x); a++) {
            x = awashX(boss, a);
        }
        if (!Double.isNaN(x)) {
            return x;
        }
        if (guard && !volleys) {
            x = strikingX(boss, cut);
            for (int a = 0; a < arena.armCount() && Double.isNaN(x); a++) {
                x = strikingX(boss, a);
            }
            if (!Double.isNaN(x)) {
                return x;
            }
        }
        var head = arena.surfaceState();
        if ((head == vanguard.sim.SlamArena.SurfaceState.UP
                        || (head == vanguard.sim.SlamArena.SurfaceState.RISING && !guard))
                && (!volleys || arena.telegraphed() != 0)) {
            return clearEyeX(boss, shipX);
        }
        return baitX(sortie, boss, guard ? cut : -1, shipX);
    }

    /** Whether some place keeps the next slam (or volley) off every convoy ship afloat. */
    private static boolean safePlace(Sortie sortie, SetPiece boss, boolean volley) {
        var arena = boss.arena().orElseThrow();
        for (int lane = 1; lane <= arena.laneCount(); lane++) {
            for (int place = 0; place < 3; place++) {
                double x = (lane - 0.5) * arena.laneWidth() + (place - 1) * (arena.laneWidth() / 2 - HULL_X - LANE_GAP);
                double cost = volley ? volleyCost(sortie, boss, -1, x) : singleCost(sortie, boss, -1, x);
                if (cost <= 0) {
                    return true;
                }
            }
        }
        return false;
    }

    /** The damage it reckons to deal an arm lying awash once, for {@link #cuttable}. */
    private static final double CUT_PER_SLAM = 70;
    /** ... and the slams beyond the ships' spare ones it allows for (the free lanes' and the last one's). */
    private static final double CUT_SLACK = 2;

    /**
     * Whether arm {@code cut} can be cut before its half's ships afloat have taken their last slam but
     * one: the slams its HP take at {@link #CUT_PER_SLAM} against the slams they can spare (each slam
     * in its half, all of them ships' lanes, is one the arm lies awash in). Otherwise the ships are lost
     * anyway and it plays the head.
     */
    private static boolean cuttable(Sortie sortie, SetPiece boss, int cut) {
        var arena = boss.arena().orElseThrow();
        double spare = 0;
        for (int k = 0; k < sortie.allyCount(); k++) {
            int lane = sortie.allyLane(k);
            if (lane > 0 && (arena.armLanes(cut) & (1 << lane)) != 0 && slammable(sortie, k)) {
                spare += Math.max(0, slamsLeft(sortie, k) - 1);
            }
        }
        return Math.ceil(boss.partHp(arena.armPart(cut)) / CUT_PER_SLAM) <= spare + CUT_SLACK;
    }

    /** The surfaced head's eye whose line is clear of the telegraphed lanes, the nearer if both are (or neither is). */
    private static double clearEyeX(SetPiece boss, double shipX) {
        var arena = boss.arena().orElseThrow();
        double left = eye(boss, -1);
        double right = eye(boss, 1);
        if (Double.isNaN(left) || Double.isNaN(right)) {
            return exposedX(boss, shipX);
        }
        double half = HULL_X + LANE_GAP;
        boolean leftClear = laneUnder(arena, arena.telegraphed(), left, half) == 0;
        boolean rightClear = laneUnder(arena, arena.telegraphed(), right, half) == 0;
        if (leftClear != rightClear) {
            return leftClear ? left : right;
        }
        return Math.abs(shipX - left) <= Math.abs(shipX - right) ? left : right;
    }

    /** Whether the boss's phase now slams in volleys (the head stays up between them). */
    private static boolean volleys(SetPiece boss) {
        var phases = boss.boss().orElseThrow().phases();
        return boss.phase() < phases.size()
                && phases.get(boss.phase()).arena().slamming() == vanguard.sim.BossSpec.Slamming.VOLLEY;
    }

    /** The x of arm {@code a}'s lane while it lies out of the water past its impact; NaN otherwise. */
    private static double awashX(SetPiece boss, int a) {
        var arena = boss.arena().orElseThrow();
        int lane = arena.armLane(a);
        if (lane == 0
                || boss.partWrecked(arena.armPart(a))
                || boss.partLayer(arena.armPart(a)) != Layer.GROUND
                || (arena.telegraphed() & (1 << lane)) != 0) {
            return Double.NaN;
        }
        return arena.laneWidth() * (lane - 0.5);
    }

    /** The x of the lane arm {@code a} is about to strike (telegraphed or rising); NaN otherwise. */
    private static double strikingX(SetPiece boss, int a) {
        var arena = boss.arena().orElseThrow();
        var state = arena.armState(a);
        if (boss.partWrecked(arena.armPart(a))
                || (state != vanguard.sim.SlamArena.ArmState.TELEGRAPH
                        && state != vanguard.sim.SlamArena.ArmState.RISE)) {
            return Double.NaN;
        }
        return arena.laneWidth() * (arena.armFirstLane(a) - 0.5);
    }

    /**
     * The living slam arm guarding the most convoy ships afloat (the fewer HP on a tie); -1 when both
     * are severed or no living arm's half holds a ship afloat.
     */
    private static int armToCut(Sortie sortie, SetPiece boss) {
        var arena = boss.arena().orElseThrow();
        int best = -1;
        int bestShips = -1;
        for (int a = 0; a < arena.armCount(); a++) {
            int part = arena.armPart(a);
            if (boss.partWrecked(part)) {
                continue;
            }
            int ships = Integer.bitCount(shipLanes(sortie) & arena.armLanes(a));
            if (ships > bestShips || (ships == bestShips && boss.partHp(part) < boss.partHp(arena.armPart(best)))) {
                best = a;
                bestShips = ships;
            }
        }
        // With no ship afloat in a living arm's half there is nothing left to guard: the head first.
        return bestShips > 0 ? best : -1;
    }

    /** The lanes holding a convoy ship a slam can still sink, bit {@code n} for lane {@code n}. */
    private static int shipLanes(Sortie sortie) {
        int mask = 0;
        for (int k = 0; k < sortie.allyCount(); k++) {
            if (slammable(sortie, k) && sortie.allyLane(k) > 0) {
                mask |= 1 << sortie.allyLane(k);
            }
        }
        return mask;
    }

    private static boolean slammable(Sortie sortie, int k) {
        return sortie.ally(k).alive() && sortie.allySpec(k).slams() > 0;
    }

    /** The slams convoy ship {@code k} still takes before it sinks (0 when it cannot be slammed). */
    private static double slamsLeft(Sortie sortie, int k) {
        if (!slammable(sortie, k)) {
            return 0;
        }
        return (sortie.allySpec(k).hp() - sortie.ally(k).hitsTaken())
                / sortie.allySpec(k).slams();
    }

    /**
     * Where the next slam (or volley) costs least while arm {@code cut} lives. It weighs a few places
     * (each lane's centre and edges, the surfaced head's eyes): the lanes the boss's rules would slam
     * from there (design/enemies/bosses/harbour-kraken: a single slam takes the ship's lane or the
     * convoy ship's nearest it in turn, a choice in a severed half going to the other; a volley takes
     * both, an arm with no target its half's lane nearest the ship), each lane by what its ship afloat
     * would lose (a ship on its last slam far more), less a little for each slam of the arm to cut
     * (cut there when it lies awash), plus a little for the way there.
     */
    private static double baitX(Sortie sortie, SetPiece boss, int cut, double shipX) {
        var arena = boss.arena().orElseThrow();
        boolean volley = volleys(boss);
        double best = shipX;
        double bestScore = Double.POSITIVE_INFINITY;
        int lanes = arena.laneCount();
        for (int c = 0; c < 3 * lanes + 2; c++) {
            double x;
            if (c < 3 * lanes) {
                int lane = c / 3 + 1;
                int place = c % 3;
                x = (lane - 0.5) * arena.laneWidth() + (place - 1) * (arena.laneWidth() / 2 - HULL_X - LANE_GAP);
            } else {
                x = eye(boss, c == 3 * lanes ? -1 : 1);
                if (Double.isNaN(x)) {
                    continue;
                }
            }
            double score = volley ? volleyCost(sortie, boss, cut, x) : singleCost(sortie, boss, cut, x);
            score += Math.abs(x - shipX) / BAIT_TRAVEL;
            if (c >= 3 * lanes && arena.surfaceState() == vanguard.sim.SlamArena.SurfaceState.UP) {
                score -= BAIT_EYE;
            }
            if (score < bestScore) {
                bestScore = score;
                best = x;
            }
        }
        return best;
    }

    /** It weighs a place's way there at one point per this many px. */
    private static final double BAIT_TRAVEL = 120;
    /** An open eye to shoot while it waits is worth this many points. */
    private static final double BAIT_EYE = 2.5;
    /** A slam of the arm to cut is worth this many points (it lies awash, ready to be cut). */
    private static final double BAIT_CUT = 3;

    /** The x of the surfaced head's eye on {@code side} (-1 left, 1 right) while it is rising or up; NaN otherwise. */
    private static double eye(SetPiece boss, int side) {
        var arena = boss.arena().orElseThrow();
        var state = arena.surfaceState();
        if (arena.surfacePart() < 0
                || (state != vanguard.sim.SlamArena.SurfaceState.UP
                        && state != vanguard.sim.SlamArena.SurfaceState.RISING)) {
            return Double.NaN;
        }
        return boss.partX(arena.surfacePart()) + side * EYE_DX;
    }

    /** A single slam from x: the worse of its two choices (the alternate rule's turn is not known). */
    private static double singleCost(Sortie sortie, SetPiece boss, int cut, double x) {
        var arena = boss.arena().orElseThrow();
        int player = arena.laneOf(x);
        int ship = nearestShipLane(sortie, arena, x);
        double worst = Double.NEGATIVE_INFINITY;
        for (int turn = 0; turn < 2; turn++) {
            int first = turn == 0 || ship == 0 ? player : ship;
            int other = turn == 0 ? ship : player;
            int lane = first;
            int arm = livingOwner(boss, first);
            if (arm < 0 && other > 0 && livingOwner(boss, other) >= 0) {
                lane = other;
                arm = livingOwner(boss, other);
            }
            if (arm < 0) {
                arm = firstLivingArm(boss);
                lane = arm < 0 ? 0 : nearestOwnedLane(arena, arm, x);
            }
            double cost = slamCost(sortie, lane) - (arm == cut ? BAIT_CUT : 0);
            worst = Math.max(worst, cost);
        }
        return worst;
    }

    /** A volley from x: the ship's lane and the nearest convoy ship's, each by its living owner; an arm without one, its lane nearest x. */
    private static double volleyCost(Sortie sortie, SetPiece boss, int cut, double x) {
        var arena = boss.arena().orElseThrow();
        int player = arena.laneOf(x);
        int ship = nearestShipLane(sortie, arena, x);
        double cost = 0;
        for (int a = 0; a < arena.armCount(); a++) {
            if (boss.partWrecked(arena.armPart(a))) {
                continue;
            }
            int mine = arena.armLanes(a);
            int lane = (mine & (1 << player)) != 0 ? player : ship > 0 && (mine & (1 << ship)) != 0 ? ship : 0;
            if (lane == 0) {
                lane = nearestOwnedLane(arena, a, x);
            }
            cost += slamCost(sortie, lane) - (a == cut ? BAIT_CUT : 0);
        }
        return cost;
    }

    /** What a slam in {@code lane} costs: what its ship afloat loses, a ship on its last slam far more; 0 without one. */
    private static double slamCost(Sortie sortie, int lane) {
        double cost = 0;
        for (int k = 0; k < sortie.allyCount(); k++) {
            if (lane > 0 && sortie.allyLane(k) == lane && slammable(sortie, k)) {
                cost = Math.max(cost, slamsLeft(sortie, k) <= 1 ? 100 : 10 - slamsLeft(sortie, k));
            }
        }
        return cost;
    }

    /** The lane of the convoy ship afloat nearest x (the lower on a tie); 0 for none. */
    private static int nearestShipLane(Sortie sortie, vanguard.sim.SlamArena arena, double x) {
        int ships = shipLanes(sortie);
        int best = 0;
        double bestDistance = Double.POSITIVE_INFINITY;
        for (int lane = 1; lane <= arena.laneCount(); lane++) {
            double distance = Math.abs((lane - 0.5) * arena.laneWidth() - x);
            if ((ships & (1 << lane)) != 0 && distance < bestDistance) {
                bestDistance = distance;
                best = lane;
            }
        }
        return best;
    }

    /** Arm {@code a}'s lane whose centre is nearest x (the lower on a tie). */
    private static int nearestOwnedLane(vanguard.sim.SlamArena arena, int a, double x) {
        int best = 0;
        double bestDistance = Double.POSITIVE_INFINITY;
        for (int lane = 1; lane <= arena.laneCount(); lane++) {
            double distance = Math.abs((lane - 0.5) * arena.laneWidth() - x);
            if ((arena.armLanes(a) & (1 << lane)) != 0 && distance < bestDistance) {
                bestDistance = distance;
                best = lane;
            }
        }
        return best;
    }

    /** The living arm that owns {@code lane}; -1 for none. */
    private static int livingOwner(SetPiece boss, int lane) {
        var arena = boss.arena().orElseThrow();
        for (int a = 0; a < arena.armCount(); a++) {
            if ((arena.armLanes(a) & (1 << lane)) != 0 && !boss.partWrecked(arena.armPart(a))) {
                return a;
            }
        }
        return -1;
    }

    private static int firstLivingArm(SetPiece boss) {
        var arena = boss.arena().orElseThrow();
        for (int a = 0; a < arena.armCount(); a++) {
            if (!boss.partWrecked(arena.armPart(a))) {
                return a;
            }
        }
        return -1;
    }

    /** M5 part E: the Kraken's eyes lie this far either side of its head's centre, px (its data's spots). */
    private static final double EYE_DX = 41.5;

    private static int vitalPart(SetPiece boss) {
        for (int p = 0; p < boss.partCount(); p++) {
            if (boss.spec().parts().get(p).vital()) {
                return p;
            }
        }
        return -1;
    }

    /** A telegraphed lane the hull (half width {@code half}) at x overlaps; 0 for none. */
    private static int laneUnder(vanguard.sim.SlamArena arena, int mask, double x, double half) {
        for (int lane = 1; lane <= arena.laneCount(); lane++) {
            double left = (lane - 1) * arena.laneWidth();
            if ((mask & (1 << lane)) != 0 && x + half > left && x - half < left + arena.laneWidth()) {
                return lane;
            }
        }
        return 0;
    }

    /**
     * An x beside every telegraphed lane: x itself when clear, else beside the run of telegraphed
     * lanes it lies in, on the side nearer {@code from} that is on the play field.
     */
    private static double clearOfLanes(vanguard.sim.SlamArena arena, double x, double half, double from) {
        int mask = arena.telegraphed();
        int lane = laneUnder(arena, mask, x, half);
        if (lane == 0) {
            return x;
        }
        int lo = lane;
        int hi = lane;
        while (lo > 1 && (mask & (1 << (lo - 1))) != 0) {
            lo--;
        }
        while (hi < arena.laneCount() && (mask & (1 << (hi + 1))) != 0) {
            hi++;
        }
        double left = (lo - 1) * arena.laneWidth() - half;
        double right = hi * arena.laneWidth() + half;
        boolean leftOk = left >= 30;
        boolean rightOk = right <= PlayField.WIDTH - 30;
        if (leftOk && (!rightOk || Math.abs(left - from) <= Math.abs(right - from))) {
            return left;
        }
        return rightOk ? right : x;
    }

    /** The moves the boss-fight planner weighs: stay, the four sides, the four diagonals. */
    private static final int[] MOVES = {
        0,
        Command.LEFT.bit(),
        Command.RIGHT.bit(),
        Command.UP.bit(),
        Command.DOWN.bit(),
        Command.LEFT.bit() | Command.UP.bit(),
        Command.RIGHT.bit() | Command.UP.bit(),
        Command.LEFT.bit() | Command.DOWN.bit(),
        Command.RIGHT.bit() | Command.DOWN.bit()
    };

    private static final double DIAGONAL = Math.sqrt(0.5);
    private static final double[] MOVE_X = {0, -1, 1, 0, 0, -DIAGONAL, DIAGONAL, -DIAGONAL, DIAGONAL};
    private static final double[] MOVE_Y = {0, 0, 0, 1, -1, DIAGONAL, DIAGONAL, -DIAGONAL, -DIAGONAL};
    /** How far ahead the planner looks, s, in samples of {@link #SAMPLE} s. */
    private static final int SAMPLES = 12;

    private static final double SAMPLE = 0.05;
    /** The ship's hull from its centre, px, with a small bullet's radius added. */
    private static final double HULL_X = 22;

    private static final double HULL_Y = 25;
    /** A move is safe when nothing comes closer to the hull than this in the look-ahead, px. */
    private static final double SAFE = 6;
    /** The band of the field it prefers while it dodges a boss, px from the left. */
    private static final double BAND_LEFT = 130;

    private static final double BAND_RIGHT = PlayField.WIDTH - 130.0;

    /**
     * In a boss fight: the move toward the boss's vital part ({@code targetX}, {@code targetY}) if
     * no bullet or air enemy comes too close to the hull while it flies it for the next 0.6 s (each
     * flying straight on, the ship at full speed); otherwise the safe move that keeps nearest the
     * target and the field's centre band (it keeps away from the edges, where an aimed burst traps
     * it); with no safe move, the one that keeps farthest from them.
     */
    private static int bossMove(Sortie sortie, double targetX, double targetY) {
        return bossMove(sortie, targetX, targetY, null);
    }

    /** As {@link #bossMove(Sortie, double, double)}; with {@code lanes} (M5 part E) a move into a lane about to be slammed is a hit. */
    private static int bossMove(Sortie sortie, double targetX, double targetY, vanguard.sim.SlamArena lanes) {
        double shipX = sortie.ship().x();
        double shipY = sortie.ship().y();
        int wanted = steer(shipX, shipY, targetX, targetY);
        int best = -1;
        double bestScore = Double.NEGATIVE_INFINITY;
        int clearest = 0;
        double clearestDistance = Double.NEGATIVE_INFINITY;
        for (int m = 0; m < MOVES.length; m++) {
            double clearance = clearance(sortie, MOVE_X[m], MOVE_Y[m], lanes);
            if (clearance > clearestDistance) {
                clearestDistance = clearance;
                clearest = m;
            }
            if (clearance < SAFE) {
                continue;
            }
            if (MOVES[m] == wanted) {
                return wanted;
            }
            double x = ahead(shipX, MOVE_X[m], sortie, true);
            double y = ahead(shipY, MOVE_Y[m], sortie, false);
            double score = -Math.abs(x - targetX)
                    - 0.5 * Math.abs(y - targetY)
                    - 2 * Math.max(0, Math.max(BAND_LEFT - x, x - BAND_RIGHT));
            if (score > bestScore) {
                bestScore = score;
                best = m;
            }
        }
        return MOVES[best >= 0 ? best : clearest];
    }

    /** Where a move takes the ship in two samples, inside its bounds. */
    private static double ahead(double from, double direction, Sortie sortie, boolean horizontal) {
        double margin = sortie.ship().spec().edgeLimit();
        double to = from + direction * sortie.ship().spec().speed() * 2 * SAMPLE;
        return Math.clamp(to, margin, (horizontal ? PlayField.WIDTH : PlayField.HEIGHT) - margin);
    }

    /**
     * The closest any enemy bullet or air enemy comes to the hull's box over the look-ahead while
     * the ship flies (dx, dy) at full speed, px (0 = a hit).
     */
    private static double clearance(Sortie sortie, double dx, double dy, vanguard.sim.SlamArena lanes) {
        double speed = sortie.ship().spec().speed();
        double margin = sortie.ship().spec().edgeLimit();
        double x0 = sortie.ship().x();
        double y0 = sortie.ship().y();
        double nearest = Double.POSITIVE_INFINITY;
        for (int k = 1; k <= SAMPLES; k++) {
            double t = k * SAMPLE;
            double x = Math.clamp(x0 + dx * speed * t, margin, PlayField.WIDTH - margin);
            double y = Math.clamp(y0 + dy * speed * t, margin, PlayField.HEIGHT - margin);
            double steps = t / SimStep.SECONDS;
            if (lanes != null && y - HULL_Y < lanes.laneTop()) {
                for (int lane = 1; lane <= lanes.laneCount(); lane++) {
                    double left = (lane - 1) * lanes.laneWidth();
                    if (lanes.untilImpact(lane) <= t + LANE_WARNING
                            && x + HULL_X + LANE_GAP > left
                            && x - HULL_X - LANE_GAP < left + lanes.laneWidth()) {
                        return 0;
                    }
                }
            }
            for (int i = 0; i < sortie.bulletCount(); i++) {
                EnemyBullet bullet = sortie.bullet(i);
                double bx = bullet.renderX(1) + (bullet.renderX(1) - bullet.renderX(0)) * steps;
                double by = bullet.renderY(1) + (bullet.renderY(1) - bullet.renderY(0)) * steps;
                nearest = Math.min(nearest, gap(bx - x, by - y, HULL_X, HULL_Y));
            }
            for (int i = 0; i < sortie.enemyCount(); i++) {
                Enemy enemy = sortie.enemy(i);
                if (enemy.layer() == Layer.GROUND) {
                    continue;
                }
                double ex = enemy.renderX(1) + (enemy.renderX(1) - enemy.renderX(0)) * steps;
                double ey = enemy.renderY(1) + (enemy.renderY(1) - enemy.renderY(0)) * steps;
                nearest = Math.min(
                        nearest,
                        gap(
                                ex - x,
                                ey - y,
                                HULL_X + enemy.hitbox().width() / 2,
                                HULL_Y + enemy.hitbox().height() / 2));
            }
        }
        return nearest;
    }

    /** The distance between a point (dx, dy) from a box's centre and the box (halfW, halfH); 0 inside. */
    private static double gap(double dx, double dy, double halfW, double halfH) {
        return Math.hypot(Math.max(0, Math.abs(dx) - halfW), Math.max(0, Math.abs(dy) - halfH));
    }

    /** A set piece on the player's layer, or null. */
    private static SetPiece descended(Sortie sortie) {
        for (int i = 0; i < sortie.setPieceCount(); i++) {
            SetPiece piece = sortie.setPiece(i);
            if (piece.present() && piece.onPlane()) {
                return piece;
            }
        }
        return null;
    }

    /**
     * Where to line up under a set piece: its vital part, while it lives, else its centre; under a
     * boss, its lowest part that takes damage now (a head, then the core).
     */
    private static double vitalX(SetPiece piece) {
        if (piece.boss().isPresent()) {
            int lowest = -1;
            for (int p = 0; p < piece.partCount(); p++) {
                if (!piece.partWrecked(p)
                        && !piece.partShielded(p)
                        && (lowest < 0 || piece.partY(p) < piece.partY(lowest))) {
                    lowest = p;
                }
            }
            return lowest < 0 ? piece.renderX(1) : piece.partX(lowest);
        }
        for (int p = 0; p < piece.partCount(); p++) {
            if (piece.spec().parts().get(p).vital() && !piece.partWrecked(p)) {
                return piece.partX(p);
            }
        }
        return piece.renderX(1);
    }

    private static double bodyBottom(SetPiece piece) {
        return piece.renderY(1) - piece.spec().body().height() / 2;
    }

    /**
     * Level 05: the side to slip to out of a mortar's marker, sideways only and toward the battery
     * it is shooting at if there is one (it keeps its line); 0 when no marker lies under it.
     */
    private static double lobThreat(Sortie sortie, double shipX, double shipY) {
        for (int i = 0; i < sortie.lobCount(); i++) {
            Lob lob = sortie.lob(i);
            double dx = lob.targetX() - shipX;
            double dy = lob.targetY() - shipY;
            double reach = lob.impactRadius() + 30;
            if (dx * dx + dy * dy < reach * reach) {
                Enemy battery = battery(sortie, shipY);
                if (battery != null && Math.abs(battery.renderX(1) - lob.targetX()) > reach) {
                    return battery.renderX(1) < shipX ? -1 : 1;
                }
                return away(dx, shipX);
            }
        }
        return 0;
    }

    /**
     * The side to dodge a lit rail, a rising spore or large debris to: negative = left, positive =
     * right, 0 = nothing close.
     */
    private static double hazard(Sortie sortie, double shipX, double shipY) {
        if (sortie.sled().isPresent() && sortie.sled().get().lit()) {
            double dx = sortie.sled().get().spec().x() - shipX;
            if (Math.abs(dx) < RAIL_CLEARANCE) {
                return dx >= 0 ? -1 : 1;
            }
        }
        for (int i = 0; i < sortie.mineCount(); i++) {
            Mine mine = sortie.mine(i);
            double dx = mine.renderX(1) - shipX;
            double dy = mine.renderY(1) - shipY;
            if (mine.rising() > 0.5 && Math.abs(dx) < 26 && dy > -16 && dy < DANGER) {
                return away(dx, shipX);
            }
        }
        for (int i = 0; i < sortie.debrisCount(); i++) {
            Debris chunk = sortie.debris(i);
            double dx = chunk.renderX(1) - shipX;
            double dy = chunk.renderY(1) - shipY;
            double halfW = chunk.spec().size().width() / 2;
            double halfH = chunk.spec().size().height() / 2;
            if ((chunk.large() || chunk.thrown())
                    && Math.abs(dx) < halfW + 30
                    && dy > -halfH - 20
                    && dy < halfH + DANGER) {
                return away(dx, shipX);
            }
        }
        return 0;
    }

    /** The side to dodge bullets and rammers to: negative = left, positive = right, 0 = nothing close. */
    private static double threat(Sortie sortie, double shipX, double shipY) {
        // Level 05: with a battery unit on the screen it holds its line and dodges only the closest bullets.
        boolean holding = battery(sortie, shipY) != null || hardenedTarget(sortie) != null;
        double bulletDx = holding ? 18 : 24;
        double bulletDy = holding ? 40 : DANGER;
        boolean fromBelow = sortie.airEscort();
        for (int i = 0; i < sortie.bulletCount(); i++) {
            EnemyBullet bullet = sortie.bullet(i);
            double dx = bullet.renderX(1) - shipX;
            double dy = bullet.renderY(1) - shipY;
            if (Math.abs(dx) < bulletDx && dy > -10 && dy < bulletDy) {
                return away(dx, shipX);
            }
            // Level 10: a Wraith's burst comes up the screen from below.
            if (fromBelow
                    && Math.abs(dx) < bulletDx
                    && dy <= -10
                    && dy > -DANGER
                    && bullet.renderY(1) > bullet.renderY(0)) {
                return away(dx, shipX);
            }
        }
        // Level 05: the batteries' units lie on the ground and cannot ram, so it flies over them.
        boolean overGround = !sortie.script().targets().isEmpty();
        for (int i = 0; i < sortie.enemyCount(); i++) {
            Enemy enemy = sortie.enemy(i);
            if ((overGround && enemy.layer() == Layer.GROUND) || enemy.cloaked() || holding(enemy)) {
                continue;
            }
            double dx = enemy.renderX(1) - shipX;
            double dy = enemy.renderY(1) - shipY;
            // Level 10: a wide body (a Wraith on its way out) is dodged by its width.
            double reach = fromBelow ? Math.max(30, enemy.hitbox().width() / 2 + HULL_X + 4) : 30;
            if (Math.abs(dx) < reach && Math.abs(dy) < DANGER) {
                if (enemy.ambushPhase() == Enemy.AmbushPhase.EXIT) {
                    // it leaves for the side lane nearer its hold point: toward the middle, out of its way
                    return enemy.renderX(1) <= PlayField.WIDTH / 2.0 ? 1 : -1;
                }
                return away(dx, shipX);
            }
        }
        return 0;
    }

    /** Level 10: a rear-ambush unit at its hold point (flashing or holding): it stays put, so it is lined up over, not dodged. */
    private static boolean holding(Enemy enemy) {
        return enemy.ambushPhase() == Enemy.AmbushPhase.DECLOAK || enemy.ambushPhase() == Enemy.AmbushPhase.HOLD;
    }

    /** Level 10: whether a rammer or a rising bullet is close below the ship (then it dodges sideways only). */
    private static boolean fromBelow(Sortie sortie, double shipX, double shipY) {
        for (int i = 0; i < sortie.bulletCount(); i++) {
            EnemyBullet bullet = sortie.bullet(i);
            double dy = bullet.renderY(1) - shipY;
            if (Math.abs(bullet.renderX(1) - shipX) < 30 && dy < 0 && dy > -DANGER) {
                return true;
            }
        }
        for (int i = 0; i < sortie.enemyCount(); i++) {
            Enemy enemy = sortie.enemy(i);
            double dy = enemy.renderY(1) - shipY;
            if (!enemy.cloaked() && Math.abs(enemy.renderX(1) - shipX) < 40 && dy < 0 && dy > -DANGER - 30) {
                return true;
            }
        }
        return false;
    }

    private static double away(double dx, double shipX) {
        if (shipX < 60) {
            return 1;
        }
        if (shipX > PlayField.WIDTH - 60) {
            return -1;
        }
        return dx >= 0 ? -1 : 1;
    }

    /**
     * Level 10: a destructible on the ground that hides a secret (the ferry hatch), above the ship and
     * not yet past the screen's middle, where its crate can still drift down to the ship; null for none.
     */
    private static GroundObject secretTarget(Sortie sortie, double shipY) {
        for (int i = 0; i < sortie.groundObjectCount(); i++) {
            GroundObject object = sortie.groundObject(i);
            double y = object.renderY(1);
            if (!object.spent()
                    && object.spec().secretIndex() >= 0
                    && y > Math.max(shipY + 30, PlayField.HEIGHT / 2.0)
                    && y < PlayField.HEIGHT + object.spec().size().height()) {
                return object;
            }
        }
        return null;
    }

    private static GroundObject lowestGround(Sortie sortie, double shipY) {
        GroundObject lowest = null;
        for (int i = 0; i < sortie.groundObjectCount(); i++) {
            GroundObject object = sortie.groundObject(i);
            double y = object.renderY(1);
            if (!object.spent()
                    && y > shipY + 30
                    && y < PlayField.HEIGHT
                    && (lowest == null || y < lowest.renderY(1))) {
                lowest = object;
            }
        }
        return lowest;
    }
}
