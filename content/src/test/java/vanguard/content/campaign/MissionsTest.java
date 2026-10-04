package vanguard.content.campaign;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import vanguard.content.Difficulty;

class MissionsTest {
    private static final List<Missions.ActInfo> ACTS = List.of(
            new Missions.ActInfo("ACT I", "FIRST CONTACT", 1, 7), new Missions.ActInfo("ACT II", "HOMEFRONT", 8, 14));

    /** Levels 01–09 are built; the rest have no data file yet. */
    private static Optional<String> name(int number) {
        return number <= 9 ? Optional.of("LEVEL " + number) : Optional.empty();
    }

    /** A hard campaign that has flown Levels 01–03, with grades for them. */
    private static SaveGame flownThree() {
        SaveGame save = Campaign.start(CampaignTest.RULES, Difficulty.HARD).save(Instant.parse("2026-10-04T10:00:00Z"));
        return new SaveGame(
                save.version(),
                save.created(),
                save.playtime(),
                save.difficulty(),
                4,
                1234,
                50_000,
                save.loadout(),
                save.inventory(),
                save.unlocks(),
                save.specials(),
                22,
                Optional.of(1),
                Map.of(1, "B", 2, "A+", 3, "D"),
                save.dataCores(),
                save.storyFlags(),
                save.stats());
    }

    @Test
    void onlyFlownLevelsAreOpenAndOnlyTheyShowTheirNames() {
        List<Missions.Act> acts = Missions.of(ACTS, MissionsTest::name, flownThree());

        List<Missions.Mission> first = acts.getFirst().missions();
        assertEquals(7, first.size());
        assertEquals(new Missions.Mission(1, Optional.of("LEVEL 1"), Optional.of("B"), true), first.get(0));
        assertEquals(new Missions.Mission(3, Optional.of("LEVEL 3"), Optional.of("D"), true), first.get(2));
        assertEquals(
                new Missions.Mission(4, Optional.empty(), Optional.empty(), false),
                first.get(3),
                "the next level is not flown yet: locked, its name hidden");
        assertTrue(first.subList(3, 7).stream().noneMatch(Missions.Mission::open));
        assertTrue(
                first.subList(3, 7).stream().allMatch(mission -> mission.name().isEmpty()));
    }

    @Test
    void anActNotReachedHidesItsName() {
        List<Missions.Act> acts = Missions.of(ACTS, MissionsTest::name, flownThree());

        assertEquals(Optional.of("FIRST CONTACT"), acts.get(0).name());
        assertEquals("ACT II", acts.get(1).label());
        assertEquals(Optional.empty(), acts.get(1).name());
        assertTrue(acts.get(1).missions().stream().noneMatch(Missions.Mission::open));
    }

    @Test
    void anActIsNamedOnceTheCampaignReachesItAndAnUnbuiltLevelStaysLocked() {
        SaveGame save = flownThree();
        SaveGame atAct2 = save;
        // Flown up to Level 10, of which only 01–09 are built.
        SaveGame far = new SaveGame(
                atAct2.version(),
                atAct2.created(),
                atAct2.playtime(),
                atAct2.difficulty(),
                11,
                atAct2.credits(),
                atAct2.score(),
                atAct2.loadout(),
                atAct2.inventory(),
                atAct2.unlocks(),
                atAct2.specials(),
                atAct2.armour(),
                atAct2.retriesLeft(),
                atAct2.grades(),
                atAct2.dataCores(),
                atAct2.storyFlags(),
                atAct2.stats());

        List<Missions.Act> acts = Missions.of(ACTS, MissionsTest::name, far);

        assertEquals(Optional.of("HOMEFRONT"), acts.get(1).name());
        assertTrue(acts.get(1).missions().get(1).open(), "Level 09 is flown and built");
        assertFalse(acts.get(1).missions().get(2).open(), "Level 10 is flown but has no data file");
    }

