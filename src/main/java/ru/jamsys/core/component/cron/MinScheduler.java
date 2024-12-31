package ru.jamsys.core.component.cron;

import lombok.Getter;
import lombok.Setter;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import ru.jamsys.NhlStatisticApplication;
import ru.jamsys.core.App;
import ru.jamsys.core.component.ServicePromise;
import ru.jamsys.core.component.ServiceProperty;
import ru.jamsys.core.component.TelegramBotComponent;
import ru.jamsys.core.extension.UniqueClassName;
import ru.jamsys.core.extension.exception.ForwardException;
import ru.jamsys.core.flat.template.cron.release.Cron1m;
import ru.jamsys.core.flat.util.UtilDate;
import ru.jamsys.core.flat.util.UtilJson;
import ru.jamsys.core.flat.util.UtilNHL;
import ru.jamsys.core.flat.util.UtilRisc;
import ru.jamsys.core.handler.promise.SendNotificationGameEvent;
import ru.jamsys.core.handler.promise.SendNotificationGameEventOvi;
import ru.jamsys.core.handler.promise.Tank01Request;
import ru.jamsys.core.jt.JTGameDiff;
import ru.jamsys.core.jt.JTScheduler;
import ru.jamsys.core.promise.Promise;
import ru.jamsys.core.promise.PromiseGenerator;
import ru.jamsys.core.resource.jdbc.JdbcRequest;
import ru.jamsys.core.resource.jdbc.JdbcResource;
import ru.jamsys.tank.data.NHLBoxScore;
import ru.jamsys.tank.data.NHLPlayerList;
import ru.jamsys.telegram.GameEventData;

import java.util.*;

@SuppressWarnings("unused")
@Component
@Lazy
public class MinScheduler implements Cron1m, PromiseGenerator, UniqueClassName {

    private final ServicePromise servicePromise;
    private final ServiceProperty serviceProperty;

    private final TelegramBotComponent telegramBotComponent;

    public MinScheduler(
            ServicePromise servicePromise,
            TelegramBotComponent telegramBotComponent,
            ServiceProperty serviceProperty
    ) {
        this.servicePromise = servicePromise;
        this.telegramBotComponent = telegramBotComponent;
        this.serviceProperty = serviceProperty;
    }

    @Setter
    @Getter
    public static class Context {
        private List<String> activeGame = new ArrayList<>();
        private Map<String, String> currentData = new HashMap<>(); //key - idGame; value Api Response
        private Map<String, String> lastData = new HashMap<>();
        private Map<String, List<GameEventData>> playerEvent = new LinkedHashMap<>(); // key - idPlayer; value - template
        private Map<String, List<Integer>> playerSubscriber = new HashMap<>(); // key - idPlayer;
        private List<String> endGames = new ArrayList<>();
        private Map<String, String> mapIdPlayerGame = new HashMap<>(); // key - idPlayer; value - gameName
    }

    public void logToTelegram(String data) {
        if (!NhlStatisticApplication.startTelegramListener) {
            System.out.println("logToTelegram:" + data);
        }
        if (telegramBotComponent.getNhlStatisticsBot() != null) {
            telegramBotComponent.getNhlStatisticsBot().send(
                    -4739098379L,
                    //290029195,
                    data,
                    null
            );
        }
    }

