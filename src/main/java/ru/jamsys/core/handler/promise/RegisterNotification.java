package ru.jamsys.core.handler.promise;

import lombok.experimental.Accessors;
import ru.jamsys.core.App;
import ru.jamsys.core.component.ServicePromise;
import ru.jamsys.core.extension.builder.ArrayListBuilder;
import ru.jamsys.core.flat.util.UtilJson;
import ru.jamsys.core.jt.JTTelegramSend;
import ru.jamsys.core.promise.Promise;
import ru.jamsys.core.promise.PromiseGenerator;
import ru.jamsys.core.resource.jdbc.JdbcRequest;
import ru.jamsys.core.resource.jdbc.JdbcResource;
import ru.jamsys.telegram.NotificationObject;

import java.util.List;

@Accessors(chain = true)
public class RegisterNotification implements PromiseGenerator {

    private final List<NotificationObject> listNotificationObject;
    private final Long onTimestamp;

    public RegisterNotification(List<NotificationObject> listNotificationObject, Long onTimestamp) {
        this.listNotificationObject = listNotificationObject;
        this.onTimestamp = onTimestamp;
    }

    @Override
    public Promise generate() {
        return App.get(ServicePromise.class).get(getClass().getSimpleName(), 60_000L)
                .thenWithResource("insert", JdbcResource.class, (_, _, _, jdbcResource) -> {
                    JdbcRequest jdbcRequest = new JdbcRequest(onTimestamp == null
                            ? JTTelegramSend.INSERT
                            : JTTelegramSend.INSERT_TS_ADD
                    );
                    listNotificationObject.forEach(notificationObject -> jdbcRequest
                            .addArg("ts_add", onTimestamp)
                            .addArg("id_chat", notificationObject.getIdChat())
                            .addArg("bot", notificationObject.getBot())
                            .addArg("message", notificationObject.getMessage())
                            .addArg("path_image", notificationObject.getPathImage())
                            .addArg("buttons", notificationObject.getButtons() == null
                                    ? null
                                    : UtilJson.toStringPretty(notificationObject.getButtons(), "[]"))
                            .nextBatch()
                    );
                    jdbcResource.execute(jdbcRequest);
                })
                ;
    }

    public static void add(NotificationObject notificationObject) {
        addDeferred(notificationObject, null);
    }

    public static void addDeferred(NotificationObject notificationObject, Long onTimestamp) {
        if (notificationObject == null) {
            return;
        }
        new RegisterNotification(
                new ArrayListBuilder<NotificationObject>().append(notificationObject),
                onTimestamp
        ).generate().run();
    }

    public static void add(List<NotificationObject> listNotificationObject) {
        addDeferred(listNotificationObject, null);
    }

    public static void addDeferred(List<NotificationObject> listNotificationObject, Long onTimestamp) {
        if (listNotificationObject == null || listNotificationObject.isEmpty()) {
            return;
        }
        new RegisterNotification(listNotificationObject, onTimestamp).generate().run();
    }

}
