package vanguard.content;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import vanguard.sim.SimEvents;
import vanguard.sim.Sortie;

/**
 * Part G: a Brood-Carrier-style boss ({@link BroodCarrierDataTest}'s minimal carrier) flown in a
 * real level ({@link BossLevelTest}'s, with the carrier in the frigate's place): its open windows
 * launch their units into the level, each announced and counted among the level's enemies.
 */
class CarrierLaunchTest {
    private static final String LEVEL_01 = "campaign/act-1-first-contact/level-01-break-at-dawn/data.yaml";
    private static final String CARRIER_PATH = "enemies/bosses/test-carrier/data.yaml";

    private static Content content() {
        List<DataFile> files = new ArrayList<>(DesignTree.dataFiles());
        String level01 = files.stream()
                .filter(file -> file.path().equals(LEVEL_01))
                .findFirst()
                .orElseThrow()
                .text();
        String level = BossLevelTest.bossLevel(level01)
                .replace("boss: {enemy: gorgon-frigate,", "boss: {enemy: test-carrier,")
                .replace("phase: Last head", "phase: Broadside");
        files.removeIf(file -> file.path().equals(BossLevelTest.PATH));
        files.add(new DataFile(BossLevelTest.PATH, level));
        files.add(new DataFile(CARRIER_PATH, BroodCarrierDataTest.CARRIER));
        return ContentLoader.load(files);
    }

    @Test
    void theOpenWindowsLaunchUnitsThatCountAmongTheEnemies() {
        Content content = content();
        Sortie sortie = BossLevelTest.sortie(content, 11, Difficulty.MEDIUM);
        assertEquals("test-carrier", sortie.setPiece(0).slug());
        int before = -1;
        int launched = 0;
        int launchedKinds = 0;
        while (sortie.levelSeconds() < 172) {
            if (!sortie.flying()) {
                sortie.retry(sortie.ship().defences().maxArmour());
                before = -1;
                launched = 0;
            }
            if (before < 0 && sortie.levelSeconds() >= 149) {
                before = sortie.enemyTotal();
            }
            sortie.step(Autopilot.commands(sortie));
            SimEvents events = sortie.events();
            for (int i = 0; i < events.size(); i++) {
                if (events.type(i) == SimEvents.Type.BOSS_LAUNCHED) {
                    launched++;
                    String slug = sortie.enemyKinds().get(events.value(i)).slug();
                    launchedKinds |= slug.equals("skitter") ? 1 : slug.equals("needler") ? 2 : 4;
                }
            }
        }
        assertTrue(launched > 0, "the windows of the overhead pass launch units");
        assertEquals(3, launchedKinds, "skitters and needlers, the kinds of the pass's spawns");
        assertEquals(before + launched, sortie.enemyTotal(), "each launched unit counts among the enemies");
    }
}
