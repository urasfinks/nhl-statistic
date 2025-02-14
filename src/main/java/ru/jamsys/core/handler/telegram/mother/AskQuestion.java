package ru.jamsys.core.handler.telegram.mother;

import lombok.Getter;
import lombok.Setter;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestMapping;
import ru.jamsys.NhlStatisticApplication;
import ru.jamsys.core.component.ServicePromise;
import ru.jamsys.core.flat.util.MotherResponse;
import ru.jamsys.core.handler.promise.OpenAiRequest;
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
@RequestMapping("/ask_question/**")
public class AskQuestion implements PromiseGenerator, MotherBotCommandHandler {

    private final ServicePromise servicePromise;

    public AskQuestion(ServicePromise servicePromise) {
        this.servicePromise = servicePromise;
    }

    @Override
    public Promise generate() {
        return servicePromise.get(getClass().getSimpleName(), 31_000L)
                .extension(promise -> promise.setRepositoryMapClass(AskQuestion.class, this))
                .then("start", (_, _, promise) -> {
                    TelegramCommandContext telegramContext = promise.getRepositoryMapClass(TelegramCommandContext.class);
                    if (!telegramContext.getUriParameters().containsKey("question")) {
                        telegramContext.getStepHandler().put(telegramContext.getIdChat(), telegramContext.getUriPath() + "/?question=");
                        RegisterNotification.add(new TelegramNotification(
                                telegramContext.getIdChat(),
                                telegramContext.getTelegramBot().getBotUsername(),
                                "Напишите свой вопрос:",
                                null,
                                null
                        ));
                        promise.skipAllStep("wait question");
                        return;
                    }
                    RegisterNotification.add(new TelegramNotification(
                            telegramContext.getIdChat(),
                            telegramContext.getTelegramBot().getBotUsername(),
                            "⏳Обрабатываю ваш вопрос...",
                            null,
                            null
                    ));
                })
                .then("request", (atomicBoolean, promiseTask, promise) -> {
                    TelegramCommandContext telegramContext = promise.getRepositoryMapClass(TelegramCommandContext.class);
                    //YandexLlmRequest yandexRequest = new YandexLlmRequest(telegramContext.getUriParameters().get("question"));
                    OpenAiRequest yandexRequest = new OpenAiRequest(telegramContext.getUriParameters().get("question"));
                    Promise req = yandexRequest.generate().run().await(50_000L);
                    if (req.isException()) {
                        throw req.getExceptionSource();
                    }
                    MotherResponse motherResponse = yandexRequest.getMotherResponse();
                    if (motherResponse.isError()) {
                        if (motherResponse.isRetry()) {
                            RegisterNotification.add(new TelegramNotification(
                                    telegramContext.getIdChat(),
                                    telegramContext.getTelegramBot().getBotUsername(),
                                    "⏱️ " + motherResponse.getError() + "\nПопробуйте задать вопрос позже /ask_question",
                                    null,
                                    null
                            ));
                        } else {
                            RegisterNotification.add(new TelegramNotification(
                                    telegramContext.getIdChat(),
                                    telegramContext.getTelegramBot().getBotUsername(),
                                    "❌ " + motherResponse.getError() + "\nЗадайте другой вопрос /ask_question",
                                    null,
                                    null
                            ));
                        }
                        return;
                    }
                    if (motherResponse.isClarification()) {
                        telegramContext
                                .getStepHandler()
                                .put(
                                        telegramContext.getIdChat(),
                                        telegramContext.getUriPath()
                                                + "/?question="
                                                + telegramContext.getUriParameters().get("question")
                                                + ". "
                                );
                        RegisterNotification.add(new TelegramNotification(
                                telegramContext.getIdChat(),
                                telegramContext.getTelegramBot().getBotUsername(),
                                motherResponse.getClarification(),
                                null,
                                null
                        ));
                        return;
                    }
                    RegisterNotification.add(new TelegramNotification(
                            telegramContext.getIdChat(),
                            telegramContext.getTelegramBot().getBotUsername(),
                            motherResponse.getRec(),
                            null,
                            null
                    ));
                })
                .extension(NhlStatisticApplication::addOnError);
    }

}
