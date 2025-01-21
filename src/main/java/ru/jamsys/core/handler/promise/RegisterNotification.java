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
import ru.jamsys.telegram.TelegramNotification;

import java.util.List;

@Accessors(chain = true)
public class RegisterNotification implements PromiseGenerator {

    private final List<TelegramNotification> listTelegramNotification;
    private final Long onTimestamp;

    public RegisterNotification(List<TelegramNotification> listTelegramNotification, Long onTimestamp) {
        this.listTelegramNotification = listTelegramNotification;
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
                    listTelegramNotification.forEach(notificationObject -> jdbcRequest
                            .addArg("ts_add", onTimestamp)
                            .addArg("id_chat", notificationObject.getIdChat())
                            .addArg("bot", notificationObject.getBotName())
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

    public static void add(TelegramNotification telegramNotification) {
        addDeferred(telegramNotification, null);
    }

    public static void addDeferred(TelegramNotification telegramNotification, Long onTimestamp) {
        if (telegramNotification == null) {
            return;
        }
        new RegisterNotification(
                new ArrayListBuilder<TelegramNotification>().append(telegramNotification),
                onTimestamp
        ).generate().run();
    }

    public static void add(List<TelegramNotification> listTelegramNotification) {
        addDeferred(listTelegramNotification, null);
    }

    public static void addDeferred(List<TelegramNotification> listTelegramNotification, Long onTimestamp) {
        if (listTelegramNotification == null || listTelegramNotification.isEmpty()) {
            return;
        }
        new RegisterNotification(listTelegramNotification, onTimestamp).generate().run();
    }

}
