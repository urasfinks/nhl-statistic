package ru.jamsys.tank.data;

import org.junit.jupiter.api.Test;
import ru.jamsys.core.extension.builder.HashMapBuilder;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

class NHLGamesForPlayerTest {

    @Test
    void goals() throws Throwable {

        Set<String> game2024 = new HashSet<>();
        NHLTeamSchedule.parseGameRaw(NHLTeamSchedule.getExample_18_2024()).forEach(
                stringObjectMap -> game2024.add(stringObjectMap.get("gameID").toString())
        );
        //System.out.println(game2024);
        Set<String> game2025 = new HashSet<>();
        NHLTeamSchedule.parseGameRaw(NHLTeamSchedule.getExample_18_2025()).forEach(
                stringObjectMap -> game2025.add(stringObjectMap.get("gameID").toString())
        );
        //System.out.println(game2025);
        Map<String, Map<String, Object>> goals = NHLGamesForPlayer.goals(NHLGamesForPlayer.getExample());
        HashMap<String, AtomicInteger> xx = new HashMapBuilder<String, AtomicInteger>()
                .append("2024", new AtomicInteger(0))
                .append("2025", new AtomicInteger(0));
        goals.forEach((s, o) -> {
            if (game2024.contains(s)) {
                xx.get("2024").addAndGet(Integer.parseInt(o.get("goals").toString()));
            }
            if (game2025.contains(s)) {
                xx.get("2025").addAndGet(Integer.parseInt(o.get("goals").toString()));
            }
        });
        System.out.println(xx);
    }

}