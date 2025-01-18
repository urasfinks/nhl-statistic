package ru.jamsys.core.handler.promise;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import ru.jamsys.core.App;
import ru.jamsys.core.component.ServicePromise;
import ru.jamsys.core.jt.JTTeamScheduler;
import ru.jamsys.core.promise.Promise;
import ru.jamsys.core.promise.PromiseGenerator;
import ru.jamsys.core.resource.jdbc.JdbcRequest;
import ru.jamsys.core.resource.jdbc.JdbcResource;

@Getter
@Setter
@Accessors(chain = true)
public class RemoveScheduler implements PromiseGenerator {

    final public String idGame;

    public RemoveScheduler(String idGame) {
        this.idGame = idGame;
    }

    @Override
    public Promise generate() {
        return App.get(ServicePromise.class).get(getClass().getSimpleName(), 60_000L)
                .then("bug01", (_, _, _) -> {})
                .thenWithResource(
                        "unsubscribe",
                        JdbcResource.class,
                        (_, _, _, jdbcResource) -> jdbcResource.execute(new JdbcRequest(JTTeamScheduler.DELETE_IS_POSTPONED)
                                .addArg("id_game", idGame)
                        )
                );
    }


}
