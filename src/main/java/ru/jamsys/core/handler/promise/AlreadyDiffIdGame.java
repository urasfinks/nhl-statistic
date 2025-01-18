package ru.jamsys.core.handler.promise;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import ru.jamsys.core.App;
import ru.jamsys.core.component.ServicePromise;
import ru.jamsys.core.jt.JTGameDiff;
import ru.jamsys.core.promise.Promise;
import ru.jamsys.core.promise.PromiseGenerator;
import ru.jamsys.core.resource.jdbc.JdbcRequest;
import ru.jamsys.core.resource.jdbc.JdbcResource;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Getter
@Setter
@Accessors(chain = true)
public class AlreadyDiffIdGame implements PromiseGenerator {

    final public List<String> listIdGames;

    public List<String> listAlreadyDiffIdGame = new ArrayList<>();

    public AlreadyDiffIdGame(List<String> listIdGames) {
        this.listIdGames = listIdGames;
    }

    @Override
    public Promise generate() {
        return App.get(ServicePromise.class).get(getClass().getSimpleName(), 60_000L)
                .then("bug01", (_, _, _) -> {})
                .thenWithResource(
                        "select",
                        JdbcResource.class,
                        (_, _, _, jdbcResource) -> {
                            List<Map<String, Object>> execute = jdbcResource.execute(new JdbcRequest(JTGameDiff.SELECT_ALREADY)
                                    .addArg("id_game", listIdGames)
                            );
                            execute.forEach(map -> listAlreadyDiffIdGame.add(map.get("id_game").toString()));
                        }
                );
    }


}
