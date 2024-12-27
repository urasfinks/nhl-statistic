package ru.jamsys.core.handler.promise;

import lombok.Getter;
import lombok.Setter;
import ru.jamsys.core.App;
import ru.jamsys.core.component.ServicePromise;
import ru.jamsys.core.flat.util.UtilDate;
import ru.jamsys.core.promise.Promise;
import ru.jamsys.core.promise.PromiseGenerator;
import ru.jamsys.tank.data.NHLGamesForPlayer;
import ru.jamsys.tank.data.NHLPlayerList;
import ru.jamsys.tank.data.NHLTeamSchedule;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

@Getter
@Setter
public class PlayerStatistic implements PromiseGenerator {

    private String date;

    private final AtomicInteger scoreLastSeasons;

    private final List<String> lisIdGameInSeason = new ArrayList<>();

    private final NHLPlayerList.Player player;

    private String gameToday;

    public PlayerStatistic(NHLPlayerList.Player player, int scoreLastSeason) {
        this.player = player;
        this.scoreLastSeasons = new AtomicInteger(scoreLastSeason);
    }

    @Override
    public Promise generate() {
        return App.get(ServicePromise.class).get(getClass().getSimpleName(), 60_000L)
                .extension(promise -> promise.setRepositoryMapClass(PlayerStatistic.class, this)) // Просто для отладки
                .then("init", (_, _, promise) -> {
                    promise.getRepositoryMapClass(PlayerStatistic.class);
                    setDate(UtilDate.get("dd/MM/yyyy"));
                })
                // На текущий момент мы не знаем конкретную игру, поэтому получаем всё
                .then("requestGameInSeason", new Tank01Request(() -> NHLTeamSchedule.getUri(
                        getPlayer().getTeamID(),
                        NHLTeamSchedule.getActiveSeasonOrNext() + ""
                )).generate())
                .then("parseGameInSeason", (_, _, promise) -> {
                    promise.getRepositoryMapClass(PlayerStatistic.class);
                    Tank01Request response = promise
                            .getRepositoryMapClass(Promise.class, "requestGameInSeason")
                            .getRepositoryMapClass(Tank01Request.class);

                    List<Map<String, Object>> listGame = NHLTeamSchedule.parseGameRaw(response.getResponseData());
                    listGame.forEach(map -> getLisIdGameInSeason().add(map.get("gameID").toString()));

                    String gameToday = NHLTeamSchedule.getGameToday(listGame, NHLTeamSchedule.getCurrentDateEpoch());
                    if (gameToday != null && !gameToday.isEmpty()) {
                        setGameToday(gameToday);
                        getLisIdGameInSeason().remove(gameToday);
                    }
                })
                .then("requestGameByPlayer", new Tank01Request(() -> NHLGamesForPlayer.getUri(getPlayer().getPlayerID())).generate())
                .then("parseGameByPlayer", (_, _, promise) -> {
                    promise.getRepositoryMapClass(PlayerStatistic.class);
                    Tank01Request response = promise
                            .getRepositoryMapClass(Promise.class, "requestGameByPlayer")
                            .getRepositoryMapClass(Tank01Request.class);

                    NHLGamesForPlayer.getOnlyGoalsFilter(
                            response.getResponseData(),
                            getLisIdGameInSeason()
                    ).forEach((_, countGoal) -> getScoreLastSeasons().addAndGet(countGoal));
                })
                .setDebug(true)
                ;
    }

}
