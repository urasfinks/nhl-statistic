package ru.jamsys.core.component;

import lombok.Getter;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;
import ru.jamsys.core.component.manager.ManagerExpiration;
import ru.jamsys.core.component.manager.item.Session;
import ru.jamsys.core.extension.LifeCycleComponent;
import ru.jamsys.telegram.TelegramBotHandler;
import ru.jamsys.telegram.command.TelegramContext;

import java.util.Map;

@SuppressWarnings("unused")
@Component
@Lazy
public class TelegramBotComponent implements LifeCycleComponent {

    private final TelegramBotsApi api;

    private final SecurityComponent securityComponent;

    @Getter
    private final Map<Long, TelegramContext> map;

    @Getter
    private TelegramBotHandler handler;

    public TelegramBotComponent(
            SecurityComponent securityComponent,
            ManagerExpiration managerExpiration
    ) throws TelegramApiException {
        this.securityComponent = securityComponent;
        api = new TelegramBotsApi(DefaultBotSession.class);
        map = new Session<>("TelegramContext", Long.class, 60_000L);
    }

    @Override
    public int getInitializationIndex() {
        return 900;
    }

    @Override
    public void run() {
        try {
            handler = new TelegramBotHandler(new String(securityComponent.get("telegram.api.token")));
            api.registerBot(handler);
        } catch (TelegramApiException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void shutdown() {
    }

}
