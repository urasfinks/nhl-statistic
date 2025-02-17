package ru.jamsys.core.handler.promise;

import lombok.experimental.Accessors;
import ru.jamsys.core.App;
import ru.jamsys.core.component.ServicePromise;
import ru.jamsys.core.jt.JTHttpCache;
import ru.jamsys.core.promise.Promise;
import ru.jamsys.core.promise.PromiseGenerator;
import ru.jamsys.core.resource.jdbc.JdbcRequest;
import ru.jamsys.core.resource.jdbc.JdbcResource;

// Сброс http кеша
// Используется когда игра закончена - мы удаляем кеш статистики по играм у игрока (NHLGamesForPlayer)
// По этой статистике строится рекорд до Гретцки

@Accessors(chain = true)
public class HttpCacheReset implements PromiseGenerator {

    final public String url;

    public HttpCacheReset(String url) {
        this.url = url;
    }

    @Override
    public Promise generate() {
        return App.get(ServicePromise.class).get(getClass().getSimpleName(), 60_000L)
                .thenWithResource(
                        "cacheReset",
                        JdbcResource.class,
                        (_, _, _, jdbcResource)
                                -> jdbcResource.execute(new JdbcRequest(JTHttpCache.DELETE).addArg("url", url))
                );
    }


}
