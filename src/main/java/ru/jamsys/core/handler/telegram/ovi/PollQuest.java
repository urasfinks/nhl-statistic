package ru.jamsys.core.handler.telegram.ovi;

import lombok.Getter;
import lombok.Setter;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestMapping;
import ru.jamsys.NhlStatisticApplication;
import ru.jamsys.core.component.ServicePromise;
import ru.jamsys.core.extension.builder.HashMapBuilder;
import ru.jamsys.core.flat.util.Util;
import ru.jamsys.core.handler.promise.RegisterNotification;
import ru.jamsys.core.jt.JTOviSubscriber;
import ru.jamsys.core.promise.Promise;
import ru.jamsys.core.promise.PromiseGenerator;
import ru.jamsys.core.resource.jdbc.JdbcRequest;
import ru.jamsys.core.resource.jdbc.JdbcResource;
import ru.jamsys.telegram.TelegramCommandContext;
import ru.jamsys.telegram.TelegramNotification;
import ru.jamsys.telegram.handler.OviGoalsBotCommandHandler;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

@SuppressWarnings("unused")
@Setter
@Getter
@Component
@RequestMapping({"/poll_quest/**"})
public class PollQuest implements PromiseGenerator, OviGoalsBotCommandHandler {

    private final ServicePromise servicePromise;

    public PollQuest(ServicePromise servicePromise) {
        this.servicePromise = servicePromise;
    }

    @Getter
    @Setter
    public static class Context{
        private List<Map<String, Object>> agg = new ArrayList<>();
    }

    @Override
    public Promise generate() {
        Promise gen = servicePromise.get(getClass().getSimpleName(), 12_000L);
        gen
                .extension(promise -> promise.setRepositoryMapClass(Context.class, new Context()))
                .then("check", (atomicBoolean, promiseTask, promise) -> {
                    TelegramCommandContext context = promise.getRepositoryMapClass(TelegramCommandContext.class);
                    if (context.getUriParameters().isEmpty()) {
                        promise.goTo("selectAgg");
                    }
                })
                .thenWithResource("updateQuest", JdbcResource.class, (_, _, promise, jdbcResource) -> {
                    TelegramCommandContext context = promise.getRepositoryMapClass(TelegramCommandContext.class);
                    boolean win = context.getUriParameters().getOrDefault("value", "true").equals("true");
                    jdbcResource.execute(new JdbcRequest(JTOviSubscriber.UPDATE_QUEST_1)
                            .addArg("quest", win ? "true" : "false")
                            .addArg("id_chat", context.getIdChat())
                    );
                })
                .thenWithResource("selectAgg", JdbcResource.class, (_, _, promise, jdbcResource) -> {
                    Context context = promise.getRepositoryMapClass(Context.class);
                    context.getAgg().addAll(jdbcResource.execute(new JdbcRequest(JTOviSubscriber.SELECT_QUEST_AGG)));
                    if (context.getAgg().isEmpty()) {
                        promise.skipAllStep("agg is empty");
                    }
                })
                .then("send", (atomicBoolean, promiseTask, promise) -> {
                    TelegramCommandContext commandContext = promise.getRepositoryMapClass(TelegramCommandContext.class);
                    Context context = promise.getRepositoryMapClass(Context.class);
                    RegisterNotification.add(new TelegramNotification(
                            commandContext.getIdChat(),
                            commandContext.getTelegramBot().getBotUsername(),
                            getStat(context.getAgg(), ""),
                            null,
                            null
                    ));
                })
                .extension(NhlStatisticApplication::addOnError);
        return gen;
    }

    public static String getStat(List<Map<String, Object>> agg, String extra) {
        Map<String, AtomicInteger> stat = new HashMapBuilder<String, AtomicInteger>()
                .append("true", new AtomicInteger(0))
                .append("false", new AtomicInteger(0));

        agg.forEach(map -> {
                    String count = map.getOrDefault("count", "0").toString();
                    if (!Util.isNumeric(count)) {
                        return;
                    }
                    stat.computeIfAbsent(
                            Objects.toString(map.getOrDefault("unit", "none"), "none"),
                            _ -> new AtomicInteger(0)
                    ).addAndGet(Integer.parseInt(count));
                }
        );

        int countTrue = stat.get("true").get();
        int countFalse = stat.get("false").get();

        int total = countTrue + countFalse;

        // Подсчитаем проценты и форматируем вывод
        double percentTrue = total == 0 ? 0 : ((countTrue * 100.0) / total);
        double percentFalse = total == 0 ? 0 : ((countFalse * 100.0) / total);

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
