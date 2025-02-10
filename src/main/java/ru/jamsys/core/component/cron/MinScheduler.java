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
import ru.jamsys.core.component.TelegramBotManager;
import ru.jamsys.core.component.manager.item.Session;
import ru.jamsys.core.extension.UniqueClassName;
import ru.jamsys.core.extension.builder.HashMapBuilder;
import ru.jamsys.core.extension.exception.ForwardException;
import ru.jamsys.core.flat.template.cron.release.Cron1m;
import ru.jamsys.core.flat.util.Util;
import ru.jamsys.core.flat.util.UtilJson;
import ru.jamsys.core.flat.util.UtilNHL;
import ru.jamsys.core.flat.util.UtilRisc;
import ru.jamsys.core.handler.promise.*;
import ru.jamsys.core.jt.JTGameDiff;
import ru.jamsys.core.jt.JTLogRequest;
import ru.jamsys.core.jt.JTTeamScheduler;
import ru.jamsys.core.promise.Promise;
import ru.jamsys.core.promise.PromiseGenerator;
import ru.jamsys.core.resource.jdbc.JdbcRequest;
import ru.jamsys.core.resource.jdbc.JdbcResource;
import ru.jamsys.tank.data.NHLBoxScore;
import ru.jamsys.tank.data.NHLPlayerList;
import ru.jamsys.telegram.GameEventData;
import ru.jamsys.telegram.TelegramNotification;
import ru.jamsys.telegram.template.GameEventTemplate;

import java.util.*;

@SuppressWarnings("unused")
@Component
@Lazy
public class MinScheduler implements Cron1m, PromiseGenerator, UniqueClassName {

    private final ServicePromise servicePromise;

    private final ServiceProperty serviceProperty;

    private final Session<String, Long> finishGameRegister = new Session<>(getClassName(), 600_000L); // key - idGame, value timestamp

