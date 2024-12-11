package ru.jamsys.tank.data;

import ru.jamsys.core.App;
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

    public static String hashObject(Map<String, Object> stringObjectMap) {
        try {
            return Util.getHash(stringObjectMap.toString(), "md5");
        } catch (Throwable e) {
            App.error(e);
        }
        return null;
    }

    public static <T> List<T> getDifference(List<T> list1, List<T> list2) {
        Set<T> set1 = new HashSet<>(list1);
        list2.forEach(set1::remove);
        return new ArrayList<>(set1);
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

}
