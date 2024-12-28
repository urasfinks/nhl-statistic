package ru.jamsys.core.handler.promise;

import lombok.Getter;
import lombok.Setter;
import ru.jamsys.core.App;
import ru.jamsys.core.component.ServicePromise;
import ru.jamsys.core.flat.util.UtilNHL;
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
public class ScorePlayerCurrentSeasonBeforeGame implements PromiseGenerator {

    private String idGame;

    private NHLPlayerList.Player player;

    private List<String> lisIdGameInSeason = new ArrayList<>();

    private AtomicInteger countGoal = new AtomicInteger(0);

    public ScorePlayerCurrentSeasonBeforeGame(NHLPlayerList.Player player, String idGame) {
        this.idGame = idGame;
        this.player = player;
    }

    @Override
    public Promise generate() {
        return App.get(ServicePromise.class).get(getClass().getSimpleName(), 60_000L)
                .thenWithResource("select", JdbcResource.class, (_, _, promise, jdbcResource) -> {
                    List<Map<String, Object>> execute = jdbcResource.execute(new JdbcRequest(JTPrevGoal.SELECT)
                            .addArg("id_game", getIdGame())
                            .addArg("id_player", getPlayer().getPlayerID())
                            .setDebug(false)
                    );
                    if (!execute.isEmpty()) {
                        promise.setRepositoryMap("prev_goal", execute.getFirst().get("prev_goal").toString());
                        promise.skipAllStep("already cache");
                    }
                })
                .then("requestGameInSeason", new Tank01Request(() -> NHLTeamSchedule.getUri(
                        getPlayer().getTeamID(),
                        UtilNHL.getActiveSeasonOrNext() + ""
                )).generate())
                .then("parseGameInSeason", (_, _, promise) -> {
                    Tank01Request response = promise
                            .getRepositoryMapClass(Promise.class, "requestGameInSeason")
                            .getRepositoryMapClass(Tank01Request.class);
                    NHLTeamSchedule.parseGameRaw(response.getResponseData()).forEach(
                            map -> getLisIdGameInSeason().add(map.get("gameID").toString())
                    );
                    //Вычитаем текущий матч так как надо считать кол-во голов до матча
                    getLisIdGameInSeason().remove(getIdGame());
                })
                .then("requestGamesForPlayer", new Tank01Request(() -> NHLGamesForPlayer.getUri(
                        getPlayer().getPlayerID()
                )).generate())
                .then("parseGamesForPlayer", (_, _, promise) -> {
                    Tank01Request tank01Request = promise
                            .getRepositoryMapClass(Promise.class, "requestGamesForPlayer")
                            .getRepositoryMapClass(Tank01Request.class);
                    NHLGamesForPlayer.getOnlyGoalsFilter(
                            tank01Request.getResponseData(),
                            getLisIdGameInSeason()
                    ).forEach((_, countGoal) -> getCountGoal().addAndGet(countGoal));
                    promise.setRepositoryMap("prev_goal", String.valueOf(getCountGoal().get()));
                })
                .thenWithResource(
                        "insert",
                        JdbcResource.class,
                        (_, _, _, jdbcResource)
                                -> jdbcResource.execute(new JdbcRequest(JTPrevGoal.INSERT)
                                .addArg("id_game", getIdGame())
                                .addArg("id_player", getPlayer().getPlayerID())
                                .addArg("prev_goal", getCountGoal().get())
                                .setDebug(false)
                        ))
                .setDebug(true)
                ;
    }

}
