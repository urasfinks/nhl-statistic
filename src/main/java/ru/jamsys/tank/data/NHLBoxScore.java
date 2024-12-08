package ru.jamsys.tank.data;

import ru.jamsys.core.flat.util.Util;
import ru.jamsys.core.flat.util.UtilFileResource;
import ru.jamsys.core.flat.util.UtilJson;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class NHLBoxScore {

    public static String getUri(String gameId) throws Exception {
        return "/getNHLBoxScore?gameID=" + Util.urlEncode(gameId);
    }

    public static String getExample() throws IOException {
        return UtilFileResource.getAsString("example/getNHLBoxScore5.json");
    }

    public static List<Map<String, Object>> getScoringPlays(String json) throws Throwable {
        @SuppressWarnings("unchecked")
        Map<String, Object> parsed = UtilJson.toObject(json, Map.class);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> selector = (List<Map<String, Object>>) UtilJson.selector(parsed, "body.scoringPlays");
        return selector;
    }

    public static List<Map<String, Object>> getNewEventScoring(String jsonLast, String jsonCurrent) {
        List<Map<String, Object>> result = new ArrayList<>();
        return result;
    }

}
