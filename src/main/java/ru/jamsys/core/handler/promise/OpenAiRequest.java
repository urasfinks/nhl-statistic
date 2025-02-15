package ru.jamsys.core.handler.promise;

import lombok.Getter;
import lombok.experimental.Accessors;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.springframework.http.HttpStatus;
import ru.jamsys.core.App;
import ru.jamsys.core.component.SecurityComponent;
import ru.jamsys.core.component.ServicePromise;
import ru.jamsys.core.component.ServiceProperty;
import ru.jamsys.core.extension.builder.HashMapBuilder;
import ru.jamsys.core.extension.exception.ForwardException;
import ru.jamsys.core.flat.util.MotherResponse;
import ru.jamsys.core.flat.util.Util;
import ru.jamsys.core.flat.util.UtilJson;
import ru.jamsys.core.jt.JTLogRequest;
import ru.jamsys.core.promise.Promise;
import ru.jamsys.core.promise.PromiseGenerator;
import ru.jamsys.core.resource.http.HttpResource;
import ru.jamsys.core.resource.http.client.HttpResponse;
import ru.jamsys.core.resource.jdbc.JdbcRequest;
import ru.jamsys.core.resource.jdbc.JdbcResource;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

@Accessors(chain = true)
public class OpenAiRequest implements PromiseGenerator {

    @Getter
    private MotherResponse motherResponse;

    @Getter
    private HttpResponse httpResponse;

    String question;

    String prev = """
            Ты — детский врач или детский врач-диетолог. Твоя задача — помогать молодым мамам, анализируя их вопросы о здоровье и питании детей, и предоставлять точные, полезные и понятные ответы. Действуй по следующему алгоритму:
            Если вопрос вообще не связан с детским здоровьем и питанием (например, о ремонте, технике, финансах и т. п.), верни JSON: {"error": "..."}
            Если вопрос конкретный и относится к детскому здоровью, питанию (включая введение прикорма, допустимые продукты, аллергии, нормы питания), дай максимально полезные рекомендации (не более 10) в виде JSON: {"recommendations": []}"
            Дополнительно:
            Используй только проверенную и научно обоснованную информацию.
            Будь вежливым, поддерживающим и понимающим.
            Если вопрос требует срочного медицинского вмешательства, порекомендуй немедленно обратиться к врачу.""";

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
                    httpResponse = getHttpClient(question, prev);
                    motherResponse = checkResponse(httpResponse);
                })
                .thenWithResource("logData", JdbcResource.class, (run, _, promise, jdbcResource) -> {
                    jdbcResource.execute(
                            new JdbcRequest(JTLogRequest.INSERT)
                                    .addArg("url", "meta")
                                    .addArg("data", UtilJson.toStringPretty(new HashMapBuilder<String, Object>()
                                                    .append("prev", prev)
                                                    .append("question", question)
                                                    .append("motherResponse", motherResponse)
                                                    .append("httpResponse", httpResponse),
                                            "{}"))
                                    .setDebug(false)
                    );
                })
                ;
    }

    public static HttpResponse getHttpClient(String question, String prev) {
        ServiceProperty serviceProperty = App.get(ServiceProperty.class);
        String proxyHost = serviceProperty.get("http.proxy.server.host"); // Адрес прокси-сервера
        int proxyPort = serviceProperty.get(Integer.class, "http.proxy.server.port", 3128); // Порт прокси-сервера
        String proxyUser = serviceProperty.get("http.proxy.server.user"); // Логин для прокси
        String proxyPassword = new String(App
                .get(SecurityComponent.class)
                .get(serviceProperty.get("http.proxy.server.password.security.alias"))
        );

        // Настройка прокси и авторизации
        HttpHost proxy = new HttpHost(proxyHost, proxyPort);
        CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
        credentialsProvider.setCredentials(
                new AuthScope(proxyHost, proxyPort), // Указываем область авторизации
                new UsernamePasswordCredentials(proxyUser, proxyPassword) // Учетные данные
        );

        // Создание HTTP-клиента с прокси и авторизацией
        try (CloseableHttpClient httpClient = HttpClients.custom()
                .setProxy(proxy) // Указываем прокси
                .setDefaultCredentialsProvider(credentialsProvider) // Указываем провайдер учетных данных
                .build()) {

            // Создаем HTTP-запрос
            HttpPost request = new HttpPost(serviceProperty.get("openai.host"));
            request.setHeader("Content-Type", "application/json; charset=utf-8");
            request.setHeader("Accept", "application/json");
            request.setHeader("Authorization",
                    "Bearer " + new String(App
                            .get(SecurityComponent.class)
                            .get(serviceProperty.get("openai.security.alias"))
                    ));

            request.setEntity(new StringEntity(String.format("""
                            {
                                  "model": "gpt-4o",
                                  "messages": [
                                    {
                                      "role": "developer",
                                      "content": "%s"
                                    },
                                    {
                                      "role": "user",
                                      "content": "%s"
                                    }
                                  ]
                                }""",

                    escape(prev),
                    escape(question)
            ), "UTF-8"));
            // Выполняем запрос
            try (CloseableHttpResponse response = httpClient.execute(request)) {
                // Получаем и выводим ответ
                return getHttpResponse(
                        response.getStatusLine().getStatusCode(),
                        EntityUtils.toString(response.getEntity(), "UTF-8")
                );
            }
        } catch (IOException e) {
            App.error(e);
            return getHttpResponse(
                    -1,
                    null
            );
        }

    }

    public static HttpResponse getHttpResponse(int status, String body) {
        HttpResponse httpResponse = new HttpResponse();
        httpResponse.setStatusCode(status);
        if (status == -1) {
            httpResponse.addException("Запроса не было");
        } else {
            httpResponse.setStatusDesc(HttpStatus.valueOf(status));
        }
        if (httpResponse.getException() == null) {
            try {
                httpResponse.setBody(body);

            } catch (Exception e) {
                httpResponse.addException(e);
            }
        }
        return httpResponse;
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
