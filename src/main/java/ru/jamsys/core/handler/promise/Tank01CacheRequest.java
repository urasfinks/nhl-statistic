package ru.jamsys.core.handler.promise;

import io.reactivex.rxjava3.functions.Supplier;
import lombok.Getter;
import lombok.Setter;
import ru.jamsys.core.App;
import ru.jamsys.core.component.ServicePromise;
import ru.jamsys.core.flat.util.tank.UtilTank01;
import ru.jamsys.core.jt.JTHttpCache;
import ru.jamsys.core.promise.Promise;
import ru.jamsys.core.promise.PromiseGenerator;
import ru.jamsys.core.resource.http.HttpResource;
import ru.jamsys.core.resource.http.client.HttpResponse;
import ru.jamsys.core.resource.jdbc.JdbcRequest;
import ru.jamsys.core.resource.jdbc.JdbcResource;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Getter
@Setter
public class Tank01CacheRequest implements PromiseGenerator {

    private Supplier<String> uriSupplier;

    private List<String> idGamesSeason = new ArrayList<>();

    public Tank01CacheRequest(Supplier<String> uriSupplier) {
        this.uriSupplier = uriSupplier;
    }

    @Override
    public Promise generate() {
        return App.get(ServicePromise.class).get(getClass().getSimpleName(), 60_000L)
                .extension(promise -> promise.setRepositoryMapClass(Tank01Response.class, new Tank01Response()))
                .thenWithResource("select", JdbcResource.class, (_, _, promise, jdbcResource) -> {
                    Tank01Response response = promise.getRepositoryMapClass(Tank01Response.class);
                    response.setData(null); // Обнуляем данные, если последовательные цепочки
                    List<Map<String, Object>> execute = jdbcResource.execute(new JdbcRequest(JTHttpCache.SELECT)
                            .addArg("url", uriSupplier.get())
                            .setDebug(false)
                    );
                    response.setCache(!execute.isEmpty());
                    if (!execute.isEmpty()) {
                        response.setData(execute.getFirst().get("data").toString());
                        promise.skipAllStep("already cache");
                    }
                })
                .thenWithResource("request", HttpResource.class, (_, _, promise, httpResource) -> {
                    Tank01Response response = promise.getRepositoryMapClass(Tank01Response.class);
                    System.out.println("Request: " + uriSupplier.get());
                    HttpResponse execute = httpResource.execute(UtilTank01.getHttpClient(uriSupplier.get()));
                    UtilTank01.checkResponse(execute);
                    response.setData(execute.getBody());
                })
                .thenWithResource("insert", JdbcResource.class, (_, _, promise, jdbcResource) -> {
                    Tank01Response response = promise.getRepositoryMapClass(Tank01Response.class);
                    jdbcResource.execute(
                            new JdbcRequest(response.isCache() ? JTHttpCache.UPDATE : JTHttpCache.INSERT)
                                    .addArg("url", uriSupplier.get())
                                    .addArg("data", response.getData())
                                    .setDebug(false)
                    );
                    response.setData(response.getData());
                })
        ;
    }

}
