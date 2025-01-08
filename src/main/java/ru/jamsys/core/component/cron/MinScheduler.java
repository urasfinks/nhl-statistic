package ru.jamsys.core.component.cron;

import com.fasterxml.jackson.annotation.JsonIgnore;
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
import ru.jamsys.core.extension.builder.HashMapBuilder;
import ru.jamsys.core.extension.exception.ForwardException;
import ru.jamsys.core.flat.template.cron.release.Cron1m;
import ru.jamsys.core.flat.util.UtilJson;
import ru.jamsys.core.flat.util.UtilNHL;
import ru.jamsys.core.flat.util.UtilRisc;
import ru.jamsys.core.handler.promise.SendNotificationGameEvent;
import ru.jamsys.core.handler.promise.SendNotificationGameEventOvi;
import ru.jamsys.core.handler.promise.Tank01Request;
import ru.jamsys.core.jt.JTGameDiff;
import ru.jamsys.core.jt.JTLogRequest;
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
        private ActiveRepository activeRepository = new ActiveRepository();
        private Map<String, String> currentData = new HashMap<>(); //key - idGame; value Api Response
        private Map<String, String> lastData = new HashMap<>();
        private Map<String, List<GameEventData>> playerEvent = new LinkedHashMap<>(); // key - idPlayer; value - template
        private List<String> endGames = new ArrayList<>();
        private List<PromiseGenerator> notificationList = new ArrayList<>();
    }

    @Getter
    @Setter
    public static class ActiveRepository {

        private List<ActiveObject> list = new ArrayList<>();

        @JsonIgnore
        public ActiveRepository add(ActiveObject activeObject) {
            list.add(activeObject);
            return this;
        }

        @JsonIgnore
        public List<String> getListIdGame() {
            Set<String> result = new HashSet<>();
            list.forEach(activeObject -> result.add(activeObject.getIdGame()));
            return result.stream().toList();
        }

        @JsonIgnore
        public String getIdGame(String idPlayer) {
            for (ActiveObject activeObject : list) {
                if (activeObject.getIdPlayer().equals(idPlayer)) {
                    return activeObject.getIdGame();
                }
            }
            throw new RuntimeException("undefined idGame by idPlayer = " + idPlayer);
        }

        @JsonIgnore
        public Map<String, Set<Long>> getPlayerListIdChatByPlayer() {
            Map<String, Set<Long>> result = new HashMap<>();
            list.forEach(activeObject -> result
                    .computeIfAbsent(activeObject.getIdPlayer(), s -> new HashSet<>())
                    .add(activeObject.getIdChat()));
            return result;
        }

        @JsonIgnore
        public Set<String> getListIdPlayer(String idGame) {
            Set<String> result = new HashSet<>();
            list.forEach(activeObject -> {
                if (activeObject.getIdGame().equals(idGame)) {
                    result.add(activeObject.getIdPlayer());
                }
            });
            return result;
        }

    }

    @Getter
    public static class ActiveObject {

        private final Long idChat;

        private final String idPlayer;

        private final String idGame;

        public ActiveObject(Long idChat, String idPlayer, String idGame) {
            this.idChat = idChat;
            this.idPlayer = idPlayer;
            this.idGame = idGame;
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
                    jdbcResource
                            .execute(new JdbcRequest(JTScheduler.SELECT_ACTIVE_GAME).setDebug(false))
                            .forEach(map -> context.getActiveRepository().getList().add(new ActiveObject(
                                    Long.parseLong(map.getOrDefault("id_chat", "0").toString()),
                                    map.getOrDefault("id_player", "0").toString(),
                                    map.getOrDefault("id_game", "--").toString()
                            )));
                    // Если нет активных игр, нечего тут делать
                    if (context.getActiveRepository().getList().isEmpty()) {
                        promise.skipAllStep("active game is empty");
                    }
                    jdbcResource.execute(
                            new JdbcRequest(JTLogRequest.INSERT)
                                    .addArg("url", ActiveRepository.class.getSimpleName())
                                    .addArg("data", UtilJson.toStringPretty(context.getActiveRepository(), "{}"))
                                    .setDebug(false)
                    );
                })
                .then("getBoxScoreByActiveGame", (run, promiseTask, promise) -> {
                    Context context = promise.getRepositoryMapClass(Context.class);
                    for (String idGame : context.getActiveRepository().getListIdGame()) {
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
                        // Если что-то не спарсилось - другие игры не должны страдать
                        try {
                            @SuppressWarnings("unchecked")
                            Map<String, Object> parsed = UtilJson.toObject(data, Map.class);
                            if (!parsed.containsKey("error")) {
                                NHLBoxScore.Instance instance = new NHLBoxScore.Instance(data);
                                // Получилось, что при старте был пустой список статистики и мы выслали уведомление, что
                                // Овечкин не участвует в игре
                                // Нет статистики нет начала игры
                                if (instance.getPlayerStats().isEmpty()) { // Блок статистики не пустой
                                    App.error(new RuntimeException("idGame: " + idGame + " continue; cause: getPlayerStats().isEmpty()"));
                                    continue;
                                }
                                if (!instance.isValidate()) { // Блок статистики не пустой
                                    App.error(new RuntimeException("idGame: " + idGame + " continue; cause: !instance.isValidate()"));
                                    continue;
                                }
                                context.getCurrentData().put(idGame, data);
                            }
                        } catch (Throwable e) {
                            App.error(e);
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
                    UtilRisc.forEach(atomicBoolean, context.getCurrentData(), (idGame, data) -> {
                        try {
                            NHLBoxScore.Instance currentBoxScore = new NHLBoxScore.Instance(data);
                            // Дополнительный блок зачистки, нужен для юнитов, так как блоки getBoxScoreByActiveGame
                            // и getLastData в основном переопределяются
                            if (currentBoxScore.getPlayerStats().isEmpty()) {
                                context.getCurrentData().remove(idGame);
                                context.getLastData().remove(idGame);
                                App.error(new RuntimeException("idGame: " + idGame + " continue; cause: getPlayerStats().isEmpty()"));
                                return;
                            }
                            if (!currentBoxScore.isValidate()) {
                                context.getCurrentData().remove(idGame);
                                context.getLastData().remove(idGame);
                                App.error(new RuntimeException("idGame: " + idGame + " continue; cause: !instance.isValidate()"));
                                return;
                            }
                            if (context.getLastData().get(idGame) != null) { // События генерируются только при наличии двух снимков
                                NHLBoxScore
                                        .getEvent(context.getLastData().get(idGame), data)
                                        .forEach((idPlayer, gameEventData) -> context
                                                .getPlayerEvent()
                                                .computeIfAbsent(idPlayer, _ -> new ArrayList<>())
                                                .addAll(gameEventData));
                            }

                            if (context.getLastData().get(idGame) == null) {
                                currentBoxScore
                                        .getListIdPlayer(context.getActiveRepository().getListIdPlayer(idGame))
                                        .forEach((idPlayer) -> {
                                            NHLBoxScore.Player player = currentBoxScore.getPlayer(idPlayer);
                                            if (player == null) {
                                                player = NHLBoxScore.Player.getPlayerOrEmpty(idPlayer);
                                                context
                                                        .getPlayerEvent()
                                                        .computeIfAbsent(idPlayer, _ -> new ArrayList<>())
                                                        .add(new GameEventData(
                                                                        GameEventData.Action.NOT_PLAY,
                                                                        currentBoxScore.getAboutGame(),
                                                                        currentBoxScore.getScoreGame(),
                                                                        player.getLongName(),
                                                                        "now"
                                                                )
                                                        );
                                            }
                                            context
                                                    .getPlayerEvent()
                                                    .computeIfAbsent(idPlayer, _ -> new ArrayList<>())
                                                    .add(new GameEventData(
                                                            GameEventData.Action.START_GAME,
                                                                    currentBoxScore.getAboutGame(),
                                                                    currentBoxScore.getScoreGame(),
                                                                    player.getLongName(),
                                                                    "now"
                                                            )
                                                    );
                                        }
                                );
                            }
                            if (currentBoxScore.isFinish()) {
                                currentBoxScore
                                        .getListIdPlayer(context.getActiveRepository().getListIdPlayer(idGame))
                                        .forEach((idPlayer) -> {
                                            NHLBoxScore.Player player = currentBoxScore.getPlayer(idPlayer);
                                            if (player != null) {
                                                context
                                                        .getPlayerEvent()
                                                        .computeIfAbsent(idPlayer, _ -> new ArrayList<>())
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
                                                                .setPenaltyShot(currentBoxScore.isPenaltyShot())
                                                                .setOverTime(currentBoxScore.isOverTime())
                                                        );
                                            }
                                });
                                context.getEndGames().add(idGame);
                            }
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
                .then("getPlayerList", new Tank01Request(NHLPlayerList::getUri).generate())
                .then("createNotification", (atomicBoolean, _, promise) -> {
                    Context context = promise.getRepositoryMapClass(Context.class);
                    Tank01Request response = promise
                            .getRepositoryMapClass(Promise.class, "getPlayerList")
                            .getRepositoryMapClass(Tank01Request.class);

                    Map<String, Set<Long>> startGameNotify = new HashMap<>(); // key - idGame

                    UtilRisc.forEach(atomicBoolean, context.getActiveRepository().getPlayerListIdChatByPlayer(), (idPlayer, listIdChat) -> {
                        try {
                            if (listIdChat.isEmpty()) {
                                return;
                            }
                            Map<String, Object> player = NHLPlayerList.findById(idPlayer, response.getResponseData());
                            if (player == null || player.isEmpty()) {
                                return;
                            }
                            List<GameEventData> gameEventDataList = context.getPlayerEvent().get(idPlayer);
                            // Может быть такое, что по одному игроку будут события, а по другому нет
                            // Но context.getActiveRepository().getPlayerListIdChatByPlayer() всё равно будем пробегаться по каждому
                            if (gameEventDataList != null && !gameEventDataList.isEmpty()) {
                                gameEventDataList.forEach(gameEventData -> {
                                    if (UtilNHL.isOvi(idPlayer)) {
                                        context.getNotificationList().add(new SendNotificationGameEventOvi(
                                                context.getActiveRepository().getIdGame(idPlayer),
                                                gameEventData
                                        ));
                                    }
                                    Set<Long> to = new HashSet<>(listIdChat);
                                    if (gameEventData.getAction().equals(GameEventData.Action.START_GAME)) {
                                        Set<Long> alreadySend = startGameNotify
                                                .computeIfAbsent(gameEventData.getGameAbout(), _ -> new HashSet<>());
                                        to.removeAll(alreadySend);
                                        if (to.isEmpty()) {
                                            return;
                                        }
                                        alreadySend.addAll(to);
                                    }

                                    context.getNotificationList().add(new SendNotificationGameEvent(
                                            context.getActiveRepository().getIdGame(idPlayer),
                                            NHLPlayerList.Player.fromMap(player),
                                            gameEventData,
                                            to.stream().toList()
                                    ));
                                });
                            }
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
                    jdbcResource.execute(
                            new JdbcRequest(JTLogRequest.INSERT)
                                    .addArg("url", "getPlayerEvent")
                                    .addArg("data", UtilJson.toStringPretty(context.getPlayerEvent(), "{}"))
                                    .setDebug(false)
                    );
                    jdbcResource.execute(
                            new JdbcRequest(JTLogRequest.INSERT)
                                    .addArg("url", "meta")
                                    .addArg("data", UtilJson.toStringPretty(new HashMapBuilder<String, Object>()
                                                    .append("getPlayerEvent", context.getPlayerEvent())
                                                    .append("getCurrentDataKeySet", context.getCurrentData().keySet())
                                                    .append("getLastDataKeySet", context.getLastData().keySet()),
                                            "{}"))
                                    .setDebug(false)
                    );
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
