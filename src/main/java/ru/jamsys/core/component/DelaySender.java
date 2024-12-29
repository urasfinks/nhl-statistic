package ru.jamsys.core.component;

import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import ru.jamsys.core.component.manager.ManagerBroker;
import ru.jamsys.core.component.manager.item.Broker;
import ru.jamsys.core.handler.promise.SendNotification;
import ru.jamsys.core.statistic.expiration.immutable.ExpirationMsImmutableEnvelope;
import ru.jamsys.telegram.NotificationObject;
import ru.jamsys.telegram.TelegramCommandContext;

@Component
@Lazy
public class DelaySender {

    final Broker<NotificationObject> broker;

    public DelaySender(ManagerBroker managerBroker) {
        managerBroker.initAndGet(
                NotificationObject.class.getSimpleName(),
                NotificationObject.class,
                notificationObject -> new SendNotification(notificationObject).generate().run()
        );
        broker = managerBroker.get(NotificationObject.class.getSimpleName(), NotificationObject.class);
    }

    public void add(TelegramCommandContext context, String message, long delayMs) {
        broker.add(new ExpirationMsImmutableEnvelope<>(new NotificationObject(context, message), delayMs));
    }
}
