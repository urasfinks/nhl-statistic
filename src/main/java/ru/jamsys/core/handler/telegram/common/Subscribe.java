package ru.jamsys.core.handler.telegram.common;

import lombok.Getter;
import lombok.Setter;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestMapping;
import ru.jamsys.core.App;
import ru.jamsys.core.component.ServicePromise;
import ru.jamsys.core.extension.builder.HashMapBuilder;
import ru.jamsys.core.extension.http.ServletResponseWriter;
import ru.jamsys.core.flat.util.*;
import ru.jamsys.core.flat.util.telegram.Button;
import ru.jamsys.core.handler.promise.Tank01Request;
import ru.jamsys.core.jt.JTScheduler;
import ru.jamsys.core.promise.Promise;
import ru.jamsys.core.promise.PromiseGenerator;
import ru.jamsys.core.resource.jdbc.JdbcRequest;
import ru.jamsys.core.resource.jdbc.JdbcResource;
import ru.jamsys.tank.data.NHLPlayerList;
import ru.jamsys.tank.data.NHLTeamSchedule;
import ru.jamsys.telegram.TelegramCommandContext;
import ru.jamsys.telegram.handler.NhlStatisticsBotCommandHandler;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@SuppressWarnings("unused")
@Setter
@Getter
@Component
@RequestMapping({"/subscribe/**", "/stp/**"})
public class Subscribe implements PromiseGenerator, NhlStatisticsBotCommandHandler {

    private final ServicePromise servicePromise;

    public Subscribe(ServicePromise servicePromise) {
        this.servicePromise = servicePromise;
    }

