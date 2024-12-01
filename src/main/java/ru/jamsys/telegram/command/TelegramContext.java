package ru.jamsys.telegram.command;

import org.telegram.telegrambots.meta.api.objects.Update;

public interface TelegramContext {

    @SuppressWarnings("unused")
    long getIdChat();

    TelegramContext setIdChat(long idChat);

    void onNext(Update msg);

    void start();

}