    public MinScheduler(
            ServicePromise servicePromise,
            ServiceProperty serviceProperty
    ) {
        this.servicePromise = servicePromise;
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

        private List<TelegramNotification> listNotify = new ArrayList<>();

        RegisterNotificationGameEventOvi oviNotificationPromise;

        @JsonIgnore
        private Map<String, NHLBoxScore.Instance> currentNHLBoxScoreInstance = new HashMap<>();

        @JsonIgnore
        private Map<String, NHLBoxScore.Instance> lastNHLBoxScoreInstance = new HashMap<>();

        @JsonIgnore
        public NHLBoxScore.Instance getCurrentNHLBoxScoreInstance(String idGame) {
            return currentNHLBoxScoreInstance.computeIfAbsent(idGame, s -> {
                try {
                    return new NHLBoxScore.Instance(getCurrentData().get(idGame));
                } catch (Throwable e) {
                    throw new RuntimeException(e);
                }
            });
        }

        @JsonIgnore
        public NHLBoxScore.Instance getLastNHLBoxScoreInstance(String idGame) {
            if (!lastNHLBoxScoreInstance.containsKey(idGame)) {
                try {
                    lastNHLBoxScoreInstance.put(
                            idGame,
                            new NHLBoxScore.Instance(getLastData().get(idGame))
                    );
                } catch (Throwable _) {
                }
            }
            return lastNHLBoxScoreInstance.get(idGame);
        }
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
        public List<String> getIdGames() {
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
        public Map<String, Set<Long>> getPlayerSubscribers() {
            Map<String, Set<Long>> result = new HashMap<>();
            list.forEach(activeObject -> result
                    .computeIfAbsent(activeObject.getIdPlayer(), s -> new HashSet<>())
                    .add(activeObject.getIdChat()));
            return result;
        }

        @JsonIgnore
        public Set<String> getIdPlayers(String idGame) {
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
                    String mode = serviceProperty.get(String.class, "run.mode", "test");
                    if (mode.equals("test")) { // Если запущены в режиме тест - не надо ничего делать
                        promise.skipAllStep("mode test");
                        return;
                    }
                    if (mode.equals("dev")) { // Если запущены в режиме ift - не надо ничего делать
                        promise.skipAllStep("mode ift");
                        return;
                    }
                    if (!NhlStatisticApplication.startTelegramListener) {
                        promise.skipAllStep("startTelegramListener = false");
                        return;
                    }
                    new InviteGameCommon().generate().run();
                })
                .thenWithResource("getActiveGame", JdbcResource.class, (_, _, promise, jdbcResource) -> {
                    Context context = promise.getRepositoryMapClass(Context.class);
                    jdbcResource
                            .execute(new JdbcRequest(JTTeamScheduler.SELECT_ACTIVE_GAME).setDebug(false))
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
                    for (String idGame : context.getActiveRepository().getIdGames()) {
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
                        try { // Если что-то не спарсилось - другие игры не должны страдать
                            NHLBoxScore.Instance currentBoxScore = new NHLBoxScore.Instance(data);
                            if (currentBoxScore.isPostponed()) {
                                App.error(new RuntimeException("idGame: " + idGame + " continue; cause: isPostponed()"));
                                continue;
                            }
                            // Получилось, что при старте был пустой список статистики и мы выслали уведомление, что
                            // Овечкин не участвует в игре
                            // Нет статистики нет начала игры
                            // 10.02.2025 Информацию о не принятии участника вынесли в конец, более не актуально проверять блок статистики
//                            if (currentBoxScore.getPlayerStats().isEmpty()) { // Блок статистики пустой
//                                App.error(new RuntimeException("idGame: " + idGame + " continue; cause: getPlayerStats().isEmpty()"));
//                                continue;
//                            }
                            // Случилась проблема, что Ови забил за 0.1 секунду до конца и статисты не успели внести корректировки
                            // Добавлена 2-х минутная задержка
                            if (currentBoxScore.isFinish()) {
                                if (!finishGameRegister.containsKey(currentBoxScore.getIdGame())) {
                                    finishGameRegister.put(currentBoxScore.getIdGame(), System.currentTimeMillis());
                                }
                                long l = finishGameRegister.get(currentBoxScore.getIdGame()) + 2 * 60 * 1000;
                                if (System.currentTimeMillis() < l) {
                                    App.error(new RuntimeException("idGame: " + idGame + " continue; cause: Finish delay remaining: " + (l - System.currentTimeMillis()) + " ms"));
                                    continue;
                                }
                            }
                            context.getCurrentData().put(idGame, data);
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
                .then("validator", (run, promiseTask, promise) -> {
                    // Надо проверить ответы от api на несоответствие голов в стате и расшифровке
                    // Если есть несовпадение - надо в ответе Api заменить блок статистики по игроку из данных БД
                    // Если это начало игры - ничего менять не надо
                    Context context = promise.getRepositoryMapClass(Context.class);
                    UtilRisc.forEach(run, context.getCurrentData(), (idGame, currentData) -> {
                        List<String> playerProblemStatistic = context
                                .getCurrentNHLBoxScoreInstance(idGame)
                                .getPlayerProblemStatistic();
                        if (!playerProblemStatistic.isEmpty()) {
                            if (context.getLastData().get(idGame) == null) {
                                App.error(new RuntimeException("idGame: " + idGame + " continue; cause: lastData == null and playerProblemStatistic: " + playerProblemStatistic + "; "));
                                context.getCurrentData().remove(idGame);
                                context.getLastData().remove(idGame);
                            } else {
                                Util.logConsole(
                                        getClass(),
                                        "idGame: "
                                                + idGame
                                                + " modify; cause: lastData != null and playerProblemStatistic: "
                                                + playerProblemStatistic
                                                + "; "
                                );
                                context
                                        .getCurrentNHLBoxScoreInstance(idGame)
                                        .modify(context.getLastNHLBoxScoreInstance(idGame));
                            }
                        }
                    });
                })
                .then("getEvent", (atomicBoolean, _, promise) -> {
                    Context context = promise.getRepositoryMapClass(Context.class);
                    UtilRisc.forEach(atomicBoolean, context.getCurrentData(), (idGame, data) -> {
                        try {
                            NHLBoxScore.Instance currentBoxScore = context.getCurrentNHLBoxScoreInstance(idGame);
                            if (currentBoxScore.isPostponed()) {
                                context.getCurrentData().remove(idGame);
                                context.getLastData().remove(idGame);
                                App.error(new RuntimeException("idGame: " + idGame + " continue; cause: isPostponed()"));
                                return;
                            }
                            // Дополнительный блок зачистки, нужен для юнитов, так как блоки getBoxScoreByActiveGame
                            // и getLastData в основном переопределяются
//                            if (currentBoxScore.getPlayerStats().isEmpty()) {
//                                context.getCurrentData().remove(idGame);
//                                context.getLastData().remove(idGame);
//                                App.error(new RuntimeException("idGame: " + idGame + " continue; cause: getPlayerStats().isEmpty()"));
//                                return;
//                            }

                            // Если нет данных о прошлом состоянии значит игра началась
                            if (context.getLastData().get(idGame) == null) {
                                currentBoxScore
                                        .mergeIdPlayers(context.getActiveRepository().getIdPlayers(idGame))
                                        .forEach((idPlayer) -> {
                                                    NHLPlayerList.Player player = currentBoxScore.getPlayer(idPlayer);
                                                    if (player == null) {
                                                        player = NHLPlayerList.findByIdStaticOrEmpty(idPlayer);
                                                    }
                                                    context
                                                            .getPlayerEvent()
                                                            .computeIfAbsent(idPlayer, _ -> new ArrayList<>())
                                                            .add(new GameEventData(
                                                                    GameEventData.Action.START_GAME,
                                                                    currentBoxScore.getIdGame(),
                                                                    currentBoxScore.getAboutGame(),
                                                                    currentBoxScore.getScoreGame(),
                                                                    player,
                                                                    "now"
                                                            ));
                                                }
                                        );
                            }
                            // Начало игры может прийти сразу с блоком статистики и там уже могут быть голы
                            // Так что хочешь - не хочешь надо получать события
                            NHLBoxScore
                                    .getEvent(
                                            context.getLastNHLBoxScoreInstance(idGame),
                                            context.getCurrentNHLBoxScoreInstance(idGame)
                                    )
                                    .forEach((idPlayer, gameEventData) -> context
                                            .getPlayerEvent()
                                            .computeIfAbsent(idPlayer, _ -> new ArrayList<>())
                                            .addAll(gameEventData));

                            if (currentBoxScore.isFinish()) {
                                currentBoxScore
                                        .mergeIdPlayers(context.getActiveRepository().getIdPlayers(idGame))
                                        .forEach((idPlayer) -> {
                                            NHLPlayerList.Player player = currentBoxScore.getPlayer(idPlayer);
                                            if (player == null) {
                                                context
                                                        .getPlayerEvent()
                                                        .computeIfAbsent(idPlayer, _ -> new ArrayList<>())
                                                        .add(new GameEventData(
                                                                GameEventData.Action.FINISH_NOT_PLAY,
                                                                currentBoxScore.getIdGame(),
                                                                currentBoxScore.getAboutGame(),
                                                                currentBoxScore.getScoreGame(),
                                                                NHLPlayerList.findByIdStaticOrEmpty(idPlayer),
                                                                "now"
                                                        ));
                                            }
                                            NHLBoxScore.PlayerStat playerStat = currentBoxScore.getPlayerStat(idPlayer);
                                            if (playerStat != null) {
                                                context
                                                        .getPlayerEvent()
                                                        .computeIfAbsent(idPlayer, _ -> new ArrayList<>())
                                                        .add(
                                                                new GameEventData(
                                                                        GameEventData.Action.FINISH_GAME,
                                                                        currentBoxScore.getIdGame(),
                                                                        currentBoxScore.getAboutGame(),
                                                                        currentBoxScore.getScoreGame(),
                                                                        playerStat.getPlayerOrEmpty(),
                                                                        playerStat.getFinishTimeScore()
                                                                )
                                                                .setScoredGoal(playerStat.getGoals())
                                                                .setScoredAssists(playerStat.getAssists())
                                                                .setScoredShots(playerStat.getShots())
                                                                .setScoredHits(playerStat.getHits())
                                                                .setScoredPenaltiesInMinutes(playerStat.getPenaltiesInMinutes())
                                                                .setScoredTimeOnIce(playerStat.getTimeOnIce())
                                                                .setPenaltyShot(currentBoxScore.isPenaltyShot())
                                                                .setOverTime(currentBoxScore.isOverTime())
                                                        );
                                            }
                                });
                                context.getEndGames().add(idGame);
                            }
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
                    // Если мы пришли к тому, что надо контролировать задвоения на отправку одному пользователю
                    // Следовательно ключём корневой карты должен быть пользователь
                    // Вторым ключём составной ключ от события
                    // Если это START_GAME -> START_GAME+idGame
                    //          default -> uuid
                    Map<Long, ClientEvent> mapClientEvent = new HashMap<>();
                    UtilRisc.forEach(atomicBoolean, context.getActiveRepository().getPlayerSubscribers(), (idPlayer, listIdChat) -> {
                        try {
                            if (listIdChat.isEmpty()) {
                                return;
                            }
                            NHLPlayerList.Player player = NHLPlayerList.findById(idPlayer, response.getResponseData());
                            if (player == null) {
                                return;
                            }

                            List<GameEventData> listPlayerGameEventData = context.getPlayerEvent().get(idPlayer);
                            if (listPlayerGameEventData != null && !listPlayerGameEventData.isEmpty()) {
                                listPlayerGameEventData.forEach(playerGameEventData -> {
                                    if (UtilNHL.isOvi(idPlayer)) {
                                        if (context.getOviNotificationPromise() == null) {
                                            // Мы не можем раньше создать так как getIdGame вызывает исключение если
                                            // игра не найдена по игроку
                                            context.setOviNotificationPromise(
                                                    new RegisterNotificationGameEventOvi(
                                                            context
                                                                    .getActiveRepository()
                                                                    .getIdGame(UtilNHL.getOvi().getPlayerID())
                                                    )
                                            );
                                        }
                                        context
                                                .getOviNotificationPromise()
                                                .getListGameEventData()
                                                .add(playerGameEventData);
                                    }
                                    listIdChat.forEach(idChat -> {
                                        ClientEvent clientEvent = mapClientEvent
                                                .computeIfAbsent(idChat, _ -> new ClientEvent(idChat));
                                        clientEvent.add(playerGameEventData);
                                    });
                                });
                            }
                        } catch (Throwable e) {
                            App.error(e);
                        }
                    });
                    TelegramBotManager telegramBotManager = App.get(TelegramBotManager.class);
                    Map<NHLPlayerList.Player, ScorePlayerCurrentSeasonBeforeGame> mapStat = new HashMap<>();
                    mapClientEvent
                            .forEach((idChat, clientEvent) -> clientEvent.getMap().forEach((_, gameEventData) -> {
                                ScorePlayerCurrentSeasonBeforeGame stat = mapStat
                                        .computeIfAbsent(gameEventData.getPlayer(), player -> {
                                            ScorePlayerCurrentSeasonBeforeGame scorePlayerCurrentSeasonBeforeGame =
                                                    new ScorePlayerCurrentSeasonBeforeGame(
                                                            player,
                                                            gameEventData.getIdGame()
                                                    );
                                            scorePlayerCurrentSeasonBeforeGame.generate().run().await(60_000L);
                                            return scorePlayerCurrentSeasonBeforeGame;
                                        });
                                gameEventData.setScoredPrevGoal(stat.getCountGoal().get());
                                TelegramNotification telegramNotification = new TelegramNotification(
                                        idChat,
                                        telegramBotManager.getCommonBotProperty().getName(),
                                        new GameEventTemplate(gameEventData).toString(),
                                        null,
                                        null
                                );
                                context.getListNotify().add(telegramNotification);
                            }));
                })
                .then("send", (atomicBoolean, promiseTask, promise) -> {
                    Context context = promise.getRepositoryMapClass(Context.class);
                    if (context.getOviNotificationPromise() != null
                            && !context.getOviNotificationPromise().getListGameEventData().isEmpty()
                    ) {
                        context.getOviNotificationPromise().generate().run();
                    }
                    if (context.getListNotify() != null && !context.getListNotify().isEmpty()) {
                        RegisterNotification.add(context.getListNotify());
                    }
                })
                // saveData в конце Promise специально на случай критичных рассылок, если сломается, то будет всё по
                // новой рассылать
                .thenWithResource("saveData", JdbcResource.class, (run, _, promise, jdbcResource) -> {
                    Context context = promise.getRepositoryMapClass(Context.class);
                    UtilRisc.forEach(run, context.getCurrentData(), (idGame, _) -> {
                        try {
                            jdbcResource.execute(
                                    new JdbcRequest(context.getLastData().get(idGame) == null
                                            ? JTGameDiff.INSERT
                                            : JTGameDiff.UPDATE
                                    )
                                            .addArg("id_game", idGame)
                                            // Собираем из parsedJson так как могли быть изменения при валидации
                                            // А именно - заменён блок статистики из прошлых данных
                                            .addArg("scoring_plays", UtilJson.toStringPretty(
                                                    context.getCurrentNHLBoxScoreInstance(idGame).getParsedJson(),
                                                    "{}"
                                            ))
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
                                jdbcResource.execute(new JdbcRequest(JTTeamScheduler.DELETE_FINISH_GAME)
                                        .addArg("id_game", idGame)
                                        .setDebug(false)
                                );
                            } catch (Throwable e) {
                                App.error(e);
                            }
                        });
                    }
                })
                .setDebug(false);
    }

    @Getter
    @Setter
    public static class ClientEvent {

        // 5560 -> {RegisterNotificationGameEvent@8032}
        // 3942335 -> {RegisterNotificationGameEvent@8034}

        private final Long idChat;

        private Map<String, GameEventData> map = new HashMap<>(); // key - complexKey

        public ClientEvent(Long idChat) {
            this.idChat = idChat;
        }

        public void add(GameEventData gameEventData) {
            String complexKey = gameEventData.getAction().equals(GameEventData.Action.START_GAME)
                    ? gameEventData.getGameAbout()
                    : UUID.randomUUID().toString();
            map.put(complexKey, gameEventData);
        }

        public boolean contains(GameEventData gameEventData) {
            return map.containsValue(gameEventData);
        }

    }

}
