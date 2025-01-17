package ru.jamsys.core.handler.telegram.ovi;

import lombok.Getter;
import lombok.Setter;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestMapping;
import ru.jamsys.core.App;
import ru.jamsys.core.component.ServicePromise;
import ru.jamsys.core.extension.builder.HashMapBuilder;
import ru.jamsys.core.flat.util.Util;
import ru.jamsys.core.handler.promise.SaveTelegramSend;
import ru.jamsys.core.jt.JTOviSubscriber;
import ru.jamsys.core.promise.Promise;
import ru.jamsys.core.promise.PromiseGenerator;
import ru.jamsys.core.resource.jdbc.JdbcRequest;
import ru.jamsys.core.resource.jdbc.JdbcResource;
import ru.jamsys.telegram.NotificationObject;
import ru.jamsys.telegram.TelegramCommandContext;
import ru.jamsys.telegram.handler.OviGoalsBotCommandHandler;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

@SuppressWarnings("unused")
@Setter
@Getter
@Component
@RequestMapping({"/poll_results/**"})
public class PollResults implements PromiseGenerator, OviGoalsBotCommandHandler {

    private final ServicePromise servicePromise;

    private List<Map<String, Object>> vote;

    public PollResults(ServicePromise servicePromise) {
        this.servicePromise = servicePromise;
    }

    @Override
    public Promise generate() {
        Promise gen = servicePromise.get(getClass().getSimpleName(), 12_000L);
        gen
                .then("check", (atomicBoolean, promiseTask, promise) -> {
                    promise.setRepositoryMapClass(String.class, "Побьет ли Александр Овечкин рекорд Уэйна Гретцки в этом сезоне?\n\n");
                    TelegramCommandContext context = promise.getRepositoryMapClass(TelegramCommandContext.class);
                    if (context.getUriParameters().isEmpty()) {
                        promise.goTo("agg");
                    }
                })
                .thenWithResource("vote", JdbcResource.class, (_, _, promise, jdbcResource) -> {
                    TelegramCommandContext context = promise.getRepositoryMapClass(TelegramCommandContext.class);
                    boolean win = context.getUriParameters().getOrDefault("value", "true").equals("true");
                    jdbcResource.execute(new JdbcRequest(JTOviSubscriber.UPDATE_VOTE)
                            .addArg("vote", win ? "true" : "false")
                            .addArg("id_chat", context.getIdChat())
                    );
                    promise.setRepositoryMapClass(String.class, "");
                })
                .thenWithResource("agg", JdbcResource.class, (_, _, promise, jdbcResource) -> {
                    vote = jdbcResource.execute(new JdbcRequest(JTOviSubscriber.VOTE));
                    if (vote.isEmpty()) {
                        promise.skipAllStep("vote empty");
                    }
                })
                .then("send", (atomicBoolean, promiseTask, promise) -> {
                    TelegramCommandContext context = promise.getRepositoryMapClass(TelegramCommandContext.class);
                    SaveTelegramSend.add(new NotificationObject(
                            context.getIdChat(),
                            context.getTelegramBot().getBotUsername(),
                            getStat(vote, promise.getRepositoryMapClass(String.class)),
                            null,
                            null
                    ));
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

    public static String getStat(List<Map<String, Object>> vote, String extra) {
        Map<String, AtomicInteger> stat = new HashMapBuilder<String, AtomicInteger>()
                .append("true", new AtomicInteger(0))
                .append("false", new AtomicInteger(0));

        vote.forEach(map -> {
                    String count = map.getOrDefault("count", "0").toString();
                    if (!Util.isNumeric(count)) {
                        return;
                    }
                    stat.computeIfAbsent(
                            Objects.toString(map.getOrDefault("vote", "none"), "none"),
                            _ -> new AtomicInteger(0)
                    ).addAndGet(Integer.parseInt(count));
                }
        );

        int aTrue = stat.get("true").get();
        int aFalse = stat.get("false").get();

        int totalVotes = aTrue + aFalse;

        // Подсчитаем проценты и форматируем вывод
        double percentTrue = totalVotes == 0 ? 0 : ((aTrue * 100.0) / totalVotes);
        double percentFalse = totalVotes == 0 ? 0 : ((aFalse * 100.0) / totalVotes);

        return String.format("""
                        %sСтатистика голосования:
                        
                        Да 🔥 – %.2f%%
                        Нет ⛔ – %.2f%%""",
                extra,
                percentTrue,
                percentFalse
        );
    }

}
