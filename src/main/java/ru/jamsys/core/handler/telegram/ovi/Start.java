package ru.jamsys.core.handler.telegram.ovi;

import lombok.Getter;
import lombok.Setter;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestMapping;
import ru.jamsys.core.component.ServicePromise;
import ru.jamsys.core.promise.Promise;
import ru.jamsys.core.promise.PromiseGenerator;
import ru.jamsys.telegram.TelegramCommandContext;
import ru.jamsys.telegram.handler.NhlStatisticsBotCommandHandler;
import ru.jamsys.telegram.handler.OviGoalsBotCommandHandler;

@Setter
@Getter
@Component
@RequestMapping({"/start/**"})
public class Start implements PromiseGenerator, OviGoalsBotCommandHandler {

    private final ServicePromise servicePromise;

    public Start(ServicePromise servicePromise) {
        this.servicePromise = servicePromise;
    }

    private int maxLength = 3000;

    @Override
    public Promise generate() {
        return servicePromise.get(getClass().getSimpleName(), 12_000L)
                .then("check", (_, _, promise) -> {
                    TelegramCommandContext context = promise.getRepositoryMapClass(TelegramCommandContext.class);
                    context.getTelegramBot().send(
                            context.getIdChat(),
                            "Hello world",
                            null
                    );
                })
                ;
    }

}
