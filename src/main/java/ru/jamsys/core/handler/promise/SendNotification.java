package ru.jamsys.core.handler.promise;

import lombok.Getter;
import lombok.Setter;
import ru.jamsys.core.App;
import ru.jamsys.core.component.ServicePromise;
import ru.jamsys.core.promise.Promise;
import ru.jamsys.core.promise.PromiseGenerator;
import ru.jamsys.telegram.NotificationObject;
import ru.jamsys.telegram.TelegramCommandContext;

@Getter
@Setter
public class SendNotification implements PromiseGenerator {

    private final NotificationObject notificationObject;

    public SendNotification(NotificationObject notificationObject) {
        this.notificationObject = notificationObject;
    }

    @Override
    public Promise generate() {
        return App.get(ServicePromise.class).get(getClass().getSimpleName(), 60_000L)
                .then("send", (_, _, _) -> {
                    TelegramCommandContext context = getNotificationObject().getContext();
                    context.getTelegramBot().send(
                            context.getIdChat(),
                            getNotificationObject().getMessage(),
                            null
                    );
                })
                .setDebug(true)
                ;
    }

}
