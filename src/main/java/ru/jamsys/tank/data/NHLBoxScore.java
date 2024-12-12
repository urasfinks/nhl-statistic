package ru.jamsys.tank.data;

import ru.jamsys.core.App;
import ru.jamsys.core.extension.builder.HashMapBuilder;
import ru.jamsys.core.flat.util.Util;
import ru.jamsys.core.flat.util.UtilFileResource;
import ru.jamsys.core.flat.util.UtilJson;

import java.io.IOException;
import java.util.*;

public class NHLBoxScore {

    public static String getUri(String gameId) throws Exception {
        return "/getNHLBoxScore?gameID=" + Util.urlEncode(gameId);
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

    public static String getExampleError() throws IOException {
        return UtilFileResource.getAsString("example/getNHLBoxScore_error.json");
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

    public static Map<String, List<Map<String, Object>>> getScoringPlaysMap(String json) throws Throwable {
        Map<String, List<Map<String, Object>>> result = new HashMap<>();
        if (json == null || json.isEmpty()) { //Так как в БД может быть ничего
            return result;
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> parsed = UtilJson.toObject(json, Map.class);
        if (parsed.containsKey("error")) {
            throw new RuntimeException(parsed.get("error").toString());
        }
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> selector = (List<Map<String, Object>>) UtilJson.selector(parsed, "body.scoringPlays");

        selector.forEach(map -> {
            @SuppressWarnings("unchecked")
            Map<String, Object> o = (Map<String, Object>) map.get("goal");
            result.computeIfAbsent(o.get("playerID").toString(), _ -> new ArrayList<>()).add(
                    new HashMapBuilder<String, Object>()
                            .append("period", map.get("period"))
                            .append("scoreTime", map.get("scoreTime"))
            );
        });
        return result;
    }

    public static String hashObject(Map<String, Object> stringObjectMap) {
        try {
            return Util.getHash(stringObjectMap.toString(), "md5");
        } catch (Throwable e) {
            App.error(e);
        }
        return null;
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

    public static List<Map<String, Object>> getNewEventScoring(String jsonLast, String jsonCurrent) throws Throwable {
        List<Map<String, Object>> scoringPlaysLast = getScoringPlays(jsonLast);
        List<Map<String, Object>> scoringPlaysCurrent = getScoringPlays(jsonCurrent);

        List<String> listLast = scoringPlaysLast.stream().map(NHLBoxScore::hashObject).toList();
        List<String> listCurrent = new ArrayList<>(scoringPlaysCurrent.stream().map(NHLBoxScore::hashObject).toList());

        for (int i = listLast.size() - 1; i >= 0; i--) {
            if (!listCurrent.isEmpty() && listCurrent.getLast().equals(listLast.get(i))) {
                listCurrent.removeLast();
                scoringPlaysCurrent.removeLast();
            } else {
                break;
            }
        }
        return scoringPlaysCurrent;
    }

    public static Map<String, List<Map<String, Object>>> getNewEventScoringByPlayer(String last, String current) throws Throwable {

        Map<String, List<Map<String, Object>>> result = new HashMap<>();

        Map<String, List<Map<String, Object>>> scoringPlaysLast = getScoringPlaysMap(last);
        Map<String, List<Map<String, Object>>> scoringPlaysCurrent = getScoringPlaysMap(current);

        Set<String> idPlayers = new HashSet<>();
        idPlayers.addAll(scoringPlaysLast.keySet());
        idPlayers.addAll(scoringPlaysCurrent.keySet());
        idPlayers.forEach(idPlayer -> {
            List<Map<String, Object>> newEventScoringByPlayer = getNewEventScoringByPlayer(
                    scoringPlaysLast.getOrDefault(idPlayer, new ArrayList<>()),
                    scoringPlaysCurrent.getOrDefault(idPlayer, new ArrayList<>())
            );
            if (!newEventScoringByPlayer.isEmpty()) {
                result.put(idPlayer, newEventScoringByPlayer);
            }
        });
        return result;
    }

    public static List<Map<String, Object>> getNewEventScoringByPlayer(List<Map<String, Object>> last, List<Map<String, Object>> current) {
        List<Map<String, Object>> res = new ArrayList<>();

        last.reversed().forEach(map -> {
            for (int i = current.size() - 1; i >= 0; i--) {
                if (current.get(i).containsKey("findInLast")) {
                    continue;
                }
                if (map.get("scoreTime").equals(current.get(i).get("scoreTime"))) {
                    current.get(i).put("findInLast", true);
                    map.put("findInCurrent", true);
                    break;
                }
            }
        });
        current.reversed().forEach(map -> {
            if (!map.containsKey("findInLast")) {
                for (int i = last.size() - 1; i >= 0; i--) {
                    if (last.get(i).containsKey("findInCurrent")) {
                        continue;
                    }
                    if (map.get("scoreTime").equals(last.get(i).get("scoreTime"))) {
                        last.get(i).put("findInCurrent", true);
                        map.put("findInLast", true);
                        break;
                    }
                }
            }
        });

        last.forEach(map -> {
            if (!map.containsKey("findInCurrent")) {
                map.put("type", "cancel");
                res.add(map);
            }
        });
        current.forEach(map -> {
            if (!map.containsKey("findInLast")) {
                map.put("type", "goal");
                res.add(map);
            }
        });
        return res;
    }

}
