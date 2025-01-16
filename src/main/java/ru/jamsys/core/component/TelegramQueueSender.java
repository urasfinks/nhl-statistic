package ru.jamsys.core.component;

import lombok.Getter;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import ru.jamsys.core.flat.util.telegram.Button;
import ru.jamsys.telegram.AbstractBot;
import ru.jamsys.telegram.NotificationObject;
import ru.jamsys.telegram.TelegramCommandContext;

import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

@Component
@Lazy
@Getter
public class TelegramQueueSender {

    private final ConcurrentLinkedQueue<NotificationObject> queue = new ConcurrentLinkedQueue<>();

    public void add(AbstractBot abstractBot, Long idChat, String message, List<Button> buttons, String pathImage) {
        queue.add(new NotificationObject(
                        new TelegramCommandContext()
                                .setTelegramBot(abstractBot)
                                .setIdChat(idChat),
                        message,
                        buttons,
                        pathImage
                )
        );
    }

    public void add(TelegramCommandContext context, String message, List<Button> buttons, String pathImage) {
        queue.add(new NotificationObject(context, message, buttons, pathImage));
    }

    public void add(NotificationObject notificationObject) {
        queue.add(notificationObject);
    }

}
