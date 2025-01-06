package ru.jamsys.core.handler.telegram.ovi;

import lombok.Getter;
import lombok.Setter;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestMapping;
import ru.jamsys.core.App;
import ru.jamsys.core.component.DelaySenderComponent;
import ru.jamsys.core.component.ServicePromise;
import ru.jamsys.core.extension.builder.ArrayListBuilder;
import ru.jamsys.core.extension.builder.HashMapBuilder;
import ru.jamsys.core.extension.http.ServletResponseWriter;
import ru.jamsys.core.flat.util.UtilNHL;
import ru.jamsys.core.flat.util.telegram.Button;
import ru.jamsys.core.handler.promise.PlayerStatistic;
import ru.jamsys.core.jt.JTOviSubscriber;
import ru.jamsys.core.promise.Promise;
import ru.jamsys.core.promise.PromiseGenerator;
import ru.jamsys.core.resource.jdbc.JdbcRequest;
import ru.jamsys.core.resource.jdbc.JdbcResource;
import ru.jamsys.telegram.TelegramCommandContext;
import ru.jamsys.telegram.handler.OviGoalsBotCommandHandler;

import java.util.List;
import java.util.Map;

@SuppressWarnings("unused")
@Setter
@Getter
@Component
@RequestMapping({"/start/**"})
public class Start implements PromiseGenerator, OviGoalsBotCommandHandler {

    private final ServicePromise servicePromise;

    public Start(ServicePromise servicePromise) {
        this.servicePromise = servicePromise;
    }

    @Override
    public Promise generate() {
        return servicePromise.get(getClass().getSimpleName(), 12_000L)
                .extension(promise -> promise.setRepositoryMapClass(Start.class, this))
                .thenWithResource("subscribe", JdbcResource.class, (_, _, promise, jdbcResource) -> {
                    TelegramCommandContext context = promise.getRepositoryMapClass(TelegramCommandContext.class);
                    List<Map<String, Object>> result = jdbcResource.execute(new JdbcRequest(JTOviSubscriber.SELECT)
                            .addArg("id_chat", context.getIdChat())
                    );
                    if (result.isEmpty()) {
                        promise.setRepositoryMapClass(Boolean.class, true);
                        jdbcResource.execute(new JdbcRequest(JTOviSubscriber.INSERT)
                                .addArg("id_chat", context.getIdChat())
                                .addArg("user_info", context.getUserInfo())
                                .addArg("playload", context.getUriParameters().getOrDefault("playload", ""))
                        );
                        return;
                    }
                    if (!"0".equals(result.getFirst().get("remove").toString())) {
                        promise.setRepositoryMapClass(Boolean.class, true);
                        jdbcResource.execute(new JdbcRequest(JTOviSubscriber.UPDATE_REMOVE)
                                .addArg("id_chat", context.getIdChat())
                                .addArg("remove", 0)
                        );
                        return;
                    }
                    promise.setRepositoryMapClass(Boolean.class, false);
                })
                .then("check", (_, _, promise) -> {
                    TelegramCommandContext context = promise.getRepositoryMapClass(TelegramCommandContext.class);
                    if (!promise.getRepositoryMapClass(Boolean.class)) {
                        context.getTelegramBot().send(
                                context.getIdChat(),
                                "Уведомления включены",
                                null
                        );
                        promise.skipAllStep("already notification");
                    }
                })
                .then("ovi", new PlayerStatistic(UtilNHL.getOvi(), UtilNHL.getOviScoreLastSeason()).generate())
                .then("send", (_, _, promise) -> {
                    TelegramCommandContext context = promise.getRepositoryMapClass(TelegramCommandContext.class);
                    PlayerStatistic ovi = promise.getRepositoryMapClass(Promise.class, "ovi")
                            .getRepositoryMapClass(PlayerStatistic.class);
                    context.getTelegramBot().send(
                            context.getIdChat(),
                            String.format("""
                                            Саня снова в деле! Теперь ты будешь одним из первых узнавать о каждом его новом голе в режиме реального времени.
                                            
                                            Поддержим Ови на пути к величию! 🏒🔥
                                            
                                            %s
                                            """,
                                    ovi.getMessage()
                            ),
                            null
                    );
                    App.get(DelaySenderComponent.class).add(
                            context,
                            """
                                    Ты также можешь воспользоваться дополнительными командами:
                                    
                                    /stats — Текущая статистика: количество голов, оставшихся до рекорда, и статистика по сезону
                                    /poll_results — Статистика голосования
                                    /schedule — Ближайшие игры Александра Овечкина и команды Washington Capitals
                                    /stop — Отключить уведомления""",
                            null,
                            null,
                            10_000L);

                    App.get(DelaySenderComponent.class).add(
                            context,
                            "Побьет ли Александр Овечкин рекорд Уэйна Гретцки в этом сезоне?",
                            new ArrayListBuilder<Button>()
                                    .append(new Button(
                                            "Да 🔥",
                                            ServletResponseWriter.buildUrlQuery(
                                                    "/poll_results/",
                                                    new HashMapBuilder<>(context.getUriParameters())
                                                            .append("value", "true")

                                            )
                                    ))
                                    .append(new Button(
                                            "Нет ⛔",
                                            ServletResponseWriter.buildUrlQuery(
                                                    "/poll_results/",
                                                    new HashMapBuilder<>(context.getUriParameters())
                                                            .append("value", "false")

                                            )
                                    ))
                            ,
                            null,
                            20_000L);
                })
                ;
    }

}
