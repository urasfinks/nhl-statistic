package ru.jamsys.core.handler.telegram.ovi;

import lombok.Getter;
import lombok.Setter;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestMapping;
import ru.jamsys.NhlStatisticApplication;
import ru.jamsys.core.component.ServicePromise;
import ru.jamsys.core.flat.util.UtilNHL;
import ru.jamsys.core.handler.promise.PlayerStatistic;
import ru.jamsys.core.handler.promise.RegisterNotification;
import ru.jamsys.core.promise.Promise;
import ru.jamsys.core.promise.PromiseGenerator;
import ru.jamsys.telegram.TelegramNotification;
import ru.jamsys.telegram.TelegramCommandContext;
import ru.jamsys.telegram.handler.OviGoalsBotCommandHandler;

@SuppressWarnings("unused")
@Setter
@Getter
@Component
@RequestMapping("/stats")
public class Stats implements PromiseGenerator, OviGoalsBotCommandHandler {

    private final ServicePromise servicePromise;

    public Stats(ServicePromise servicePromise) {
        this.servicePromise = servicePromise;
    }

    @Override
    public Promise generate() {
        return servicePromise.get(getClass().getSimpleName(), 12_000L)
                .then("ovi", new PlayerStatistic(UtilNHL.getOvi(), UtilNHL.getOviScoreLastSeason()).generate())
                .then("send", (_, _, promise) -> {
                    TelegramCommandContext context = promise.getRepositoryMapClass(TelegramCommandContext.class);
                    PlayerStatistic ovi = promise.getRepositoryMapClass(Promise.class, "ovi")
                            .getRepositoryMapClass(PlayerStatistic.class);
                    RegisterNotification.add(new TelegramNotification(
                            context.getIdChat(),
                            context.getTelegramBot().getBotUsername(),
                            ovi.getMessage(),
                            null,
                            null
                    ));
                })
                .extension(NhlStatisticApplication::addOnError);
    }

}
