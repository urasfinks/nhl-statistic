package ru.jamsys.tank.data;

import ru.jamsys.core.flat.util.UtilFileResource;
import ru.jamsys.core.flat.util.UtilJson;
import ru.jamsys.core.flat.util.tank.UtilTank01;
import ru.jamsys.core.promise.Promise;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NHLTeams {

    public static void promiseExtensionGetTeams(Promise promiseSource) {
        UtilTank01.cacheRequest(promiseSource, _ -> "getNHLTeams?teamStats=true&topPerformers=true&includeDefunctTeams=false");
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
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> selector = (List<Map<String, Object>>) UtilJson.selector(parsed, "body");
        Map<String, Object> result = new HashMap<>();
        selector.forEach(stringObjectMap -> {
            result.put(
                    stringObjectMap.get("teamAbv").toString(),
                    stringObjectMap.get("teamCity") + " " + stringObjectMap.get("teamName")
            );
        });
        return result;
    }
}
