package ru.jamsys.core.handler.telegram.ovi;

import lombok.Getter;
import lombok.Setter;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestMapping;
import ru.jamsys.NhlStatisticApplication;
import ru.jamsys.core.component.ServicePromise;
import ru.jamsys.core.extension.builder.ArrayListBuilder;
import ru.jamsys.core.extension.builder.HashMapBuilder;
import ru.jamsys.core.extension.http.ServletResponseWriter;
import ru.jamsys.core.flat.util.OviStatisticMessage;
import ru.jamsys.core.flat.util.UtilNHL;
import ru.jamsys.core.flat.util.telegram.Button;
import ru.jamsys.core.handler.promise.PlayerStatistic;
import ru.jamsys.core.handler.promise.RegisterNotification;
import ru.jamsys.core.jt.JTOviSubscriber;
import ru.jamsys.core.promise.Promise;
import ru.jamsys.core.promise.PromiseGenerator;
import ru.jamsys.core.resource.jdbc.JdbcRequest;
import ru.jamsys.core.resource.jdbc.JdbcResource;
import ru.jamsys.telegram.TelegramCommandContext;
import ru.jamsys.telegram.TelegramNotification;
import ru.jamsys.telegram.handler.OviGoalsBotCommandHandler;

import java.util.List;
import java.util.Map;

@SuppressWarnings("unused")
@Setter
@Getter
@Component
@RequestMapping("/start/**")
public class StartOvi implements PromiseGenerator, OviGoalsBotCommandHandler {

    private final ServicePromise servicePromise;

    public StartOvi(ServicePromise servicePromise) {
        this.servicePromise = servicePromise;
    }

    @Override
    public Promise generate() {
        return servicePromise.get(getClass().getSimpleName(), 12_000L)
                .extension(promise -> promise.setRepositoryMapClass(StartOvi.class, this))
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
                        RegisterNotification.add(new TelegramNotification(
                                context.getIdChat(),
                                context.getTelegramBot().getBotUsername(),
                                "Уведомления включены",
                                null,
                                null
                        ));
                        promise.skipAllStep("already notification");
                    }
                })
                .then("ovi", new PlayerStatistic(UtilNHL.getOvi(), UtilNHL.getOviScoreLastSeason()).generate())
                .then("send", (_, _, promise) -> {
                    TelegramCommandContext context = promise.getRepositoryMapClass(TelegramCommandContext.class);
                    PlayerStatistic ovi = promise.getRepositoryMapClass(Promise.class, "ovi")
                            .getRepositoryMapClass(PlayerStatistic.class);
                    RegisterNotification.add(new TelegramNotification(
                            context.getIdChat(),
                            context.getTelegramBot().getBotUsername(),
                            String.format("""
                                            Привет! Теперь ты будешь одним из первых узнавать о каждом новом голе Александра Овечкина в режиме реального времени.
                                            
                                            Поддержим Ови на пути к величию! 🏒🔥
                                            
                                             %s
                                            """,
                                    OviStatisticMessage.get(ovi)
                            ),
                            null,
                            null
                    ));
                    RegisterNotification.addDeferred(new TelegramNotification(
                            context.getIdChat(),
                            context.getTelegramBot().getBotUsername(),
                            """
                                    Ты также можешь воспользоваться дополнительными командами:
                                    
                                    /stats — Текущая статистика по сезону
                                    /poll_results — Статистика голосования
                                    /schedule — Ближайшие игры Александра Овечкина и команды Washington Capitals
                                    /prediction — Когда Овечкин побьет рекорд Гретцки?
                                    /quiz — Насколько хорошо ты знаешь Александра Овечкина?
                                    /bets — Ставки на Овечкина
                                    /stop — Отключить уведомления
                                    """,
                            null,
                            null
                    ), System.currentTimeMillis() + 10_000L);

                    RegisterNotification.addDeferred(new TelegramNotification(
                            context.getIdChat(),
                            context.getTelegramBot().getBotUsername(),
                            "Побьет ли Александр Овечкин рекорд Уэйна Гретцки в этом сезоне?",
                            new ArrayListBuilder<Button>()
                                    .append(new Button(
                                            "Да 🔥",
                                            ServletResponseWriter.buildUrlQuery(
                                                    "/vote/",
                                                    new HashMapBuilder<String, String>()
                                                            .append("g", "Ovi")
                                                            .append("p", UtilNHL.getOvi().getPlayerID())
                                                            .append("v", "true")
                                            )
                                    ))
                                    .append(new Button(
                                            "Нет ⛔",
                                            ServletResponseWriter.buildUrlQuery(
                                                    "/vote/",
                                                    new HashMapBuilder<String, String>()
                                                            .append("g", "Ovi")
                                                            .append("p", UtilNHL.getOvi().getPlayerID())
                                                            .append("v", "false")

                                            )
                                    ))
                            ,
                            null
                    ), System.currentTimeMillis() + 20_000L);

                    RegisterNotification.addDeferred(new TelegramNotification(
                            context.getIdChat(),
                            context.getTelegramBot().getBotUsername(),
                            """
                                    Пока Ови готовится к игре, у тебя есть минутка проверить свои знания о нём? 🔥
                                    
                                    Пройди наш квиз и узнай, насколько хорошо ты знаешь карьеру Александра Овечкина! 🏒💪
                                    
                                    ⚡️ 10 вопросов, 4 варианта ответов – только самые интересные факты и рекорды. Сможешь набрать 10/10?
                                    
                                    Жми «Начать» и покажи, кто здесь главный эксперт по Ови!""",
                            new ArrayListBuilder<Button>()
                                    .append(new Button("Начать 🚀")
                                            .setWebapp("https://quiz.ovechkingoals.ru/?utm_source=bot_menu&mode=tg")
                                    )
                            ,
                            null
                    ), System.currentTimeMillis() + 30_000L);
                })
                .extension(NhlStatisticApplication::addOnError);
    }

}
