package ru.jamsys.core.flat.util.tank;

import io.reactivex.rxjava3.functions.Function;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import ru.jamsys.core.App;
import ru.jamsys.core.component.SecurityComponent;
import ru.jamsys.core.component.ServiceProperty;
import ru.jamsys.core.jt.JTHttpCache;
import ru.jamsys.core.promise.Promise;
import ru.jamsys.core.resource.http.HttpResource;
import ru.jamsys.core.resource.http.client.HttpClient;
import ru.jamsys.core.resource.http.client.HttpClientImpl;
import ru.jamsys.core.resource.http.client.HttpResponse;
import ru.jamsys.core.resource.jdbc.JdbcRequest;
import ru.jamsys.core.resource.jdbc.JdbcResource;

import java.util.List;
import java.util.Map;

public class UtilTank01 {

    @Getter
    @Setter
    @Accessors(chain = true)
    public static class Response {
        String data;
        boolean cache = false;
        String teamId;
    }

    private static HttpClient getHttpClient(String uri) {
        ServiceProperty serviceProperty = App.get(ServiceProperty.class);
        SecurityComponent securityComponent = App.get(SecurityComponent.class);
        return new HttpClientImpl()
                .setUrl(serviceProperty.get("rapidapi.host") + uri)
                .setRequestHeader("x-rapidapi-key", new String(securityComponent.get("rapidapi.tank01.key")))
                .setRequestHeader("x-rapidapi-host", serviceProperty.get("rapidapi.tank01.host"));
    }

    public static void cacheRequest(Promise refPromise, Function<Promise, String> uriSupplier) {
        refPromise.setRepositoryMapClass(Response.class, new Response());
        refPromise
                .thenWithResource("cacheSelect", JdbcResource.class, (_, _, promise, jdbcResource) -> {
                    Response response = promise.getRepositoryMapClass(Response.class);
                    response.setData(null); // Обнуляем данные, если последовательные цепочки
                    List<Map<String, Object>> execute = jdbcResource.execute(new JdbcRequest(JTHttpCache.SELECT)
                            .addArg("url", uriSupplier.apply(promise))
                            .setDebug(false)
                    );
                    response.setCache(!execute.isEmpty());
                    if (execute.isEmpty()) {
                        return;
                    }
                    response.setData(execute.getFirst().get("data").toString());
                    promise.goTo("cacheComplete");
                })
                .thenWithResource("request", HttpResource.class, (_, _, promise, httpResource) -> {
                    Response response = promise.getRepositoryMapClass(Response.class);
                    System.out.println("Request: " + uriSupplier.apply(promise));
                    HttpResponse execute = httpResource.execute(getHttpClient(uriSupplier.apply(promise)));
                    response.setData(execute.getBody());
                })
                .thenWithResource("cacheInsert", JdbcResource.class, (_, _, promise, jdbcResource) -> {
                    Response response = promise.getRepositoryMapClass(Response.class);
                    jdbcResource.execute(
                            new JdbcRequest(response.isCache() ? JTHttpCache.UPDATE : JTHttpCache.INSERT)
                                    .addArg("url", uriSupplier.apply(promise))
                                    .addArg("data", response.getData())
                                    .setDebug(false)
                    );
                    response.setData(response.getData());
                })
                .then("cacheComplete", (_, _, _) -> {
                });
    }
}
