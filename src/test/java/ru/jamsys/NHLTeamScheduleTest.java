package ru.jamsys;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import ru.jamsys.core.flat.util.JsonEnvelope;
import ru.jamsys.core.flat.util.UtilJson;
import ru.jamsys.tank.data.NHLTeamSchedule;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Calendar;
import java.util.List;
import java.util.Map;

class NHLTeamScheduleTest {

    @Test
    void getYear() {
        System.out.println(Calendar.getInstance().get(Calendar.YEAR));
        long timestampMills = Long.parseLong("1733274000000");
        LocalDateTime timestamp = LocalDateTime.ofInstant(Instant.ofEpochMilli(timestampMills), ZoneId.systemDefault());
        System.out.println(timestamp);
    }

    @Test
    void parseGame() throws Throwable {
        List<Map<String, Object>> game = NHLTeamSchedule.parseGame(NHLTeamSchedule.getExample());
        System.out.println(UtilJson.toStringPretty(game, "{}"));
    }

    @Test
    void getGameSortAndFilterByTime() throws Throwable {
        List<Map<String, Object>> game = NHLTeamSchedule.parseGame(NHLTeamSchedule.getExample());
        List<Map<String, Object>> sortGameByTime = NHLTeamSchedule.getGameSortAndFilterByTime(game);
        System.out.println(UtilJson.toStringPretty(sortGameByTime.getFirst(), "{}"));
    }

    @Test
    void test() throws Throwable {
        Map<String, Object> game = NHLTeamSchedule.parseGame(NHLTeamSchedule.getExample()).getFirst();
        NHLTeamSchedule.extendGameTimeZone(game);
        Assertions.assertEquals("-05:00", game.get("timeZone"));
    }

    @Test
    void test2() throws Throwable {
        String data = """
                {
                  "gameID" : "20241210_COL@PIT",
                  "seasonType" : "Regular Season",
                  "away" : "COL",
                  "gameTime" : "7:00p",
                  "teamIDHome" : "23",
                  "gameDate" : "20241210",
                  "gameStatus" : "Scheduled",
                  "gameTime_epoch" : "1733875200.0",
                  "teamIDAway" : "8",
                  "home" : "PIT",
                  "gameStatusCode" : "0",
                  "homeTeam" : "Pittsburgh Penguins (PIT)",
                  "awayTeam" : "Colorado Avalanche (COL)",
                  "about" : "Pittsburgh Penguins (PIT) vs Colorado Avalanche (COL)"
                }
                """;
        Map<String, Object> game = UtilJson.getMapOrThrow(data);
        NHLTeamSchedule.extendGameTimeZone(game);
        System.out.println(UtilJson.toStringPretty(game, "{}"));
    }

}