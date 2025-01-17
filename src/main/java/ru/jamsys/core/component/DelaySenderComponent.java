package ru.jamsys.core.component;

import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import ru.jamsys.core.component.manager.ManagerBroker;
import ru.jamsys.core.component.manager.item.Broker;
import ru.jamsys.core.handler.promise.SaveTelegramSend;
import ru.jamsys.core.statistic.expiration.immutable.ExpirationMsImmutableEnvelope;
import ru.jamsys.telegram.NotificationObject;

import java.util.List;

@Component
@Lazy
public class DelaySenderComponent {

    @SuppressWarnings("all")
    final Broker<List> broker;

    public DelaySenderComponent(ManagerBroker managerBroker) {
        managerBroker.initAndGet(
                NotificationObject.class.getSimpleName(),
                NotificationObject.class,
                SaveTelegramSend::add
        );
        broker = managerBroker.get(NotificationObject.class.getSimpleName(), List.class);
    }

    public void add(List<NotificationObject> notificationObject, long delayMs) {
        if (notificationObject == null || notificationObject.isEmpty()) {
            return;
        }
        broker.add(new ExpirationMsImmutableEnvelope<>(notificationObject, delayMs));
    }

}
