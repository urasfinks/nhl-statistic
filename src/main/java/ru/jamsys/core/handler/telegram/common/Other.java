package ru.jamsys.core.handler.telegram.common;

import lombok.Getter;
import lombok.Setter;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestMapping;
import ru.jamsys.NhlStatisticApplication;
import ru.jamsys.core.component.ServicePromise;
import ru.jamsys.core.extension.builder.ArrayListBuilder;
import ru.jamsys.core.extension.builder.HashMapBuilder;
import ru.jamsys.core.extension.http.ServletResponseWriter;
import ru.jamsys.core.flat.util.Util;
import ru.jamsys.core.flat.util.UtilDate;
import ru.jamsys.core.flat.util.UtilNHL;
import ru.jamsys.core.flat.util.telegram.Button;
import ru.jamsys.core.handler.promise.RegisterNotification;
import ru.jamsys.core.jt.JTOviSubscriber;
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

@SuppressWarnings("unused")
@Setter
@Getter
@Component
@RequestMapping("/other/**")
public class Other implements PromiseGenerator, NhlStatisticsBotCommandHandler {

    private final ServicePromise servicePromise;

    public Other(ServicePromise servicePromise) {
        this.servicePromise = servicePromise;
    }

    @Getter
    @Setter
    public static class Context {
        private final List<Long> listIdChat = new ArrayList<>();
    }

    @Override
    public Promise generate() {
        Promise gen = servicePromise.get(getClass().getSimpleName(), 12_000L);
        gen
                .extension(promise -> promise.setRepositoryMapClass(Context.class, new Context()))
                .then("prep", (_, _, promise) -> {
                    TelegramCommandContext context = promise.getRepositoryMapClass(TelegramCommandContext.class);
                    if (context.getIdChat() != 290029195L && context.getIdChat() != 241022301L) {
                        promise.skipAllStep("not admin test");
                    }
                })
                .thenWithResource("select", JdbcResource.class, (_, _, promise, jdbcResource) -> {
                    Context context = promise.getRepositoryMapClass(Context.class);
                    List<Map<String, Object>> execute = jdbcResource.execute(new JdbcRequest(JTOviSubscriber.SELECT_NOT_REMOVE));
                    execute.forEach(map -> context.getListIdChat().add(Long.parseLong(map.get("id_chat").toString())));
                })
                .then("handle", (_, _, promise) -> {
                    Context context = promise.getRepositoryMapClass(Context.class);

                    context.getListIdChat().clear();
                    context.getListIdChat().add(290029195L); // Ura
                    context.getListIdChat().add(294097034L); // Alex
                    context.getListIdChat().add(241022301L); // Igor
                    Util.logConsoleJson(getClass(), context);

                    List<TelegramNotification> listTelegramNotification = new ArrayList<>();

                    String idGame = "20250128_WSH@CGY";
                    String dataSend = "28.01.2025 19:00:00";

                    context.getListIdChat().forEach(idChat -> listTelegramNotification.add(new TelegramNotification(
                            idChat,
                            "ovi_goals_bot",
                            //"test_ovi_goals_bot",
                            """
                            Матч Washington Capitals (WSH) 🆚 Calgary Flames (CGY) начнется уже через 10 часов — 29 января в 05:00 (МСК).
                            
                            Как думаешь, сможет ли Александр Овечкин забить сегодня?
                            
                            """,
                            new ArrayListBuilder<Button>()
                                    .append(new Button(
                                            "Да 🔥",
                                            ServletResponseWriter.buildUrlQuery(
                                                    "/vote/",
                                                    new HashMapBuilder<String, String>()
                                                            .append("g", idGame)
                                                            .append("p", UtilNHL.getOvi().getPlayerID())
                                                            .append("v", "true")

                                            )
                                    ))
                                    .append(new Button(
                                            "Нет ⛔",
                                            ServletResponseWriter.buildUrlQuery(
                                                    "/vote/",
                                                    new HashMapBuilder<String, String>()
                                                            .append("g", idGame)
                                                            .append("p", UtilNHL.getOvi().getPlayerID())
                                                            .append("v", "false")

                                            )
                                    ))
                            ,
                            null
                    )));
                    RegisterNotification.addDeferred(
                            listTelegramNotification,
                            UtilDate.getTimestamp(dataSend, "dd.MM.yyyy HH:mm:ss") * 1000
                    );

                })
                .extension(NhlStatisticApplication::addOnError);
        return gen;
    }

}
