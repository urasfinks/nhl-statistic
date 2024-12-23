package ru.jamsys.core.component;

import lombok.Getter;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;
import ru.jamsys.NhlStatisticApplication;
import ru.jamsys.core.component.manager.ManagerExpiration;
import ru.jamsys.core.component.manager.item.RouteGeneratorRepository;
import ru.jamsys.core.extension.LifeCycleComponent;
import ru.jamsys.telegram.TelegramBot;
import ru.jamsys.telegram.TelegramCommandHandler;

@SuppressWarnings("unused")
@Component
@Lazy
public class TelegramBotComponent implements LifeCycleComponent {

    private final TelegramBotsApi api;

    private final SecurityComponent securityComponent;

    @Getter
    private final RouteGeneratorRepository routerRepository;

    @Getter
    private TelegramBot handler;

    public TelegramBotComponent(
            SecurityComponent securityComponent,
            ManagerExpiration managerExpiration,
            RouteGenerator routeGenerator
    ) throws TelegramApiException {
        this.securityComponent = securityComponent;
        api = new TelegramBotsApi(DefaultBotSession.class);
        routerRepository = routeGenerator.getRouterRepository(TelegramCommandHandler.class);
    }

    @Override
    public int getInitializationIndex() {
        return 900;
    }

    @Override
    public void run() {
        if (NhlStatisticApplication.startTelegramListener) {
            try {
                handler = new TelegramBot(new String(securityComponent.get("telegram.api.token")), routerRepository);
                api.registerBot(handler);
            } catch (TelegramApiException e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Override
    public void shutdown() {
    }

}
