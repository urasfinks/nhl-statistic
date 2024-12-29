package ru.jamsys.core.handler.telegram.ovi;

import lombok.Getter;
import lombok.Setter;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestMapping;
import ru.jamsys.core.component.ServicePromise;
import ru.jamsys.core.handler.promise.PlayerStatistic;
import ru.jamsys.core.handler.promise.PlayerStatisticOvi;
import ru.jamsys.core.promise.Promise;
import ru.jamsys.core.promise.PromiseGenerator;
import ru.jamsys.telegram.TelegramCommandContext;
import ru.jamsys.telegram.handler.OviGoalsBotCommandHandler;

@SuppressWarnings("unused")
@Setter
@Getter
@Component
@RequestMapping({"/stats"})
public class Stats implements PromiseGenerator, OviGoalsBotCommandHandler {

    private final ServicePromise servicePromise;

    public Stats(ServicePromise servicePromise) {
        this.servicePromise = servicePromise;
    }

    @Override
    public Promise generate() {
        return servicePromise.get(getClass().getSimpleName(), 12_000L)
                .then("ovi", new PlayerStatisticOvi().generate())
                .then("send", (_, _, promise) -> {
                    TelegramCommandContext context = promise.getRepositoryMapClass(TelegramCommandContext.class);
                    PlayerStatistic ovi = promise.getRepositoryMapClass(Promise.class, "ovi")
                            .getRepositoryMapClass(PlayerStatistic.class);
                    context.getTelegramBot().send(
                            context.getIdChat(),
                            ovi.getMessage(),
                            null
                    );
                })
                ;
    }

}