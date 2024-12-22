package ru.jamsys.core.flat.util.tank;

import io.reactivex.rxjava3.functions.Function;
import ru.jamsys.core.App;
import ru.jamsys.core.component.SecurityComponent;
import ru.jamsys.core.component.ServiceProperty;
import ru.jamsys.core.promise.Promise;
import ru.jamsys.core.resource.http.HttpResource;
import ru.jamsys.core.resource.http.client.HttpClient;
import ru.jamsys.core.resource.http.client.HttpClientImpl;
import ru.jamsys.core.resource.http.client.HttpResponse;

public class UtilTank01 {

    public static HttpClient getHttpClient(String uri) {
        ServiceProperty serviceProperty = App.get(ServiceProperty.class);
        SecurityComponent securityComponent = App.get(SecurityComponent.class);
        return new HttpClientImpl()
                .setUrl(serviceProperty.get("rapidapi.host") + uri)
                .setRequestHeader("x-rapidapi-key", new String(securityComponent.get("rapidapi.tank01.key")))
                .setRequestHeader("x-rapidapi-host", serviceProperty.get("rapidapi.tank01.host"));
    }

    public static HttpResponse request(
            HttpResource httpResource,
            Promise promise,
            Function<Promise, String> uriSupplier
    ) throws Throwable {
        HttpResponse response = httpResource.execute(getHttpClient(uriSupplier.apply(promise)));
        checkResponse(response);
        return response;
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
