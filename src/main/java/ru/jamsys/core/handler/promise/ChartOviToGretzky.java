package ru.jamsys.core.handler.promise;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import ru.jamsys.core.App;
import ru.jamsys.core.component.ServicePromise;
import ru.jamsys.core.flat.util.UtilNHL;
import ru.jamsys.core.promise.Promise;
import ru.jamsys.core.promise.PromiseGenerator;
import ru.jamsys.tank.data.NHLGamesForPlayer;

import java.util.Map;

@Getter
@Setter
@ToString
public class ChartOviToGretzky implements PromiseGenerator {

    public static class Context {

    }

    @Override
    public Promise generate() {
        return App.get(ServicePromise.class).get(getClass().getSimpleName(), 60_000L)
                .extension(promise -> promise.setRepositoryMapClass(Context.class, new Context()))
                .then("requestGamesForPlayer", new Tank01Request(() -> NHLGamesForPlayer.getUri(
                        UtilNHL.getOvi().getPlayerID()
                )).generate())
                .then("init", (_, _, promise) -> {
                    Tank01Request tank01Request = promise
                            .getRepositoryMapClass(Promise.class, "requestGamesForPlayer")
                            .getRepositoryMapClass(Tank01Request.class);
                    Map<String, Integer> onlyGoalsFilter = NHLGamesForPlayer
                            .getOnlyGoalsFilter(tank01Request.getResponseData(), null);

                })
                .setDebug(false)
                ;
    }

}
