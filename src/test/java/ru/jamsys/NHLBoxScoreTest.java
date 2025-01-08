package ru.jamsys;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import ru.jamsys.core.flat.util.UtilFileResource;
import ru.jamsys.core.flat.util.UtilJson;
import ru.jamsys.tank.data.NHLBoxScore;
import ru.jamsys.tank.data.NHLPlayerList;
import ru.jamsys.telegram.GameEventData;
import ru.jamsys.telegram.template.GameEventTemplate;
import ru.jamsys.telegram.template.GameEventTemplateOvi;

import java.io.IOException;
import java.util.List;
import java.util.Map;

class NHLBoxScoreTest {

    @Test
    void isFinish() throws Throwable {
        NHLBoxScore.Instance instance1 = new NHLBoxScore.Instance(NHLBoxScore.getExample4());
        NHLBoxScore.Instance instance2 = new NHLBoxScore.Instance(UtilFileResource.getAsString("example/getNHLBoxScore6.json"));
        NHLBoxScore.Instance instance3 = new NHLBoxScore.Instance(UtilFileResource.getAsString("example/PenaltyShot.json"));

        Assertions.assertFalse(instance1.isFinish());
        Assertions.assertTrue(instance2.isFinish());
        Assertions.assertFalse(instance3.isFinish());

        Assertions.assertFalse(instance1.isPenaltyShot());
        Assertions.assertFalse(instance2.isPenaltyShot());
        Assertions.assertTrue(instance3.isPenaltyShot());

        Assertions.assertFalse(instance1.isOverTime());
        Assertions.assertFalse(instance2.isOverTime());
        Assertions.assertTrue(instance3.isOverTime());
    }

    @Test
    void getError() {
        try {
            NHLBoxScore.Instance instance = new NHLBoxScore.Instance(NHLBoxScore.getExampleError());
            instance.isFinish();
            Assertions.fail();
        } catch (Throwable e) {
            Assertions.assertEquals("Game hasn't started yet. Game time is: 7:00p(ET) on 20241210", e.getMessage());
        }
    }

    @Test
    void testSeasonGoal() {
        NHLPlayerList.Player player = new NHLPlayerList.Player()
                .setPlayerID("4874723")
                .setPos("RW")
                .setLongName("Dylan Guenther")
                .setTeam("UTA")
                .setTeamID("33");
        GameEventData gameEventData = new GameEventData(
                GameEventData.Action.GOAL,
                "Washington Capitals (WSH) 🆚 Detroit Red Wings (DET)",
                "Washington Capitals (WSH) 1 - 1 Detroit Red Wings (DET)",
                NHLPlayerList.getPlayerName(player),
                "31.12.2024 16:43:53"
        );
        gameEventData
                .setScoredGoal(5)
                .setScoredLastSeason(400)
                .setScoredPrevGoal(10);
        Assertions.assertEquals(
                """
                        🚨 ГОООЛ! 31.12.2024 16:43:53. Dylan Guenther (UTA) забивает свой 15-й гол в сезоне!
                        Washington Capitals (WSH) 1 - 1 Detroit Red Wings (DET)""",
                new GameEventTemplate(gameEventData).toString()
        );
    }

    @Test
    void periodExpandEn() {
        Assertions.assertEquals("1st period", NHLBoxScore.periodExpandEn("1P"));
        Assertions.assertEquals("2nd period", NHLBoxScore.periodExpandEn("2P"));
        Assertions.assertEquals("3rd period", NHLBoxScore.periodExpandEn("3P"));
        Assertions.assertEquals("overtime", NHLBoxScore.periodExpandEn("OT"));
        Assertions.assertEquals("time is it", NHLBoxScore.periodExpandEn("time is it"));
        String idGame = "20241012_NJ@WSH";
        Assertions.assertEquals("NJ@WSH", idGame.substring(idGame.indexOf("_") + 1));
    }

    @Test
    void getPlayerStat() throws IOException {
        Map<String, Object> playerStat = NHLBoxScore.getPlayerStat(NHLBoxScore.getExample5(), "4565257");
        Assertions.assertEquals("1", playerStat.get("goals"));
    }

    @Test
    void parse() throws Throwable {
        NHLBoxScore.Instance instance = new NHLBoxScore.Instance(NHLBoxScore.getExample4());
        NHLBoxScore.Player player = instance.getPlayer("3900169");


        Assertions.assertEquals("(4:24, 1-й период)", player.getFinishTimeScore());
        Assertions.assertEquals("1", player.getStat().get("goals"));
        Assertions.assertEquals("Philadelphia Flyers (PHI) 2 - 0 New York Rangers (NYR)", instance.getScoreGame("PHI"));
        Assertions.assertEquals("New York Rangers (NYR) 0 - 2 Philadelphia Flyers (PHI)", instance.getScoreGame());
        Assertions.assertEquals("New York Rangers (NYR) 0 - 2 Philadelphia Flyers (PHI)", instance.getScoreGame("NYR"));
        System.out.println(UtilJson.toStringPretty(instance, "{}"));

        Assertions.assertEquals(1, player.getGoals());
        Assertions.assertEquals(1, player.getShots());
        Assertions.assertEquals(0, player.getAssists());
        Assertions.assertEquals(0, player.getHits());
        Assertions.assertEquals(0, player.getPenaltiesInMinutes());
        Assertions.assertEquals("1:49", player.getTimeOnIce());
    }

    @Test
    void parse2() throws Throwable {
        NHLBoxScore.Instance instance = new NHLBoxScore.Instance(NHLBoxScore.getExampleChange());
        NHLBoxScore.Player player = instance.getPlayer("4874723");
        Assertions.assertEquals("(4:56, 1-й период | 19:18, 3-й период)", player.getFinishTimeScore());
    }

    @Test
    void getEvent() throws Throwable {
        Map<String, List<GameEventData>> event = NHLBoxScore.getEvent(NHLBoxScore.getExample(), NHLBoxScore.getExampleChange());
        System.out.println(UtilJson.toStringPretty(event, "{}"));
        Assertions.assertEquals("19:18, 3-й период", event.get("4874723").getFirst().getTime());
        Assertions.assertEquals("""
                🚨 ГОООЛ! 19:18, 3-й период. Dylan Guenther забивает свой 2-й гол в сезоне!
                Chicago Blackhawks (CHI) 2 - 5 Utah Hockey Club (UTA)""", new GameEventTemplate(event.get("4874723").getFirst()).toString());
        Assertions.assertEquals("""
               🚨 ГОООЛ! 19:18, 3-й период. Александр Овечкин забивает свой 2-й гол в карьере! До рекорда Гретцки осталось 892 гола.
               Chicago Blackhawks (CHI) 2 - 5 Utah Hockey Club (UTA)""", new GameEventTemplateOvi(event.get("4874723").getFirst()).toString());

    }

}