    public Promise generate() {
        String mode = serviceProperty.get(String.class, "run.mode", "prod");
        if (mode.equals("test")) {
            return null;
        }
        if (telegramBotComponent.getNhlStatisticsBot() == null && NhlStatisticApplication.startTelegramListener) {
            return null;
        }
        return servicePromise.get(getClass().getSimpleName(), 50_000L)
                .thenWithResource("getActiveGame", JdbcResource.class, (_, _, promise, jdbcResource) -> {
                    Context context = promise.setRepositoryMapClass(Context.class, new Context());
                    jdbcResource.execute(
                            new JdbcRequest(JTScheduler.SELECT_ACTIVE_GAME).setDebug(false)
                    ).forEach(map -> context.getActiveGame().add(map.get("id_game").toString()));
                    // Если нет активных игр, нечего тут делать
                    if (context.getActiveGame().isEmpty()) {
                        promise.skipAllStep("active game is empty");
                    }
                })
                .then("getBoxScoreByActiveGame", (run, promiseTask, promise) -> {
                    Context context = promise.getRepositoryMapClass(Context.class);
                    for (String idGame : context.getActiveGame()) {
                        if (!run.get()) {
                            return;
                        }
                        String data;
                        if (NhlStatisticApplication.dummySchedulerBoxScore) {
                            data = NHLBoxScore.getExample6();
                        } else {
                            Tank01Request tank01Request = new Tank01Request(() -> NHLBoxScore.getUri(idGame))
                                    .setAlwaysRequestApi(true);
                            Promise req = tank01Request.generate().run().await(50_000L);
                            if (req.isException()) {
                                throw req.getExceptionSource();
                            }
                            data = tank01Request.getResponseData();
                        }
                        @SuppressWarnings("unchecked")
                        Map<String, Object> parsed = UtilJson.toObject(data, Map.class);
                        if (!parsed.containsKey("error")) {
                            context.getCurrentData().put(idGame, data);
                        }
                    }
                    // Если нет никаких данных, нет смысла ничего сверять
                    if (context.getCurrentData().isEmpty()) {
                        promise.skipAllStep("NHLBoxScore response by active game is empty");
                    }
                })
                .thenWithResource("getLastData", JdbcResource.class, (run, _, promise, jdbcResource) -> {
                    Context context = promise.getRepositoryMapClass(Context.class);
                    UtilRisc.forEach(run, context.getCurrentData(), (idGame, _) -> {
                        try {
                            List<Map<String, Object>> execute = jdbcResource.execute(new JdbcRequest(JTGameDiff.SELECT)
                                    .addArg("id_game", idGame)
                            );
                            context.getLastData().put(idGame, execute.isEmpty()
                                    ? null
                                    : execute.getFirst().get("scoring_plays").toString()
                            );
                        } catch (Throwable e) {
                            throw new ForwardException(e);
                        }
                    });
                })
                .then("getEvent", (atomicBoolean, _, promise) -> {
                    Context context = promise.getRepositoryMapClass(Context.class);
                    String curData = UtilDate.timestampFormatUTC(UtilDate.getTimestamp() + 3 * 60 * 60, "dd.MM.yyyy HH:mm");
                    UtilRisc.forEach(atomicBoolean, context.getCurrentData(), (idGame, data) -> {
                        try {
                            NHLBoxScore.Instance instance = new NHLBoxScore.Instance(data);
                            Map<String, List<GameEventData>> playerEvent = NHLBoxScore.getEvent(
                                    context.getLastData().get(idGame),
                                    data
                            );
                            if (context.getLastData().get(idGame) == null) {
                                logToTelegram("Start game: " + idGame);
                                instance.
                                        getPlayerStats()
                                        .forEach(
                                                (idPlayer, _) -> {
                                                    NHLBoxScore.Player player = instance.getPlayer(idPlayer);
                                                    playerEvent
                                                            .computeIfAbsent(idPlayer, _ -> new ArrayList<>())
                                                            .add(new GameEventData(
                                                                            GameEventData.Action.START_GAME,
                                                                            instance.getAboutGame(),
                                                                            instance.getScoreGame(),
                                                                            player.getLongName(),
                                                                            curData
                                                                    )
                                                            );
                                                }
                                        );
                            }
                            if (NHLBoxScore.isFinish(data)) {
                                logToTelegram("Finish game: " + idGame);
                                instance.
                                        getPlayerStats()
                                        .forEach(
                                                (idPlayer, _) -> {
                                                    NHLBoxScore.Player player = instance.getPlayer(idPlayer);
                                                    playerEvent
                                                            .computeIfAbsent(idPlayer, _ -> new ArrayList<>())
                                                            .add(new GameEventData(
                                                                            GameEventData.Action.FINISH_GAME,
                                                                            instance.getAboutGame(),
                                                                            instance.getScoreGame(),
                                                                            player.getLongName(),
                                                                            curData
                                                                    )
                                                                            .setScoredGoal(player.getGoals())
                                                                            .setScoredAssists(player.getScoredAssists())
                                                                            .setScoredShots(player.getScoredShots())
                                                                            .setScoredAssists(player.getScoredAssists())
                                                                            .setScoredHits(player.getScoredHits())
                                                                            .setScoredPenaltiesInMinutes(player.getScoredPenaltiesInMinutes())
                                                                            .setScoredTimeOnIce(player.getScoredTimeOnIce())
                                                            );
                                                }
                                        );

                                context.getEndGames().add(idGame);
                            }

                            playerEvent.forEach((idPlayer, listGameEventData) -> context
                                    .getMapIdPlayerGame()
                                    .put(idPlayer, idGame)
                            );
                            context.getPlayerEvent().putAll(playerEvent);
                            //logToTelegram(idGame + ":" + UtilJson.toStringPretty(context.getEvent(), "{}"));
                        } catch (Throwable e) {
                            throw new ForwardException(e);
                        }
                    });
                    // Если некому рассылать события, просто сохраним данные в БД
                    if (context.getPlayerEvent().isEmpty()) {
                        promise.goTo("saveData");
                    }
                })
                .thenWithResource("selectSubscribers", JdbcResource.class, (_, _, promise, jdbcResource) -> {
                    Context context = promise.getRepositoryMapClass(Context.class);
                    List<Map<String, Object>> execute = jdbcResource.execute(
                            new JdbcRequest(JTScheduler.SELECT_SUBSCRIBER_BY_PLAYER)
                                    .addArg("id_players", context.getPlayerEvent().keySet().stream().toList())
                                    .setDebug(false)
                    );
                    execute.forEach(map -> context.getPlayerSubscriber().computeIfAbsent(
                            map.get("id_player").toString(),
                            _ -> new ArrayList<>()
                    ).add(Integer.parseInt(map.get("id_chat").toString())));
                })
                .then("getPlayerList", new Tank01Request(NHLPlayerList::getUri).generate())
                .then("sendNotification", (atomicBoolean, _, promise) -> {
                    Context context = promise.getRepositoryMapClass(Context.class);
                    Tank01Request response = promise
                            .getRepositoryMapClass(Promise.class, "getPlayerList")
                            .getRepositoryMapClass(Tank01Request.class);

                    UtilRisc.forEach(atomicBoolean, context.getPlayerSubscriber(), (idPlayer, listIdChat) -> {
                        try {
                            if (listIdChat.isEmpty()) {
                                return;
                            }
                            Map<String, Object> player = NHLPlayerList.findById(idPlayer, response.getResponseData());
                            if (player == null || player.isEmpty()) {
                                return;
                            }
                            List<GameEventData> gameEventDataList = context.getPlayerEvent().get(idPlayer);
                            gameEventDataList.forEach(gameEventData -> {
                                if (UtilNHL.isOvi(idPlayer)) {
                                    new SendNotificationGameEventOvi(
                                            context.getMapIdPlayerGame().getOrDefault(idPlayer, ""),
                                            gameEventData
                                    ).generate().run();
                                }
                                new SendNotificationGameEvent(
                                        context.getMapIdPlayerGame().getOrDefault(idPlayer, ""),
                                        NHLPlayerList.Player.fromMap(player),
                                        gameEventData,
                                        listIdChat
                                ).generate().run();
                            });
                        } catch (Throwable e) {
                            App.error(e);
                        }
                    });
                })
                // saveData в конце Promise специально на случай критичных рассылок, если сломается, то будет всё по
                // новой рассылать
                .thenWithResource("saveData", JdbcResource.class, (run, _, promise, jdbcResource) -> {
                    Context context = promise.getRepositoryMapClass(Context.class);
                    UtilRisc.forEach(run, context.getCurrentData(), (idGame, data) -> {
                        try {
                            jdbcResource.execute(
                                    new JdbcRequest(context.getLastData().get(idGame) == null
                                            ? JTGameDiff.INSERT
                                            : JTGameDiff.UPDATE
                                    )
                                            .addArg("id_game", idGame)
                                            .addArg("scoring_plays", data)
                            );
                        } catch (Throwable e) {
                            App.error(e);
                        }
                    });
                })
                .thenWithResource("removeFinish", JdbcResource.class, (_, _, promise, jdbcResource) -> {
                    Context context = promise.getRepositoryMapClass(Context.class);
                    if (!context.getEndGames().isEmpty()) {
                        context.getEndGames().forEach(idGame -> {
                            try {
                                jdbcResource.execute(new JdbcRequest(JTScheduler.REMOVE_FINISH_GAME)
                                        .addArg("id_game", idGame)
                                        .setDebug(false)
                                );
                            } catch (Throwable e) {
                                App.error(e);
                            }
                        });
                    }
                })
                .onError((_, _, promise) -> System.out.println(promise.getLogString()))
                .setDebug(false);
    }

}
