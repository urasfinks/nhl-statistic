package ru.jamsys.core.handler.promise;

import io.reactivex.rxjava3.functions.Supplier;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import ru.jamsys.core.App;
import ru.jamsys.core.component.RateLimitNhlApi;
import ru.jamsys.core.component.SecurityComponent;
import ru.jamsys.core.component.ServicePromise;
import ru.jamsys.core.component.ServiceProperty;
import ru.jamsys.core.flat.util.Util;
import ru.jamsys.core.flat.util.UtilJson;
import ru.jamsys.core.jt.JTHttpCache;
import ru.jamsys.core.jt.JTLogRequest;
import ru.jamsys.core.promise.Promise;
import ru.jamsys.core.promise.PromiseGenerator;
import ru.jamsys.core.resource.http.HttpResource;
import ru.jamsys.core.resource.http.client.HttpConnector;
import ru.jamsys.core.resource.http.client.HttpConnectorDefault;
import ru.jamsys.core.resource.http.client.HttpResponse;
import ru.jamsys.core.resource.jdbc.JdbcRequest;
import ru.jamsys.core.resource.jdbc.JdbcResource;

import java.util.List;
import java.util.Map;

//  Запрос к API Tank01

@Accessors(chain = true)
public class RequestTank01 implements PromiseGenerator {

    @Getter
    private String responseData;

    private boolean dataFindInDb = false;

    private final Supplier<String> uriSupplier;

    @Getter
    @Setter
    private boolean onlyCache = false;

    @Getter
    @Setter
    private boolean alwaysRequestApi = false;

    public RequestTank01(Supplier<String> uriSupplier) {
        this.uriSupplier = uriSupplier;
    }

    @Override
    public Promise generate() {
        return App.get(ServicePromise.class).get(getClass().getSimpleName(), 60_000L)
                .extension(promise -> promise.setRepositoryMapClass(RequestTank01.class, this))
                .thenWithResource("select", JdbcResource.class, (_, _, promise, jdbcResource) -> {
                    promise.getRepositoryMapClass(RequestTank01.class);
                    responseData = null; // Обнуляем данные, если последовательные цепочки
                    List<Map<String, Object>> execute = jdbcResource.execute(new JdbcRequest(JTHttpCache.SELECT)
                            .addArg("url", uriSupplier.get())
                            .setDebug(false)
                    );
                    dataFindInDb = !execute.isEmpty();

                    if (!execute.isEmpty()) {
                        responseData = execute.getFirst().get("data").toString();
                        // Если данные есть и не надо всегда делать запрос - скипаем задачи
                        if (!alwaysRequestApi) {
                            promise.skipAllStep("already cache");
                        }
                    }
                    // Если говорят нужен запрос, это будет противоречить в режиме только кеш
                    if (onlyCache && !alwaysRequestApi) {
                        promise.skipAllStep("onlyCache");
                    }
                })
                .then("checkRateLimit", (_, _, _) -> {
                    if (!App.get(RateLimitNhlApi.class).isRateLimit()) {
                        throw new RuntimeException("RateLimit Finish");
                    }
                })
                .thenWithResource("request", HttpResource.class, (_, _, promise, httpResource) -> {
                    promise.getRepositoryMapClass(RequestTank01.class);
                    Util.logConsole(getClass(), "Request: " + uriSupplier.get() + "; isAlwaysRequestApi: " + alwaysRequestApi);
                    HttpResponse execute = httpResource.execute(getHttpClient(uriSupplier.get()));
                    checkResponse(execute);
                    responseData = execute.getBody();
                })
                .thenWithResource("insert", JdbcResource.class, (_, _, promise, jdbcResource) -> {
                    promise.getRepositoryMapClass(RequestTank01.class);
                    jdbcResource.execute(
                            new JdbcRequest(dataFindInDb ? JTHttpCache.UPDATE : JTHttpCache.INSERT)
                                    .addArg("url", uriSupplier.get())
                                    .addArg("data", getResponseData())
                                    .setDebug(false)
                    );
                    jdbcResource.execute(
                            new JdbcRequest(JTLogRequest.INSERT)
                                    .addArg("url", uriSupplier.get())
                                    .addArg("data", getResponseData())
                                    .setDebug(false)
                    );
                })
                ;
    }

    public static HttpConnector getHttpClient(String uri) {
        ServiceProperty serviceProperty = App.get(ServiceProperty.class);
        SecurityComponent securityComponent = App.get(SecurityComponent.class);
        return new HttpConnectorDefault()
                .setUrl(serviceProperty.get("rapidapi.host") + uri)
                .setRequestHeader("x-rapidapi-key", new String(securityComponent.get("rapidapi.tank01.key")))
                .setRequestHeader("x-rapidapi-host", serviceProperty.get("rapidapi.tank01.host"));
    }

    public static void checkResponse(HttpResponse execute) throws Throwable {
        if (execute.getException() != null) {
            throw execute.getException();
        }
        if (execute.getBody() == null || execute.getBody().isEmpty()) {
            throw new RuntimeException("empty response");
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> parsed = UtilJson.toObject(execute.getBody(), Map.class);
        if (parsed.containsKey("error")) {
            throw new RuntimeException(parsed.get("error").toString());
        }
        if (!parsed.containsKey("body")) {
            throw new RuntimeException("body does not exist");
        }
        if (!parsed.containsKey("statusCode")) {
            throw new RuntimeException("statusCode does not exist");
        }
        if (!parsed.get("statusCode").equals(200)) {
            throw new RuntimeException("statusCode not equals 200");
        }
    }

}
