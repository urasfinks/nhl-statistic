package ru.jamsys.telegram.command;

import org.telegram.telegrambots.meta.api.objects.Update;
import ru.jamsys.core.App;
import ru.jamsys.core.component.TelegramBotComponent;
import ru.jamsys.telegram.TelegramBotHandler;

public class SubscribeToPlayer implements TelegramContext {

    private long idChat;

    TelegramBotHandler telegramBotHandler = App.get(TelegramBotComponent.class).getTelegramBotHandler();

    @Override
    public long getIdChat() {
        return idChat;
    }

    @Override
    public TelegramContext setIdChat(long idChat) {
        this.idChat = idChat;
        return this;
    }

    @Override
    public void onNext(Update msg) {

    }

    @Override
    public void start() {
        try {
            telegramBotHandler.send(idChat, "Enter the player's name", null);
        } catch (Throwable th) {
            App.error(th);
        }
    }

}
