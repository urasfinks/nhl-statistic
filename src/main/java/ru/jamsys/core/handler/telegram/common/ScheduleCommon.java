package ru.jamsys.core.handler.telegram.common;

import lombok.Getter;
import lombok.Setter;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestMapping;
import ru.jamsys.core.App;
import ru.jamsys.core.component.ServicePromise;
import ru.jamsys.core.extension.builder.HashMapBuilder;
import ru.jamsys.core.extension.http.ServletResponseWriter;
import ru.jamsys.core.flat.util.UtilJson;
import ru.jamsys.core.flat.util.UtilTelegram;
import ru.jamsys.core.flat.util.telegram.Button;
import ru.jamsys.core.handler.promise.RegisterNotification;
import ru.jamsys.core.handler.telegram.ovi.Schedule;
import ru.jamsys.core.jt.JTPlayerSubscriber;
import ru.jamsys.core.jt.JTTeamScheduler;
import ru.jamsys.core.promise.Promise;
import ru.jamsys.core.promise.PromiseGenerator;
import ru.jamsys.core.resource.jdbc.JdbcRequest;
import ru.jamsys.core.resource.jdbc.JdbcResource;
import ru.jamsys.tank.data.NHLPlayerList;
import ru.jamsys.tank.data.NHLTeamSchedule;
import ru.jamsys.telegram.TelegramNotification;
import ru.jamsys.telegram.TelegramCommandContext;
import ru.jamsys.telegram.handler.NhlStatisticsBotCommandHandler;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@SuppressWarnings("unused")
@Setter
@Getter
@Component
@RequestMapping({"/schedule/**", "/ms/**"})
public class ScheduleCommon implements PromiseGenerator, NhlStatisticsBotCommandHandler {

    private final ServicePromise servicePromise;

    public ScheduleCommon(ServicePromise servicePromise) {
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
                    List<JTPlayerSubscriber.Row> execute = jdbcResource.execute(new JdbcRequest(JTPlayerSubscriber.SELECT_MY_PLAYERS)
                            .addArg("id_chat", context.getIdChat())
                                    .setDebug(false),
                            JTPlayerSubscriber.Row.class
                    );
                    if (execute.isEmpty()) {
                        RegisterNotification.add(new TelegramNotification(
                                context.getIdChat(),
                                context.getTelegramBot().getBotUsername(),
                                "В текущий момент подписок нет",
                                null,
                                null
                        ));
                        promise.skipAllStep("subscriptions empty");
                        return;
                    }
                    List<Button> buttons = new ArrayList<>();
                    execute.forEach(row -> {
                        NHLPlayerList.Player player = row.getPlayer();
                        if (player == null) {
                            App.error(new RuntimeException("player is null"));
                            return;
                        }
                        buttons.add(new Button(
                                player.getLongNameWithTeamAbv(),
                                ServletResponseWriter.buildUrlQuery(
                                        "/ms/",
                                        new HashMapBuilder<>(context.getUriParameters())
                                                .append("id", player.getPlayerID())
                                )
                        ));
                    });
                    RegisterNotification.add(new TelegramNotification(
                            context.getIdChat(),
                            context.getTelegramBot().getBotUsername(),
                            "Выбери игрока для отображения расписания",
                            buttons,
                            null
                    ));

                    promise.skipAllStep("wait read id_player for more information");
                })
                .then("getSubscriptionsMarker", (_, _, promise) -> {
                    TelegramCommandContext context = promise.getRepositoryMapClass(TelegramCommandContext.class);
                    if (!context.getUriParameters().containsKey("page")) {
                        NHLPlayerList.Player player = NHLPlayerList.findByIdStatic(context.getUriParameters().get("id"));
                        if (player == null) {
                            promise.skipAllStep("player is null");
                            return;
                        }
                        context.getTelegramBot().send(
                                UtilTelegram.editMessage(
                                context.getMsg(),
                                player.getLongNameWithTeamAbv()
                        ), context.getIdChat());
                    }
                })
                .thenWithResource("getSubscriptionsPlayerGames", JdbcResource.class, (_, _, promise, jdbcResource) -> {
                    TelegramCommandContext context = promise.getRepositoryMapClass(TelegramCommandContext.class);
                    NHLPlayerList.Player player = NHLPlayerList.findByIdStatic(context.getUriParameters().get("id"));
                    if (player == null) {
                        promise.skipAllStep("player is null");
                        return;
                    }
                    List<Map<String, Object>> execute = jdbcResource.execute(new JdbcRequest(JTTeamScheduler.SELECT_TEAM_SCHEDULER)
                            .addArg("id_team", player.getTeamID())
                            .setDebug(false)
                    );
                    if (execute.isEmpty()) {
                        RegisterNotification.add(new TelegramNotification(
                                context.getIdChat(),
                                context.getTelegramBot().getBotUsername(),
                                "В текущий момент запланированных игр нет",
                                null,
                                null
                        ));
                        return;
                    }
                    int page = Integer.parseInt(context.getUriParameters().getOrDefault("page", "1"));
                    Schedule.paging(execute.stream().map(map -> {
                        try {
                            return new NHLTeamSchedule.Game(UtilJson.getMapOrThrow(map.get("json").toString()));
                        } catch (Throwable e) {
                            App.error(e);
                        }
                        return null;
                    }).toList(), page, context, """
                            📅 Расписание ближайших игр:
                            
                            %s
                            
                            📍 Время начала игр указано по МСК
                            """);
                })
                ;
    }

}
