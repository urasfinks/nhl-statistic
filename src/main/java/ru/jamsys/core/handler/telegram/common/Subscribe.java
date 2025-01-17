package ru.jamsys.core.handler.telegram.common;

import lombok.Getter;
import lombok.Setter;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestMapping;
import ru.jamsys.core.App;
import ru.jamsys.core.component.ServicePromise;
import ru.jamsys.core.extension.builder.HashMapBuilder;
import ru.jamsys.core.extension.http.ServletResponseWriter;
import ru.jamsys.core.flat.util.Util;
import ru.jamsys.core.flat.util.UtilNHL;
import ru.jamsys.core.flat.util.UtilTelegram;
import ru.jamsys.core.flat.util.telegram.Button;
import ru.jamsys.core.handler.promise.SaveTelegramSend;
import ru.jamsys.core.handler.promise.Tank01Request;
import ru.jamsys.core.handler.promise.UpdateScheduler;
import ru.jamsys.core.jt.JTPlayerSubscriber;
import ru.jamsys.core.promise.Promise;
import ru.jamsys.core.promise.PromiseGenerator;
import ru.jamsys.core.resource.jdbc.JdbcRequest;
import ru.jamsys.core.resource.jdbc.JdbcResource;
import ru.jamsys.tank.data.NHLPlayerList;
import ru.jamsys.tank.data.NHLTeamSchedule;
import ru.jamsys.telegram.NotificationObject;
import ru.jamsys.telegram.TelegramCommandContext;
import ru.jamsys.telegram.handler.NhlStatisticsBotCommandHandler;

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
                        SaveTelegramSend.add(new NotificationObject(
                                context.getIdChat(),
                                context.getTelegramBot().getBotUsername(),
                                "Регулярный сезон ещё не начался. Подписка доступна с октября по Апрель",
                                null,
                                null
                        ));
                        promise.skipAllStep("Not found run season");
                        return;
                    }
                    if (!context.getUriParameters().containsKey("namePlayer")) {
                        context.getStepHandler().put(context.getIdChat(), context.getUriPath() + "/?namePlayer=");
                        SaveTelegramSend.add(new NotificationObject(
                                context.getIdChat(),
                                context.getTelegramBot().getBotUsername(),
                                "Введи имя игрока на английском языке:",
                                null,
                                null
                        ));
                        promise.skipAllStep("wait player name");
                        return;
                    }
                    if (context.getUriParameters().containsKey("idPlayer")) {
                        promise.goTo("findPlayerByIdMarker");
                    }
                })
                .then("getPlayerList", new Tank01Request(NHLPlayerList::getUri).generate())
                .then("findPlayerByName", (_, _, promise) -> {
                    TelegramCommandContext context = promise.getRepositoryMapClass(TelegramCommandContext.class);
                    List<Map<String, Object>> userList = NHLPlayerList.findByName(
                            context.getUriParameters().get("namePlayer"),
                            promise
                                    .getRepositoryMapClass(Promise.class, "getPlayerList")
                                    .getRepositoryMapClass(Tank01Request.class).getResponseData()
                    );
                    if (userList.isEmpty()) {
                        SaveTelegramSend.add(new NotificationObject(
                                context.getIdChat(),
                                context.getTelegramBot().getBotUsername(),
                                "Игрок не найден",
                                null,
                                null
                        ));
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
                    SaveTelegramSend.add(new NotificationObject(
                            context.getIdChat(),
                            context.getTelegramBot().getBotUsername(),
                            "Выбери игрока:",
                            buttons,
                            null
                    ));
                    promise.skipAllStep("wait id_player");
                })
                .then("findPlayerByIdMarker", (_, _, _) -> {
                })
                .thenWithResource("checkAlready", JdbcResource.class, (_, _, promise, jdbcResource) -> {
                    TelegramCommandContext context = promise.getRepositoryMapClass(TelegramCommandContext.class);
                    List<Map<String, Object>> execute = jdbcResource.execute(new JdbcRequest(JTPlayerSubscriber.SELECT_IS_SUBSCRIBE_PLAYER)
                            .addArg("id_chat", context.getIdChat())
                            .addArg("id_player", context.getUriParameters().get("idPlayer"))
                            .setDebug(false)
                    );
                    if (!execute.isEmpty()) {
                        SaveTelegramSend.add(new NotificationObject(
                                context.getIdChat(),
                                context.getTelegramBot().getBotUsername(),
                                "Подписка уже существует",
                                null,
                                null
                        ));
                        promise.skipAllStep("The subscription already exists");
                    }
                })
                .then("getPlayerList2", new Tank01Request(NHLPlayerList::getUri).generate())
                .then("findPlayerById", (_, _, promise) -> {
                    TelegramCommandContext context = promise.getRepositoryMapClass(TelegramCommandContext.class);
                    NHLPlayerList.Player player = NHLPlayerList.findById(
                            context.getUriParameters().get("idPlayer"),
                            promise
                                    .getRepositoryMapClass(Promise.class, "getPlayerList2")
                                    .getRepositoryMapClass(Tank01Request.class).getResponseData()
                    );
                    if (player == null) {
                        context.getTelegramBot().send(
                                UtilTelegram.editMessage(context.getMsg(), "Игрок не найден"),
                                context.getIdChat()
                        );
                        promise.skipAllStep("Not found player");
                        return;
                    }
                    String playerInfo = player.getLongNameWithTeamAbv();
                    context.getTelegramBot().send(
                            UtilTelegram.editMessage(context.getMsg(), playerInfo),
                            context.getIdChat()
                    );
                    context.getUriParameters().put("infoPlayer", playerInfo);
                    context.getUriParameters().put("idTeam", player.getTeamID());
                })
                .thenWithResource("insertPlayerSubscriber", JdbcResource.class, (_, _, promise, jdbcResource) -> {
                    TelegramCommandContext context = promise.getRepositoryMapClass(TelegramCommandContext.class);
                    jdbcResource.execute(new JdbcRequest(JTPlayerSubscriber.INSERT)
                            .addArg("id_chat", UtilTelegram.getIdChat(context.getMsg()))
                            .addArg("id_player", context.getUriParameters().get("idPlayer"))
                            .addArg("id_team", context.getUriParameters().get("idTeam"))
                    );
                })
                .then("updateScheduler", new UpdateScheduler(false).generate())
                .then("notify", (atomicBoolean, promiseTask, promise) -> {
                    TelegramCommandContext context = promise.getRepositoryMapClass(TelegramCommandContext.class);
                    UpdateScheduler updateScheduler = promise
                            .getRepositoryMapClass(Promise.class, "updateScheduler")
                            .getRepositoryMapClass(UpdateScheduler.class);
                    List<NHLTeamSchedule.Game> games = updateScheduler
                            .getTeamsGameInstance()
                            .get(context.getUriParameters().get("idTeam"));
                    if (games.isEmpty()) {
                        context.getTelegramBot().send(
                                UtilTelegram.editMessage(context.getMsg(), "Игры не найдены"),
                                context.getIdChat()
                        );
                        return;
                    }
                    context.getTelegramBot().send(
                            UtilTelegram.editMessage(context.getMsg(), String.format("""
                                    Создана подписка на %d %s %s.
                                    Первая игра будет: %s, последняя: %s.
                                    Для детального отображения запланированных игр используй: /schedule
                                    
                                    📍 Время указано по МСК
                                    """,
                            games.size(),
                            Util.digitTranslate(games.size(), "игру", "игры", "игр"),
                            context.getUriParameters().get("infoPlayer"),
                            games.getFirst().getMoscowDate(),
                            games.getLast().getMoscowDate()
                    )), context.getIdChat());
                })
                .onError((atomicBoolean, promiseTask, promise) -> {
                    try {
                        TelegramCommandContext context = promise.getRepositoryMapClass(TelegramCommandContext.class);
                        SaveTelegramSend.add(new NotificationObject(
                                context.getIdChat(),
                                context.getTelegramBot().getBotUsername(),
                                "Бот сломался",
                                null,
                                null
                        ));
                    } catch (Throwable th) {
                        App.error(th);
                    }
                });
        return gen;
    }

}
