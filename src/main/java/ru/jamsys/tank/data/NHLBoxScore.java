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

    public static String getExample() throws IOException {
        return UtilFileResource.getAsString("example/getNHLBoxScore4.json");
    }

    public static String getExample2() throws IOException {
        return UtilFileResource.getAsString("example/getNHLBoxScore5.json");
    }

    public static List<Map<String, Object>> getScoringPlays(String json) throws Throwable {
        @SuppressWarnings("unchecked")
        Map<String, Object> parsed = UtilJson.toObject(json, Map.class);
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
        set1.removeAll(list2);
        return new ArrayList<>(set1);
    }

    public static List<Map<String, Object>> getNewEventScoring(String jsonLast, String jsonCurrent) throws Throwable {
        List<Map<String, Object>> result = new ArrayList<>();
        List<Map<String, Object>> scoringPlaysLast = getScoringPlays(jsonLast);
        List<Map<String, Object>> scoringPlaysCurrent = getScoringPlays(jsonCurrent);
        List<String> listLast = scoringPlaysLast.stream().map(NHLBoxScore::hashObject).toList();
        List<String> listCurrent = scoringPlaysCurrent.stream().map(NHLBoxScore::hashObject).toList();
        List<String> difference = getDifference(listCurrent, listLast);
        scoringPlaysCurrent.forEach(map -> {
            if (difference.contains(hashObject(map))) {
                result.add(map);
            }
        });
        return result;
    }

}
