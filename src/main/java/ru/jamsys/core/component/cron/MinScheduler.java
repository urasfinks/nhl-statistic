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
        List<String> listIdGame = new ArrayList<>();
        Map<String, String> response = new HashMap<>();
        Map<String, String> lastDB = new HashMap<>();
        Map<Integer, String> event = new LinkedHashMap<>();
        Map<Integer, List<Integer>> subscriber = new HashMap<>();
        List<String> endGames = new ArrayList<>();
    }

    @Override
    public Promise generate() {
        return servicePromise.get(index, 6_000L)
                .then("check", (_, _, promise) -> {
                    if (telegramBotComponent.getHandler() == null) {
                        promise.skipAllStep();
                    }
                })
                .thenWithResource("getSubscriptionsPlayer", JdbcResource.class, (_, _, promise, jdbcResource) -> {
                    Context context = promise.setRepositoryMapClass(Context.class, new Context());
                    List<Map<String, Object>> execute = jdbcResource.execute(new JdbcRequest(JTScheduler.SELECT_ACTIVE_GAME)
                            .setDebug(false)
                    );
                    if (execute.isEmpty()) {
                        promise.skipAllStep();
                        return;
                    }
                    List<String> listIdGame = context.getListIdGame();
                    execute.forEach(stringObjectMap -> listIdGame.add(stringObjectMap.get("id_game").toString()));
                })
                .thenWithResource("request", HttpResource.class, (run, _, promise, httpResource) -> {
                    Context context = promise.getRepositoryMapClass(Context.class);
                    List<String> listIdGame = context.getListIdGame();
                    for (String idGame : listIdGame) {
                        if (!run.get()) {
                            return;
                        }
                        HttpResponse response = UtilTank01.request(httpResource, promise, _ -> NHLBoxScore.getUri(idGame));
                        context.getResponse().put(idGame, response.getBody());
//                        context.getResponse().put(idGame, NHLBoxScore.getExample3());
                    }
                    if (context.getResponse().isEmpty()) {
                        promise.skipAllStep();
                    }
                })
                .thenWithResource("prepareLastData", JdbcResource.class, (run, _, promise, jdbcResource) -> {
                    Context context = promise.getRepositoryMapClass(Context.class);
                    UtilRisc.forEach(run, context.getResponse(), (idGame, _) -> {
                        try {
                            List<Map<String, Object>> execute = jdbcResource.execute(new JdbcRequest(JTGameDiff.SELECT)
                                    .addArg("id_game", idGame)
                            );
                            context.getLastDB().put(idGame, execute.isEmpty()
                                    ? null
                                    : execute.getFirst().get("scoring_plays").toString()
                            );
                        } catch (Throwable e) {
                            throw new ForwardException(e);
                        }
                    });
                })
                .then("getDiff", (atomicBoolean, _, promise) -> {
                    Context context = promise.getRepositoryMapClass(Context.class);
                    UtilRisc.forEach(atomicBoolean, context.getResponse(), (idGame, data) -> {
                        try {
                            if (NHLBoxScore.isFinish(data)) {
                                context.getEndGames().add(idGame);
                            }
                            NHLBoxScore.getNewEventScoring(context.getLastDB().get(idGame), data).forEach(map -> {
                                telegramBotComponent.getHandler().send(
                                        -4739098379L,
                                        idGame + ":" + UtilJson.toStringPretty(map, "{}"),
                                        null
                                );
                                @SuppressWarnings("uncheched")
                                int idPlayer = Integer.parseInt(((Map<String, ?>) map.get("goal")).get("playerID").toString());
                                context.getEvent().put(idPlayer, String.format(
                                        "%s of the %s",
                                        map.get("scoreTime"),
                                        map.get("period")
                                ));
                            });
                        } catch (Throwable e) {
                            throw new ForwardException(e);
                        }
                    });
                    if (context.getEvent().isEmpty()) {
                        if (!context.getEndGames().isEmpty()) {
                            promise.goTo("removeFinish");
                        } else {
                            promise.skipAllStep();
                        }
                    }
                })
                .thenWithResource("selectSubscribers", JdbcResource.class, (_, _, promise, jdbcResource) -> {
                    Context context = promise.getRepositoryMapClass(Context.class);
                    List<Map<String, Object>> execute = jdbcResource.execute(
                            new JdbcRequest(JTScheduler.SELECT_SUBSCRIBER_BY_PLAYER)
                                    .addArg("id_players", context.getEvent().keySet().stream().toList())
                                    .setDebug(false)
                    );
                    if (execute.isEmpty()) {
                        promise.skipAllStep();
                        return;
                    }
                    execute.forEach(map -> context.getSubscriber().computeIfAbsent(
                            Integer.parseInt(map.get("id_player").toString()),
                            _ -> new ArrayList<>()
                    ).add(Integer.parseInt(map.get("id_chat").toString())));
                    if (context.getSubscriber().isEmpty()) {
                        promise.skipAllStep();
                    }
                })
                .extension(NHLPlayerList::promiseExtensionGetPlayerList)
                .then("send", (atomicBoolean, _, promise) -> {
                    Context context = promise.getRepositoryMapClass(Context.class);
                    UtilTank01.Response response = promise.getRepositoryMapClass(UtilTank01.Response.class);
                    Map<Integer, String> event = context.getEvent();

                    UtilRisc.forEach(atomicBoolean, context.getSubscriber(), (idPlayer, listIdChat) -> {
                        try {
                            Map<String, Object> player = NHLPlayerList.findById(
                                    String.valueOf(idPlayer),
                                    response.getData()
                            );
                            if (player == null || player.isEmpty()) {
                                return;
                            }
                            String message = String.format(
                                    "%s scored a goal at %s.",
                                    NHLPlayerList.getPlayerName(player),
                                    event.get(idPlayer)
                            );

                            UtilRisc.forEach(atomicBoolean, listIdChat, idChat -> {
                                telegramBotComponent.getHandler().send(idChat, message, null);
                            });
                        } catch (Throwable e) {
                            App.error(e);
                        }
                    });
                })
                .thenWithResource("updateDB", JdbcResource.class, (run, _, promise, jdbcResource) -> {
                    Context context = promise.getRepositoryMapClass(Context.class);
                    UtilRisc.forEach(run, context.getResponse(), (key, data) -> {
                        try {
                            jdbcResource.execute(
                                    new JdbcRequest(context.getLastDB().get(key) == null
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
