package ru.jamsys.telegram;

import lombok.Getter;
import ru.jamsys.core.flat.util.telegram.Button;

import java.util.List;

@Getter
public class NotificationObject {

    final long idChat;

    final String bot;

    final String message;

    final List<Button> buttons;

    final String pathImage;

    public NotificationObject(long idChat, String bot, String message, List<Button> buttons, String pathImage) {
        this.idChat = idChat;
        this.bot = bot;
        this.message = message;
        this.buttons = buttons;
        this.pathImage = pathImage;
    }

}
