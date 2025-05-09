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

// Получить статистику игрока по сезону, за исключением установленной игры

@Getter
@Setter
public class ScorePlayerCurrentSeasonBeforeGame implements PromiseGenerator {

    private String idGame;

    private NHLPlayerList.Player player;

    private NHLTeamSchedule.Instance allGameInSeason;

    private List<String> lisIdGameInSeason = new ArrayList<>();

    private final AtomicInteger countGoal = new AtomicInteger(0);

    public ScorePlayerCurrentSeasonBeforeGame(NHLPlayerList.Player player, String idGame) {
        this.idGame = idGame;
        this.player = player;
    }

    @Override
    public Promise generate() {
        return App.get(ServicePromise.class).get(getClass().getSimpleName(), 60_000L)
                .extension(promise -> promise.setRepositoryMapClass(ScorePlayerCurrentSeasonBeforeGame.class, this))
                .thenWithResource("select", JdbcResource.class, (_, _, promise, jdbcResource) -> {
                    List<Map<String, Object>> execute = jdbcResource.execute(new JdbcRequest(JTPrevGoal.SELECT)
                            .addArg("id_game", getIdGame())
                            .addArg("id_player", getPlayer().getPlayerID())
                            .setDebug(false)
                    );
                    if (!execute.isEmpty()) {
                        countGoal.set(Integer.parseInt(execute.getFirst().get("prev_goal").toString()));
                        promise.skipAllStep("already cache");
                    }
                })
                .then("requestGameInSeason", new RequestTank01(() -> NHLTeamSchedule.getUri(
                        getPlayer().getTeamID(),
                        UtilNHL.getActiveSeasonOrNext() + ""
                )).generate())
                .then("parseGameInSeason", (_, _, promise) -> {
                    RequestTank01 response = promise
                            .getRepositoryMapClass(Promise.class, "requestGameInSeason")
                            .getRepositoryMapClass(RequestTank01.class);
                    NHLTeamSchedule.Instance instance = new NHLTeamSchedule.Instance(response.getResponseData());
                    setAllGameInSeason(instance);
                    getLisIdGameInSeason().addAll(instance.getIdGame());
                    //Вычитаем текущий матч так как надо считать кол-во голов до матча
                    getLisIdGameInSeason().remove(getIdGame());
                })
                .then("requestGamesForPlayer", new RequestTank01(() -> NHLGamesForPlayer.getUri(
                        getPlayer().getPlayerID()
                )).generate())
                .then("parseGamesForPlayer", (_, _, promise) -> {
                    RequestTank01 requestTank01 = promise
                            .getRepositoryMapClass(Promise.class, "requestGamesForPlayer")
                            .getRepositoryMapClass(RequestTank01.class);
                    NHLGamesForPlayer.getOnlyGoalsFilter(
                            requestTank01.getResponseData(),
                            getLisIdGameInSeason()
                    ).forEach((_, countGoal) -> getCountGoal().addAndGet(countGoal));
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
                .setDebug(false)
                ;
    }

}
