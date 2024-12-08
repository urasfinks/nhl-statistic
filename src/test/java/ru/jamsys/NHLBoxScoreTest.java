package ru.jamsys;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import ru.jamsys.core.flat.util.UtilJson;
import ru.jamsys.tank.data.NHLBoxScore;

import java.util.List;
import java.util.Map;

class NHLBoxScoreTest {

    @Test
    void getScoringPlays() throws Throwable {
        List<Map<String, Object>> scoringPlays = NHLBoxScore.getScoringPlays(NHLBoxScore.getExample());
        System.out.println(UtilJson.toStringPretty(scoringPlays, "{}"));
    }

    @Test
    void isFinish() throws Throwable {
        Assertions.assertFalse(NHLBoxScore.isFinish(NHLBoxScore.getExample()));
        Assertions.assertTrue(NHLBoxScore.isFinish(NHLBoxScore.getExample3()));
    }

    @Test
    void getDiff() throws Throwable {
        List<Map<String, Object>> newEventScoring = NHLBoxScore.getNewEventScoring(
                NHLBoxScore.getExample(),
                NHLBoxScore.getExample2()
        );
        Assertions.assertEquals(
                "[{period=2P, goal={longName=Vincent Trocheck, playerID=2563036}, shortHanded=False, shootout=False, assists=[], powerPlay=False, teamID=20, scoreTime=10:18, team=NYR, teamAbv=NYR}]",
                newEventScoring.toString()
        );
    }

    @Test
    void getDiff2() throws Throwable {
        List<Map<String, Object>> newEventScoring = NHLBoxScore.getNewEventScoring(
                NHLBoxScore.getExample2(),
                NHLBoxScore.getExample()
        );
        Assertions.assertEquals("[]", newEventScoring.toString());
    }

}