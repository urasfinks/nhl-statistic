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
import ru.jamsys.core.flat.util.UtilJson;
import ru.jamsys.core.flat.util.UtilNHL;
import ru.jamsys.core.flat.util.UtilTelegram;
import ru.jamsys.core.flat.util.telegram.Button;
import ru.jamsys.core.jt.JTScheduler;
import ru.jamsys.core.promise.Promise;
import ru.jamsys.core.promise.PromiseGenerator;
import ru.jamsys.core.resource.jdbc.JdbcRequest;
import ru.jamsys.core.resource.jdbc.JdbcResource;
import ru.jamsys.tank.data.NHLTeamSchedule;
import ru.jamsys.telegram.AbstractBot;
import ru.jamsys.telegram.TelegramCommandContext;
import ru.jamsys.telegram.handler.NhlStatisticsBotCommandHandler;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

@SuppressWarnings("unused")
@Setter
@Getter
@Component
@RequestMapping({"/my/**", "/ms/**"})
public class My implements PromiseGenerator, NhlStatisticsBotCommandHandler {

    private final ServicePromise servicePromise;

    public My(ServicePromise servicePromise) {
        this.servicePromise = servicePromise;
    }

    @Override
    public Promise generate() {
        return servicePromise.get(getClass().getSimpleName(), 12_000L)
                .then("check", (_, _, promise) -> {
                    TelegramCommandContext context = promise.getRepositoryMapClass(TelegramCommandContext.class);
                    if (context.getUriParameters().containsKey("id")) {
                        promise.goTo("getSubscriptionsMarker");
                    }
                })
                .thenWithResource("getSubscriptionsPlayer", JdbcResource.class, (_, _, promise, jdbcResource) -> {
                    TelegramCommandContext context = promise.getRepositoryMapClass(TelegramCommandContext.class);
                    List<Map<String, Object>> execute = jdbcResource.execute(new JdbcRequest(JTScheduler.SELECT_MY_SUBSCRIBED_PLAYER)
                            .addArg("id_chat", context.getIdChat())
                            .setDebug(false)
                    );
                    if (execute.isEmpty()) {
                        context.getTelegramBot().send(
                                context.getIdChat(),
                                "В текущий момент подписок нет",
                                null
                        );
                        promise.skipAllStep("subscriptions empty");
                        return;
                    }
                    List<Button> buttons = new ArrayList<>();
                    AtomicInteger activeGame = new AtomicInteger();
                    execute.forEach(map -> {
                        buttons.add(new Button(
                                String.format(
                                        "%s, game: %s",
                                        map.get("player_about").toString(),
                                        map.get("count").toString()
                                ),
                                ServletResponseWriter.buildUrlQuery(
                                        "/ms/",
                                        new HashMapBuilder<>(context.getUriParameters())
                                                .append("id", map.get("id_player").toString())
                                                .append("a", map.get("player_about").toString())
                                )
                        ));
                        activeGame.addAndGet(Integer.parseInt(map.get("count").toString()));
                    });
                    context.getTelegramBot().send(context.getIdChat(), String.format(
                            "Подписка на %d %s. Выбери игрока для отображения детальной информации",
                            activeGame.get(),
                            Util.digitTranslate(activeGame.get(), "матч", "матча", "матчей")
                    ), buttons);
                    promise.skipAllStep("wait read id_player for more information");
                })
                .then("getSubscriptionsMarker", (_, _, promise) -> {
                    TelegramCommandContext context = promise.getRepositoryMapClass(TelegramCommandContext.class);
                    context.getTelegramBot().send(UtilTelegram.editMessage(
                            context.getMsg(),
                            context.getUriParameters().get("a")
                    ));
                })
                .thenWithResource("getSubscriptionsPlayerGames", JdbcResource.class, (_, _, promise, jdbcResource) -> {
                    TelegramCommandContext context = promise.getRepositoryMapClass(TelegramCommandContext.class);
                    List<Map<String, Object>> execute = jdbcResource.execute(new JdbcRequest(JTScheduler.SELECT_MY_SUBSCRIBED_GAMES)
                            .addArg("id_chat", context.getIdChat())
                            .addArg("id_player", context.getUriParameters().get("id"))
                            .setDebug(false)
                    );
                    if (execute.isEmpty()) {
                        context.getTelegramBot().send(
                                context.getIdChat(),
                                "В текущий момент запланированных игр нет",
                                null
                        );
                        return;
                    }
                    StringBuilder sb = new StringBuilder();
                    execute.forEach(map -> {
                        try {
                            NHLTeamSchedule.Game game = new NHLTeamSchedule.Game(UtilJson.getMapOrThrow(map.get("test").toString()));
                            sb.append(String.format("""
                                            %s — 🆚 %s, %s (GMT+03:00)
                                            """,
                                    game.getMoscowDate("dd.MM.yyyy"),
                                    game.toggleTeam(UtilNHL.getOvi().getTeam()),
                                    game.getMoscowDate("HH:mm")
                            )).append("\n");
                        } catch (Throwable th) {
                            App.error(th);
                        }
                    });

                    AbstractBot.splitMessageSmart(String.format("""
                                            📅 Расписание ближайших игр:
                                            
                                            %s
                                            
                                            📍 Время начала игр указано по МСК (GMT+03:00)
                                            """,
                                    sb
                            ), 3000)
                            .forEach(s -> context.getTelegramBot().send(context.getIdChat(), s, null));
                })
                ;
    }

}
