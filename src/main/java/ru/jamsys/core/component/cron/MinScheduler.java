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
        private Map<String, List<Long>> playerSubscriber = new HashMap<>(); // key - idPlayer;
        private List<String> endGames = new ArrayList<>();
        private List<PromiseGenerator> notificationList = new ArrayList<>();
        private Map<String, Set<String>> activeGamePlayer = new HashMap<>(); // key idGame; value: list idPlayer

        public String getIdGame(String idPlayer) {
            System.out.println();
            for (String idGame : activeGamePlayer.keySet()) {
                for (String curIdPlayer : activeGamePlayer.get(idGame)) {
                    if (curIdPlayer.equals(idPlayer)) {
                        return idGame;
                    }
                }
            }
            throw new RuntimeException("undefined idGame by idPlayer = " + idPlayer);
        }
    }

    public Promise generate() {
        return servicePromise.get(getClass().getSimpleName(), 50_000L)
                .extension(promise -> promise.setRepositoryMapClass(Context.class, new Context()))
                .then("check", (atomicBoolean, promiseTask, promise) -> {
                    String mode = serviceProperty.get(String.class, "run.mode", "prod");
                    if (mode.equals("test")) {
                        promise.skipAllStep("mode test");
                        return;
                    }
                    if (telegramBotComponent.getNhlStatisticsBot() == null && NhlStatisticApplication.startTelegramListener) {
                        promise.skipAllStep("startTelegramListener = false");
                    }
                })
                .thenWithResource("getActiveGame", JdbcResource.class, (_, _, promise, jdbcResource) -> {
                    Context context = promise.getRepositoryMapClass(Context.class);
                    jdbcResource.execute(
                            new JdbcRequest(JTScheduler.SELECT_ACTIVE_GAME).setDebug(false)
                    ).forEach(map -> context.getActiveGame().add(map.get("id_game").toString()));
                    // Если нет активных игр, нечего тут делать
                    if (context.getActiveGame().isEmpty()) {
                        promise.skipAllStep("active game is empty");
                        return;
                    }
                    jdbcResource.execute(
                            new JdbcRequest(JTScheduler.SELECT_ACTIVE_GAME_PLAYER).setDebug(false)
                    ).forEach(map -> context
                            .getActiveGamePlayer()
                            .computeIfAbsent(
                                    map.getOrDefault("id_game", "--").toString(),
                                    _ -> new HashSet<>()
                            )
                            .add(map.getOrDefault("id_player", "0").toString()));
                })
                .then("getBoxScoreByActiveGame", (run, promiseTask, promise) -> {
                    Context context = promise.getRepositoryMapClass(Context.class);
                    for (String idGame : context.getActiveGame()) {
                        if (!run.get()) {
                            return;
                        }
                        Tank01Request tank01Request = new Tank01Request(() -> NHLBoxScore.getUri(idGame))
                                .setAlwaysRequestApi(true);
                        Promise req = tank01Request.generate().run().await(50_000L);
                        if (req.isException()) {
                            throw req.getExceptionSource();
                        }
                        String data = tank01Request.getResponseData();

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
                            NHLBoxScore.Instance currentBoxScore = new NHLBoxScore.Instance(data);
                            Map<String, List<GameEventData>> curGamePlayerEvent = context.getLastData().get(idGame) == null
                                    ? new HashMap<>()
                                    : NHLBoxScore.getEvent(context.getLastData().get(idGame), data);
                            // Мержим idPlayer из BoxScore с idPlayer из подписок, так как первый json приходит без статистики по игрокам
                            List<String> listIdPlayer = currentBoxScore.getListIdPlayer(context.getActiveGamePlayer().get(idGame));
                            if (context.getLastData().get(idGame) == null) {
                                listIdPlayer.forEach((idPlayer) -> {
                                    context
                                            .getActiveGamePlayer()
                                            .computeIfAbsent(idGame, _ -> new HashSet<>())
                                            .add(idPlayer);
                                    NHLBoxScore.Player player = currentBoxScore.getPlayer(idPlayer);
                                    boolean inPlay = true;
                                    if (player == null) {
                                        player = NHLBoxScore.Player.getEmpty(idPlayer);
                                        inPlay = false;
                                    }
                                    curGamePlayerEvent
                                            .computeIfAbsent(idPlayer, _ -> new ArrayList<>())
                                            .add(new GameEventData(
                                                            inPlay
                                                                    ? GameEventData.Action.START_GAME
                                                                    : GameEventData.Action.START_GAME_NOT_PLAY,
                                                            currentBoxScore.getAboutGame(),
                                                            currentBoxScore.getScoreGame(),
                                                            player.getLongName(),
                                                            curData
                                                    )
                                            );
                                        }
                                );
                            }
                            if (NHLBoxScore.isFinish(data)) {
                                listIdPlayer.forEach((idPlayer) -> {
                                    NHLBoxScore.Player player = currentBoxScore.getPlayer(idPlayer);
                                    if (player != null) {
                                        curGamePlayerEvent.computeIfAbsent(idPlayer, _ -> new ArrayList<>())
                                                .add(new GameEventData(
                                                                GameEventData.Action.FINISH_GAME,
                                                                currentBoxScore.getAboutGame(),
                                                                currentBoxScore.getScoreGame(),
                                                                player.getLongName(),
                                                                player.getFinishTimeScore()
                                                        )
                                                                .setScoredGoal(player.getGoals())
                                                                .setScoredAssists(player.getAssists())
                                                                .setScoredShots(player.getShots())
                                                                .setScoredHits(player.getHits())
                                                                .setScoredPenaltiesInMinutes(player.getPenaltiesInMinutes())
                                                                .setScoredTimeOnIce(player.getTimeOnIce())
                                                );
                                    }

                                });
                                context.getEndGames().add(idGame);
                            }
                            context.getPlayerEvent().putAll(curGamePlayerEvent);
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
                            ).add(Long.parseLong(map.get("id_chat").toString()))
                    );
                })
                .then("getPlayerList", new Tank01Request(NHLPlayerList::getUri).generate())
                .then("createNotification", (atomicBoolean, _, promise) -> {
                    Context context = promise.getRepositoryMapClass(Context.class);
                    Tank01Request response = promise
                            .getRepositoryMapClass(Promise.class, "getPlayerList")
                            .getRepositoryMapClass(Tank01Request.class);

                    Map<String, List<Long>> startGameNotify = new HashMap<>(); // key - idGame

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
                                    context.getNotificationList().add(new SendNotificationGameEventOvi(
                                            context.getIdGame(idPlayer),
                                            gameEventData
                                    ));
                                }
                                List<Long> to = new ArrayList<>(listIdChat);
                                if (gameEventData.getAction().equals(GameEventData.Action.START_GAME)) {
                                    List<Long> alreadySend = startGameNotify.computeIfAbsent(gameEventData.getGameAbout(), _ -> new ArrayList<>());
                                    to.removeAll(alreadySend);
                                    if (to.isEmpty()) {
                                        return;
                                    }
                                    alreadySend.addAll(to);
                                }

                                context.getNotificationList().add(new SendNotificationGameEvent(
                                        context.getIdGame(idPlayer),
                                        NHLPlayerList.Player.fromMap(player),
                                        gameEventData,
                                        to
                                ));
                            });
                        } catch (Throwable e) {
                            App.error(e);
                        }
                    });
                })
                .then("send", (atomicBoolean, promiseTask, promise) -> {
                    Context context = promise.getRepositoryMapClass(Context.class);
                    context.getNotificationList().forEach(promiseGenerator -> promiseGenerator.generate().run());
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
