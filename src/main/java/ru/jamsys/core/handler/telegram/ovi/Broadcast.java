package ru.jamsys.core.handler.telegram.ovi;

import lombok.Getter;
import lombok.Setter;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestMapping;
import ru.jamsys.NhlStatisticApplication;
import ru.jamsys.core.App;
import ru.jamsys.core.component.ServicePromise;
import ru.jamsys.core.component.TelegramBotManager;
import ru.jamsys.core.component.manager.item.Session;
import ru.jamsys.core.extension.builder.ArrayListBuilder;
import ru.jamsys.core.extension.builder.HashMapBuilder;
import ru.jamsys.core.extension.http.ServletResponseWriter;
import ru.jamsys.core.flat.util.UtilJson;
import ru.jamsys.core.flat.util.telegram.Button;
import ru.jamsys.core.handler.promise.RegisterNotification;
import ru.jamsys.core.jt.JTOviSubscriber;
import ru.jamsys.core.promise.Promise;
import ru.jamsys.core.promise.PromiseGenerator;
import ru.jamsys.core.resource.jdbc.JdbcRequest;
import ru.jamsys.core.resource.jdbc.JdbcResource;
import ru.jamsys.telegram.TelegramBotHttpSender;
import ru.jamsys.telegram.TelegramCommandContext;
import ru.jamsys.telegram.TelegramNotification;
import ru.jamsys.telegram.handler.OviGoalsBotCommandHandler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@SuppressWarnings("unused")
@Setter
@Getter
@Component
@Lazy
@RequestMapping({"/broadcast/**", "/bt/**"})
public class Broadcast implements PromiseGenerator, OviGoalsBotCommandHandler {

    private final ServicePromise servicePromise;

    @Getter
    @Setter
    public static class CreateMessage {

        public final Long idChat;
        public final String bot;


        Map<String, String> map = new HashMap<>();

        public CreateMessage(Long idChat, String bot) {
            this.idChat = idChat;
            this.bot = bot;
        }

        public TelegramNotification get() {
            TelegramNotification telegramNotification = new TelegramNotification(
                    idChat,
                    bot,
                    map.get("text"),
                    null,
                    null
            );
            telegramNotification.setIdImage(map.get("image"));
            telegramNotification.setIdVideo(map.get("video"));
            return telegramNotification;
        }
    }

    private final Session<Long, CreateMessage> mapSession;

    public Broadcast(ServicePromise servicePromise) {
        this.servicePromise = servicePromise;
        mapSession = new Session<>(getClass().getSimpleName(), 6_000_000L);
    }

    @Getter
    @Setter
    public static class Context {
        private final List<Long> listIdChat = new ArrayList<>();
        private boolean all = false;
    }

