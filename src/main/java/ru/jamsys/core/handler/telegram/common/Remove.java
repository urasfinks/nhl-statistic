package ru.jamsys.core.handler.telegram.common;

import lombok.Getter;
import lombok.Setter;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestMapping;
import ru.jamsys.core.component.ServicePromise;
import ru.jamsys.core.extension.builder.HashMapBuilder;
import ru.jamsys.core.extension.http.ServletResponseWriter;
import ru.jamsys.core.flat.util.UtilTelegram;
import ru.jamsys.core.flat.util.telegram.Button;
import ru.jamsys.core.jt.JTScheduler;
import ru.jamsys.core.promise.Promise;
import ru.jamsys.core.promise.PromiseGenerator;
import ru.jamsys.core.resource.jdbc.JdbcRequest;
import ru.jamsys.core.resource.jdbc.JdbcResource;
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
@RequestMapping({"/remove/**", "/rs/**"})
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
                    List<Map<String, Object>> execute = jdbcResource.execute(new JdbcRequest(JTScheduler.SELECT_MY_SUBSCRIBED_PLAYER)
                            .addArg("id_chat", context.getIdChat())
                            .setDebug(false)
                    );
                    if (execute.isEmpty()) {
                        context.getTelegramBot().send(
                                context.getIdChat(),
                                "В текущей момент подписок нет",
                                null
                        );
                        promise.skipAllStep("subscribe empty");
                        return;
                    }
                    List<Button> buttons = new ArrayList<>();
                    AtomicInteger activeGame = new AtomicInteger();
                    execute.forEach(map -> {
                        buttons.add(new Button(
                                map.get("player_about").toString(),
                                ServletResponseWriter.buildUrlQuery(
                                        "/rs/",
                                        new HashMapBuilder<>(context.getUriParameters())
                                                .append("id", map.get("id_player").toString())
                                                .append("a", map.get("player_about").toString())
                                )
                        ));
                        activeGame.addAndGet(Integer.parseInt(map.get("count").toString()));
                    });
                    context.getTelegramBot().send(
                            context.getIdChat(),
                            "Выбери игрока для удаления подписки",
                            buttons
                    );
                    promise.skipAllStep("wait read id_player for unsubscribe");
                })
                .then("getSubscriptionsMarker", (_, _, promise) -> {
                    TelegramCommandContext context = promise.getRepositoryMapClass(TelegramCommandContext.class);
                    context.getTelegramBot().send(UtilTelegram.editMessage(
                            context.getMsg(),
                            context.getUriParameters().get("a")
                    ));
                })
                .thenWithResource("removeSubscription", JdbcResource.class, (_, _, promise, jdbcResource) -> {
                    TelegramCommandContext context = promise.getRepositoryMapClass(TelegramCommandContext.class);
                    jdbcResource.execute(new JdbcRequest(JTScheduler.REMOVE_MY_SUBSCRIBED)
                            .addArg("id_chat", context.getIdChat())
                            .addArg("id_player", context.getUriParameters().get("id"))
                            .setDebug(false)
                    );
                    context.getTelegramBot().send(context.getIdChat(), "Подписка удалена", null);
                });
    }

}
