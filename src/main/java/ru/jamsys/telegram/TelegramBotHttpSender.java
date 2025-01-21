package ru.jamsys.telegram;

import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands;
import ru.jamsys.core.App;
import ru.jamsys.core.component.SecurityComponent;
import ru.jamsys.core.extension.builder.HashMapBuilder;
import ru.jamsys.core.flat.util.Util;
import ru.jamsys.core.flat.util.UtilJson;
import ru.jamsys.core.flat.util.UtilTelegram;
import ru.jamsys.core.flat.util.telegram.Button;
import ru.jamsys.core.handler.promise.RemoveSubscriberOvi;
import ru.jamsys.core.resource.http.client.HttpClientImpl;
import ru.jamsys.core.resource.http.client.HttpResponse;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TelegramBotHttpSender implements TelegramSender {

    private final String token;

    public TelegramBotHttpSender(BotProperty botProperty) { //example propertyAlias = telegram.bot.common
        SecurityComponent securityComponent = App.get(SecurityComponent.class);
        this.token = new String(securityComponent.get(botProperty.getSecurityAlias()));
    }

    public UtilTelegram.Result send(long idChat, String data, List<Button> buttons) {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("chat_id", idChat);
        requestBody.put("text", data);

        if (buttons != null && !buttons.isEmpty()) {
            List<List<Map<String, Object>>> list = new ArrayList<>();
            buttons.forEach(button -> list.add(List.of(new HashMapBuilder<String, Object>()
                    .append("text", button.getData())
                    .append("callback_data", button.getCallback()))));
            requestBody.put(
                    "reply_markup", new HashMapBuilder<>().append("inline_keyboard", list)
            );
        }
        Util.logConsoleJson(getClass(), requestBody);
        return nativeSend(idChat, UtilJson.toStringPretty(requestBody, "{}"));
    }

    public UtilTelegram.Result sendImage(long idChat, InputStream is, String fileName, String description) {
        return null;
    }

    @Override
    public void setMyCommands(SetMyCommands setMyCommands) {
        // Как будто для Sender не надо ничего высылать по командам
    }

    private UtilTelegram.Result nativeSend(long idChat, String data) {
        if (data == null) {
            return new UtilTelegram.Result()
                    .setException(UtilTelegram.ResultException.OTHER)
                    .setCause("data is null");
        }
        HttpClientImpl httpClient = new HttpClientImpl();
        httpClient.setUrl(String.format(
                        "https://api.telegram.org/bot%s/sendMessage",
                        token
                ))
                .putRequestHeader("Content-Type", "application/json")
                .setPostData(data.getBytes())
                .setTimeoutMs(10_000);
        httpClient.exec();
        UtilTelegram.Result sandbox = UtilTelegram.sandbox(result -> {
            HttpResponse httpResponse = httpClient.getHttpResponse();
            if (httpResponse.getStatusCode() == 200) {
                result.setResponse(UtilJson.getMapOrThrow(httpResponse.getBody()));
            } else {
                Map<String, Object> mapOrThrow = UtilJson.getMapOrThrow(httpResponse.getBody());
                result.setResponse(mapOrThrow);
                throw new RuntimeException(mapOrThrow.get("description").toString());
            }
        });
        if (UtilTelegram.ResultException.REVOKE.equals(sandbox.getException())) {
            new RemoveSubscriberOvi(idChat).generate().run();
        }
        return sandbox;
    }

}
