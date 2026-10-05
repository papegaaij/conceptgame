package vanguard.game.audio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class VoiceLimitTest {
    private static final long SECOND = 1_000_000_000L;

    private final List<Sfx> stolen = new ArrayList<>();
    private long nextId;

    private boolean play(VoiceLimit limit, Sfx sfx, long now) {
        if (!limit.admit(sfx, now, false)) {
            return false;
        }
        limit.started(sfx, nextId++, now + SECOND);
        return true;
    }

    @Test
    void neverMoreThanThirtyTwoVoicesPlayAtOnce() {
        var limit = new VoiceLimit(VoiceLimit.MAX_VOICES, (sfx, id) -> stolen.add(sfx));
        int admitted = 0;
        for (int round = 0; round < 3; round++) {
            for (Sfx sfx : Sfx.values()) {
                if (play(limit, sfx, round)) {
                    admitted++;
                }
                assertTrue(limit.playing(round) <= VoiceLimit.MAX_VOICES);
            }
        }
        assertEquals(VoiceLimit.MAX_VOICES, limit.playing(2));
        assertEquals(admitted - VoiceLimit.MAX_VOICES, stolen.size(), "every voice past the limit was stolen");
    }

    @Test
    void aNewSoundStealsTheLowestPriorityVoice() {
        var limit = new VoiceLimit(3, (sfx, id) -> stolen.add(sfx));
        play(limit, Sfx.EXPLOSION_SMALL_A, 0);
        play(limit, Sfx.PULSE_SHOT, 1);
        play(limit, Sfx.ENEMY_SHOT_A, 2);

        assertTrue(play(limit, Sfx.SHIELD_HIT, 3));
        assertEquals(List.of(Sfx.PULSE_SHOT), stolen, "player fire ranks below enemy fire and explosions");
        assertEquals(0, limit.playing(Sfx.PULSE_SHOT, 3));
    }

    @Test
    void amongTheLowestPriorityTheOldestVoiceIsStolen() {
        var limit = new VoiceLimit(3, (sfx, id) -> stolen.add(sfx));
        play(limit, Sfx.LASER_SHOT, 0);
        play(limit, Sfx.PULSE_SHOT, 1);
        play(limit, Sfx.EXPLOSION_SMALL_A, 2);

        assertTrue(play(limit, Sfx.VULCAN_SHOT, 3), "the same priority steals");
        assertEquals(List.of(Sfx.LASER_SHOT), stolen);
        assertTrue(play(limit, Sfx.ENEMY_SHOT_A, 4));
        assertEquals(List.of(Sfx.LASER_SHOT, Sfx.PULSE_SHOT), stolen);
    }

    @Test
    void aSoundBelowEveryVoiceIsDropped() {
        var limit = new VoiceLimit(2, (sfx, id) -> stolen.add(sfx));
        play(limit, Sfx.EXPLOSION_SMALL_A, 0);
        play(limit, Sfx.SHIELD_HIT, 1);

        assertFalse(play(limit, Sfx.SALVAGE_SMALL, 2));
        assertFalse(play(limit, Sfx.PULSE_SHOT, 2));
        assertEquals(List.of(), stolen);
        assertEquals(2, limit.playing(2));
    }

    @Test
    void anEndedVoiceFreesItsPlaceWithoutAStealing() {
        var limit = new VoiceLimit(2, (sfx, id) -> stolen.add(sfx));
        play(limit, Sfx.EXPLOSION_SMALL_A, 0);
        play(limit, Sfx.EXPLOSION_SMALL_B, 0);

        assertFalse(play(limit, Sfx.PULSE_SHOT, SECOND - 1));
        assertTrue(play(limit, Sfx.PULSE_SHOT, SECOND));
        assertEquals(List.of(), stolen);
    }

    @Test
    void anEffectBeyondItsInstanceLimitStopsItsOwnOldestInstance() {
        var limit = new VoiceLimit(VoiceLimit.MAX_VOICES, (sfx, id) -> stolen.add(sfx));
        for (int i = 0; i < Sfx.PULSE_SHOT.instanceLimit(); i++) {
            play(limit, Sfx.PULSE_SHOT, i);
        }
        play(limit, Sfx.EXPLOSION_SMALL_A, 5);

        assertTrue(play(limit, Sfx.PULSE_SHOT, 6));
        assertEquals(List.of(Sfx.PULSE_SHOT), stolen);
        assertEquals(Sfx.PULSE_SHOT.instanceLimit(), limit.playing(Sfx.PULSE_SHOT, 6));
    }

    @Test
    void aLoopIsNeverStolenAndAlwaysGetsAVoice() {
        var limit = new VoiceLimit(2, (sfx, id) -> stolen.add(sfx));
        limit.admit(Sfx.AMBIENCE_ORBIT, 0, true);
        limit.started(Sfx.AMBIENCE_ORBIT, nextId++, VoiceLimit.LOOPING);
        play(limit, Sfx.SHIELD_HIT, 1);

        assertTrue(play(limit, Sfx.SHIELD_BREAK, 2), "the loop's voice is not the victim");
        assertEquals(List.of(Sfx.SHIELD_HIT), stolen);
        assertTrue(limit.admit(Sfx.AMBIENCE_LUNA, 3, true), "a loop takes a voice from a higher priority");
        assertEquals(List.of(Sfx.SHIELD_HIT, Sfx.SHIELD_BREAK), stolen);
    }

    @Test
    void aStoppedEffectGivesUpItsVoices() {
        var limit = new VoiceLimit(2, (sfx, id) -> stolen.add(sfx));
        play(limit, Sfx.EXPLOSION_SMALL_A, 0);
        play(limit, Sfx.EXPLOSION_SMALL_A, 0);
        limit.stopped(Sfx.EXPLOSION_SMALL_A);

        assertEquals(0, limit.playing(0));
        assertTrue(play(limit, Sfx.PULSE_SHOT, 1));
        assertEquals(List.of(), stolen);
    }

    @Test
    void thePrioritiesFollowTheMixingRules() {
        // design/audio/sfx: warnings and player damage > boss sounds > explosions > enemy fire >
        // player fire > pickups > ambience.
        assertEquals(Sfx.Priority.WARNING, Sfx.ARMOUR_HIT.priority());
        assertEquals(Sfx.Priority.WARNING, Sfx.LOW_ARMOUR.priority());
        assertEquals(Sfx.Priority.BOSS, Sfx.CARRIER_ROAR.priority());
        assertEquals(Sfx.Priority.EXPLOSION, Sfx.EXPLOSION_HUGE_A.priority());
        assertEquals(Sfx.Priority.ENEMY_FIRE, Sfx.ENEMY_SHOT_A.priority());
        assertEquals(Sfx.Priority.PLAYER_FIRE, Sfx.PULSE_SHOT.priority());
        assertEquals(Sfx.Priority.PICKUP, Sfx.SALVAGE_SMALL.priority());
        assertEquals(Sfx.Priority.AMBIENCE, Sfx.AMBIENCE_ORBIT.priority());
        assertTrue(Sfx.Priority.WARNING.compareTo(Sfx.Priority.BOSS) > 0);
        assertTrue(Sfx.Priority.BOSS.compareTo(Sfx.Priority.EXPLOSION) > 0);
        assertTrue(Sfx.Priority.EXPLOSION.compareTo(Sfx.Priority.ENEMY_FIRE) > 0);
        assertTrue(Sfx.Priority.ENEMY_FIRE.compareTo(Sfx.Priority.PLAYER_FIRE) > 0);
        assertTrue(Sfx.Priority.PLAYER_FIRE.compareTo(Sfx.Priority.PICKUP) > 0);
        assertTrue(Sfx.Priority.PICKUP.compareTo(Sfx.Priority.AMBIENCE) > 0);
    }
}
