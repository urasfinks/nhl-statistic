package ru.jamsys.tank.data;

import ru.jamsys.core.flat.util.UtilFileResource;
import ru.jamsys.core.flat.util.UtilJson;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NHLTeams {

    public static String getUri() {
        return "getNHLTeams?teamStats=true&topPerformers=true&includeDefunctTeams=false";
    }

    public static String getExample() throws IOException {
        return UtilFileResource.getAsString("example/getNHLTeams.json");
    }

    public static Map<String, Object> getTeams() throws Throwable {
        return getTeams(getExample());
    }

    public static Map<String, Object> getTeams(String json) throws Throwable {
        @SuppressWarnings("unchecked")
        Map<String, Object> parsed = UtilJson.toObject(json, Map.class);
        if (parsed.containsKey("error")) {
            throw new RuntimeException(parsed.get("error").toString());
        }
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> selector = (List<Map<String, Object>>) UtilJson.selector(parsed, "body");
        Map<String, Object> result = new HashMap<>();
        selector.forEach(stringObjectMap -> result.put(
                stringObjectMap.get("teamAbv").toString(),
                stringObjectMap.get("teamCity") + " " + stringObjectMap.get("teamName")
        ));
        return result;
    }
}
