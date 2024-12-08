package ru.jamsys;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
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
    void findGame() throws Throwable {
        List<Map<String, Object>> game = NHLTeamSchedule.findGame(NHLTeamSchedule.getExample());
        System.out.println(UtilJson.toStringPretty(game, "{}"));
    }

    @Test
    void findEarlierGame() throws Throwable {
        List<Map<String, Object>> game = NHLTeamSchedule.findGame(NHLTeamSchedule.getExample());
        List<Map<String, Object>> sortGameByTime = NHLTeamSchedule.getSortGameByTime(game);
        System.out.println(UtilJson.toStringPretty(sortGameByTime, "{}"));
    }

    @Test
    void test() throws Throwable {
        Map<String, Object> game = NHLTeamSchedule.findGame(NHLTeamSchedule.getExample()).getFirst();
        Assertions.assertEquals("-05:00", NHLTeamSchedule.getGameTimeZone(game));
    }

}