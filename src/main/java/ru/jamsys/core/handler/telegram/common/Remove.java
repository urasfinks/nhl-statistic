package ru.jamsys.core.handler.telegram.common;

import lombok.Getter;
import lombok.Setter;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestMapping;
import ru.jamsys.NhlStatisticApplication;
import ru.jamsys.core.component.ServicePromise;
import ru.jamsys.core.extension.builder.HashMapBuilder;
import ru.jamsys.core.extension.http.ServletResponseWriter;
import ru.jamsys.core.flat.util.UtilTelegramMessage;
import ru.jamsys.core.flat.util.telegram.Button;
import ru.jamsys.core.handler.promise.RegisterNotification;
import ru.jamsys.core.jt.JTPlayerSubscriber;
import ru.jamsys.core.promise.Promise;
import ru.jamsys.core.promise.PromiseGenerator;
import ru.jamsys.core.resource.jdbc.JdbcRequest;
import ru.jamsys.core.resource.jdbc.JdbcResource;
import ru.jamsys.tank.data.NHLPlayerList;
import ru.jamsys.telegram.TelegramNotification;
import ru.jamsys.telegram.TelegramCommandContext;
import ru.jamsys.telegram.handler.NhlStatisticsBotCommandHandler;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("unused")
@Setter
@Getter
@Component
@RequestMapping("/remove/**")
public class Remove implements PromiseGenerator, NhlStatisticsBotCommandHandler {

    private final ServicePromise servicePromise;

    public Remove(ServicePromise servicePromise) {
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
                                "В текущей момент подписок нет",
                                null,
                                null
                        ));
                        promise.skipAllStep("subscribe empty");
                        return;
                    }
                    List<Button> buttons = new ArrayList<>();
                    execute.forEach(map -> {
                        NHLPlayerList.Player player = map.getPlayer();
                        buttons.add(new Button(
                                player.getLongNameWithTeamAbv(),
                                ServletResponseWriter.buildUrlQuery(
                                        "/remove/",
                                        new HashMapBuilder<>(context.getUriParameters())
                                                .append("id", player.getPlayerID())
                                )
                        ));
                    });
                    RegisterNotification.add(new TelegramNotification(
                            context.getIdChat(),
                            context.getTelegramBot().getBotUsername(),
                            "Выбери игрока для удаления подписки",
                            buttons,
                            null
                    ));
                    promise.skipAllStep("wait read id_player for unsubscribe");
                })
                .then("getSubscriptionsMarker", (_, _, promise) -> {
                    TelegramCommandContext context = promise.getRepositoryMapClass(TelegramCommandContext.class);
                    NHLPlayerList.Player player = NHLPlayerList.findByIdStatic(context.getUriParameters().get("id"));
                    if (player == null) {
                        promise.skipAllStep("player is null");
                        return;
                    }
                    context.getTelegramBot().send(
                            UtilTelegramMessage.editMessage(
                            context.getMsg(),
                            player.getLongNameWithTeamAbv()
                    ), context.getIdChat());
                })
                .thenWithResource("removeSubscription", JdbcResource.class, (_, _, promise, jdbcResource) -> {
                    TelegramCommandContext context = promise.getRepositoryMapClass(TelegramCommandContext.class);
                    jdbcResource.execute(new JdbcRequest(JTPlayerSubscriber.DELETE_SUBSCRIBE_PLAYER)
                            .addArg("id_chat", context.getIdChat())
                            .addArg("id_player", context.getUriParameters().get("id"))
                            .setDebug(false)
                    );
                    RegisterNotification.add(new TelegramNotification(
                            context.getIdChat(),
                            context.getTelegramBot().getBotUsername(),
                            "Подписка удалена",
                            null,
                            null
                    ));
                })
                .extension(NhlStatisticApplication::addOnError);
    }

}
