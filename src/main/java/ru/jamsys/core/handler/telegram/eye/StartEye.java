package ru.jamsys.core.handler.telegram.eye;

import lombok.Getter;
import lombok.Setter;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestMapping;
import ru.jamsys.NhlStatisticApplication;
import ru.jamsys.core.component.ServicePromise;
import ru.jamsys.core.jt.JTSubscriber;
import ru.jamsys.core.promise.Promise;
import ru.jamsys.core.promise.PromiseGenerator;
import ru.jamsys.core.resource.jdbc.JdbcRequest;
import ru.jamsys.core.resource.jdbc.JdbcResource;
import ru.jamsys.telegram.TelegramCommandContext;
import ru.jamsys.telegram.handler.EyeBotCommandHandler;

import java.util.List;
import java.util.Map;

@SuppressWarnings("unused")
@Setter
@Getter
@Component
@RequestMapping("/start/**")
public class StartEye implements PromiseGenerator, EyeBotCommandHandler {

    private final ServicePromise servicePromise;

    public StartEye(ServicePromise servicePromise) {
        this.servicePromise = servicePromise;
    }

    @Override
    public Promise generate() {
        return servicePromise.get(getClass().getSimpleName(), 12_000L)
                .extension(promise -> promise.setRepositoryMapClass(StartEye.class, this))
                .thenWithResource("subscribe", JdbcResource.class, (_, _, promise, jdbcResource) -> {
                    TelegramCommandContext context = promise.getRepositoryMapClass(TelegramCommandContext.class);
                    List<Map<String, Object>> result = jdbcResource.execute(new JdbcRequest(JTSubscriber.SELECT)
                            .addArg("id_chat", context.getIdChat())
                            .addArg("bot", context.getTelegramBot().getBotUsername())
                    );
                    if (result.isEmpty()) {
                        promise.setRepositoryMapClass(Boolean.class, true);
                        jdbcResource.execute(new JdbcRequest(JTSubscriber.INSERT)
                                .addArg("bot", context.getTelegramBot().getBotUsername())
                                .addArg("id_chat", context.getIdChat())
                                .addArg("user_info", context.getUserInfo())
                                .addArg("playload", context.getUriParameters().getOrDefault("playload", ""))
                        );
                    }
                })
                .then("start", (_, _, promise) -> {
                    TelegramCommandContext telegramContext = promise.getRepositoryMapClass(TelegramCommandContext.class);
                    telegramContext.getTelegramBot().send(
                            telegramContext.getIdChat(),
                            """
                                    🔮 Добро пожаловать в "Око Судьбы"! 🔮
                                    
                                    Приветствую тебя, искатель ответов! 🃏 Здесь ты можешь получить мудрость карт Таро и пролить свет на свою ситуацию. Задай свой вопрос, выбери расклад – и Судьба откроет перед тобой свои тайны.
                                    
                                    ✨ Что ты хочешь узнать? ✨
                                    🔹 О любви и отношениях 💞
                                    🔹 О карьере и финансах 💰
                                    🔹 О будущем и судьбе 🔮
                                    🔹 О духовном пути и саморазвитии 🌿
                                    
                                    Нажми "Сделать расклад", выбери колоду и доверься магии карт! 🃏✨""",
                            null
                    );
                })
                .extension(NhlStatisticApplication::addOnError);
    }

}
