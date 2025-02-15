package ru.jamsys.core.handler.telegram.mother;

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
import ru.jamsys.telegram.handler.MotherBotCommandHandler;

import java.util.List;
import java.util.Map;

@SuppressWarnings("unused")
@Setter
@Getter
@Component
@RequestMapping("/start/**")
public class StartMother implements PromiseGenerator, MotherBotCommandHandler {

    private final ServicePromise servicePromise;

    public StartMother(ServicePromise servicePromise) {
        this.servicePromise = servicePromise;
    }

    @Override
    public Promise generate() {
        return servicePromise.get(getClass().getSimpleName(), 12_000L)
                .extension(promise -> promise.setRepositoryMapClass(StartMother.class, this))
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
                                    👋 Привет! Я — *Грудничок Helper*, твой помощник в вопросах кормления малыша. 🌟\s
                                    
                                    Я могу помочь тебе с:
                                    🤱 Грудным вскармливанием: как правильно прикладывать ребёнка, увеличить лактацию, решить проблемы с отказом от груди.
                                    🥕🍎 Прикормом: когда и как начинать, какие продукты выбрать, как справляться с аллергией.
                                    🍼 Искусственным вскармливанием: как выбрать смесь, правильно её приготовить и наладить режим кормления.
                                    🍼😔 Проблемами с кормлением: если ребёнок отказывается от еды, плохо набирает вес или у него колики.
                                    
                                    Просто задай мне вопрос, и я постараюсь дать полезные рекомендации. Если твой вопрос будет недостаточно конкретным, я уточню, чтобы помочь лучше.\s
                                    
                                    ❗ Важно:\s
                                    Я использую только проверенную информацию, но если ситуация требует индивидуального подхода, всегда лучше проконсультироваться с врачом или специалистом по грудному вскармливанию.\s
                                    
                                    📩 Задайте вопрос, и я с радостью подскажу! 😊""",
                            null
                    );
                })
                .extension(NhlStatisticApplication::addOnError);
    }

}
