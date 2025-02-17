package ru.jamsys.core.handler.promise;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import ru.jamsys.core.App;
import ru.jamsys.core.component.ServicePromise;
import ru.jamsys.core.jt.JTOviSubscriber;
import ru.jamsys.core.promise.Promise;
import ru.jamsys.core.promise.PromiseGenerator;
import ru.jamsys.core.resource.jdbc.JdbcRequest;
import ru.jamsys.core.resource.jdbc.JdbcResource;

// Если пользователь удалил чат с блокировкой бота - помечаем пользователя как удалённого

@Getter
@Setter
@Accessors(chain = true)
public class RemoveSubscriberOvi implements PromiseGenerator {

    final public Long idChat;

    public RemoveSubscriberOvi(Long idChat) {
        this.idChat = idChat;
    }

    @Override
    public Promise generate() {
        return App.get(ServicePromise.class).get(getClass().getSimpleName(), 60_000L)
                .thenWithResource(
                        "unsubscribe",
                        JdbcResource.class,
                        (_, _, _, jdbcResource) -> jdbcResource.execute(new JdbcRequest(JTOviSubscriber.UPDATE_REMOVE)
                                .addArg("remove", 1)
                                .addArg("id_chat", idChat)
                        )
                );
    }


}
