package ru.jamsys.core.handler.promise;

import lombok.Getter;
import lombok.experimental.Accessors;
import ru.jamsys.core.App;
import ru.jamsys.core.component.SecurityComponent;
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
import ru.jamsys.core.resource.http.client.HttpClient;
import ru.jamsys.core.resource.http.client.HttpClientImpl;
import ru.jamsys.core.resource.http.client.HttpMethodEnum;
import ru.jamsys.core.resource.http.client.HttpResponse;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

@Accessors(chain = true)
public class OpenAiRequest implements PromiseGenerator {

    @Getter
    private MotherResponse motherResponse;

    String question;

    public OpenAiRequest(String question) {
        this.question = question;
    }

    @Override
    public Promise generate() {
        return App.get(ServicePromise.class).get(getClass().getSimpleName(), 600_000L)
                .extension(promise -> promise.setRepositoryMapClass(OpenAiRequest.class, this))
                .thenWithResource("request", HttpResource.class, (_, _, promise, httpResource) -> {
                    promise.getRepositoryMapClass(OpenAiRequest.class);
                    Util.logConsole(getClass(), "Request openai");
                    HttpResponse execute = httpResource.execute(getHttpClient(question));
                    motherResponse = checkResponse(execute);
                })
                ;
    }

    public static HttpClient getHttpClient(String question) {
        ServiceProperty serviceProperty = App.get(ServiceProperty.class);
        return new HttpClientImpl()
                .setUrl(serviceProperty.get("openai.host"))
                .putRequestHeader("Content-Type", "application/json")

                .putRequestHeader(
                        "Authorization",
                        "Bearer " + new String(App
                                .get(SecurityComponent.class)
                                .get(serviceProperty.get("openai.security.alias"))
                        )
                )
                .setMethod(HttpMethodEnum.POST)
                .setPostData(String.format("""
                                        {
                                              "model": "gpt-4o",
                                              "messages": [
                                                {
                                                  "role": "developer",
                                                  "content": "You are a breastfeeding consultant. Your task is to analyze users' questions and provide accurate and helpful answers. Follow this algorithm:\\nIf the question is not related to breastfeeding, return JSON: {\\"error\\": \\"...\\"}\\nIf the question is related to breastfeeding but lacks specificity, return JSON with a clarifying question: {\\"clarification\\": \\"...\\"}\\nIf the question is specific, provide a maximum of 10 recommendations in JSON format: {\\"recommendations\\": []}\\nUse only verified and scientifically based information.\\nBe polite, supportive, and understanding.\\nIf the question requires urgent medical attention, recommend consulting a doctor."
                                                },
                                                {
                                                  "role": "user",
                                                  "content": "%s"
                                                }
                                              ]
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
        //Util.logConsole(OpenAiRequest.class, response);
        Map<String, Object> result = new LinkedHashMap<>();
        try {
            UtilJson.selector(
                    response,
                    new HashMapBuilder<String, String>()
                            .append("text", "$.choices[0].message.content"),
                    result
            );
            if (result.containsKey("text")) {
                Map<String, Object> mapOrThrow = UtilJson.getMapOrThrow(
                        ((String) result.getOrDefault("text", "{}"))
                                .replaceAll("```json", "")
                                .replaceAll("```", "")
                );
                //Util.logConsoleJson(YandexLlmRequest.class, mapOrThrow);
                //Util.logConsoleJson(OpenAiRequest.class, Util.mapToObject(mapOrThrow, MotherResponse.class));
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
                    Util.logConsole(OpenAiRequest.class, result.get("error").toString());
                    return new MotherResponse().setError("Ошибка, попробуйте позже").setRetry(true);
                }
            } catch (Throwable th2) {
                App.error(new ForwardException(th1));
            }
            return new MotherResponse().setError("Ошибка обработки ответа").setRetry(true);
        }
    }

    public static MotherResponse checkResponse(HttpResponse execute) throws Throwable {
        //Util.logConsoleJson(OpenAiRequest.class, execute);
        if (execute.getException() != null) {
            throw execute.getException();
        }
        if (execute.getBody() == null || execute.getBody().isEmpty()) {
            throw new RuntimeException("empty response");
        }
        return checkResponse(execute.getBody());
    }

}
