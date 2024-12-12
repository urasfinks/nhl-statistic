package ru.jamsys;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import ru.jamsys.core.extension.builder.ArrayListBuilder;
import ru.jamsys.core.extension.builder.HashMapBuilder;
import ru.jamsys.core.flat.util.UtilJson;
import ru.jamsys.tank.data.NHLBoxScore;

import java.util.List;
import java.util.Map;

class NHLBoxScoreTest {

    @Test
    void getScoringPlays() throws Throwable {
        List<Map<String, Object>> scoringPlays = NHLBoxScore.getScoringPlays(NHLBoxScore.getExample4());
        System.out.println(UtilJson.toStringPretty(scoringPlays, "{}"));
    }

    @Test
    void getScoringPlaysMap() throws Throwable {
        Map<String, List<Map<String, Object>>> scoringPlaysMap = NHLBoxScore.getScoringPlaysMap(NHLBoxScore.getExample4());
        Assertions.assertEquals("{4565257=[{period=1P, scoreTime=3:14}], 3900169=[{period=1P, scoreTime=4:24}]}", scoringPlaysMap.toString());
    }

    @Test
    void getScoringPlaysMapDiff() {
        List<Map<String, Object>> last = new ArrayListBuilder<Map<String, Object>>()
                .append(new HashMapBuilder<String, Object>().append("period", "1P").append("scoreTime", "3:16"))
                .append(new HashMapBuilder<String, Object>().append("period", "1P").append("scoreTime", "3:14"));
        List<Map<String, Object>> current = new ArrayListBuilder<Map<String, Object>>()
                .append(new HashMapBuilder<String, Object>().append("period", "1P").append("scoreTime", "3:20"))
                .append(new HashMapBuilder<String, Object>().append("period", "1P").append("scoreTime", "3:18"))
                //.append(new HashMapBuilder<String, Object>().append("period", "1P").append("scoreTime", "3:16"))
                .append(new HashMapBuilder<String, Object>().append("period", "1P").append("scoreTime", "3:14"));

        System.out.println("last");
        System.out.println(last);
        System.out.println("current");
        System.out.println(current);
        List<Map<String, Object>> result = NHLBoxScore.getNewEventScoringByPlayer(last, current);

        System.out.println("result");
        System.out.println(result);
        Assertions.assertEquals("[{period=1P, scoreTime=3:16, type=cancel}, {period=1P, scoreTime=3:20, type=goal}, {period=1P, scoreTime=3:18, type=goal}]", result.toString());
    }

    @Test
    void getScoringPlaysMapDiff2() {
        List<Map<String, Object>> last = new ArrayListBuilder<Map<String, Object>>()
                .append(new HashMapBuilder<String, Object>().append("period", "1P").append("scoreTime", "3:16"))
                .append(new HashMapBuilder<String, Object>().append("period", "1P").append("scoreTime", "3:14"));
        List<Map<String, Object>> current = new ArrayListBuilder<Map<String, Object>>()
                .append(new HashMapBuilder<String, Object>().append("period", "1P").append("scoreTime", "3:16"))
                .append(new HashMapBuilder<String, Object>().append("period", "1P").append("scoreTime", "3:14"));
        System.out.println("last");
        System.out.println(last);
        System.out.println("current");
        System.out.println(current);

        List<Map<String, Object>> result = NHLBoxScore.getNewEventScoringByPlayer(last, current);

        System.out.println("result");
        System.out.println(result);
        Assertions.assertEquals("[]", result.toString());
    }

    @Test
    void getScoringPlaysMapDiff3() {
        List<Map<String, Object>> last = new ArrayListBuilder<Map<String, Object>>()
                .append(new HashMapBuilder<String, Object>().append("period", "1P").append("scoreTime", "3:16"))
                .append(new HashMapBuilder<String, Object>().append("period", "1P").append("scoreTime", "3:14"));
        List<Map<String, Object>> current = new ArrayListBuilder<Map<String, Object>>()
                .append(new HashMapBuilder<String, Object>().append("period", "1P").append("scoreTime", "3:16"))
                .append(new HashMapBuilder<String, Object>().append("period", "1P").append("scoreTime", "3:15"));
        System.out.println("last");
        System.out.println(last);
        System.out.println("current");
        System.out.println(current);

        List<Map<String, Object>> result = NHLBoxScore.getNewEventScoringByPlayer(last, current);

        System.out.println("result");
        System.out.println(result);
        Assertions.assertEquals("[{period=1P, scoreTime=3:14, type=cancel}, {period=1P, scoreTime=3:15, type=goal}]", result.toString());
    }

