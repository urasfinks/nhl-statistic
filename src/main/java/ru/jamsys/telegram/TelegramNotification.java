package ru.jamsys.telegram;

import lombok.Getter;
import ru.jamsys.core.flat.util.telegram.Button;

import java.util.List;

@Getter
public class TelegramNotification {

    final long idChat;

    final String botName;

    final String message;

    final List<Button> buttons;

    final String pathImage;

    public TelegramNotification(long idChat, String botName, String message, List<Button> buttons, String pathImage) {
        this.idChat = idChat;
        this.botName = botName;
        this.message = message;
        this.buttons = buttons;
        this.pathImage = pathImage;
    }

}
