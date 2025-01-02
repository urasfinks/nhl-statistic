package ru.jamsys.tank.data;

import lombok.Getter;
import lombok.Setter;
import ru.jamsys.core.flat.util.UtilFileResource;
import ru.jamsys.core.flat.util.UtilJson;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class NHLTeams {

    public static String getUri() {
        return "getNHLTeams?teamStats=true&topPerformers=true&includeDefunctTeams=false";
    }

    public static String getExample() throws IOException {
        return UtilFileResource.getAsString("example/getNHLTeams.json");
    }

    public static Instance teams;

    static {
        try {
            teams = new Instance(getExample());
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    @Getter
    public static class Instance {

        private final List<Map<String, Object>> list;

        public Instance(String json) throws Throwable {
            @SuppressWarnings("unchecked")
            Map<String, Object> parsed = UtilJson.toObject(json, Map.class);
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> selector = (List<Map<String, Object>>) UtilJson.selector(parsed, "body");
            this.list = selector;
        }

        public Team getById(String idTeam) {
            for (Map<String, Object> team : list) {
                if (team.getOrDefault("teamID", "").equals(idTeam)) {
                    return new Team(team);
                }
            }
            return null;
        }

        public Team getByAbv(String idTeam) {
            for (Map<String, Object> team : list) {
                if (team.getOrDefault("teamAbv", "").equals(idTeam)) {
                    return new Team(team);
                }
            }
            return null;
        }

        public List<Team> getListTeam() {
            List<Team> result = new ArrayList<>();
            list.forEach(map -> result.add(new Team(map)));
            return result;
        }

    }

    @Getter
    @Setter
    public static class Team {

        final Map<String, Object> data;

        public Team(Map<String, Object> data) {
            this.data = data;
        }

        public String getAbout() {
            return data.getOrDefault("teamCity", "--")
                    + " "
                    + data.getOrDefault("teamName", "--")
                    + " ("
                    + data.getOrDefault("teamAbv", "--")
                    + ")";
        }

        public String getAbv() {
            return data.getOrDefault("teamAbv", "--").toString();
        }

    }
}
