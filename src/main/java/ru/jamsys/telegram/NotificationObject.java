package ru.jamsys.telegram;

import lombok.Getter;

@Getter
public class NotificationObject {

    final TelegramCommandContext context;

    final String message;

    public NotificationObject(TelegramCommandContext context, String message) {
        this.context = context;
        this.message = message;
    }

}
