package ru.jamsys.tank.data;

import ru.jamsys.core.flat.util.UtilFileResource;
import ru.jamsys.core.flat.util.UtilJson;
import ru.jamsys.core.flat.util.tank.UtilTank01;
import ru.jamsys.core.promise.Promise;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class NHLTeamSchedule {

    public static void promiseExtensionGetTeamSchedule(Promise promiseSource) {
        UtilTank01.cacheRequest(promiseSource, promise -> "/getNHLTeamSchedule?teamID="
                + promise.getRepositoryMap(String.class, "teamId")
                + "&season="
                + promise.getRepositoryMap(String.class, "season", "2025"));
    }

    public static String getExample() throws IOException {
        return UtilFileResource.getAsString("example/getNHLTeamSchedule.json");
    }

    public static List<Map<String, Object>> findGame(String json) throws Throwable {
        @SuppressWarnings("unchecked")
        Map<String, Object> parsed = UtilJson.toObject(json, Map.class);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> selector = (List<Map<String, Object>>) UtilJson.selector(parsed, "body.schedule");
        List<Map<String, Object>> result = new ArrayList<>();
        selector.forEach(stringObjectMap -> {
            if (stringObjectMap.containsKey("gameStatus") && stringObjectMap.get("gameStatus").equals("Scheduled")) {
                result.add(stringObjectMap);
            }
        });
        return result;
    }

}