    @Test
    void aNewCampaignHasNothingToReplay() {
        SaveGame fresh = Campaign.start(CampaignTest.RULES, Difficulty.EASY).save(Instant.EPOCH);

        List<Missions.Act> acts = Missions.of(ACTS, MissionsTest::name, fresh);

        assertTrue(acts.stream().flatMap(act -> act.missions().stream()).noneMatch(Missions.Mission::open));
        assertEquals(Optional.of("FIRST CONTACT"), acts.get(0).name(), "the campaign is in Act I");
    }

    @Test
    void onlyABetterGradeIsWrittenAndTheRestOfTheSaveStays() {
        SaveGame save = flownThree();
        List<String> order = CampaignTest.RULES.grades();

        assertSame(save, Missions.withGrade(save, 1, "C", order), "a worse grade changes nothing");
        assertSame(save, Missions.withGrade(save, 1, "B", order), "an equal grade changes nothing");
        SaveGame better = Missions.withGrade(save, 3, "A", order);

        assertEquals(Map.of(1, "B", 2, "A+", 3, "A"), better.grades());
        assertEquals(save.created(), better.created());
        assertEquals(save.nextLevel(), better.nextLevel());
        assertEquals(save.credits(), better.credits());
        assertEquals(save.score(), better.score());
        assertEquals(save.retriesLeft(), better.retriesLeft());
    }

    @Test
    void aReplayEarnsNothingAndKeepsTheProgressButRecordsABetterGrade(@TempDir Path directory) throws IOException {
        SaveGame save = flownThree();
        SaveSlots slots = new SaveSlots(directory);
        SaveSlots.Slot slot = new SaveSlots.Slot(2);
        Campaign replay = Campaign.replay(CampaignTest.RULES, slot, save, 3);

        assertEquals(Optional.of(new Campaign.Replay(slot, 3)), replay.replay());
        assertEquals(3, replay.nextLevel(), "the replay flies its own level");
        assertEquals(save.loadout(), replay.loadout(), "with the campaign's loadout");
        assertEquals(replay.maxArmour(), replay.armour(), "at full armour");
        assertEquals(Optional.empty(), replay.retriesLeft(), "retries are unlimited, even on hard");
        assertEquals(Campaign.Failure.MISSION_FAILED, replay.fail());
        assertEquals(Campaign.Failure.MISSION_FAILED, replay.fail());

        boolean best = replay.complete(CampaignTest.won("A", 80, 12_000), 30, 1, 0);

        assertTrue(best);
        assertEquals(Optional.of("A"), replay.grade(3));
        assertEquals(1234, replay.credits(), "no credits, no grade bonus");
        assertEquals(50_000, replay.score());
        assertEquals(3, replay.nextLevel(), "no progress");
        assertThrows(IllegalStateException.class, () -> replay.save(Instant.now()), "a replay is never saved");

        slots.write(slot, save);
        SaveGame kept = Missions.withGrade(save, 3, "A", CampaignTest.RULES.grades());
        slots.write(slot, kept);
        SaveGame reread = ((SaveSlots.Entry.Saved) slots.read(slot)).save();
        assertEquals(4, reread.nextLevel());
        assertEquals(1234, reread.credits());
        assertEquals(Optional.of(1), reread.retriesLeft());
        assertEquals("A", reread.grades().get(3));
    }

    @Test
    void aWorseReplayGradeIsNoNewBest() {
        Campaign replay = Campaign.replay(CampaignTest.RULES, SaveSlots.Slot.AUTOSAVE, flownThree(), 2);

        assertFalse(replay.complete(CampaignTest.won("B", 40, 9_000), 30));
        assertEquals(Optional.of("A+"), replay.grade(2));
    }

    @Test
    void onlyAFlownLevelCanBeReplayed() {
        SaveGame save = flownThree();

        assertThrows(
                IllegalArgumentException.class,
                () -> Campaign.replay(CampaignTest.RULES, SaveSlots.Slot.AUTOSAVE, save, 4));
        assertThrows(
                IllegalArgumentException.class,
                () -> Campaign.replay(CampaignTest.RULES, SaveSlots.Slot.AUTOSAVE, save, 0));
    }
}
