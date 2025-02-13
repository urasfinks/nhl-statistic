package ru.jamsys.core.handler.promise;

import lombok.Getter;
import lombok.Setter;
import ru.jamsys.core.App;
import ru.jamsys.core.component.SecurityComponent;
import ru.jamsys.core.component.ServicePromise;
import ru.jamsys.core.component.ServiceProperty;
import ru.jamsys.core.extension.exception.ForwardException;
import ru.jamsys.core.flat.util.Util;
import ru.jamsys.core.flat.util.UtilJson;
import ru.jamsys.core.promise.Promise;
import ru.jamsys.core.promise.PromiseGenerator;
import ru.jamsys.core.resource.http.HttpResource;
import ru.jamsys.core.resource.http.client.HttpClient;
import ru.jamsys.core.resource.http.client.HttpClientImpl;
import ru.jamsys.core.resource.http.client.HttpMethodEnum;
import ru.jamsys.core.resource.http.client.HttpResponse;

import java.nio.charset.StandardCharsets;
import java.util.Map;

@Getter
public class YandexTokenRequest implements PromiseGenerator {

    public static String token;

    @Setter
    private boolean onlyCache = false;

    @Setter
    private boolean alwaysRequestApi = false;

    @Override
    public Promise generate() {
        return App.get(ServicePromise.class).get(getClass().getSimpleName(), 60_000L)
                .extension(promise -> promise.setRepositoryMapClass(YandexTokenRequest.class, this))
                .thenWithResource("request", HttpResource.class, (_, _, promise, httpResource) -> {
                    promise.getRepositoryMapClass(YandexTokenRequest.class);
                    Util.logConsole(getClass(), "Request Yandex.Token; isAlwaysRequestApi: " + alwaysRequestApi);
                    HttpResponse execute = httpResource.execute(getHttpClient());
                    token = checkResponse(execute);
                    Util.logConsole(getClass(), token);
                })
                ;
    }

    public static HttpClient getHttpClient() {
        ServiceProperty serviceProperty = App.get(ServiceProperty.class);
        SecurityComponent securityComponent = App.get(SecurityComponent.class);
        return new HttpClientImpl()
                .setUrl(serviceProperty.get("yandex.token.host"))
                .setMethod(HttpMethodEnum.POST)
                .setPostData(String.format("""
                                        {"yandexPassportOauthToken":"%s"}""",
                                new String(securityComponent.get("yandex.passport.oauth.token"))
                        ).getBytes(StandardCharsets.UTF_8)
                );
    }

    public static String checkResponse(String response) {
        try {
            Map<String, Object> mapOrThrow = UtilJson.getMapOrThrow(response);
            if (mapOrThrow.containsKey("iamToken")) {
                return mapOrThrow.get("iamToken").toString();
            }
        } catch (Throwable th1) {
            App.error(new ForwardException(th1));

        }
        return null;
    }

    public static String checkResponse(HttpResponse execute) throws Throwable {
        if (execute.getException() != null) {
            throw execute.getException();
        }
        if (execute.getBody() == null || execute.getBody().isEmpty()) {
            throw new RuntimeException("empty response");
        }
        return checkResponse(execute.getBody());
    }

}
