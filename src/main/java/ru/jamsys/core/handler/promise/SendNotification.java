package ru.jamsys.core.handler.promise;

import lombok.Getter;
import lombok.Setter;
import ru.jamsys.core.App;
import ru.jamsys.core.component.ServicePromise;
import ru.jamsys.core.component.TelegramQueueSender;
import ru.jamsys.core.flat.util.UtilFile;
import ru.jamsys.core.flat.util.UtilFileResource;
import ru.jamsys.core.promise.Promise;
import ru.jamsys.core.promise.PromiseGenerator;
import ru.jamsys.core.promise.PromiseTask;
import ru.jamsys.core.promise.PromiseTaskExecuteType;
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
        Promise promise = App.get(ServicePromise.class).get(getClass().getSimpleName(), 600_000L);
        promise.append(new PromiseTask("send", promise, PromiseTaskExecuteType.IO, (_, _, _) -> {
                    TelegramCommandContext context = getNotificationObject().getContext();
                    if (
                            getNotificationObject().getPathImage() == null
                                    || getNotificationObject().getPathImage().isEmpty()
                    ) {
                        App.get(TelegramQueueSender.class).add(
                                context.getTelegramBot(),
                                context.getIdChat(),
                                getNotificationObject().getMessage(),
                                getNotificationObject().getButtons(),
                                null
                        );
                    } else {
                        try {
                            context.getTelegramBot().sendImage(
                                    context.getIdChat(),
                                    UtilFileResource.get(
                                            getNotificationObject().getPathImage(),
                                            UtilFileResource.Direction.PROJECT
                                    ),
                                    UtilFile.getFileName(getNotificationObject().getPathImage()),
                                    getNotificationObject().getMessage()
                            );
                        } catch (Throwable th) {
                            App.error(th);
                        }
                    }
                }))
                .setDebug(false)
                ;
        return promise;
    }

}
