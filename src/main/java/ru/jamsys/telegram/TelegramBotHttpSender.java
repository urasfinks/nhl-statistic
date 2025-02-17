package ru.jamsys.telegram;

import org.apache.http.HttpEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands;
import ru.jamsys.core.App;
import ru.jamsys.core.component.SecurityComponent;
import ru.jamsys.core.component.manager.item.Session;
import ru.jamsys.core.extension.builder.HashMapBuilder;
import ru.jamsys.core.flat.util.UtilJson;
import ru.jamsys.core.flat.util.UtilTelegramResponse;
import ru.jamsys.core.flat.util.telegram.Button;
import ru.jamsys.core.handler.promise.RemoveSubscriberOvi;
import ru.jamsys.core.resource.http.client.HttpConnector;
import ru.jamsys.core.resource.http.client.HttpConnectorDefault;
import ru.jamsys.core.resource.http.client.HttpResponse;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TelegramBotHttpSender implements TelegramSender {

    //private static final String urlEndpoint = "https://api.telegram.org/bot%s/%s";
    private static final String urlEndpoint = "http://147.45.146.13:8081/bot%s/%s";

    private final String token;

    private final Session<String, String> fileUpload; //key - filePath; value - file_id

    public TelegramBotHttpSender(BotProperty botProperty) { //example propertyAlias = telegram.bot.common
        SecurityComponent securityComponent = App.get(SecurityComponent.class);
        this.token = new String(securityComponent.get(botProperty.getSecurityAlias()));
        fileUpload = new Session<>(
                TelegramBotHttpSender.class.getSimpleName() + "_" + botProperty.getName(),
                600_000L
        );
        //fileUpload.put("873.png", "AgACAgIAAxkDAAMjZ3xKa0B2nbPxHBFLcT-bOhNblMIAAvrvMRtnQ-BLLPvtL97-fIUBAAMCAANzAAM2BA");
    }

    public UtilTelegramResponse.Result send(long idChat, String data, List<Button> buttons) {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("chat_id", idChat);
        if (data != null && !data.isEmpty()) {
            requestBody.put("text", data);
        }

        if (buttons != null && !buttons.isEmpty()) {
            List<List<Map<String, Object>>> list = new ArrayList<>();
            buttons.forEach(button -> list.add(List.of(new HashMapBuilder<String, Object>()
                    .append("text", button.getData())
                    .append("callback_data", button.getCallback()))));
            requestBody.put(
                    "reply_markup", new HashMapBuilder<>().append("inline_keyboard", list)
            );
        }
        return httpSend(idChat, UtilJson.toStringPretty(requestBody, "{}"), "sendMessage");
    }

    public UtilTelegramResponse.Result sendImage(long idChat, InputStream is, String fileName, String description) {
        if (fileUpload.containsKey(fileName)) {
            return sendImage(idChat, fileUpload.get(fileName), description);
        } else {
            try {
                return sendImageMultipart(idChat, fileName, is, description);
            } catch (Throwable th) {
                return new UtilTelegramResponse.Result()
                        .setException(UtilTelegramResponse.ResultException.OTHER)
                        .setCause(th.getMessage());
            }
        }
    }

    @Override
    public void setMyCommands(SetMyCommands setMyCommands) {
        // Как будто для Sender не надо ничего высылать по командам
    }

    @Override
    public TelegramSender setNotCommandPrefix(String notCommandPrefix) {
        // Для реализации http предполагается только рассылка без возможности обрабатывать сообщения
        return this;
    }

    private UtilTelegramResponse.Result httpSend(long idChat, String data, String apiMethod) {
        //Util.logConsole(getClass(), idChat + ">>" + data);
        if (data == null) {
            return new UtilTelegramResponse.Result()
                    .setException(UtilTelegramResponse.ResultException.OTHER)
                    .setCause("data is null");
        }
        HttpConnector httpClient = new HttpConnectorDefault();
        httpClient.setUrl(String.format(
                        urlEndpoint,
                        token,
                        apiMethod
                ))
                .setRequestHeader("Content-Type", "application/json")
                .setPostData(data.getBytes())
                .setTimeoutMs(10_000);
        httpClient.exec();
        UtilTelegramResponse.Result sandbox = UtilTelegramResponse.sandbox(result -> {
            HttpResponse httpResponse = httpClient.getResponseObject();
            if (httpResponse.getStatusCode() == 200) {
                result.setResponse(UtilJson.getMapOrThrow(httpResponse.getBody()));
            } else {
                Map<String, Object> mapOrThrow = UtilJson.getMapOrThrow(httpResponse.getBody());
                result.setResponse(mapOrThrow);
                throw new RuntimeException(mapOrThrow.get("description").toString());
            }
        });
        if (UtilTelegramResponse.ResultException.REVOKE.equals(sandbox.getException())) {
            new RemoveSubscriberOvi(idChat).generate().run();
        }
        sandbox.setRequestTiming(httpClient.getTiming());
        return sandbox;
    }

    private UtilTelegramResponse.Result sendImageMultipart(long idChat, String fileName, InputStream is, String description) throws Exception {
        MultipartEntityBuilder builder = MultipartEntityBuilder.create();
        builder.addTextBody("chat_id", String.valueOf(idChat));
        if (description != null && !description.isEmpty()) {
            builder.addTextBody("caption", description);
        }
        builder.addBinaryBody(
                "photo",
                is,
                ContentType.parse(Files.probeContentType(Path.of(fileName))),
                fileName
        );
        HttpEntity httpEntity = builder.build();
        byte[] postData;
        try (ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream()) {
            httpEntity.writeTo(byteArrayOutputStream); // keeps us from calling getContent which will throw Content Length unknown
            postData = byteArrayOutputStream.toByteArray();
        }

        HttpConnector httpClient = new HttpConnectorDefault();
        httpClient.setUrl(String.format(
                        urlEndpoint,
                        token,
                        "sendPhoto"
                ))
                .setRequestHeader(httpEntity.getContentType().getName(), httpEntity.getContentType().getValue())
                .setPostData(postData)
                .setTimeoutMs(10_000);
        httpClient.exec();
        UtilTelegramResponse.Result sandbox = UtilTelegramResponse.sandbox(result -> {
            HttpResponse httpResponse = httpClient.getResponseObject();
            if (httpResponse.getStatusCode() == 200) {
                String filePhotoId = getFilePhotoId(httpResponse.getBody());
                if (filePhotoId != null) {
                    fileUpload.put(fileName, filePhotoId);
                }
                result.setResponse(UtilJson.getMapOrThrow(httpResponse.getBody()));
            } else {
                Map<String, Object> mapOrThrow = UtilJson.getMapOrThrow(httpResponse.getBody());
                result.setResponse(mapOrThrow);
                throw new RuntimeException(mapOrThrow.get("description").toString());
            }
        });
        if (UtilTelegramResponse.ResultException.REVOKE.equals(sandbox.getException())) {
            new RemoveSubscriberOvi(idChat).generate().run();
        }
        sandbox.setRequestTiming(httpClient.getTiming());
        return sandbox;
    }

    public static String getFilePhotoId(String messageBlock) {
        try {
            @SuppressWarnings("all")
            Map<String, Object> message = UtilJson.getMapOrThrow(messageBlock);
            if (!message.containsKey("photo")) {
                if(!message.containsKey("result")){
                    return null;
                }
                message = (Map<String, Object>) message.get("result");
            }
            @SuppressWarnings("all")
            List<Map<String, Object>> photos = (List<Map<String, Object>>) message.get("photo");
            // Проходим по всем фото в массиве
            String largestFileId = null;
            int maxResolution = 0;
            for (Map<String, Object> photo : photos) {
                int width = Integer.parseInt(photo.get("width").toString());
                int height = Integer.parseInt(photo.get("height").toString());
                int resolution = width * height; // Вычисляем разрешение (ширина * высота)
                // Если текущее фото имеет большее разрешение, обновляем данные
                if (resolution > maxResolution) {
                    maxResolution = resolution;
                    largestFileId = photo.get("file_id").toString();
                }
            }
            return largestFileId;
        } catch (Throwable e) {
            App.error(e);
        }
        return null;
    }

    public static String getTextFileId(String text) {
        //return text.replaceAll(".*\"file_id\":\"([^\"]+)\".*", "$1");
        if (text == null || text.isEmpty()) {
            return null;
        }
        if (!text.contains("\"file_id\"")) {
            return null;
        }
        text = text.substring(text.indexOf("\"file_id\"") + 9);

        text = text.substring(text.indexOf("\"") + 1);
        return text.substring(0, text.indexOf("\""));
    }

    @Override
    public UtilTelegramResponse.Result sendImage(long idChat, String idFile, String description) {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("chat_id", idChat);
        if (description != null && !description.isEmpty()) {
            requestBody.put("caption", description);
        }
        requestBody.put("photo", idFile);
        return httpSend(idChat, UtilJson.toStringPretty(requestBody, "{}"), "sendPhoto");
    }

    @Override
    public UtilTelegramResponse.Result sendVideo(long idChat, String idFile, String description) {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("chat_id", idChat);
        if (description != null && !description.isEmpty()) {
            requestBody.put("caption", description);
        }
        requestBody.put("video", idFile);
        return httpSend(idChat, UtilJson.toStringPretty(requestBody, "{}"), "sendVideo");
    }

}