    @Override
    public Promise generate() {
        Promise gen = servicePromise.get(getClass().getSimpleName(), 12_000L);
        gen
                .extension(promise -> promise.setRepositoryMapClass(Context.class, new Context()))
                .then("prep", (_, _, promise) -> {
                    Context context = promise.getRepositoryMapClass(Context.class);
                    TelegramCommandContext contextTelegram = promise.getRepositoryMapClass(TelegramCommandContext.class);
                    if (contextTelegram.getIdChat() != 290029195L && contextTelegram.getIdChat() != 241022301L) {
                        promise.skipAllStep("not admin test");
                        return;
                    }
                    CreateMessage session = mapSession.computeIfAbsent(contextTelegram.getIdChat(), aLong -> new CreateMessage(
                            contextTelegram.getIdChat(),
                            contextTelegram.getTelegramBot().getBotUsername()
                    ));
                    if (contextTelegram.getUriParameters().isEmpty()) {
                        session.getMap().clear();
                        RegisterNotification.add(new TelegramNotification(
                                contextTelegram.getIdChat(),
                                contextTelegram.getTelegramBot().getBotUsername(),
                                "Сборщик сообщения",
                                new ArrayListBuilder<Button>()
                                        .append(new Button(
                                                "Текст",
                                                ServletResponseWriter.buildUrlQuery(
                                                        "/bt/",
                                                        new HashMapBuilder<String, String>().append("setup", "text")
                                                )
                                        ))
                                        .append(new Button(
                                                "Картинка",
                                                ServletResponseWriter.buildUrlQuery(
                                                        "/bt/",
                                                        new HashMapBuilder<String, String>().append("setup", "image")
                                                )
                                        ))
                                        .append(new Button(
                                                "Видео",
                                                ServletResponseWriter.buildUrlQuery(
                                                        "/bt/",
                                                        new HashMapBuilder<String, String>().append("setup", "video")
                                                )
                                        ))
                                        .append(new Button(
                                                "Превью",
                                                ServletResponseWriter.buildUrlQuery(
                                                        "/bt/",
                                                        new HashMapBuilder<String, String>().append("setup", "preview")
                                                )
                                        ))
                                        .append(new Button(
                                                "Разослать тестовой группе",
                                                ServletResponseWriter.buildUrlQuery(
                                                        "/bt/",
                                                        new HashMapBuilder<String, String>().append("setup", "test")
                                                )
                                        ))
                                        .append(new Button(
                                                "Разослать всем",
                                                ServletResponseWriter.buildUrlQuery(
                                                        "/bt/",
                                                        new HashMapBuilder<String, String>().append("setup", "all")
                                                )
                                        ))
                                ,
                                null
                        ));
                    } else if (contextTelegram.getUriParameters().containsKey("setup")) {
                        String setup = contextTelegram.getUriParameters().get("setup");
                        switch (setup) {
                            case "all" -> {
                                context.setAll(true);
                                return;
                            }
                            case "test" -> {
                                context.setAll(false);
                                return;
                            }
                            case "preview" -> RegisterNotification.add(new ArrayListBuilder<TelegramNotification>()
                                    .append(session.get())
                                    .append(new TelegramNotification(
                                            contextTelegram.getIdChat(),
                                            contextTelegram.getTelegramBot().getBotUsername(),
                                            UtilJson.toStringPretty(session, "{}"),
                                            null,
                                            null
                                    ))
                            );
                            default -> {
                                contextTelegram.getStepHandler().put(contextTelegram.getIdChat(), contextTelegram.getUriPath() + "/?" + setup + "=");
                                RegisterNotification.add(new TelegramNotification(
                                        contextTelegram.getIdChat(),
                                        contextTelegram.getTelegramBot().getBotUsername(),
                                        setup.equals("text") ? "Вводи:" : "Грузи",
                                        null,
                                        null
                                ));
                            }
                        }
                    } else {
                        if (contextTelegram.getUriParameters().containsKey("image")) {
                            String fileId = TelegramBotHttpSender
                                    .getFilePhotoId(UtilJson.toStringPretty(contextTelegram.getMsg().getMessage(), "{}"));
                            // К сожалению, файл от пользователя] нельзя рассылать другим пользователям
                            // поэтому надо его загрузить на сервер и от своего имени отправить
                            // и только этот fileId прихранить для рассылки
                            if (fileId != null && !fileId.isEmpty()) {
                                session.getMap().put("image", fileId);
                                RegisterNotification.add(new TelegramNotification(
                                        contextTelegram.getIdChat(),
                                        contextTelegram.getTelegramBot().getBotUsername(),
                                        "Зафиксировал изображение: " + fileId,
                                        null,
                                        null
                                ));
//                                String filePathDownloaded = contextTelegram.getTelegramBot().downloadFileCustom(fileId);
//                                //Util.logConsole(getClass(), filePathDownloaded);
//                                UtilTelegramResponse.Result result = contextTelegram.getTelegramBot().sendImage(
//                                        contextTelegram.getIdChat(),
//                                        new FileInputStream(filePathDownloaded),
//                                        filePathDownloaded,
//                                        null
//                                );
//                                Util.logConsoleJson(getClass(), result.getResponse());
//                                String fileId2 = TelegramBotHttpSender
//                                        .getFilePhotoId(UtilJson.toStringPretty(result.getResponse(), "{}"));
//                                session.getMap().put("image", fileId2);
//                                RegisterNotification.add(new TelegramNotification(
//                                        contextTelegram.getIdChat(),
//                                        contextTelegram.getTelegramBot().getBotUsername(),
//                                        "Зафиксировал изображение: " + fileId2,
//                                        null,
//                                        null
//                                ));
                            }
                        } else if (contextTelegram.getUriParameters().containsKey("video")) {
                            String fileId = TelegramBotHttpSender
                                    .getTextFileId(UtilJson.toStringPretty(contextTelegram.getMsg(), "{}"));
                            if (fileId != null && !fileId.isEmpty()) {
                                session.getMap().put("video", fileId);
                                RegisterNotification.add(new TelegramNotification(
                                        contextTelegram.getIdChat(),
                                        contextTelegram.getTelegramBot().getBotUsername(),
                                        "Зафиксировал видео: " + fileId,
                                        null,
                                        null
                                ));
                            }
                        } else {
                            session.getMap().putAll(contextTelegram.getUriParameters());
                            RegisterNotification.add(new TelegramNotification(
                                    contextTelegram.getIdChat(),
                                    contextTelegram.getTelegramBot().getBotUsername(),
                                    "Зафиксировал",
                                    null,
                                    null
                            ));
                        }
                    }
                    promise.skipAllStep("--");
                })
                .thenWithResource("select", JdbcResource.class, (_, _, promise, jdbcResource) -> {
                    Context context = promise.getRepositoryMapClass(Context.class);
                    List<Map<String, Object>> execute = jdbcResource.execute(new JdbcRequest(JTOviSubscriber.SELECT_NOT_REMOVE));
                    execute.forEach(map -> context.getListIdChat().add(Long.parseLong(map.get("id_chat").toString())));
                })
                .then("handle", (_, _, promise) -> {
                    Context context = promise.getRepositoryMapClass(Context.class);

                    context.getListIdChat().clear();
                    context.getListIdChat().add(290029195L); // Ura
                    context.getListIdChat().add(241022301L); // Igor
                    context.getListIdChat().add(294097034L); // Alex
                    context.getListIdChat().add(-4739098379L); // Group
                    //Util.logConsoleJson(getClass(), context);

                    List<TelegramNotification> listTelegramNotification = new ArrayList<>();

                    TelegramCommandContext contextTelegram = promise.getRepositoryMapClass(TelegramCommandContext.class);
                    CreateMessage session = mapSession.computeIfAbsent(contextTelegram.getIdChat(), aLong -> new CreateMessage(
                            contextTelegram.getIdChat(),
                            contextTelegram.getTelegramBot().getBotUsername()
                    ));
                    TelegramNotification telegramNotification = session.get();
                    String botName = App.get(TelegramBotManager.class).getOviBotProperty().getName();

                    context.getListIdChat().forEach(idChat -> {
                        TelegramNotification n = new TelegramNotification(
                                idChat,
                                botName,
                                telegramNotification.getMessage(),
                                telegramNotification.getButtons(),
                                telegramNotification.getPathImage()
                        );
                        n.setIdImage(telegramNotification.getIdImage());
                        n.setIdVideo(telegramNotification.getIdVideo());
                        listTelegramNotification.add(n);
                    });
                    RegisterNotification.add(listTelegramNotification);
                })
                .extension(NhlStatisticApplication::addOnError);
        return gen;
    }

}
