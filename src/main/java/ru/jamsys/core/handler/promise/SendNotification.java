package ru.jamsys.core.handler.promise;

import lombok.Getter;
import lombok.Setter;
import ru.jamsys.core.App;
import ru.jamsys.core.component.ServicePromise;
import ru.jamsys.core.component.TelegramBotComponent;
import ru.jamsys.core.flat.util.UtilFile;
import ru.jamsys.core.flat.util.UtilFileResource;
import ru.jamsys.core.flat.util.UtilTelegram;
import ru.jamsys.core.promise.Promise;
import ru.jamsys.core.promise.PromiseGenerator;
import ru.jamsys.core.promise.PromiseTask;
import ru.jamsys.core.promise.PromiseTaskExecuteType;
import ru.jamsys.telegram.AbstractBot;
import ru.jamsys.telegram.NotificationObject;

@Getter
@Setter
public class SendNotification implements PromiseGenerator {

    private final NotificationObject notificationObject;

    public SendNotification(NotificationObject notificationObject) {
        this.notificationObject = notificationObject;
    }

    @Override
    public Promise generate() {
        Promise promise = App.get(ServicePromise.class).get(getClass().getSimpleName(), 60_000L);
        promise.append(new PromiseTask("send", promise, PromiseTaskExecuteType.IO, (_, _, _) -> send(getNotificationObject())))
                .setDebug(false)
                ;
        return promise;
    }

    public static UtilTelegram.Result send(NotificationObject notificationObject) {
        UtilTelegram.Result telegramResult = new UtilTelegram.Result();
        AbstractBot telegramBot = App.get(TelegramBotComponent.class).getBotRepository().get(notificationObject.getBot());
        if(telegramBot == null){
            return telegramResult
                    .setException(UtilTelegram.ResultException.RETRY)
                    .setCause("telegramBot is null");
        }
        if (
                notificationObject.getPathImage() == null
                        || notificationObject.getPathImage().isEmpty()
        ) {
            return telegramBot.send(
                    notificationObject.getIdChat(),
                    notificationObject.getMessage(),
                    notificationObject.getButtons()
            );
        } else {
            try { // Потому что UtilFileResource.get throw exception
                return telegramBot.sendImage(
                        notificationObject.getIdChat(),
                        UtilFileResource.get(
                                notificationObject.getPathImage(),
                                UtilFileResource.Direction.PROJECT
                        ),
                        UtilFile.getFileName(notificationObject.getPathImage()),
                        notificationObject.getMessage()
                );
            } catch (Throwable th) {
                telegramResult
                        .setException(UtilTelegram.ResultException.OTHER)
                        .setCause(th.getMessage());
                App.error(th);
            }
        }
        return telegramResult;
    }

}
