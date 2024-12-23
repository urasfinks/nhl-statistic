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
import ru.jamsys.core.flat.util.UtilJson;
import ru.jamsys.core.flat.util.UtilRisc;
import ru.jamsys.core.flat.util.tank.UtilTank01;
import ru.jamsys.core.handler.promise.SendNotification;
import ru.jamsys.core.handler.promise.Tank01CacheRequest;
import ru.jamsys.core.handler.promise.Tank01Response;
import ru.jamsys.core.jt.JTGameDiff;
import ru.jamsys.core.jt.JTScheduler;
import ru.jamsys.core.promise.Promise;
import ru.jamsys.core.promise.PromiseGenerator;
import ru.jamsys.core.resource.http.HttpResource;
import ru.jamsys.core.resource.http.client.HttpResponse;
import ru.jamsys.core.resource.jdbc.JdbcRequest;
import ru.jamsys.core.resource.jdbc.JdbcResource;
import ru.jamsys.tank.data.NHLBoxScore;
import ru.jamsys.tank.data.NHLPlayerList;
import ru.jamsys.telegram.NotificationDataAndTemplate;

import java.util.*;

@Component
@Lazy
@SuppressWarnings("unused")
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
        private Map<String, String> boxScore = new HashMap<>();
        private Map<String, String> savedData = new HashMap<>();
        private Map<String, NotificationDataAndTemplate> event = new LinkedHashMap<>(); // key - idPlayer; value - template
        private Map<String, List<Integer>> subscriber = new HashMap<>(); // key - idPlayer;
        private List<String> endGames = new ArrayList<>();
        private Map<String, String> mapIdPlayerGame = new HashMap<>(); // key - idPlayer; value - gameName
    }

    public void logToTelegram(String data) {
        if (!NhlStatisticApplication.startTelegramListener) {
            System.out.println("logToTelegram:" + data);
        }
        if (telegramBotComponent.getHandler() != null) {
            telegramBotComponent.getHandler().send(
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
        if (telegramBotComponent.getHandler() == null && NhlStatisticApplication.startTelegramListener) {
            return null;
        }
        return servicePromise.get(getClass().getSimpleName(), 50_000L)
                .setDebug(false)
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
                .thenWithResource("getBoxScoreByActiveGame", HttpResource.class, (run, _, promise, httpResource) -> {
                    Context context = promise.getRepositoryMapClass(Context.class);
                    for (String idGame : context.getActiveGame()) {
                        if (!run.get()) {
                            return;
                        }
                        String data;
                        if (NhlStatisticApplication.dummySchedulerBoxScore) {
                            data = NHLBoxScore.getExample6();
                        } else {
                            HttpResponse response = UtilTank01.request(httpResource, promise, _ -> NHLBoxScore.getUri(idGame));
                            data = response.getBody();
                        }
                        @SuppressWarnings("unchecked")
                        Map<String, Object> parsed = UtilJson.toObject(data, Map.class);
                        if (!parsed.containsKey("error")) {
                            context.getBoxScore().put(idGame, data);
                        }
                    }
                    // Если нет никаких данных, нет смысла ничего сверять
                    if (context.getBoxScore().isEmpty()) {
                        promise.skipAllStep("NHLBoxScore response by active game is empty");
                    }
                })
                .thenWithResource("getSavedData", JdbcResource.class, (run, _, promise, jdbcResource) -> {
                    Context context = promise.getRepositoryMapClass(Context.class);
                    UtilRisc.forEach(run, context.getBoxScore(), (idGame, _) -> {
                        try {
                            List<Map<String, Object>> execute = jdbcResource.execute(new JdbcRequest(JTGameDiff.SELECT)
                                    .addArg("id_game", idGame)
                            );
                            context.getSavedData().put(idGame, execute.isEmpty()
                                    ? null
                                    : execute.getFirst().get("scoring_plays").toString()
                            );
                        } catch (Throwable e) {
                            throw new ForwardException(e);
                        }
                    });
                })
                .then("diffEvent", (atomicBoolean, _, promise) -> {
                    Context context = promise.getRepositoryMapClass(Context.class);
                    UtilRisc.forEach(atomicBoolean, context.getBoxScore(), (idGame, data) -> {
                        try {
                            if (NHLBoxScore.isFinish(data)) {
                                context.getEndGames().add(idGame);
                                logToTelegram("Finish game: " + idGame);
                            }
                            Map<String, NotificationDataAndTemplate> newEventScoringByPlayer = NHLBoxScore.getNewEventScoringByPlayer(
                                    context.getSavedData().get(idGame),
                                    data
                            );
                            newEventScoringByPlayer.forEach((idPlayer, _) -> context
                                    .getMapIdPlayerGame()
                                    .put(idPlayer, idGame)
                            );
                            context.getEvent().putAll(newEventScoringByPlayer);
                            //logToTelegram(idGame + ":" + UtilJson.toStringPretty(context.getEvent(), "{}"));
                        } catch (Throwable e) {
                            throw new ForwardException(e);
                        }
                    });
                    // Если некому рассылать события, просто сохраним данные в БД
                    if (context.getEvent().isEmpty()) {
                        promise.goTo("saveData");
                    }
                })
                .thenWithResource("selectSubscribers", JdbcResource.class, (_, _, promise, jdbcResource) -> {
                    Context context = promise.getRepositoryMapClass(Context.class);
                    List<Map<String, Object>> execute = jdbcResource.execute(
                            new JdbcRequest(JTScheduler.SELECT_SUBSCRIBER_BY_PLAYER)
                                    .addArg("id_players", context.getEvent().keySet().stream().toList())
                                    .setDebug(false)
                    );
                    execute.forEach(map -> context.getSubscriber().computeIfAbsent(
                            map.get("id_player").toString(),
                            _ -> new ArrayList<>()
                    ).add(Integer.parseInt(map.get("id_chat").toString())));
                })
                .then("getPlayerList", new Tank01CacheRequest(NHLPlayerList::getUri).generate())
                .then("sendNotification", (atomicBoolean, _, promise) -> {
                    Context context = promise.getRepositoryMapClass(Context.class);
                    Tank01Response response = promise
                            .getRepositoryMapClass(Promise.class, "getPlayerList")
                            .getRepositoryMapClass(Tank01Response.class);

                    UtilRisc.forEach(atomicBoolean, context.getSubscriber(), (idPlayer, listIdChat) -> {
                        try {
                            if (listIdChat.isEmpty()) {
                                return;
                            }
                            Map<String, Object> player = NHLPlayerList.findById(idPlayer, response.getData());
                            if (player == null || player.isEmpty()) {
                                return;
                            }
                            new SendNotification(
                                    context.getMapIdPlayerGame().getOrDefault(idPlayer, ""),
                                    NHLPlayerList.Player.fromMap(player),
                                    context.getEvent().get(idPlayer),
                                    listIdChat
                            ).generate().run();
                        } catch (Throwable e) {
                            App.error(e);
                        }
                    });
                })
                // saveData в конце Promise специально на случай критичных рассылок, если сломается, то будет всё по
                // новой рассылать
                .thenWithResource("saveData", JdbcResource.class, (run, _, promise, jdbcResource) -> {
                    Context context = promise.getRepositoryMapClass(Context.class);
                    UtilRisc.forEach(run, context.getBoxScore(), (key, data) -> {
                        if (context.getSavedData().get(key) == null) {
                            logToTelegram("Start game: " + key);
                        }
                        try {
                            jdbcResource.execute(
                                    new JdbcRequest(context.getSavedData().get(key) == null
                                            ? JTGameDiff.INSERT
                                            : JTGameDiff.UPDATE
                                    )
                                            .addArg("id_game", key)
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
                .onError((_, _, promise) -> System.out.println(promise.getLogString()));
    }

}
