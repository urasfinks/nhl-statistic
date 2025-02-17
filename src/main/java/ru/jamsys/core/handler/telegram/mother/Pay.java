package ru.jamsys.core.handler.telegram.mother;

import lombok.Getter;
import lombok.Setter;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestMapping;
import ru.jamsys.NhlStatisticApplication;
import ru.jamsys.core.component.ServicePromise;
import ru.jamsys.core.flat.util.UtilTelegramMessage;
import ru.jamsys.core.promise.Promise;
import ru.jamsys.core.promise.PromiseGenerator;
import ru.jamsys.telegram.TelegramCommandContext;
import ru.jamsys.telegram.handler.MotherBotCommandHandler;

@SuppressWarnings("unused")
@Setter
@Getter
@Component
@RequestMapping("/pay/**")
public class Pay implements PromiseGenerator, MotherBotCommandHandler {

    private final ServicePromise servicePromise;

    public Pay(ServicePromise servicePromise) {
        this.servicePromise = servicePromise;
    }

    @Override
    public Promise generate() {
        return servicePromise.get(getClass().getSimpleName(), 12_000L)
                .extension(promise -> promise.setRepositoryMapClass(Pay.class, this))
                .then("start", (_, _, promise) -> {
                    TelegramCommandContext telegramContext = promise.getRepositoryMapClass(TelegramCommandContext.class);
                    String uuid = java.util.UUID.randomUUID().toString();
                    telegramContext.getTelegramBot().send(UtilTelegramMessage.getInvoice(
                            telegramContext.getIdChat(),
                            "Покупка подписки",
                            "Подписка даёт возможность без ограничений пользоваться консультантом",
                            uuid,
                            "381764678:TEST:112476",
                            "К оплате",
                            100 * 100,
                            uuid
                    ), telegramContext.getIdChat());
                })
                .extension(NhlStatisticApplication::addOnError);
    }

}
