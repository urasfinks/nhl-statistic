package ru.jamsys.tank.data;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import ru.jamsys.core.extension.builder.ArrayListBuilder;
import ru.jamsys.core.extension.builder.HashMapBuilder;
import ru.jamsys.core.flat.util.UtilJson;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

class NHLGamesForPlayerTest {

    @Test
    void goals() throws Throwable {
        NHLTeamSchedule.Instance sch2024 = new NHLTeamSchedule.Instance(NHLTeamSchedule.getExample_WSH_2024());
        Set<String> game2024 = sch2024.getIdGame();

        NHLTeamSchedule.Instance sch2025 = new NHLTeamSchedule.Instance(NHLTeamSchedule.getExample_WSH_2025());
        Set<String> game2025 = sch2025.getIdGame();

        Map<String, Map<String, Object>> goals = NHLGamesForPlayer.parseBody(NHLGamesForPlayer.getExampleOvechkin());
        HashMap<String, AtomicInteger> xx = new HashMapBuilder<String, AtomicInteger>()
                .append("2024", new AtomicInteger(0))
                .append("2025", new AtomicInteger(0));
        AtomicInteger all = new AtomicInteger(0);
        goals.forEach((s, o) -> {
            all.addAndGet(Integer.parseInt(o.get("goals").toString()));
            if (game2024.contains(s)) {
                xx.get("2024").addAndGet(Integer.parseInt(o.get("goals").toString()));
            }
            if (game2025.contains(s)) {
                xx.get("2025").addAndGet(Integer.parseInt(o.get("goals").toString()));
            }
        });
        Assertions.assertEquals("{2024=31, 2025=16}", xx.toString());
        Assertions.assertEquals("47", all.toString());
    }

    @Test
    public void test1() throws Throwable {
        Map<String, Integer> onlyGoals = NHLGamesForPlayer.getOnlyGoals(NHLGamesForPlayer.getExampleOvechkin());
        Assertions.assertEquals(0, onlyGoals.get("20240303_ARI@WSH"));

        Map<String, Integer> onlyGoals2 = NHLGamesForPlayer.getOnlyGoalsFilter(
                NHLGamesForPlayer.getExampleOvechkin(),
                new ArrayListBuilder<String>().append("20241115_WSH@COL")
        );
        Assertions.assertEquals(0, onlyGoals2.get("20241115_WSH@COL"));
    }

    @Test
    public void stat() throws Throwable {
        List<String> lisIdGameInSeason = new ArrayList<>();
        new NHLTeamSchedule.Instance(NHLTeamSchedule.getExample_WSH_2025())
                .getListGame()
                .forEach(map -> lisIdGameInSeason.add(map.get("gameID").toString()));
        Map<String, Object> aggregateStatistic = NHLGamesForPlayer.getAggregateStatistic(
                NHLGamesForPlayer.getExampleOvechkin(),
                lisIdGameInSeason
        );
        System.out.println(UtilJson.toStringPretty(aggregateStatistic, "{}"));
        Assertions.assertEquals(15, aggregateStatistic.get("plusMinus"));
        Assertions.assertEquals("00:51", aggregateStatistic.get("shortHandedTimeOnIce"));
    }

    @Test
    public void checkSec() {
        Assertions.assertEquals(60, NHLGamesForPlayer.getSec("00:01"));
        Assertions.assertEquals(3600, NHLGamesForPlayer.getSec("01:00"));
        Assertions.assertEquals(3660, NHLGamesForPlayer.getSec("01:01"));
        Assertions.assertEquals(3660, NHLGamesForPlayer.getSec("1:1"));
        Assertions.assertEquals("00:00", NHLGamesForPlayer.getSecFormat(1));
        Assertions.assertEquals("01:01", NHLGamesForPlayer.getSecFormat(3660));
    }

}