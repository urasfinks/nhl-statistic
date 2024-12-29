package ru.jamsys.core.handler.telegram.ovi;

import lombok.Getter;
import lombok.Setter;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestMapping;
import ru.jamsys.core.component.ServicePromise;
import ru.jamsys.core.handler.promise.ovi.SendStartNotification;
import ru.jamsys.core.jt.JTOviSubscriber;
import ru.jamsys.core.promise.Promise;
import ru.jamsys.core.promise.PromiseGenerator;
import ru.jamsys.core.resource.jdbc.JdbcRequest;
import ru.jamsys.core.resource.jdbc.JdbcResource;
import ru.jamsys.telegram.TelegramCommandContext;
import ru.jamsys.telegram.handler.OviGoalsBotCommandHandler;

import java.util.List;
import java.util.Map;

@SuppressWarnings("unused")
@Setter
@Getter
@Component
@RequestMapping({"/start"})
public class Start implements PromiseGenerator, OviGoalsBotCommandHandler {

    private final ServicePromise servicePromise;

    public Start(ServicePromise servicePromise) {
        this.servicePromise = servicePromise;
    }

    private int maxLength = 3000;

    private boolean success;

    @Override
    public Promise generate() {
        return servicePromise.get(getClass().getSimpleName(), 12_000L)
                .extension(promise -> promise.setRepositoryMapClass(Start.class, this))
                .thenWithResource("subscribe", JdbcResource.class, (_, _, promise, jdbcResource) -> {
                    TelegramCommandContext context = promise.getRepositoryMapClass(TelegramCommandContext.class);
                    List<Map<String, Object>> result = jdbcResource.execute(new JdbcRequest(JTOviSubscriber.SELECT)
                            .addArg("id_chat", context.getIdChat())
                    );
                    if (!result.isEmpty()) {
                        setSuccess(false);
                    } else {
                        setSuccess(true);
                        jdbcResource.execute(new JdbcRequest(JTOviSubscriber.INSERT)
                                .addArg("id_chat", context.getIdChat())
                        );
                    }
                })
                .then("check", (_, _, promise) -> {
                    TelegramCommandContext context = promise.getRepositoryMapClass(TelegramCommandContext.class);
                    if (!isSuccess()) {
                        context.getTelegramBot().send(
                                context.getIdChat(),
                                "Уведомления включены",
                                null
                        );
                    } else {
                        new SendStartNotification(context).generate().run();
                    }
                })
                ;
    }

}
