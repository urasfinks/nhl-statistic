package ru.jamsys.core.component;

import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;
import ru.jamsys.TelegramBotHandler;
import ru.jamsys.core.extension.LifeCycleComponent;

@SuppressWarnings("unused")
@Component
@Lazy
public class TelegramBotComponent implements LifeCycleComponent {

    private final TelegramBotsApi api;
    private final SecurityComponent securityComponent;

    public TelegramBotComponent(SecurityComponent securityComponent) throws TelegramApiException {
        this.securityComponent = securityComponent;
        api = new TelegramBotsApi(DefaultBotSession.class);
    }

    @Override
    public int getInitializationIndex() {
        return 900;
    }

    @Override
    public void run() {
        try {
            api.registerBot(new TelegramBotHandler(new String(securityComponent.get("telegram.api.token"))));
        } catch (TelegramApiException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void shutdown() {
    }

}
