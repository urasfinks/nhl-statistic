package ru.jamsys;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
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
        Assertions.assertFalse(NHLBoxScore.isFinish(NHLBoxScore.getExample4()));
        Assertions.assertTrue(NHLBoxScore.isFinish(NHLBoxScore.getExample6()));
    }

    @Test
    void getError() {
        try {
            NHLBoxScore.isFinish(NHLBoxScore.getExampleError());
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
                        🚨 ГОООЛ! 31.12.2024 16:43:53. Dylan Guenther (UTA) забивает свой 15-й гол в сезоне!.
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

        Assertions.assertEquals("1", player.getStat().get("goals"));
        Assertions.assertEquals("Philadelphia Flyers (PHI) 2 - 0 New York Rangers (NYR)", instance.getScoreGame("PHI"));
        Assertions.assertEquals("Philadelphia Flyers (PHI) 2 - 0 New York Rangers (NYR)", instance.getScoreGame());
        Assertions.assertEquals("New York Rangers (NYR) 0 - 2 Philadelphia Flyers (PHI)", instance.getScoreGame("NYR"));
        System.out.println(UtilJson.toStringPretty(instance, "{}"));

        Assertions.assertEquals(1, player.getGoals());
        Assertions.assertEquals(1, player.getShots());
        Assertions.assertEquals(0, player.getAssists());
        Assertions.assertEquals(0, player.getHits());
        Assertions.assertEquals(0, player.getPenaltiesInMinutes());
        Assertions.assertEquals("1:49", player.getTimeOnIce());
        //{
        //  "gameID" : "20241129_NYR@PHI",
        //  "penalties" : "0",
        //  "shootoutGoals" : "0",
        //  "blockedShots" : "0",
        //  "shotsMissedNet" : "0",
        //  "shortHandedTimeOnIce" : "0:00",
        //  "assists" : "0",
        //  "timeOnIce" : "1:49",
        //  "teamID" : "22",
        //  "shifts" : "3",
        //  "powerPlayPoints" : "0",
        //  "powerPlayTimeOnIce" : "0:00",
        //  "goals" : "1",
        //  "faceoffsWon" : "0",
        //  "plusMinus" : "+1",
        //  "faceoffs" : "0",
        //  "powerPlayGoals" : "0",
        //  "takeaways" : "1",
        //  "team" : "PHI",
        //  "teamAbv" : "PHI",
        //  "penaltiesInMinutes" : "0",
        //  "hits" : "0",
        //  "powerPlayAssists" : "0",
        //  "shots" : "1",
        //  "faceoffsLost" : "0",
        //  "playerID" : "3900169",
        //  "giveaways" : "0",
        //  "longName" : "Travis Konecny"
        //}
    }

    @Test
    void getEvent() throws Throwable {
        Map<String, List<GameEventData>> event = NHLBoxScore.getEvent(NHLBoxScore.getExample(), NHLBoxScore.getExampleChange());
        System.out.println(UtilJson.toStringPretty(event, "{}"));
        Assertions.assertEquals("19:18, 3-й период", event.get("4874723").getFirst().getTime());
        Assertions.assertEquals("""
                🚨 ГОООЛ! 19:18, 3-й период. Dylan Guenther забивает свой 2-й гол в сезоне!.
                Utah Hockey Club (UTA) 5 - 2 Chicago Blackhawks (CHI)""", new GameEventTemplate(event.get("4874723").getFirst()).toString());
        Assertions.assertEquals("""
                🚨 ГОООЛ! 19:18, 3-й период. Александр Овечкин забивает свой 2-й гол в карьере! До рекорда Гретцки осталось 892 гола.
                Utah Hockey Club (UTA) 5 - 2 Chicago Blackhawks (CHI)""", new GameEventTemplateOvi(event.get("4874723").getFirst()).toString());

    }

}