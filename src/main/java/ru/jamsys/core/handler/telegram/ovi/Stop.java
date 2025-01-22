package ru.jamsys.core.handler.telegram.ovi;

import lombok.Getter;
import lombok.Setter;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestMapping;
import ru.jamsys.NhlStatisticApplication;
import ru.jamsys.core.component.ServicePromise;
import ru.jamsys.core.handler.promise.RegisterNotification;
import ru.jamsys.core.jt.JTOviSubscriber;
import ru.jamsys.core.promise.Promise;
import ru.jamsys.core.promise.PromiseGenerator;
import ru.jamsys.core.resource.jdbc.JdbcRequest;
import ru.jamsys.core.resource.jdbc.JdbcResource;
import ru.jamsys.telegram.TelegramNotification;
import ru.jamsys.telegram.TelegramCommandContext;
import ru.jamsys.telegram.handler.OviGoalsBotCommandHandler;

import java.util.List;
import java.util.Map;

@SuppressWarnings("unused")
@Setter
@Getter
@Component
@RequestMapping({"/stop"})
public class Stop implements PromiseGenerator, OviGoalsBotCommandHandler {

    private final ServicePromise servicePromise;

    public Stop(ServicePromise servicePromise) {
        this.servicePromise = servicePromise;
    }

    @Override
    public Promise generate() {
        return servicePromise.get(getClass().getSimpleName(), 12_000L)
                .extension(promise -> promise.setRepositoryMapClass(Stop.class, this))
                .thenWithResource("unsubscribe", JdbcResource.class, (_, _, promise, jdbcResource) -> {
                    TelegramCommandContext context = promise.getRepositoryMapClass(TelegramCommandContext.class);
                    List<Map<String, Object>> result = jdbcResource.execute(new JdbcRequest(JTOviSubscriber.SELECT)
                            .addArg("id_chat", context.getIdChat())
                    );
                    // Если нет записей вообще
                    if (result.isEmpty()) {
                        promise.setRepositoryMapClass(Boolean.class, false);
                        return;
                    }
                    // Если remove не 0
                    if (!"0".equals(result.getFirst().get("remove").toString())) {
                        promise.setRepositoryMapClass(Boolean.class, false);
                        return;
                    }
                    // Штатная ситуация, подписка существует и remove = 0
                    promise.setRepositoryMapClass(Boolean.class, true);
                    jdbcResource.execute(new JdbcRequest(JTOviSubscriber.UPDATE_REMOVE)
                            .addArg("id_chat", context.getIdChat())
                            .addArg("remove", 1)
                    );
                })
                .then("send", (_, _, promise) -> {
                            TelegramCommandContext context = promise.getRepositoryMapClass(TelegramCommandContext.class);
                            RegisterNotification.add(new TelegramNotification(
                                    context.getIdChat(),
                                    context.getTelegramBot().getBotUsername(),
                                    promise.getRepositoryMapClass(Boolean.class)
                                            ? "Уведомления отключены. Буду рад видеть тебя снова!"
                                            : "Включить уведомления /start",
                                    null,
                                    null
                            ));
                        }
                )
                .extension(NhlStatisticApplication::addOnError);
    }

}
