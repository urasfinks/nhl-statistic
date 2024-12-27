package ru.jamsys.core.handler.promise;

import io.reactivex.rxjava3.functions.Supplier;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import ru.jamsys.core.App;
import ru.jamsys.core.component.SecurityComponent;
import ru.jamsys.core.component.ServicePromise;
import ru.jamsys.core.component.ServiceProperty;
import ru.jamsys.core.jt.JTHttpCache;
import ru.jamsys.core.promise.Promise;
import ru.jamsys.core.promise.PromiseGenerator;
import ru.jamsys.core.resource.http.HttpResource;
import ru.jamsys.core.resource.http.client.HttpClient;
import ru.jamsys.core.resource.http.client.HttpClientImpl;
import ru.jamsys.core.resource.http.client.HttpResponse;
import ru.jamsys.core.resource.jdbc.JdbcRequest;
import ru.jamsys.core.resource.jdbc.JdbcResource;

import java.util.List;
import java.util.Map;

@Accessors(chain = true)
public class Tank01Request implements PromiseGenerator {

    @Getter
    private String responseData;

    private boolean dataFindInDb = false;

    private final Supplier<String> uriSupplier;

    @Getter
    @Setter
    private boolean onlyCache = false;

    @Getter
    @Setter
    private boolean needRequestApi = false;

    public Tank01Request(Supplier<String> uriSupplier) {
        this.uriSupplier = uriSupplier;
    }

    @Override
    public Promise generate() {
        return App.get(ServicePromise.class).get(getClass().getSimpleName(), 60_000L)
                .extension(promise -> promise.setRepositoryMapClass(Tank01Request.class, this))
                .thenWithResource("select", JdbcResource.class, (_, _, promise, jdbcResource) -> {
                    promise.getRepositoryMapClass(Tank01Request.class);
                    responseData = null; // Обнуляем данные, если последовательные цепочки
                    List<Map<String, Object>> execute = jdbcResource.execute(new JdbcRequest(JTHttpCache.SELECT)
                            .addArg("url", uriSupplier.get())
                            .setDebug(false)
                    );
                    dataFindInDb = !execute.isEmpty();

                    if (!execute.isEmpty()) {
                        responseData = execute.getFirst().get("data").toString();
                        if (!needRequestApi) {
                            promise.skipAllStep("already cache");
                        }
                        // onlyCache можно использовать только если данные есть в БД
                        // Если говорят нужен запрос, это будет противоречить в режиме только кеш
                        if (onlyCache && !needRequestApi) {
                            promise.skipAllStep("onlyCache");
                        }
                    }
                })
                .thenWithResource("request", HttpResource.class, (_, _, promise, httpResource) -> {
                    promise.getRepositoryMapClass(Tank01Request.class);
                    System.out.println("Request: " + uriSupplier.get());
                    HttpResponse execute = httpResource.execute(getHttpClient(uriSupplier.get()));
                    checkResponse(execute);
                    responseData = execute.getBody();
                })
                .thenWithResource("insert", JdbcResource.class, (_, _, promise, jdbcResource) -> {
                    promise.getRepositoryMapClass(Tank01Request.class);
                    jdbcResource.execute(
                            new JdbcRequest(dataFindInDb ? JTHttpCache.UPDATE : JTHttpCache.INSERT)
                                    .addArg("url", uriSupplier.get())
                                    .addArg("data", getResponseData())
                                    .setDebug(false)
                    );
                })
                ;
    }

    public static HttpClient getHttpClient(String uri) {
        ServiceProperty serviceProperty = App.get(ServiceProperty.class);
        SecurityComponent securityComponent = App.get(SecurityComponent.class);
        return new HttpClientImpl()
                .setUrl(serviceProperty.get("rapidapi.host") + uri)
                .setRequestHeader("x-rapidapi-key", new String(securityComponent.get("rapidapi.tank01.key")))
                .setRequestHeader("x-rapidapi-host", serviceProperty.get("rapidapi.tank01.host"));
    }

    public static void checkResponse(HttpResponse execute) throws Throwable {
        if (!execute.isStatus()) {
            throw execute.getException().getFirst().getValueRaw();
        }
        if (!execute.getBody().contains("\"statusCode\": 200")) {
            throw new RuntimeException("Not found statusCode 200");
        }
    }

}
