package ru.jamsys.core.handler.promise;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import ru.jamsys.core.App;
import ru.jamsys.core.component.ServicePromise;
import ru.jamsys.core.flat.util.UtilJson;
import ru.jamsys.core.flat.util.telegram.Button;
import ru.jamsys.core.jt.JTBroadcastTemplate;
import ru.jamsys.core.promise.Promise;
import ru.jamsys.core.promise.PromiseGenerator;
import ru.jamsys.core.resource.jdbc.JdbcRequest;
import ru.jamsys.core.resource.jdbc.JdbcResource;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

// Получить bet TelegramNotification

@Getter
@Setter
@ToString
public class BetSourceNotification implements PromiseGenerator {

    private final String key;

    private String message;
    private List<Button> listButton;
    private String idImage;
    private String idVideo;
    private String pathImage;

    public BetSourceNotification(String key) {
        this.key = key;
    }

    @Override
    public Promise generate() {
        return App.get(ServicePromise.class).get(getClass().getSimpleName(), 60_000L)
                .extension(promise -> promise.setRepositoryMapClass(BetSourceNotification.class, this)) // Просто для отладки
                .thenWithResource("get", JdbcResource.class, (_, _, _, jdbcResource) -> {
                    List<JTBroadcastTemplate.Row> res = jdbcResource.execute(new JdbcRequest(JTBroadcastTemplate.SELECT)
                            .addArg("key", key), JTBroadcastTemplate.Row.class);
                    if (!res.isEmpty()) {
                        JTBroadcastTemplate.Row first = res.getFirst();
                        Map<String, Object> mapOrThrow = UtilJson.getMapOrThrow(first.getTelegramNotification());
                        //{
                        //  "message" : "Привет",
                        //  "buttons" : [
                        //    {
                        //      "data" : "Ку-ку",
                        //      "callback" : null,
                        //      "url" : "https://ya.ru"
                        //    }
                        //  ],
                        //  "pathImage" : null
                        //}
                        //{
                        //  "message" : null,
                        //  "buttons" : null,
                        //  "idImage" : "AgACAgIAAxkBAAIoY2e_cQZ8UGFIRy256LlDZO3MDZJhAAKy5zEbGx4AAUrtgw6LRJRITwEAAwIAA3gAAzYE",
                        //  "idVideo" : null
                        //}
                        if (mapOrThrow.containsKey("message") && mapOrThrow.get("message") != null) {
                            message = mapOrThrow.get("message").toString();
                        }
                        if (mapOrThrow.containsKey("pathImage") && mapOrThrow.get("pathImage") != null) {
                            pathImage = mapOrThrow.get("pathImage").toString();
                        }
                        if (mapOrThrow.containsKey("idImage") && mapOrThrow.get("idImage") != null) {
                            idImage = mapOrThrow.get("idImage").toString();
                        }
                        if (mapOrThrow.containsKey("idVideo") && mapOrThrow.get("idVideo") != null) {
                            idVideo = mapOrThrow.get("idVideo").toString();
                        }
                        if (mapOrThrow.containsKey("buttons") && mapOrThrow.get("buttons") != null) {
                            @SuppressWarnings("unchecked")
                            List<Object> listOrThrow = (List<Object>) mapOrThrow.get("buttons");
                            listButton = parseButton(listOrThrow);
                        }
                    }
                })
                .setDebug(false)
                ;
    }

    public static List<Button> parseButton(List<Object> listOrThrow) {
        List<Button> result = new ArrayList<>();
        try {
            listOrThrow.forEach(o -> {
                @SuppressWarnings("unchecked")
                Map<String, Object> o1 = (Map<String, Object>) o;
                Button btn = new Button(o1.get("data").toString());

                if (o1.containsKey("callback") && o1.get("callback") != null) {
                    btn.setCallback(o1.get("callback").toString());
                }
                if (o1.containsKey("url") && o1.get("url") != null) {
                    btn.setUrl(o1.get("url").toString());
                }
                if (o1.containsKey("webapp") && o1.get("webapp") != null) {
                    btn.setWebapp(o1.get("webapp").toString());
                }
                result.add(btn);
            });
        } catch (Throwable th) {
            App.error(th);
        }
        return result.isEmpty() ? null : result;
    }

    public boolean isNotEmpty() {
        return message != null || listButton != null || pathImage != null || idImage != null || idVideo != null;
    }
}
