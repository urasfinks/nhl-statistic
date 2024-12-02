package ru.jamsys.core.flat.util.tank;

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
    public static class Context {
        HttpClient httpClient;
        String data;
        boolean cache = false;
    }

    private static HttpClient getHttpClient(String uri) {
        ServiceProperty serviceProperty = App.get(ServiceProperty.class);
        SecurityComponent securityComponent = App.get(SecurityComponent.class);
        return new HttpClientImpl()
                .setUrl(serviceProperty.get("rapidapi.host") + uri)
                .setRequestHeader("x-rapidapi-key", new String(securityComponent.get("rapidapi.tank01.key")))
                .setRequestHeader("x-rapidapi-host", serviceProperty.get("rapidapi.tank01.host"))
                ;
    }

    public static void request(Promise refPromise, String uri) {
        refPromise.setRepositoryMapClass(Context.class, new Context().setHttpClient(getHttpClient(uri)));
        refPromise
                .thenWithResource("request", HttpResource.class, (_, _, promise, httpResource) -> {
                    Context context = promise.getRepositoryMapClass(Context.class);
                    HttpResponse execute = httpResource.execute(context.getHttpClient());
                    context.setData(execute.getBody());
                })
        ;
    }

    public static void cacheRequest(Promise refPromise, String uri) {
        refPromise.setRepositoryMapClass(Context.class, new Context().setHttpClient(getHttpClient(uri)));
        refPromise
                .thenWithResource("cacheSelect", JdbcResource.class, (_, _, promise, jdbcResource) -> {
                    Context context = promise.getRepositoryMapClass(Context.class);
                    List<Map<String, Object>> execute = jdbcResource.execute(new JdbcRequest(JTHttpCache.SELECT)
                            .addArg("url", context.getHttpClient().getUrl())
                            .setDebug(false)
                    );
                    context.setCache(!execute.isEmpty());
                    if (execute.isEmpty()) {
                        return;
                    }
                    context.setData(execute.getFirst().get("data").toString());
                    promise.goTo("cacheComplete");
                })
                .thenWithResource("request", HttpResource.class, (_, _, promise, httpResource) -> {
                    Context context = promise.getRepositoryMapClass(Context.class);
                    System.out.println("Request: " + context.getHttpClient().getUrl());
                    HttpResponse execute = httpResource.execute(context.getHttpClient());
                    context.setData(execute.getBody());

                })
                .thenWithResource("cacheInsert", JdbcResource.class, (_, _, promise, jdbcResource) -> {
                    Context context = promise.getRepositoryMapClass(Context.class);
                    jdbcResource.execute(
                            new JdbcRequest(context.isCache() ? JTHttpCache.UPDATE : JTHttpCache.INSERT)
                                    .addArg("url", context.getHttpClient().getUrl())
                                    .addArg("data", context.getData())
                                    .setDebug(false)
                    );
                    context.setData(context.getData());
                })
                .then("cacheComplete", (_, _, _) -> {
                })
        ;
    }
}