    @Override
    public Promise generate() {
        Promise gen = servicePromise.get(getClass().getSimpleName(), 12_000L);
        gen.then("check", (_, _, promise) -> {
                    TelegramCommandContext context = promise.getRepositoryMapClass(TelegramCommandContext.class);
                    if (UtilNHL.getActiveSeasonOrNext() == null) {
                        context.getTelegramBot().send(
                                context.getIdChat(),
                                "Регулярный сезон ещё не начался. Подписка доступна с октября по Апрель",
                                null
                        );
                        promise.skipAllStep("Not found run season");
                        return;
                    }
                    if (!context.getUriParameters().containsKey("namePlayer")) {
                        context.getStepHandler().put(context.getIdChat(), context.getUriPath() + "/?namePlayer=");
                        context.getTelegramBot().send(
                                context.getIdChat(),
                                "Введи имя игрока на английском языке:",
                                null
                        );
                        promise.skipAllStep("wait player name");
                        return;
                    }
                    if (context.getUriParameters().containsKey("idPlayer")) {
                        promise.goTo("findPlayerByIdMarker");
                    }
                })
                .then("getPlayerList", new Tank01Request(NHLPlayerList::getUri).generate())
                .then("findPlayerByName", (_, _, promise) -> {
                    Tank01Request response = promise
                            .getRepositoryMapClass(Promise.class, "getPlayerList")
                            .getRepositoryMapClass(Tank01Request.class);
                    TelegramCommandContext context = promise.getRepositoryMapClass(TelegramCommandContext.class);
                    List<Map<String, Object>> userList = NHLPlayerList.findByName(
                            context.getUriParameters().get("namePlayer"),
                            response.getResponseData()
                    );
                    if (userList.isEmpty()) {
                        context.getTelegramBot().send(context.getIdChat(), "Игрок не найден", null);
                        return;
                    }
                    int counter = 0;
                    List<Button> buttons = new ArrayList<>();
                    for (Map<String, Object> player : userList) {
                        buttons.add(new Button(
                                player.get("longName").toString() + " (" + player.get("team").toString() + ")",
                                ServletResponseWriter.buildUrlQuery(
                                        "/stp/",
                                        new HashMapBuilder<>(context.getUriParameters())
                                                .append("idPlayer", player.get("playerID").toString())
                                )
                        ));
                        counter++;
                        if (counter > 9) {
                            break;
                        }
                    }
                    context.getTelegramBot().send(context.getIdChat(), "Выбери игрока:", buttons);
                    promise.skipAllStep("wait id_player");
                })
                .then("findPlayerByIdMarker", (_, _, _) -> {
                })
                .thenWithResource("checkAlready", JdbcResource.class, (_, _, promise, jdbcResource) -> {
                    TelegramCommandContext context = promise.getRepositoryMapClass(TelegramCommandContext.class);
                    List<Map<String, Object>> execute = jdbcResource.execute(new JdbcRequest(JTScheduler.SELECT_MY_SUBSCRIBED_GAMES)
                            .addArg("id_chat", context.getIdChat())
                            .addArg("id_player", context.getUriParameters().get("idPlayer"))
                            .setDebug(false)
                    );
                    if (!execute.isEmpty()) {
                        context.getTelegramBot().send(UtilTelegram.editMessage(context.getMsg(), "Подписка уже существует"));
                        promise.skipAllStep("The subscription already exists");
                    }
                })
                .then("getPlayerList2", new Tank01Request(NHLPlayerList::getUri).generate())
                .then("findPlayerById", (_, _, promise) -> {
                    Tank01Request response = promise
                            .getRepositoryMapClass(Promise.class, "getPlayerList2")
                            .getRepositoryMapClass(Tank01Request.class);
                    TelegramCommandContext context = promise.getRepositoryMapClass(TelegramCommandContext.class);
                    Map<String, Object> player = NHLPlayerList.findById(
                            context.getUriParameters().get("idPlayer"),
                            response.getResponseData()
                    );
                    if (player == null || player.isEmpty()) {
                        context.getTelegramBot().send(UtilTelegram.editMessage(context.getMsg(), "Игрок не найден"));
                        promise.skipAllStep("Not found player");
                        return;
                    }
                    String playerInfo = NHLPlayerList.getPlayerName(player);
                    context.getTelegramBot().send(UtilTelegram.editMessage(context.getMsg(), playerInfo));
                    context.getUriParameters().put("infoPlayer", playerInfo);
                    context.getUriParameters().put("idTeam", player.get("teamID").toString());
                })
                .then("getGameInSeason", new Tank01Request(() -> {
                    TelegramCommandContext context = gen.getRepositoryMapClass(TelegramCommandContext.class);
                    return NHLTeamSchedule.getUri(
                            context.getUriParameters().get("idTeam"),
                            UtilNHL.getActiveSeasonOrNext() + ""
                    );
                }).generate())
                .thenWithResource("insertSchedule", JdbcResource.class, (_, _, promise, jdbcResource) -> {
                    TelegramCommandContext context = promise.getRepositoryMapClass(TelegramCommandContext.class);
                    Tank01Request response = promise
                            .getRepositoryMapClass(Promise.class, "getGameInSeason")
                            .getRepositoryMapClass(Tank01Request.class);
                    NHLTeamSchedule.Instance schedule = new NHLTeamSchedule.Instance(response.getResponseData());
                    List<Map<String, Object>> sortGameByTime = schedule
                            .getFutureGame()
                            .sort(UtilListSort.Type.ASC)
                            .getListGame();
                    if (sortGameByTime.isEmpty()) {
                        context.getTelegramBot().send(UtilTelegram.editMessage(context.getMsg(), "Игры не найдены"));
                        return;
                    }
                    JdbcRequest jdbcRequest = new JdbcRequest(JTScheduler.INSERT);
                    sortGameByTime.forEach(map -> jdbcRequest
                            .addArg("id_chat", UtilTelegram.getIdChat(context.getMsg()))
                            .addArg("id_player", context.getUriParameters().get("idPlayer"))
                            .addArg("id_team", context.getUriParameters().get("idTeam"))
                            .addArg("id_game", map.get("gameID"))
                            .addArg("time_game_start", new BigDecimal(
                                    map.get("gameTime_epoch").toString()
                            ).longValue() * 1000)
                            .addArg("game_about", new NHLTeamSchedule.Game(map).getGameAbout())
                            .addArg("player_about", context.getUriParameters().get("infoPlayer"))
                            .addArg("test", UtilJson.toStringPretty(map, "{}"))
                            .nextBatch());

                    jdbcResource.execute(jdbcRequest);

                    context.getTelegramBot().send(UtilTelegram.editMessage(context.getMsg(), String.format("""
                                    Создана подписка на %d %s %s.
                                    Первая игра будет: %s, последняя: %s.
                                    Для детального отображения запланированных игр используй: /schedule
                                    
                                    📍 Время указано по МСК
                                    """,
                            sortGameByTime.size(),
                            Util.digitTranslate(sortGameByTime.size(), "игру", "игры", "игр"),
                            context.getUriParameters().get("infoPlayer"),
                            new NHLTeamSchedule.Game(sortGameByTime.getFirst()).getMoscowDate(),
                            new NHLTeamSchedule.Game(sortGameByTime.getLast()).getMoscowDate()
                    )));
                })
                .onError((atomicBoolean, promiseTask, promise) -> {
                    System.out.println(promise.getLogString());
                    try {
                        TelegramCommandContext context = promise.getRepositoryMapClass(TelegramCommandContext.class);
                        context.getTelegramBot().send(context.getIdChat(), "Бот сломался", null);
                    } catch (Throwable th) {
                        App.error(th);
                    }
                });
        return gen;
    }

}
