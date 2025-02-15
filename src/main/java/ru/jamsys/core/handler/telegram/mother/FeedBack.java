package ru.jamsys.core.handler.telegram.mother;

import lombok.Getter;
import lombok.Setter;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestMapping;
import ru.jamsys.NhlStatisticApplication;
import ru.jamsys.core.component.ServicePromise;
import ru.jamsys.core.extension.builder.ArrayListBuilder;
import ru.jamsys.core.handler.promise.RegisterNotification;
import ru.jamsys.core.promise.Promise;
import ru.jamsys.core.promise.PromiseGenerator;
import ru.jamsys.telegram.TelegramCommandContext;
import ru.jamsys.telegram.TelegramNotification;
import ru.jamsys.telegram.handler.MotherBotCommandHandler;

@SuppressWarnings("unused")
@Setter
@Getter
@Component
@RequestMapping("/feedback/**")
public class FeedBack implements PromiseGenerator, MotherBotCommandHandler {

    private final ServicePromise servicePromise;

    public FeedBack(ServicePromise servicePromise) {
        this.servicePromise = servicePromise;
    }

    @Override
    public Promise generate() {
        return servicePromise.get(getClass().getSimpleName(), 31_000L)
                .extension(promise -> promise.setRepositoryMapClass(FeedBack.class, this))
                .then("start", (_, _, promise) -> {
                    TelegramCommandContext telegramContext = promise.getRepositoryMapClass(TelegramCommandContext.class);
                    if (!telegramContext.getUriParameters().containsKey("question")) {
                        telegramContext.getStepHandler().put(telegramContext.getIdChat(), telegramContext.getUriPath() + "/?question=");
                        RegisterNotification.add(new TelegramNotification(
                                telegramContext.getIdChat(),
                                telegramContext.getTelegramBot().getBotUsername(),
                                "Напишите ваш отзыв/предложение:",
                                null,
                                null
                        ));
                        promise.skipAllStep("wait question");
                        return;
                    }
                    ;
                    RegisterNotification.add(new ArrayListBuilder<TelegramNotification>()
                            .append(new TelegramNotification(
                                    -4748226035L, //Breast Feeding Feedback
                                    //290029195L, //Ura
                                    telegramContext.getTelegramBot().getBotUsername(),
                                    String.format("""
                                                    Обратная связь от: %s
                                                    Сообщение: %s
                                                    """,
                                            telegramContext.getUserInfo(),
                                            telegramContext.getUriParameters().get("question")
                                    ),
                                    null,
                                    null
                            ))
                            .append(new TelegramNotification(
                                    telegramContext.getIdChat(),
                                    telegramContext.getTelegramBot().getBotUsername(),
                                    "Спасибо, сообщение отправлено!",
                                    null,
                                    null
                            )));
                })
                .extension(NhlStatisticApplication::addOnError);
    }

}
