package ru.jamsys.tank.data;

import ru.jamsys.core.App;
import ru.jamsys.core.flat.util.*;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class NHLTeamSchedule {

    public static String getUri(String idTeam, String season) {
        return "/getNHLTeamSchedule?teamID=" + idTeam + "&season=" + season;
    }

    public static String getExample() throws IOException {
        return UtilFileResource.getAsString("example/getNHLTeamSchedule.json");
    }

    public static String getGameTimeZone(Map<String, Object> game) throws Exception {
        String localTimeGame = NHLTeamSchedule.getGameLocalTime(game, "yyyy-MM-dd'T'HH:mm:ss");
        String realTimeUtc = UtilDate.timestampFormatUTC(new BigDecimal(game.get("gameTime_epoch").toString()).longValue(), "yyyy-MM-dd'T'HH:mm:ss");
        long timestampLocalGame = UtilDate.getTimestamp(localTimeGame, "yyyy-MM-dd'T'HH:mm:ss");
        long timestampRealGame = UtilDate.getTimestamp(realTimeUtc, "yyyy-MM-dd'T'HH:mm:ss");
        return (timestampLocalGame - timestampRealGame < 0 ? "-" : "+") + LocalTime.MIN.plusSeconds(Math.abs(timestampLocalGame - timestampRealGame)).toString();
    }

    public static String getGameLocalTime(Map<String, Object> game, String format) {
        String gameDate = game.get("gameDate").toString();
        String gameTime = game.get("gameTime").toString();
        int hourSec = Integer.parseInt(gameTime.substring(0, gameTime.indexOf(":"))) * 60 * 60;
        int offsetHourSec = gameTime.endsWith("p") ? (12 * 60 * 60) : 0;
        int minSec = Integer.parseInt(Util.readUntil(gameTime.substring(gameTime.indexOf(":") + 1), Util::isNumeric)) * 60;
        long dateSec = UtilDate.getTimestamp(gameDate, "yyyyMMdd", 0);
        dateSec += (hourSec + offsetHourSec + minSec);
        return UtilDate.timestampFormat(dateSec, format);
    }

    public static String getGameTimeFormat(Map<String, Object> map) {
        return String.format(
                "%s (UTC%s)",
                NHLTeamSchedule.getGameLocalTime(map, "dd/MM/yyyy HH:mm"),
                map.get("timeZone")
        );
    }

    public static String getGameAbout(String infoPlayer, Map<String, Object> map) {
        return String.format("[%s] %s; %s vs %s",
                getGameTimeFormat(map), // 3
                infoPlayer, // 4
                map.get("homeTeam"), // 5
                map.get("awayTeam") // 6
        );
    }

    public static List<Map<String, Object>> findGame(String json) throws Throwable {
        Map<String, Object> teams = NHLTeams.getTeams();
        @SuppressWarnings("unchecked")
        Map<String, Object> parsed = UtilJson.toObject(json, Map.class);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> selector = (List<Map<String, Object>>) UtilJson.selector(parsed, "body.schedule");
        List<Map<String, Object>> result = new ArrayList<>();

        selector.forEach(game -> {
            long timestampStart = new BigDecimal(game.get("gameTime_epoch").toString()).longValue();
            String gameStatus = game.get("gameStatus").toString(); // https://www.tank01.com/Guides_Game_Status_Code_NHL.html
            if (game.containsKey("gameStatus") &&
                    (
                            gameStatus.equals("Scheduled")
                                    || gameStatus.equals("Live - In Progress")
                    )
            ) {
                game.put("date", UtilDate.timestampFormat(timestampStart));
                game.put("homeTeam", teams.get(game.get("home")) + " (" + game.get("home") + ")");
                game.put("awayTeam", teams.get(game.get("away")) + " (" + game.get("away") + ")");
                game.put("about", game.get("homeTeam") + " vs " + game.get("awayTeam"));
                try {
                    game.put("timeZone", NHLTeamSchedule.getGameTimeZone(game));
                } catch (Exception e) {
                    App.error(e);
                }
                result.add(game);
            }
        });
        return result;
    }

    public static List<Map<String, Object>> getSortGameByTime(List<Map<String, Object>> listGame) {
        return UtilListSort.sort(
                listGame,
                UtilListSort.Type.ASC,
                stringObjectMap -> new BigDecimal(stringObjectMap.get("gameTime_epoch").toString()).longValue()
        );
    }

}
