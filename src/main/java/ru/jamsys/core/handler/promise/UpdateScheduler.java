package ru.jamsys.core.handler.promise;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import ru.jamsys.core.App;
import ru.jamsys.core.component.ServicePromise;
import ru.jamsys.core.flat.util.Util;
import ru.jamsys.core.flat.util.UtilJson;
import ru.jamsys.core.flat.util.UtilListSort;
import ru.jamsys.core.flat.util.UtilNHL;
import ru.jamsys.core.jt.JTPlayerSubscriber;
import ru.jamsys.core.jt.JTTeamScheduler;
import ru.jamsys.core.promise.Promise;
import ru.jamsys.core.promise.PromiseGenerator;
import ru.jamsys.core.resource.jdbc.JdbcRequest;
import ru.jamsys.core.resource.jdbc.JdbcResource;
import ru.jamsys.tank.data.NHLPlayerList;
import ru.jamsys.tank.data.NHLTeamSchedule;

import java.math.BigDecimal;
import java.util.*;

@Getter
@Setter
@Accessors(chain = true)
public class UpdateScheduler implements PromiseGenerator {

    private PlayerSubscribers playerSubscribers = new PlayerSubscribers();

    private Map<String, List<NHLTeamSchedule.Game>> teamsScheduledGame = new HashMap<>(); // key - idTeam; value - list game

    private boolean alwaysRequestApi;

    private Map<String, List<NHLTeamSchedule.Game>> teamsGameInstance = new LinkedHashMap<>(); // key - idTeam

    public UpdateScheduler(boolean alwaysRequestApi) {
        this.alwaysRequestApi = alwaysRequestApi;
    }

    @Getter
    @Setter
    public static class PlayerSubscribers {

        final List<JTPlayerSubscriber.Row> list = new ArrayList<>();

        final List<NHLPlayerList.Player> listPlayer = new ArrayList<>();

        public List<NHLPlayerList.Player> getListPlayer() {
            if (listPlayer.isEmpty()) {
                Set<String> setIdPlayers = new HashSet<>();
                list.forEach(row -> setIdPlayers.add(row.getIdPlayer().toString()));
                setIdPlayers.forEach(idPlayer -> {
                    NHLPlayerList.Player player = NHLPlayerList.findByIdStatic(idPlayer);
                    if (player != null) {
                        listPlayer.add(player);
                    }
                });
            }
            return listPlayer;
        }

        public List<String> getListIdTeam() {
            Set<String> result = new HashSet<>();
            getListPlayer().forEach(player -> result.add(player.getTeamID()));
            return result.stream().toList();
        }

    }

    @Override
    public Promise generate() {
        return App.get(ServicePromise.class).get(getClass().getSimpleName(), 600_000L)
                .extension(promise -> promise.setRepositoryMapClass(UpdateScheduler.class, this))
                .then("bug01", (_, _, _) -> {})
                .thenWithResource(
                        "select",
                        JdbcResource.class,
                        (_, _, promise, jdbcResource) -> {
                            UpdateScheduler updateScheduler = promise.getRepositoryMapClass(UpdateScheduler.class);
                            updateScheduler
                                    .getPlayerSubscribers()
                                    .getList()
                                    .addAll(jdbcResource.execute(
                                            new JdbcRequest(JTPlayerSubscriber.SELECT),
                                            JTPlayerSubscriber.Row.class
                                    ));
                        }
                )
                .then("getTeamSchedule", (run, _, promise) -> {
                    UpdateScheduler updateScheduler = promise.getRepositoryMapClass(UpdateScheduler.class);
                    for (String idTeam : updateScheduler.getPlayerSubscribers().getListIdTeam()) {
                        if (!run.get()) {
                            return;
                        }
                        Tank01Request tank01Request = new Tank01Request(() -> NHLTeamSchedule.getUri(
                                idTeam,
                                UtilNHL.getActiveSeasonOrNext() + "")
                        )
                                .setAlwaysRequestApi(updateScheduler.isAlwaysRequestApi());
                        Promise req = tank01Request.generate().run().await(10_000L);
                        if (req.isException()) {
                            throw req.getExceptionSource();
                        }
                        List<NHLTeamSchedule.Game> listGameInstance = teamsGameInstance
                                .computeIfAbsent(idTeam, _ -> new ArrayList<>());
                        listGameInstance
                                .addAll(new NHLTeamSchedule.Instance(tank01Request.getResponseData())
                                        .initAlreadyGame()
                                        .getFutureGame()
                                        .sort(UtilListSort.Type.ASC)
                                        .getListGameInstance()
                                );
                        listGameInstance.forEach(gameInstance -> updateScheduler
                                .getTeamsScheduledGame()
                                .computeIfAbsent(idTeam, _ -> new ArrayList<>())
                                .add(gameInstance)
                        );
                    }
                    if (updateScheduler.getTeamsScheduledGame().isEmpty()) {
                        promise.skipAllStep("UpdateScheduler scheduler is empty");
                    }
                })
                .thenWithResource(
                        "insert",
                        JdbcResource.class,
                        (_, _, promise, jdbcResource) -> {
                            UpdateScheduler updateScheduler = promise.getRepositoryMapClass(UpdateScheduler.class);
                            List<JTTeamScheduler.Row> execute = jdbcResource.execute(new JdbcRequest(JTTeamScheduler.SELECT), JTTeamScheduler.Row.class);
                            Set<String> alreadySchedule = new HashSet<>();
                            execute.forEach(row -> alreadySchedule.add(row.getIdTeam() + row.getIdGame()));
                            JdbcRequest jdbcRequest = new JdbcRequest(JTTeamScheduler.INSERT).setBatchMaybeEmpty(false);
                            updateScheduler
                                    .getTeamsScheduledGame()
                                    .forEach((idTeam, games) -> games.forEach(game -> {
                                                if (!alreadySchedule.contains(idTeam + game.getId())) {
                                                    jdbcRequest
                                                            .addArg("id_team", idTeam)
                                                            .addArg("id_game", game.getId())
                                                            .addArg("time_game_start", new BigDecimal(
                                                                    game.getData().get("gameTime_epoch").toString()
                                                            ).longValue() * 1000)
                                                            .addArg("game_about", game.getGameAbout())
                                                            .addArg("json", UtilJson.toStringPretty(game.getData(), "{}"));
                                                    Util.logConsoleJson(jdbcRequest.getListArgs().getLast());
                                                    jdbcRequest.nextBatch();
                                                }
                                            })
                                    );
                            //Util.logConsoleJson(jdbcRequest.getListArgs());
                            jdbcResource.execute(jdbcRequest);
                        }
                )
                ;
    }


}
