package ru.jamsys.core.handler.telegram.mother;

import lombok.Getter;
import lombok.Setter;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestMapping;
import org.telegram.telegrambots.meta.api.objects.payments.PreCheckoutQuery;
import ru.jamsys.NhlStatisticApplication;
import ru.jamsys.core.component.ServicePromise;
import ru.jamsys.core.flat.util.Util;
import ru.jamsys.core.flat.util.UtilTelegramMessage;
import ru.jamsys.core.flat.util.UtilTelegramResponse;
import ru.jamsys.core.promise.Promise;
import ru.jamsys.core.promise.PromiseGenerator;
import ru.jamsys.telegram.TelegramCommandContext;
import ru.jamsys.telegram.handler.MotherBotCommandHandler;

@SuppressWarnings("unused")
@Setter
@Getter
@Component
@RequestMapping("/pre_checkout_query")
public class PreCheckQuery implements PromiseGenerator, MotherBotCommandHandler {

    private final ServicePromise servicePromise;

    public PreCheckQuery(ServicePromise servicePromise) {
        this.servicePromise = servicePromise;
    }

    @Override
    public Promise generate() {
        return servicePromise.get(getClass().getSimpleName(), 12_000L)
                .extension(promise -> promise.setRepositoryMapClass(PreCheckQuery.class, this))
                .then("start", (_, _, promise) -> {
                    TelegramCommandContext telegramContext = promise.getRepositoryMapClass(TelegramCommandContext.class);
                    PreCheckoutQuery preCheckoutQuery = telegramContext.getMsg().getPreCheckoutQuery();
                    //String payload = preCheckoutQuery.getInvoicePayload(); // Ваш payload
                    UtilTelegramResponse.Result send = telegramContext.getTelegramBot().send(UtilTelegramMessage.getPreCheckAnswer(
                            preCheckoutQuery.getId(),
                            true
                    ), null);
                    Util.logConsoleJson(getClass(), send);
                })
                .extension(NhlStatisticApplication::addOnError);
    }

}
