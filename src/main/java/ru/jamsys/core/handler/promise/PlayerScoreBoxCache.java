package ru.jamsys.core.handler.promise;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;
import ru.jamsys.core.App;
import ru.jamsys.core.component.ServicePromise;
import ru.jamsys.core.extension.exception.ForwardException;
import ru.jamsys.core.promise.Promise;
import ru.jamsys.core.promise.PromiseGenerator;
import ru.jamsys.tank.data.NHLBoxScore;
import ru.jamsys.tank.data.NHLPlayerList;

import java.util.Map;

// Получаем кеш статистики игрока по игре, если кеша нет - будет пустой объект с 0 голов

@Getter
@Setter
@Accessors(chain = true)
@ToString
public class PlayerScoreBoxCache implements PromiseGenerator {

    private final NHLPlayerList.Player player;

    private final String idGame;

    private int goals = 0;

    private Map<String, Object> allStatistic = null;

    public PlayerScoreBoxCache(NHLPlayerList.Player player, String idGame) {
        this.player = player;
        this.idGame = idGame;
    }

    @Override
    public Promise generate() {
        if (idGame == null) {
            return null;
        }
        return App.get(ServicePromise.class).get(getClass().getSimpleName(), 60_000L)
                .extension(promise -> promise.setRepositoryMapClass(PlayerScoreBoxCache.class, this)) // Просто для отладки
                .then("getScoreBox", new RequestTank01(() -> NHLBoxScore.getUri(getIdGame())).setOnlyCache(true).generate())
                .then("parseScoreBox", (_, _, promise) -> {
                    promise.getRepositoryMapClass(PlayerScoreBoxCache.class); // Для отладки
                    RequestTank01 response = promise
                            .getRepositoryMapClass(Promise.class, "getScoreBox")
                            .getRepositoryMapClass(RequestTank01.class);
                    if (response.getResponseData() != null && !response.getResponseData().isEmpty()) {
                        try {
                            Map<String, Object> playerStat = NHLBoxScore.getPlayerStat(response.getResponseData(), getPlayer().getPlayerID());
                            setAllStatistic(playerStat);
                            if (playerStat.containsKey("goals")) {
                                setGoals(Integer.parseInt(playerStat.get("goals").toString()));
                            }
                        } catch (Throwable th) {
                            App.error(new ForwardException(th));
                        }
                    }
                })
                .setDebug(false)
                ;
    }

}
