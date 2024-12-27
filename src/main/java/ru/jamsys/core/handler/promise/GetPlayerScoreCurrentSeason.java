package ru.jamsys.core.handler.promise;

import lombok.Getter;
import lombok.Setter;
import ru.jamsys.core.App;
import ru.jamsys.core.component.ServicePromise;
import ru.jamsys.core.jt.JTPrevGoal;
import ru.jamsys.core.promise.Promise;
import ru.jamsys.core.promise.PromiseGenerator;
import ru.jamsys.core.resource.jdbc.JdbcRequest;
import ru.jamsys.core.resource.jdbc.JdbcResource;
import ru.jamsys.tank.data.NHLGamesForPlayer;
import ru.jamsys.tank.data.NHLPlayerList;
import ru.jamsys.tank.data.NHLTeamSchedule;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

@Getter
@Setter
public class GetPlayerScoreCurrentSeason implements PromiseGenerator {

    private String idGame;

    private NHLPlayerList.Player player;

    private List<String> lisIdGameInSeason = new ArrayList<>();

    private AtomicInteger countGoal = new AtomicInteger(0);

    public GetPlayerScoreCurrentSeason(String idGame, NHLPlayerList.Player player) {
        this.idGame = idGame;
        this.player = player;
    }

    @Override
    public Promise generate() {
        return App.get(ServicePromise.class).get(getClass().getSimpleName(), 60_000L)
                .thenWithResource("select", JdbcResource.class, (_, _, promise, jdbcResource) -> {
                    List<Map<String, Object>> execute = jdbcResource.execute(new JdbcRequest(JTPrevGoal.SELECT)
                            .addArg("id_game", idGame)
                            .addArg("id_player", player.getPlayerID())
                            .setDebug(false)
                    );
                    if (!execute.isEmpty()) {
                        promise.setRepositoryMap("prev_goal", execute.getFirst().get("prev_goal").toString());
                        promise.skipAllStep("already cache");
                    }
                })
                .then("requestGameInSeason", new Tank01Request(() -> NHLTeamSchedule.getUri(
                        player.getTeamID(),
                        NHLTeamSchedule.getActiveSeasonOrNext() + ""
                )).generate())
                .then("parseGameInSeason", (_, _, promise) -> {
                    Tank01Request response = promise
                            .getRepositoryMapClass(Promise.class, "requestGameInSeason")
                            .getRepositoryMapClass(Tank01Request.class);
                    NHLTeamSchedule.parseGameRaw(response.getResponseData()).forEach(
                            map -> lisIdGameInSeason.add(map.get("gameID").toString())
                    );
                    //Вычитаем текущий матч так как надо считать кол-во голов до матча
                    lisIdGameInSeason.remove(idGame);
                })
                .then("getBoxScore", (_, _, promise) -> {
                    Tank01Request tank01Request = new Tank01Request(() -> NHLGamesForPlayer.getUri(player.getPlayerID()));
                    tank01Request.setNeedRequestApi(true);
                    tank01Request.generate().run().await(50_000L);
                    NHLGamesForPlayer.getOnlyGoalsFilter(
                            tank01Request.getResponseData(),
                            lisIdGameInSeason
                    ).forEach((_, countGoal) -> this.countGoal.addAndGet(countGoal));
                    promise.setRepositoryMap("prev_goal", String.valueOf(this.countGoal.get()));
                })
                .thenWithResource(
                        "insert",
                        JdbcResource.class,
                        (_, _, _, jdbcResource)
                                -> jdbcResource.execute(new JdbcRequest(JTPrevGoal.INSERT)
                                .addArg("id_game", idGame)
                                .addArg("id_player", player.getPlayerID())
                                .addArg("prev_goal", countGoal.get())
                                .setDebug(false)
                        ))
                .setDebug(true)
                ;
    }

}
