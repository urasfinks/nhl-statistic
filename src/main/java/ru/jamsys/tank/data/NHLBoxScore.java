package ru.jamsys.tank.data;

import ru.jamsys.core.flat.util.UtilFileResource;
import ru.jamsys.core.flat.util.UtilJson;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

public class NHLBoxScore {

    public static String getExample() throws IOException {
        return UtilFileResource.getAsString("example/getNHLBoxScore5.json");
    }

    public static Map<String, Integer> getPlayerStat(String json) throws Throwable {
        @SuppressWarnings("unchecked")
        Map<String, Object> parsed = UtilJson.toObject(json, Map.class);
        @SuppressWarnings("unchecked")
        Map<String, Map<String, Object>> selector = (Map<String, Map<String, Object>>) UtilJson.selector(parsed, "body.playerStats");
        Map<String, Integer> goals = new LinkedHashMap<>();
        selector.forEach((s, object) -> {
            if (object.containsKey("goals")) {
                goals.put(s, Integer.parseInt(object.get("goals").toString()));
            }
        });
        return goals;
    }

    public static Map<String, Integer> getDiff(Map<String, Integer> last, Map<String, Integer> current) {
        Map<String, Integer> result = new LinkedHashMap<>();
        current.forEach((s, integer) -> {
            if (last.containsKey(s) && last.get(s) < integer) {
                result.put(s, integer - last.get(s));
            }
        });
        return result;
    }

}
