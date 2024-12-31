package ru.jamsys.tank.data;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.Setter;
import ru.jamsys.core.extension.exception.ForwardException;
import ru.jamsys.core.flat.util.Util;
import ru.jamsys.core.flat.util.UtilFileResource;
import ru.jamsys.core.flat.util.UtilJson;
import ru.jamsys.telegram.GameEventData;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NHLBoxScore {

    public static String getUri(String idGame) {
        try {
            return "/getNHLBoxScore?gameID=" + Util.urlEncode(idGame);
        } catch (Exception e) {
            throw new ForwardException(e);
        }
    }

    public static String getExample4() throws IOException {
        return UtilFileResource.getAsString("example/getNHLBoxScore4.json");
    }

    @SuppressWarnings("unused")
    public static String getExampleEmptyScore() throws IOException {
        return UtilFileResource.getAsString("example/getNHLBoxScore_empty_scoring.json");
    }

    public static String getExample5() throws IOException {
        return UtilFileResource.getAsString("example/getNHLBoxScore5.json");
    }

    public static String getExample6() throws IOException {
        return UtilFileResource.getAsString("example/getNHLBoxScore6.json");
    }

    public static String getExample6ChangeTime() throws IOException {
        return UtilFileResource.getAsString("example/getNHLBoxScore6_change_time.json");
    }

    public static String getExample7() throws IOException {
        return UtilFileResource.getAsString("example/getNHLBoxScore7.json");
    }

    public static String getExample7ManyChange() throws IOException {
        return UtilFileResource.getAsString("example/getNHLBoxScore7_many_change.json");
    }

    public static String getExampleError() throws IOException {
        return UtilFileResource.getAsString("example/getNHLBoxScore_error.json");
    }

    public static Map<String, List<GameEventData>> getEvent(){
        Map<String, List<GameEventData>> result = new HashMap<>();
        return result;
    }

    public static List<Map<String, Object>> getScoringPlays(String json) throws Throwable {
        if (json == null || json.isEmpty()) { //Так как в БД может быть ничего
            return new ArrayList<>();
        }
        @SuppressWarnings("unchecked")
        Map<String, Object> parsed = UtilJson.toObject(json, Map.class);
        if (parsed.containsKey("error")) {
            throw new RuntimeException(parsed.get("error").toString());
        }
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> selector = (List<Map<String, Object>>) UtilJson.selector(parsed, "body.scoringPlays");
        return selector;
    }

    public static boolean isFinish(String json) throws Throwable {
        if (json == null || json.isEmpty()) { //Так как в БД может быть ничего
            return false;
        }
        @SuppressWarnings("unchecked")
        Map<String, Object> parsed = UtilJson.toObject(json, Map.class);
        if (parsed.containsKey("error")) {
            throw new RuntimeException(parsed.get("error").toString());
        }
        String gameStatusCode = (String) UtilJson.selector(parsed, "body.gameStatusCode");
        return Integer.parseInt(gameStatusCode) == 2;
    }

    public static List<String> getEnumGame(List<Map<String, Object>> listPlaysCurrent) {
        List<String> timeGoal = new ArrayList<>();
        listPlaysCurrent.forEach(map -> timeGoal.add(map.get("scoreTime") + " " + periodExpand(map.get("period").toString())));
        return timeGoal;
    }

    public static String periodExpand(String period) {
        if (period == null || period.isEmpty()) {
            return "undefined period";
        } else if (period.equals("1P")) {
            return "1st period";
        } else if (period.equals("2P")) {
            return "2nd period";
        } else if (period.equals("3P")) {
            return "3rd period";
        } else if (period.equals("OT")) {
            return "overtime";
        } else {
            return period;
        }
    }


    public static Map<String, Object> getPlayerStat(String json, String idPlayer) {
        @SuppressWarnings("unchecked")
        Map<String, Object> res = (Map<String, Object>) UtilJson.selector(json, "$.body.playerStats." + idPlayer);
        return res;
    }

    @Getter
    public static class Instance {

        @JsonIgnore
        final private List<Map<String, Object>> scoringPlays;

        @JsonIgnore
        final private Map<String, Map<String, Object>> playerStats;

        final private Map<String, Integer> scoreMap = new HashMap<>();

        final String scoreHome;

        public Instance(String json) throws Throwable {
            if (json == null || json.isEmpty()) { //Так как в БД может быть ничего
                throw new RuntimeException("json empty");
            }

            @SuppressWarnings("unchecked")
            Map<String, Object> body = (Map<String, Object>) UtilJson.toObject(json, Map.class).get("body");
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> scoringPlays = (List<Map<String, Object>>) body.get("scoringPlays");
            this.scoringPlays = scoringPlays;

            @SuppressWarnings("unchecked")
            Map<String, Map<String, Object>> playerStats = (Map<String, Map<String, Object>>) body.get("playerStats");
            this.playerStats = playerStats;

            NHLTeams.Team teamHome = NHLTeams.teams.getById(body.get("teamIDHome").toString());
            NHLTeams.Team teamAway = NHLTeams.teams.getById(body.get("teamIDAway").toString());

            scoreMap.put(teamHome.getAbv(), Integer.parseInt(body.get("homeTotal").toString()));
            scoreMap.put(teamAway.getAbv(), Integer.parseInt(body.get("awayTotal").toString()));

            scoreHome = getScore(teamHome.getAbv());
        }

        public String getScore(String firstTeamAbv) {
            StringBuilder sb = new StringBuilder();
            ArrayList<String> strings = new ArrayList<>(scoreMap.keySet());
            strings.remove(firstTeamAbv);
            String lastTeamAbv = strings.getLast();
            sb.append(NHLTeams.teams.getByAbv(firstTeamAbv).getAbout())
                    .append(" ")
                    .append(scoreMap.get(firstTeamAbv))
                    .append(" - ")
                    .append(scoreMap.get(lastTeamAbv))
                    .append(" ")
                    .append(NHLTeams.teams.getByAbv(lastTeamAbv).getAbout())
            ;
            return sb.toString();
        }

        public Player getPlayer(String idPlayer) {
            if (playerStats.containsKey(idPlayer)) {
                Player player = new Player(playerStats.get(idPlayer));
                scoringPlays.forEach(map -> {
                    if (map.containsKey("goal")) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> goal = (Map<String, Object>) map.get("goal");
                        if (goal.containsKey("playerID") && goal.get("playerID").equals(idPlayer)) {
                            player.getListGoal().add(map);
                        }
                    }
                });
                return player;
            }
            return null;
        }
    }

    @Getter
    @Setter
    public static class Player {

        final private List<Map<String, Object>> listGoal = new ArrayList<>();

        final private Map<String, Object> stat;

        public Player(Map<String, Object> stat) {
            this.stat = stat;
        }

    }

}
