package ru.jamsys.core.handler.telegram;

import lombok.Getter;
import lombok.Setter;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestMapping;
import ru.jamsys.core.component.ServicePromise;
import ru.jamsys.core.flat.util.UtilJson;
import ru.jamsys.core.flat.util.UtilTelegram;
import ru.jamsys.core.promise.Promise;
import ru.jamsys.core.promise.PromiseGenerator;
import ru.jamsys.telegram.TelegramCommandContext;
import ru.jamsys.telegram.TelegramCommandHandler;

@Setter
@Getter
@Component
@RequestMapping("/subscribe_to_player/**")
public class SubscribeToPlayer implements PromiseGenerator, TelegramCommandHandler {

    private String index;

    private final ServicePromise servicePromise;

    public SubscribeToPlayer(ServicePromise servicePromise) {
        this.servicePromise = servicePromise;
    }

    @Override
    public Promise generate() {
        return servicePromise.get(index, 12_000L)
                .then("check", (_, _, promise) -> {
                    TelegramCommandContext context = promise.getRepositoryMapClass(TelegramCommandContext.class);
                    System.out.println(UtilJson.toStringPretty(context, "{}"));
                    if (!context.getUriParameters().containsKey("name")) {
                        context.getStepHandler().put(context.getIdChat(), context.getUriPath() + "/?name=");
                        context.getTelegramBot().send(UtilTelegram.message(
                                context.getIdChat(),
                                "Enter the player's name:",
                                null
                        ));
                        promise.skipAllStep();
                    }
                });
    }

}
