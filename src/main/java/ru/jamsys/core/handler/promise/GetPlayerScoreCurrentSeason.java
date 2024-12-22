package ru.jamsys.core.handler.promise;

import lombok.Getter;
import lombok.Setter;
import ru.jamsys.core.App;
import ru.jamsys.core.component.ServicePromise;
import ru.jamsys.core.flat.util.tank.UtilTank01;
import ru.jamsys.core.jt.JTPrevGoal;
import ru.jamsys.core.promise.Promise;
import ru.jamsys.core.promise.PromiseGenerator;
import ru.jamsys.core.resource.http.HttpResource;
import ru.jamsys.core.resource.http.client.HttpResponse;
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

    private String idTeam;

    private String idGame;

    private String idPlayer;

    private List<String> lisIdGameInSeason = new ArrayList<>();

    private AtomicInteger countGoal = new AtomicInteger(0);

    public GetPlayerScoreCurrentSeason(String idGame, String idPlayer) {
        this.idGame = idGame;
        this.idPlayer = idPlayer;
    }

    @Override
    public Promise generate() {
        return App.get(ServicePromise.class).get(getClass().getSimpleName(), 6000L)
                .thenWithResource("select", JdbcResource.class, (_, _, promise, jdbcResource) -> {
                    List<Map<String, Object>> execute = jdbcResource.execute(new JdbcRequest(JTPrevGoal.SELECT)
                            .addArg("id_game", idGame)
                            .addArg("id_player", idPlayer)
                            .setDebug(false)
                    );
                    if (!execute.isEmpty()) {
                        promise.setRepositoryMap("score", execute.getFirst().get("prev_goal").toString());
                        promise.skipAllStep("already cache");
                    }
                })
                .then("getPlayerList", new Tank01CacheRequest(NHLPlayerList::getUri).generate())
                .then("getIdTeam", (_, _, promise) -> {
                    Tank01Response response = promise.
                            getRepositoryMapClass(Promise.class, "getPlayerList").
                            getRepositoryMapClass(Tank01Response.class);
                    Map<String, Object> player = NHLPlayerList.findById(idPlayer, response.getData());
                    if (player == null || player.isEmpty()) {
                        throw new RuntimeException("Not found player");
                    }
                    idTeam = player.get("teamID").toString();
                })
                .then("requestGameInSeason", new Tank01CacheRequest(() -> NHLTeamSchedule.getUri(
                        idTeam,
                        NHLTeamSchedule.getActiveSeasonOrNext() + ""
                )).generate())
                .then("parseGameInSeason", (_, _, promise) -> {
                    Tank01Response response = promise
                            .getRepositoryMapClass(Promise.class, "requestGameInSeason")
                            .getRepositoryMapClass(Tank01Response.class);
                    NHLTeamSchedule.parseGameRaw(response.getData()).forEach(
                            map -> lisIdGameInSeason.add(map.get("gameID").toString())
                    );
                    //Вычитаем текущий матч так как надо считать кол-во голов до матча
                    lisIdGameInSeason.remove(idGame);
                })
                .thenWithResource(
                        "getBoxScore",
                        HttpResource.class,
                        (_, _, promise, httpResource) -> {
                            HttpResponse response = UtilTank01.request(
                                    httpResource,
                                    promise,
                                    _ -> NHLGamesForPlayer.getUri(idPlayer));

                            NHLGamesForPlayer.getOnlyGoalsFilterSeason(
                                    response.getBody(),
                                    lisIdGameInSeason
                            ).forEach((_, countGoal) -> this.countGoal.addAndGet(countGoal));
                            promise.setRepositoryMap("score", String.valueOf(this.countGoal.get()));
                        })
                .thenWithResource(
                        "insert",
                        JdbcResource.class,
                        (_, _, _, jdbcResource)
                                -> jdbcResource.execute(new JdbcRequest(JTPrevGoal.INSERT)
                                .addArg("id_game", idGame)
                                .addArg("id_player", idPlayer)
                                .addArg("prev_goal", countGoal.get())
                                .setDebug(false)
                        ))
                .setDebug(true)
                ;
    }

}
