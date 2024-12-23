package ru.jamsys.core.handler.web.http;

import lombok.Getter;
import lombok.Setter;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestMapping;
import ru.jamsys.core.App;
import ru.jamsys.core.component.ServicePromise;
import ru.jamsys.core.extension.exception.JsonSchemaException;
import ru.jamsys.core.extension.http.ServletHandler;
import ru.jamsys.core.promise.Promise;
import ru.jamsys.core.promise.PromiseGenerator;


@Component
@RequestMapping
public class Test implements PromiseGenerator, HttpHandler {

    @Getter
    @Setter
    private String index;

    private final ServicePromise servicePromise;

    public Test(ServicePromise servicePromise) {
        this.servicePromise = servicePromise;
    }

    @Override
    public Promise generate() {
        return servicePromise.get(index, 12_000L)
                .then("check", (_, _, promise) -> {
                    ServletHandler servletHandler = promise.getRepositoryMapClass(ServletHandler.class);
                    servletHandler.setResponseBody("Hello world");
                })
                .extension(Test::addErrorHandler);
    }

    public static void addErrorHandler(Promise promiseSource) {
        promiseSource.onError((_, _, promise) -> errorHandler(promise));
    }

    public static void errorHandler(Promise promise) {
        App.error(promise.getExceptionSource());
        ServletHandler servletHandler = promise.getRepositoryMapClass(ServletHandler.class);
        servletHandler.setResponseContentType("application/json");
        Throwable exception = promise.getExceptionSource();
        if (exception instanceof JsonSchemaException) {
            servletHandler.setResponseError(((JsonSchemaException) exception).getResponseError());
        } else {
            servletHandler.setResponseError(promise.getExceptionSource().getMessage());
        }
        servletHandler.responseComplete();
    }

}
