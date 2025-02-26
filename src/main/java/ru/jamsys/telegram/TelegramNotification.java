package ru.jamsys.telegram;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import ru.jamsys.core.flat.util.telegram.Button;

import java.util.List;

@Getter
@Setter
@Accessors(chain = true)
@EqualsAndHashCode
public class TelegramNotification {

    private final long idChat;

    private final String botName;

    private final String message;

    private final List<Button> buttons;

    private final String pathImage;

    // NOT FINAL
    private String idImage;

    private String idVideo;

    public TelegramNotification(long idChat, String botName, String message, List<Button> buttons, String pathImage) {
        this.idChat = idChat;
        this.botName = botName;
        this.message = message;
        this.buttons = buttons;
        this.pathImage = pathImage;
    }

}