    @Test
    void getScoringPlaysMapDiff4() {
        List<Map<String, Object>> last = new ArrayListBuilder<Map<String, Object>>()
                .append(new HashMapBuilder<String, Object>().append("period", "1P").append("scoreTime", "3:16"))
                .append(new HashMapBuilder<String, Object>().append("period", "1P").append("scoreTime", "3:15"));
        List<Map<String, Object>> current = new ArrayListBuilder<Map<String, Object>>()
                .append(new HashMapBuilder<String, Object>().append("period", "1P").append("scoreTime", "3:16"))
                .append(new HashMapBuilder<String, Object>().append("period", "1P").append("scoreTime", "3:14"));

        System.out.println("last");
        System.out.println(last);
        System.out.println("current");
        System.out.println(current);
        List<Map<String, Object>> result = NHLBoxScore.getNewEventScoringByPlayer(last, current);

        System.out.println("result");
        System.out.println(result);
        Assertions.assertEquals("[{period=1P, scoreTime=3:15, type=cancel}, {period=1P, scoreTime=3:14, type=goal}]", result.toString());
    }

    @Test
    void getScoringPlaysMapDiff5() {
        List<Map<String, Object>> last = new ArrayListBuilder<Map<String, Object>>()
                .append(new HashMapBuilder<String, Object>().append("period", "1P").append("scoreTime", "3:16"))
                .append(new HashMapBuilder<String, Object>().append("period", "1P").append("scoreTime", "3:15"));
        List<Map<String, Object>> current = new ArrayListBuilder<>();

        System.out.println("last");
        System.out.println(last);
        System.out.println("current");
        System.out.println(current);
        List<Map<String, Object>> result = NHLBoxScore.getNewEventScoringByPlayer(last, current);

        System.out.println("result");
        System.out.println(result);
        Assertions.assertEquals("[{period=1P, scoreTime=3:16, type=cancel}, {period=1P, scoreTime=3:15, type=cancel}]", result.toString());
    }


    @Test
    void getScoringPlaysMapDiff6() {
        List<Map<String, Object>> last = new ArrayListBuilder<>();
        List<Map<String, Object>> current = new ArrayListBuilder<Map<String, Object>>()
                .append(new HashMapBuilder<String, Object>().append("period", "1P").append("scoreTime", "3:16"))
                .append(new HashMapBuilder<String, Object>().append("period", "1P").append("scoreTime", "3:14"));

        System.out.println("last");
        System.out.println(last);
        System.out.println("current");
        System.out.println(current);
        List<Map<String, Object>> result = NHLBoxScore.getNewEventScoringByPlayer(last, current);

        System.out.println("result");
        System.out.println(result);
        Assertions.assertEquals("[{period=1P, scoreTime=3:16, type=goal}, {period=1P, scoreTime=3:14, type=goal}]", result.toString());
    }

    @Test
    void getScoringPlaysMapDiff7() {
        List<Map<String, Object>> last = new ArrayListBuilder<Map<String, Object>>()
                .append(new HashMapBuilder<String, Object>().append("period", "1P").append("scoreTime", "3:14"));
        List<Map<String, Object>> current = new ArrayListBuilder<Map<String, Object>>()
                .append(new HashMapBuilder<String, Object>().append("period", "1P").append("scoreTime", "3:14"))
                .append(new HashMapBuilder<String, Object>().append("period", "1P").append("scoreTime", "3:14"));
        System.out.println("last");
        System.out.println(last);
        System.out.println("current");
        System.out.println(current);
        List<Map<String, Object>> result = NHLBoxScore.getNewEventScoringByPlayer(last, current);

        System.out.println("result");
        System.out.println(result);
        Assertions.assertEquals("[{period=1P, scoreTime=3:14, type=goal}]", result.toString());
    }

