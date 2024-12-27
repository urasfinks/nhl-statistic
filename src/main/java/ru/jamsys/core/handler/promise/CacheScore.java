package ru.jamsys.core.handler.promise;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import ru.jamsys.core.App;
import ru.jamsys.core.component.ServicePromise;
import ru.jamsys.core.promise.Promise;
import ru.jamsys.core.promise.PromiseGenerator;
import ru.jamsys.tank.data.NHLBoxScore;
import ru.jamsys.tank.data.NHLPlayerList;

import java.util.Map;

@Getter
@Setter
@Accessors(chain = true)
public class CacheScore implements PromiseGenerator {

    private final NHLPlayerList.Player player;

    private final String idGame;

    private int goals = 0;

    public CacheScore(NHLPlayerList.Player player, String idGame) {
        this.player = player;
        this.idGame = idGame;
    }

    @Override
    public Promise generate() {
        return App.get(ServicePromise.class).get(getClass().getSimpleName(), 60_000L)
                .extension(promise -> promise.setRepositoryMapClass(CacheScore.class, this)) // Просто для отладки
                .then("getScoreBox", new Tank01Request(() -> NHLBoxScore.getUri(getIdGame())).setOnlyCache(true).generate())
                .then("parseScoreBox", (_, _, promise) -> {
                    Tank01Request response = promise
                            .getRepositoryMapClass(Promise.class, "getScoreBox")
                            .getRepositoryMapClass(Tank01Request.class);
                    if (response.getResponseData() != null && !response.getResponseData().isEmpty()) {
                        Map<String, Object> playerStat = NHLBoxScore.getPlayerStat(response.getResponseData(), getPlayer().getPlayerID());
                        if (playerStat.containsKey("goals")) {
                            setGoals(Integer.parseInt(playerStat.get("goals").toString()));
                        }
                    }
                })
                .setDebug(true)
                ;
    }

}
