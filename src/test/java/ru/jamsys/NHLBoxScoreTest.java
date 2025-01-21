package ru.jamsys;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import ru.jamsys.core.flat.util.Util;
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
                player,
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
        NHLBoxScore.PlayerStat playerStat = instance.getPlayerStat("3900169");


        Assertions.assertEquals("(4:24, 1-й период)", playerStat.getFinishTimeScore());
        Assertions.assertEquals("1", playerStat.getStat().get("goals"));
        Assertions.assertEquals("Philadelphia Flyers (PHI) 2 - 0 New York Rangers (NYR)", instance.getScoreGame("PHI"));
        Assertions.assertEquals("New York Rangers (NYR) 0 - 2 Philadelphia Flyers (PHI)", instance.getScoreGame());
        Assertions.assertEquals("New York Rangers (NYR) 0 - 2 Philadelphia Flyers (PHI)", instance.getScoreGame("NYR"));

        Assertions.assertEquals(1, playerStat.getGoals());
        Assertions.assertEquals(1, playerStat.getShots());
        Assertions.assertEquals(0, playerStat.getAssists());
        Assertions.assertEquals(0, playerStat.getHits());
        Assertions.assertEquals(0, playerStat.getPenaltiesInMinutes());
        Assertions.assertEquals("1:49", playerStat.getTimeOnIce());
    }

    @Test
    void firstInt() {
        Assertions.assertNull(NHLBoxScore.Instance.getFirstNumber(null));
        Assertions.assertNull(NHLBoxScore.Instance.getFirstNumber(""));
        Assertions.assertNull(NHLBoxScore.Instance.getFirstNumber("P"));
        Assertions.assertEquals(1, NHLBoxScore.Instance.getFirstNumber("1P"));
        Assertions.assertEquals(1, NHLBoxScore.Instance.getFirstNumber("1"));
        Assertions.assertEquals(1, NHLBoxScore.Instance.getFirstNumber("1P l"));
        Assertions.assertEquals(13, NHLBoxScore.Instance.getFirstNumber("13P l"));
        Assertions.assertEquals(133, NHLBoxScore.Instance.getFirstNumber("133P l"));
        Assertions.assertEquals(1334, NHLBoxScore.Instance.getFirstNumber("1334P l"));
        Assertions.assertEquals(1334, NHLBoxScore.Instance.getFirstNumber("1334"));
    }

    @Test
    void parse2() throws Throwable {
        NHLBoxScore.Instance instance = new NHLBoxScore.Instance(NHLBoxScore.getExampleChange());
        NHLBoxScore.PlayerStat playerStat = instance.getPlayerStat("4874723");
        Assertions.assertEquals("(4:56, 1-й период | 19:18, 3-й период)", playerStat.getFinishTimeScore());
    }

    @Test
    void parse3() throws Throwable {
        NHLBoxScore.Instance instance = new NHLBoxScore.Instance(UtilFileResource.getAsString("example/block2/Test3.json"));
        NHLBoxScore.PlayerStat playerStat = instance.getPlayerStat("4915856");
        Assertions.assertEquals("(3:20, 1-й период | 00:02, 2-й период | 03:15, 3-й период | 99:60, 3-й период | 00:01, !P | 00:02, доп. время)", playerStat.getFinishTimeScore());
    }

    @Test
    void validate() throws Throwable {
        Assertions.assertEquals("[3900169]", new NHLBoxScore.Instance(UtilFileResource.getAsString("example/block3/Test1.json")).getPlayerProblemStatistic().toString());
        Assertions.assertEquals("[4915856]", new NHLBoxScore.Instance(UtilFileResource.getAsString("example/block2/Test3.json")).getPlayerProblemStatistic().toString());
        Assertions.assertEquals("[]", new NHLBoxScore.Instance(NHLBoxScore.getExampleChange()).getPlayerProblemStatistic().toString());
    }

    @Test
    void modify() throws Throwable {
        NHLBoxScore.Instance last = new NHLBoxScore.Instance(UtilFileResource.getAsString("example/block1/not_valid_last.json"));
        NHLBoxScore.Instance current = new NHLBoxScore.Instance(UtilFileResource.getAsString("example/block1/not_valid.json"));
        String beforeModifyJson = UtilJson.toStringPretty(current.getParsedJson(),"{}");
        Assertions.assertEquals("[]", last.getPlayerProblemStatistic().toString());
        Assertions.assertEquals("[5436]", current.getPlayerProblemStatistic().toString());

        Assertions.assertEquals("{gameID=20250107_NSH@WPG, penalties=0, shootoutGoals=0, blockedShots=0, shotsMissedNet=2, shortHandedTimeOnIce=0:00, assists=1, timeOnIce=22:16, teamID=17, shifts=18, powerPlayPoints=1, powerPlayTimeOnIce=5:16, goals=0, faceoffsWon=0, plusMinus=0, faceoffs=0, powerPlayGoals=0, takeaways=0, team=NSH, teamAbv=NSH, penaltiesInMinutes=0, hits=0, powerPlayAssists=1, shots=2, faceoffsLost=0, playerID=5436, giveaways=1, longName=Roman Josi}", last.getPlayerStats().get("5436").toString());
        Assertions.assertEquals("{gameID=20250107_NSH@WPG, penalties=0, shootoutGoals=0, blockedShots=0, shotsMissedNet=2, shortHandedTimeOnIce=0:00, assists=1, timeOnIce=22:43, teamID=17, shifts=18, powerPlayPoints=1, powerPlayTimeOnIce=5:16, goals=1, faceoffsWon=0, plusMinus=0, faceoffs=0, powerPlayGoals=0, takeaways=0, team=NSH, teamAbv=NSH, penaltiesInMinutes=0, hits=0, powerPlayAssists=1, shots=3, faceoffsLost=0, playerID=5436, giveaways=1, longName=Roman Josi}", current.getPlayerStats().get("5436").toString());

        Assertions.assertEquals("{gameID=20250107_NSH@WPG, penalties=0, shootoutGoals=0, blockedShots=0, shotsMissedNet=2, shortHandedTimeOnIce=0:00, assists=1, timeOnIce=22:16, teamID=17, shifts=18, powerPlayPoints=1, powerPlayTimeOnIce=5:16, goals=0, faceoffsWon=0, plusMinus=0, faceoffs=0, powerPlayGoals=0, takeaways=0, team=NSH, teamAbv=NSH, penaltiesInMinutes=0, hits=0, powerPlayAssists=1, shots=2, faceoffsLost=0, playerID=5436, giveaways=1, longName=Roman Josi}", last.getParsedJsonPlayerStats().get("5436").toString());
        Assertions.assertEquals("{gameID=20250107_NSH@WPG, penalties=0, shootoutGoals=0, blockedShots=0, shotsMissedNet=2, shortHandedTimeOnIce=0:00, assists=1, timeOnIce=22:43, teamID=17, shifts=18, powerPlayPoints=1, powerPlayTimeOnIce=5:16, goals=1, faceoffsWon=0, plusMinus=0, faceoffs=0, powerPlayGoals=0, takeaways=0, team=NSH, teamAbv=NSH, penaltiesInMinutes=0, hits=0, powerPlayAssists=1, shots=3, faceoffsLost=0, playerID=5436, giveaways=1, longName=Roman Josi}", current.getParsedJsonPlayerStats().get("5436").toString());

        current.modify(last);
        Assertions.assertEquals("[]", current.getPlayerProblemStatistic().toString());
        Assertions.assertEquals("{gameID=20250107_NSH@WPG, penalties=0, shootoutGoals=0, blockedShots=0, shotsMissedNet=2, shortHandedTimeOnIce=0:00, assists=1, timeOnIce=22:16, teamID=17, shifts=18, powerPlayPoints=1, powerPlayTimeOnIce=5:16, goals=0, faceoffsWon=0, plusMinus=0, faceoffs=0, powerPlayGoals=0, takeaways=0, team=NSH, teamAbv=NSH, penaltiesInMinutes=0, hits=0, powerPlayAssists=1, shots=2, faceoffsLost=0, playerID=5436, giveaways=1, longName=Roman Josi}", current.getPlayerStats().get("5436").toString());
        Assertions.assertEquals("{gameID=20250107_NSH@WPG, penalties=0, shootoutGoals=0, blockedShots=0, shotsMissedNet=2, shortHandedTimeOnIce=0:00, assists=1, timeOnIce=22:16, teamID=17, shifts=18, powerPlayPoints=1, powerPlayTimeOnIce=5:16, goals=0, faceoffsWon=0, plusMinus=0, faceoffs=0, powerPlayGoals=0, takeaways=0, team=NSH, teamAbv=NSH, penaltiesInMinutes=0, hits=0, powerPlayAssists=1, shots=2, faceoffsLost=0, playerID=5436, giveaways=1, longName=Roman Josi}", current.getParsedJsonPlayerStats().get("5436").toString());

        current.modify(last);
        Assertions.assertEquals("[]", current.getPlayerProblemStatistic().toString());
        Assertions.assertEquals("{gameID=20250107_NSH@WPG, penalties=0, shootoutGoals=0, blockedShots=0, shotsMissedNet=2, shortHandedTimeOnIce=0:00, assists=1, timeOnIce=22:16, teamID=17, shifts=18, powerPlayPoints=1, powerPlayTimeOnIce=5:16, goals=0, faceoffsWon=0, plusMinus=0, faceoffs=0, powerPlayGoals=0, takeaways=0, team=NSH, teamAbv=NSH, penaltiesInMinutes=0, hits=0, powerPlayAssists=1, shots=2, faceoffsLost=0, playerID=5436, giveaways=1, longName=Roman Josi}", current.getPlayerStats().get("5436").toString());
        Assertions.assertEquals("{gameID=20250107_NSH@WPG, penalties=0, shootoutGoals=0, blockedShots=0, shotsMissedNet=2, shortHandedTimeOnIce=0:00, assists=1, timeOnIce=22:16, teamID=17, shifts=18, powerPlayPoints=1, powerPlayTimeOnIce=5:16, goals=0, faceoffsWon=0, plusMinus=0, faceoffs=0, powerPlayGoals=0, takeaways=0, team=NSH, teamAbv=NSH, penaltiesInMinutes=0, hits=0, powerPlayAssists=1, shots=2, faceoffsLost=0, playerID=5436, giveaways=1, longName=Roman Josi}", current.getParsedJsonPlayerStats().get("5436").toString());

        Assertions.assertNotEquals(UtilJson.toStringPretty(current.getParsedJson(), "{}"), beforeModifyJson);
    }

    @Test
    void getEvent() throws Throwable {
        Map<String, List<GameEventData>> event = NHLBoxScore.getEvent(NHLBoxScore.getExample(), NHLBoxScore.getExampleChange());
        Assertions.assertEquals("19:18, 3-й период", event.get("4874723").getFirst().getTime());
        Assertions.assertEquals("""
                🚨 ГОООЛ! 19:18, 3-й период. Dylan Guenther (UTA) забивает свой 2-й гол в сезоне!
                Chicago Blackhawks (CHI) 2 - 5 Utah Hockey Club (UTA)""", new GameEventTemplate(event.get("4874723").getFirst()).toString());
        Assertions.assertEquals("""
               🚨 ГОООЛ! 19:18, 3-й период. Александр Овечкин забивает свой 2-й гол в карьере! До рекорда Гретцки осталось 892 гола.
               Chicago Blackhawks (CHI) 2 - 5 Utah Hockey Club (UTA)""", new GameEventTemplateOvi(event.get("4874723").getFirst()).toString());

    }

}