    @Test
    void getScoringPlaysMapDiff8() {
        List<Map<String, Object>> last = new ArrayListBuilder<Map<String, Object>>()
                .append(new HashMapBuilder<String, Object>().append("period", "3P").append("scoreTime", "3:14"))
                .append(new HashMapBuilder<String, Object>().append("period", "2P").append("scoreTime", "3:14"))
                .append(new HashMapBuilder<String, Object>().append("period", "1P").append("scoreTime", "3:14"));
        List<Map<String, Object>> current = new ArrayListBuilder<Map<String, Object>>()
                .append(new HashMapBuilder<String, Object>().append("period", "2P").append("scoreTime", "3:14"))
                .append(new HashMapBuilder<String, Object>().append("period", "1P").append("scoreTime", "3:14"));

        List<Map<String, Object>> result = NHLBoxScore.getNewEventScoringByPlayer(last, current);
        System.out.println("last");
        System.out.println(last);
        System.out.println("current");
        System.out.println(current);
        System.out.println("result");
        System.out.println(result);
        Assertions.assertEquals("[{period=3P, scoreTime=3:14, type=cancel}]", result.toString());
    }

    @Test
    void getScoringPlaysMapDiff9() {
        List<Map<String, Object>> last = new ArrayListBuilder<Map<String, Object>>()
                .append(new HashMapBuilder<String, Object>().append("period", "1P").append("scoreTime", "3:15"))
                .append(new HashMapBuilder<String, Object>().append("period", "2P").append("scoreTime", "3:16"))
                .append(new HashMapBuilder<String, Object>().append("period", "3P").append("scoreTime", "3:14"))
                .append(new HashMapBuilder<String, Object>().append("period", "4P").append("scoreTime", "3:15"));
        List<Map<String, Object>> current = new ArrayListBuilder<Map<String, Object>>()
                .append(new HashMapBuilder<String, Object>().append("period", "5P").append("scoreTime", "3:14"))
                .append(new HashMapBuilder<String, Object>().append("period", "6P").append("scoreTime", "3:14"))
                .append(new HashMapBuilder<String, Object>().append("period", "7P").append("scoreTime", "3:15"));
        System.out.println("last");
        System.out.println(last);
        System.out.println("current");
        System.out.println(current);
        List<Map<String, Object>> result = NHLBoxScore.getNewEventScoringByPlayer(last, current);

        System.out.println("result");
        System.out.println(result);
        Assertions.assertEquals("[{period=1P, scoreTime=3:15, type=cancel}, {period=2P, scoreTime=3:16, type=cancel}, {period=5P, scoreTime=3:14, type=goal}]", result.toString());
    }

    @Test
    void isFinish() throws Throwable {
        Assertions.assertFalse(NHLBoxScore.isFinish(NHLBoxScore.getExample4()));
        Assertions.assertTrue(NHLBoxScore.isFinish(NHLBoxScore.getExample6()));
    }

    @Test
    void getDiff() throws Throwable {
        List<Map<String, Object>> newEventScoring = NHLBoxScore.getNewEventScoring(
                NHLBoxScore.getExample4(),
                NHLBoxScore.getExample5()
        );
        Assertions.assertEquals(
                "[{period=2P, goal={longName=Vincent Trocheck, playerID=2563036}, shortHanded=False, shootout=False, assists=[], powerPlay=False, teamID=20, scoreTime=10:18, team=NYR, teamAbv=NYR}]",
                newEventScoring.toString()
        );
    }

    @Test
    void getDiff2() throws Throwable {
        List<Map<String, Object>> newEventScoring = NHLBoxScore.getNewEventScoring(
                NHLBoxScore.getExample5(),
                NHLBoxScore.getExample4()
        );
        Assertions.assertEquals("[]", newEventScoring.toString());
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
    void getDiffPlayer() throws Throwable {
        Map<String, List<Map<String, Object>>> result = NHLBoxScore.getNewEventScoringByPlayer(
                NHLBoxScore.getExample4(),
                NHLBoxScore.getExample5()
        );
        Assertions.assertEquals("{2563036=[{period=2P, scoreTime=10:18, type=goal}]}", result.toString());
    }

    @Test
    void getDiffPlayer2() throws Throwable {
        Map<String, List<Map<String, Object>>> newEventScoringByPlayer = NHLBoxScore.getNewEventScoringByPlayer(
                NHLBoxScore.getExample5(),
                NHLBoxScore.getExample4()
        );
        Assertions.assertEquals("{2563036=[{period=2P, scoreTime=10:18, type=cancel}]}", newEventScoringByPlayer.toString());
    }

}