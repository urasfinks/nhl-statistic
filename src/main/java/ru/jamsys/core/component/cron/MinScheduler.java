package ru.jamsys.core.component.cron;

import lombok.Getter;
import lombok.Setter;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import ru.jamsys.core.App;
import ru.jamsys.core.component.ServicePromise;
import ru.jamsys.core.component.TelegramBotComponent;
import ru.jamsys.core.extension.UniqueClassName;
import ru.jamsys.core.extension.exception.ForwardException;
import ru.jamsys.core.flat.template.cron.release.Cron1m;
import ru.jamsys.core.flat.util.UtilJson;
import ru.jamsys.core.flat.util.UtilRisc;
import ru.jamsys.core.flat.util.tank.UtilTank01;
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

import java.util.*;

@Component
@Lazy
public class MinScheduler implements Cron1m, PromiseGenerator, UniqueClassName {

    private final ServicePromise servicePromise;

    @Setter
    @Getter
    private String index;

    private final TelegramBotComponent telegramBotComponent;

    public MinScheduler(ServicePromise servicePromise, TelegramBotComponent telegramBotComponent) {
        this.servicePromise = servicePromise;
        this.telegramBotComponent = telegramBotComponent;
    }

    @Setter
    @Getter
    public static class Context {
        private List<String> listIdGame = new ArrayList<>();
        private Map<String, String> response = new HashMap<>();
        private Map<String, String> savedData = new HashMap<>();
        private Map<String, List<Map<String, Object>>> event = new LinkedHashMap<>(); // key - idPlayer; value - listEvent
        private Map<String, List<Integer>> subscriber = new HashMap<>(); // key - idPlayer;
        private List<String> endGames = new ArrayList<>();
    }

    public void logToTelegram(String data) {
        //System.out.println("logToTelegram:" + data);
        if (telegramBotComponent.getHandler() != null) {
            telegramBotComponent.getHandler().send(
                    -4739098379L,
                    //290029195,
                    data,
                    null
            );
        }
    }

    @Override
    public Promise generate() {
        if (telegramBotComponent.getHandler() == null) {
            return null;
        }
        return servicePromise.get(index, 50_000L)
                .setDebug(false)
                .thenWithResource("getSubscriptionsPlayer", JdbcResource.class, (_, _, promise, jdbcResource) -> {
                    Context context = promise.setRepositoryMapClass(Context.class, new Context());
                    List<Map<String, Object>> execute = jdbcResource.execute(
                            new JdbcRequest(JTScheduler.SELECT_ACTIVE_GAME).setDebug(false)
                    );
                    // Если нет активных игр, нечего тут делать
                    if (execute.isEmpty()) {
                        promise.skipAllStep("active game is empty");
                        return;
                    }
                    execute.forEach(map -> context.getListIdGame().add(map.get("id_game").toString()));
                })
                .thenWithResource("request", HttpResource.class, (run, _, promise, httpResource) -> {
                    Context context = promise.getRepositoryMapClass(Context.class);
                    for (String idGame : context.getListIdGame()) {
                        if (!run.get()) {
                            return;
                        }
                        HttpResponse response = UtilTank01.request(httpResource, promise, _ -> NHLBoxScore.getUri(idGame));
                        String data = response.getBody();
                        //String data = NHLBoxScore.getExample6();

                        @SuppressWarnings("unchecked")
                        Map<String, Object> parsed = UtilJson.toObject(data, Map.class);
                        if (!parsed.containsKey("error")) {
                            context.getResponse().put(idGame, data);
                        }
                    }
                    // Если нет никаких данных, нет смысла ничего сверять
                    if (context.getResponse().isEmpty()) {
                        promise.skipAllStep("NHLBoxScore response by active game is empty");
                    }
                })
                .thenWithResource("getSavedData", JdbcResource.class, (run, _, promise, jdbcResource) -> {
                    Context context = promise.getRepositoryMapClass(Context.class);
                    UtilRisc.forEach(run, context.getResponse(), (idGame, _) -> {
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
                    UtilRisc.forEach(atomicBoolean, context.getResponse(), (idGame, data) -> {
                        try {
                            if (NHLBoxScore.isFinish(data)) {
                                context.getEndGames().add(idGame);
                            }
                            context.getEvent().putAll(NHLBoxScore.getNewEventScoringByPlayer(
                                    context.getSavedData().get(idGame),
                                    data
                            ));
                            logToTelegram(idGame + ":" + UtilJson.toStringPretty(context.getEvent(), "{}"));
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
                .extension(NHLPlayerList::promiseExtensionGetPlayerList)
                .then("sendNotification", (atomicBoolean, _, promise) -> {
                    Context context = promise.getRepositoryMapClass(Context.class);
                    UtilTank01.Response response = promise.getRepositoryMapClass(UtilTank01.Response.class);

                    UtilRisc.forEach(atomicBoolean, context.getSubscriber(), (idPlayer, listIdChat) -> {
                        try {
                            Map<String, Object> player = NHLPlayerList.findById(
                                    idPlayer,
                                    response.getData()
                            );
                            if (player == null || player.isEmpty()) {
                                return;
                            }
                            List<String> listMessage = new ArrayList<>();
                            context.getEvent().get(idPlayer).forEach(map -> listMessage.add(String.format(
                                    "%s %s %s of the %s",
                                    NHLPlayerList.getPlayerName(player),
                                    map.get("type").equals("goal")
                                            ? "scored a goal at"
                                            : " !CANCEL! ",
                                    map.get("scoreTime"),
                                    map.get("period")
                            )));
                            String message = String.join("\n", listMessage);
                            //System.out.println("SEND TO CLIENT: " + message);
                            UtilRisc.forEach(atomicBoolean, listIdChat, idChat -> {
                                if (telegramBotComponent.getHandler() != null) {
                                    telegramBotComponent.getHandler().send(idChat, message, null);
                                }
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
                    UtilRisc.forEach(run, context.getResponse(), (key, data) -> {
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
