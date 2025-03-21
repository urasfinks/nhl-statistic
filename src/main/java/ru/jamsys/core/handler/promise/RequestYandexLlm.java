package ru.jamsys.core.handler.promise;

import lombok.Getter;
import lombok.experimental.Accessors;
import ru.jamsys.core.App;
import ru.jamsys.core.component.ServicePromise;
import ru.jamsys.core.component.ServiceProperty;
import ru.jamsys.core.extension.builder.HashMapBuilder;
import ru.jamsys.core.extension.exception.ForwardException;
import ru.jamsys.core.flat.util.MotherResponse;
import ru.jamsys.core.flat.util.Util;
import ru.jamsys.core.flat.util.UtilJson;
import ru.jamsys.core.promise.Promise;
import ru.jamsys.core.promise.PromiseGenerator;
import ru.jamsys.core.resource.http.HttpResource;
import ru.jamsys.core.resource.http.client.HttpConnector;
import ru.jamsys.core.resource.http.client.HttpConnectorDefault;
import ru.jamsys.core.resource.http.client.HttpMethodEnum;
import ru.jamsys.core.resource.http.client.HttpResponse;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

// Одноконтекстный запрос в YandexGpt
// Если использовать - надо включить в Min30Scheduler пересоздание токена yandex для iam proxy

@Accessors(chain = true)
public class RequestYandexLlm implements PromiseGenerator {

    @Getter
    private MotherResponse motherResponse;

    String question;

    public RequestYandexLlm(String question) {
        this.question = question;
    }

    @Override
    public Promise generate() {
        return App.get(ServicePromise.class).get(getClass().getSimpleName(), 60_000L)
                .extension(promise -> promise.setRepositoryMapClass(RequestYandexLlm.class, this)).thenWithResource("request", HttpResource.class, (_, _, promise, httpResource) -> {
                    promise.getRepositoryMapClass(RequestYandexLlm.class);
                    Util.logConsole(getClass(), "Request Yandex.LL");
                    HttpResponse execute = httpResource.execute(getHttpClient(question));
                    motherResponse = checkResponse(execute);
                })
                ;
    }

    public static HttpConnector getHttpClient(String question) {
        ServiceProperty serviceProperty = App.get(ServiceProperty.class);
        return new HttpConnectorDefault()
                .setUrl(serviceProperty.get("yandex.llm.host"))
                .setRequestHeader("Content-Type", "application/json")
                .setRequestHeader("Authorization", "Bearer " + RequestYandexToken.token)
                .setMethod(HttpMethodEnum.POST)
                .setPostData(String.format("""
                                        {
                                           "messages": [
                                             {
                                               "text": "Ты — помощник, консультант по грудному вскармливанию. Твоя задача — анализировать вопросы и предоставлять точные и полезные ответы. Действуй по следующему алгоритму:\\nЕсли вопрос не связан с кормлением ребёнка, верни JSON: {\\"error\\": \\"Вопрос не связан с кормлением\\"} Если вопрос связан с кормлением, но недостаточно конкретный, верни JSON с уточняющим вопросом, чтобы помочь пользователю сформулировать запрос более точно: {\\"clarification\\": \\"...\\"} Если вопрос конкретный и связан с кормлением, проанализируй его, определи возможные причины и дай рекомендации в виде списка шагов для решения проблемы. Верни JSON: {\\"recommendations\\": [\\"...\\"]} Используй только проверенную и научно обоснованную информацию.\\nБудь вежливым, поддерживающим и понимающим.\\nЕсли вопрос требует срочного медицинского вмешательства, порекомендуй обратиться к врачу. Ничего кроме json возвращать не надо!",
                                               "role": "system"
                                             },
                                             {
                                               "text": "%s",
                                               "role": "user"
                                             }
                                           ],
                                           "completionOptions": {
                                             "temperature": 0,
                                             "maxTokens": 1000
                                           },
                                           "modelUri": "gpt://b1g05tf6j6kur2mhmotm/yandexgpt/rc"
                                         }""",
                                escape(question)
                        ).getBytes(StandardCharsets.UTF_8)
                );
    }

    public static String escape(String text) {
        String result = UtilJson.toString(text, "\"\"");
        if (result == null) {
            return "";
        }
        return result.substring(1, result.length() - 1);
    }

    public static MotherResponse checkResponse(String response) {
        Map<String, Object> result = new LinkedHashMap<>();
        try {
            UtilJson.selector(
                    response,
                    new HashMapBuilder<String, String>()
                            .append("text", "$.result.alternatives[0].message.text"),
                    result
            );
            if (result.containsKey("text")) {
                Map<String, Object> mapOrThrow = UtilJson.getMapOrThrow(
                        ((String) result.getOrDefault("text", "{}")).replaceAll("```", "")
                );
                //Util.logConsoleJson(YandexLlmRequest.class, mapOrThrow);
                return Util.mapToObject(mapOrThrow, MotherResponse.class);
            } else {
                return new MotherResponse().setError("Нет данных").setRetry(false);
            }
        } catch (Throwable th1) {
            App.error(new ForwardException(th1));
            try {
                UtilJson.selector(
                        response,
                        new HashMapBuilder<String, String>()
                                .append("error", "$.error.message"),
                        result
                );
                if (result.containsKey("error")) {
                    Util.logConsole(RequestYandexLlm.class, result.get("error").toString());
                    return new MotherResponse().setError("Ошибка, попробуйте позже").setRetry(true);
                }
            } catch (Throwable th2) {
                App.error(new ForwardException(th1));
            }
            return new MotherResponse().setError("Ошибка обработки ответа").setRetry(true);
        }
    }

    public static MotherResponse checkResponse(HttpResponse execute) throws Throwable {
        if (execute.getException() != null) {
            throw execute.getException();
        }
        if (execute.getBody() == null || execute.getBody().isEmpty()) {
            throw new RuntimeException("empty response");
        }
        return checkResponse(execute.getBody());
    }

}
