package ru.jamsys.tank.data;

import ru.jamsys.core.flat.util.UtilDate;
import ru.jamsys.core.flat.util.UtilFileResource;
import ru.jamsys.core.flat.util.UtilJson;
import ru.jamsys.core.flat.util.UtilListSort;

import java.io.IOException;
import java.math.BigDecimal;
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
            if (game.containsKey("gameStatus") && (gameStatus.equals("Scheduled") || gameStatus.equals("Live - In Progress"))) {
                game.put("date", UtilDate.timestampFormat(timestampStart));
                game.put("homeTeam", teams.get(game.get("home")) + " (" + game.get("home") + ")");
                game.put("awayTeam", teams.get(game.get("away")) + " (" + game.get("away") + ")");
                game.put("about", game.get("homeTeam") + " vs " + game.get("awayTeam"));
                //System.out.println(UtilJson.toStringPretty(game, "{}"));
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
