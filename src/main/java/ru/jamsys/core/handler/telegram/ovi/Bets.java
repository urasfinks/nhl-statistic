package ru.jamsys.core.handler.telegram.ovi;

import lombok.Getter;
import lombok.Setter;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestMapping;
import ru.jamsys.NhlStatisticApplication;
import ru.jamsys.core.component.ServicePromise;
import ru.jamsys.core.handler.promise.BetSourceNotification;
import ru.jamsys.core.handler.promise.RegisterNotification;
import ru.jamsys.core.promise.Promise;
import ru.jamsys.core.promise.PromiseGenerator;
import ru.jamsys.telegram.TelegramCommandContext;
import ru.jamsys.telegram.TelegramNotification;
import ru.jamsys.telegram.handler.OviGoalsBotCommandHandler;

@SuppressWarnings("unused")
@Setter
@Getter
@Component
@RequestMapping("/bets")
public class Bets implements PromiseGenerator, OviGoalsBotCommandHandler {

    private final ServicePromise servicePromise;

    public Bets(ServicePromise servicePromise) {
        this.servicePromise = servicePromise;
    }

    @Override
    public Promise generate() {
        return servicePromise.get(getClass().getSimpleName(), 12_000L)
                .extension(promise -> promise.setRepositoryMapClass(Bets.class, this))
                .then("betSourceNotification", new BetSourceNotification("bets").generate())
                .then("send", (_, _, promise) -> {
                            TelegramCommandContext context = promise.getRepositoryMapClass(TelegramCommandContext.class);
                            BetSourceNotification betSourceNotification = promise.getRepositoryMapClass(Promise.class, "betSourceNotification")
                                    .getRepositoryMapClass(BetSourceNotification.class);
                            if (betSourceNotification.isNotEmpty()) {
                                RegisterNotification.add(new TelegramNotification(
                                        context.getIdChat(),
                                        context.getTelegramBot().getBotUsername(),
                                        betSourceNotification.getMessage(),
                                        betSourceNotification.getListButton(),
                                        betSourceNotification.getPathImage()
                                )
                                        .setIdImage(betSourceNotification.getIdImage())
                                        .setIdVideo(betSourceNotification.getIdVideo()));
                            }
                        }
                )
                .extension(NhlStatisticApplication::addOnError);
    }

}
