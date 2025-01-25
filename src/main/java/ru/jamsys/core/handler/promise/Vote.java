package ru.jamsys.core.handler.promise;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import ru.jamsys.NhlStatisticApplication;
import ru.jamsys.core.component.ServicePromise;
import ru.jamsys.core.extension.builder.HashMapBuilder;
import ru.jamsys.core.flat.util.Util;
import ru.jamsys.core.jt.JTVote;
import ru.jamsys.core.promise.Promise;
import ru.jamsys.core.promise.PromiseGenerator;
import ru.jamsys.core.resource.jdbc.JdbcRequest;
import ru.jamsys.core.resource.jdbc.JdbcResource;
import ru.jamsys.telegram.TelegramCommandContext;
import ru.jamsys.telegram.TelegramNotification;
import ru.jamsys.telegram.handler.NhlStatisticsBotCommandHandler;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

@SuppressWarnings("unused")
@Setter
@Getter
public class Vote implements PromiseGenerator, NhlStatisticsBotCommandHandler {

    private final ServicePromise servicePromise;

    public Vote(ServicePromise servicePromise) {
        this.servicePromise = servicePromise;
    }

    @Getter
    @Setter
    @Accessors(chain = true)
    public static class Context {
        private String extraText = "";
        private List<Map<String, Object>> vote;
        private boolean already = false;
        private String defaultIdPlayer;
        private String defaultIdGame;
    }

    @Override
    public Promise generate() {
        return servicePromise.get(getClass().getSimpleName(), 12_000L)
                .extension(promise -> promise.setRepositoryMapClass(Context.class, new Context()))
                .then("check", (atomicBoolean, promiseTask, promise) -> {
                    TelegramCommandContext contextTelegram = promise.getRepositoryMapClass(TelegramCommandContext.class);
                    if (contextTelegram.getUriParameters().isEmpty()) {
                        promise.goTo("selectAgg");
                        return;
                    }
                    Map<String, String> uriParameters = contextTelegram.getUriParameters();
                    if (
                            !uriParameters.containsKey("g") //id_game, в целом это может быть просто произвольным ключём агрегации
                                    || !uriParameters.containsKey("p") //id_player
                                    || !uriParameters.containsKey("v") //vote
                    ) {
                        promise.goTo("selectAgg");
                    }
                })
                .thenWithResource("insertIfNotExist", JdbcResource.class, (_, _, promise, jdbcResource) -> {
                    TelegramCommandContext contextTelegram = promise.getRepositoryMapClass(TelegramCommandContext.class);
                    Context context = promise.getRepositoryMapClass(Context.class);
                    List<Map<String, Object>> execute = jdbcResource.execute(new JdbcRequest(JTVote.SELECT)
                            .addArg("id_chat", contextTelegram.getIdChat())
                            .addArg("id_game", contextTelegram.getUriParameters().get("g"))
                            .addArg("id_player", contextTelegram.getUriParameters().get("p"))
                    );
                    if (!execute.isEmpty()) {
                        context.setAlready(true);
                        promise.goTo("selectAgg");
                        return;
                    }
                    jdbcResource.execute(new JdbcRequest(JTVote.INSERT)
                            .addArg("id_chat", contextTelegram.getIdChat())
                            .addArg("id_game", contextTelegram.getUriParameters().get("g"))
                            .addArg("id_player", contextTelegram.getUriParameters().get("p"))
                            .addArg("vote", contextTelegram.getUriParameters().get("v"))
                    );
                    // Если мы записываем данные, значит была нажата кнопка голосования
                    // Если была нажата кнопка - значит человек осмысленнно прочитал за что он голосует
                    // Если понятно что он нажал - то приставку не будем добавлять
                    context.setExtraText("");
                })
                .thenWithResource("selectAgg", JdbcResource.class, (_, _, promise, jdbcResource) -> {
                    TelegramCommandContext contextTelegram = promise.getRepositoryMapClass(TelegramCommandContext.class);
                    Context context = promise.getRepositoryMapClass(Context.class);
                    context.setVote(jdbcResource.execute(new JdbcRequest(JTVote.SELECT_AGG)
                            .addArg(
                                    "id_game",
                                    contextTelegram
                                            .getUriParameters()
                                            .getOrDefault("g", context.getDefaultIdGame())
                            )
                            .addArg(
                                    "id_player",
                                    contextTelegram
                                            .getUriParameters()
                                            .getOrDefault("p", context.getDefaultIdPlayer())
                            )
                    ));
                })
                .then("send", (atomicBoolean, promiseTask, promise) -> {
                    TelegramCommandContext contextTelegram = promise.getRepositoryMapClass(TelegramCommandContext.class);
                    Context context = promise.getRepositoryMapClass(Context.class);
                    List<TelegramNotification> list = new ArrayList<>();
                    list.add(new TelegramNotification(
                            contextTelegram.getIdChat(),
                            contextTelegram.getTelegramBot().getBotUsername(),
                            getStat(context.getVote(), context.getExtraText()),
                            null,
                            null
                    ));
                    if (context.isAlready()) {
                        list.add(new TelegramNotification(
                                contextTelegram.getIdChat(),
                                contextTelegram.getTelegramBot().getBotUsername(),
                                "Вы уже проголосовали",
                                null,
                                null
                        ));
                    }
                    RegisterNotification.add(list);
                })
                .extension(NhlStatisticApplication::addOnError);
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
