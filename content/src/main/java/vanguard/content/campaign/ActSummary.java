package vanguard.content.campaign;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import vanguard.content.Content;
import vanguard.content.LevelData;

/**
 * The act summary of an act's last debrief (design/ui/debrief): each level of the act with what it
 * brought (banked credits and kills, recorded since save format 2; an older save shows what is
 * recorded), the act's totals and the data cores found in the act with their lore titles.
 *
 * @param act the act's number label, {@code ACT I}
 * @param name the act's name, {@code FIRST CONTACT}
 * @param kills the recorded levels' kills
 * @param credits the recorded levels' banked credits
 */
public record ActSummary(
        String act, String name, List<Mission> missions, int kills, int credits, List<DataCore> dataCores) {
    public ActSummary {
        missions = List.copyOf(missions);
        dataCores = List.copyOf(dataCores);
    }

    /**
     * A level of the act.
     *
     * @param name its name as the mission select shows it, {@code BREAK AT DAWN}
     * @param grade its best grade
     * @param stats what it brought; empty when the save did not record it
     */
    public record Mission(int number, String name, Optional<String> grade, Optional<SaveGame.LevelStats> stats) {}

    /**
     * A data core found in the act.
     *
     * @param title its lore title, the secret's name in capitals: {@code SETTLEMENT LOG}
     * @param level the level it was found in
     */
    public record DataCore(String title, int level, String levelName) {}

    /** The summary of the act the campaign has just ended. */
    public static ActSummary of(Content content, Campaign campaign, CampaignRoute.ActEnd end) {
        List<Mission> missions = new ArrayList<>();
        List<DataCore> cores = new ArrayList<>();
        int kills = 0;
        int credits = 0;
        for (int number = end.act().levels().first(); number <= end.level(); number++) {
            Optional<String> key = content.levelKey(number);
            String name = key.map(Missions::displayName).orElse(Missions.HIDDEN);
            Optional<SaveGame.LevelStats> stats = campaign.levelStats(number);
            missions.add(new Mission(number, name, campaign.grade(number), stats));
            if (stats.isPresent()) {
                kills += stats.get().kills();
                credits += stats.get().credits();
            }
            if (key.isPresent()) {
                for (LevelData.Secret secret : content.level(key.get()).secrets()) {
                    if (secret.dataCore().isPresent() && campaign.dataCores().contains(secret.name())) {
                        cores.add(new DataCore(secret.name().toUpperCase(Locale.ROOT), number, name));
                    }
                }
            }
        }
        return new ActSummary(
                end.act().titleCard().act(), end.act().titleCard().name(), missions, kills, credits, cores);
    }
}
