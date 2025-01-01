package ru.jamsys.telegram;

import lombok.Getter;
import ru.jamsys.core.flat.util.telegram.Button;

import java.util.List;

@Getter
public class NotificationObject {

    final TelegramCommandContext context;

    final String message;

    final List<Button> buttons;

    public NotificationObject(TelegramCommandContext context, String message, List<Button> buttons) {
        this.context = context;
        this.message = message;
        this.buttons = buttons;
    }

}
