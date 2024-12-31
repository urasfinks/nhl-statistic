package ru.jamsys;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import ru.jamsys.core.extension.builder.ArrayListBuilder;
import ru.jamsys.core.flat.util.UtilJson;
import ru.jamsys.tank.data.NHLBoxScore;
import ru.jamsys.tank.data.NHLPlayerList;
import ru.jamsys.telegram.GameEventData;
import ru.jamsys.telegram.template.GameEventTemplate;

import java.io.IOException;
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
        String idGame = "20241008_CHI@UTA";
        NHLPlayerList.Player player = new NHLPlayerList.Player()
                .setPlayerID("4874723")
                .setPos("RW")
                .setLongName("Dylan Guenther")
                .setTeam("UTA")
                .setTeamID("33");
        GameEventData gameEventData = new GameEventData();
        gameEventData
                .setAction(GameEventData.Action.GOAL)
                .setScoredGoal(5)
                .setScoredEnum(new ArrayListBuilder<String>().append("any enum period"))
                .setPlayerName(NHLPlayerList.getPlayerName(player))
                .setGameName(idGame.substring(idGame.indexOf("_") + 1))
                .setScoredLastSeason(400)
                .setScoredPrevGoal(10);
        Assertions.assertEquals(
                "GOAL! Game CHI@UTA. Dylan Guenther (UTA) scored 5 goals: any enum period. He has 15 goals in season, 415 goals in career and only 479 goals till Gretzky all-time record",
                new GameEventTemplate(gameEventData).toString()
        );
    }

    @Test
    void periodExpand() {
        Assertions.assertEquals("1st period", NHLBoxScore.periodExpand("1P"));
        Assertions.assertEquals("2nd period", NHLBoxScore.periodExpand("2P"));
        Assertions.assertEquals("3rd period", NHLBoxScore.periodExpand("3P"));
        Assertions.assertEquals("overtime", NHLBoxScore.periodExpand("OT"));
        Assertions.assertEquals("time is it", NHLBoxScore.periodExpand("time is it"));
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
        Assertions.assertEquals("Philadelphia Flyers (PHI) 2 - 0 New York Rangers (NYR)", instance.getScore("PHI"));
        Assertions.assertEquals("Philadelphia Flyers (PHI) 2 - 0 New York Rangers (NYR)", instance.getScoreHome());
        Assertions.assertEquals("New York Rangers (NYR) 0 - 2 Philadelphia Flyers (PHI)", instance.getScore("NYR"));
        System.out.println(UtilJson.toStringPretty(instance, "{}"));
    }

}