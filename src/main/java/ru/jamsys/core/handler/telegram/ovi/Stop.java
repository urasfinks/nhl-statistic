package ru.jamsys.core.handler.telegram.ovi;

import lombok.Getter;
import lombok.Setter;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestMapping;
import ru.jamsys.core.component.ServicePromise;
import ru.jamsys.core.jt.JTOviSubscriber;
import ru.jamsys.core.promise.Promise;
import ru.jamsys.core.promise.PromiseGenerator;
import ru.jamsys.core.resource.jdbc.JdbcRequest;
import ru.jamsys.core.resource.jdbc.JdbcResource;
import ru.jamsys.telegram.TelegramCommandContext;
import ru.jamsys.telegram.handler.OviGoalsBotCommandHandler;

import java.util.List;
import java.util.Map;

@Setter
@Getter
@Component
@RequestMapping({"/stop"})
public class Stop implements PromiseGenerator, OviGoalsBotCommandHandler {

    private final ServicePromise servicePromise;

    public Stop(ServicePromise servicePromise) {
        this.servicePromise = servicePromise;
    }

    private int maxLength = 3000;

    private boolean success = true;

    @Override
    public Promise generate() {
        return servicePromise.get(getClass().getSimpleName(), 12_000L)
                .extension(promise -> promise.setRepositoryMapClass(Stop.class, this))
                .thenWithResource("unsubscribe", JdbcResource.class, (_, _, promise, jdbcResource) -> {
                    TelegramCommandContext context = promise.getRepositoryMapClass(TelegramCommandContext.class);
                    List<Map<String, Object>> result = jdbcResource.execute(new JdbcRequest(JTOviSubscriber.SELECT)
                            .addArg("id_chat", context.getIdChat())
                    );
                    if (result.isEmpty()) {
                        setSuccess(false);
                    } else {
                        setSuccess(true);
                        jdbcResource.execute(new JdbcRequest(JTOviSubscriber.DELETE)
                                .addArg("id_chat", context.getIdChat())
                        );
                    }
                })
                .then("check", (_, _, promise) -> {
                    TelegramCommandContext context = promise.getRepositoryMapClass(TelegramCommandContext.class);
                    context.getTelegramBot().send(
                            context.getIdChat(),
                            isSuccess() ? "Уведомления отключены. Буду рад видеть тебя снова!" : "Включить уведомления /start",
                            null
                    );
                })
                ;
    }

}
