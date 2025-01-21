package ru.jamsys.core.component;

import lombok.Getter;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import ru.jamsys.core.App;
import ru.jamsys.core.component.manager.ManagerBroker;
import ru.jamsys.core.component.manager.item.Broker;
import ru.jamsys.core.handler.promise.RegisterNotification;
import ru.jamsys.core.statistic.expiration.immutable.ExpirationMsImmutableEnvelope;
import ru.jamsys.telegram.TelegramNotification;

import java.util.List;

@Component
@Lazy
@Getter
public class RegisterDelayNotification {

    @SuppressWarnings("all")
    final Broker<List> broker;

    public RegisterDelayNotification(ManagerBroker managerBroker) {
        managerBroker.initAndGet(
                TelegramNotification.class.getSimpleName(),
                List.class,
                RegisterNotification::add
        );
        broker = managerBroker.get(TelegramNotification.class.getSimpleName(), List.class);
    }

    public static void add(List<TelegramNotification> telegramNotification, long delayMs) {
        if (telegramNotification == null || telegramNotification.isEmpty()) {
            return;
        }
        App.get(RegisterDelayNotification.class)
                .getBroker()
                .add(new ExpirationMsImmutableEnvelope<>(telegramNotification, delayMs));
    }

}
