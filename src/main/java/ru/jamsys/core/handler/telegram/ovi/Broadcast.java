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
import ru.jamsys.core.jt.JTBroadcastTemplate;
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
            List<Button> buttonList = null;
            if (map.containsKey("btn") && map.get("btn") != null && !map.get("btn").isEmpty()) {
                String btn = map.get("btn");
                String btnTitle = btn.substring(0, btn.indexOf(";")).trim();
                String btnUrl = btn.substring(btn.indexOf(";") + 1).trim();
                Button button = new Button(btnTitle);
                button.setUrl(btnUrl);
                buttonList = new ArrayList<>();
                buttonList.add(button);
            }
            TelegramNotification telegramNotification = new TelegramNotification(
                    idChat,
                    bot,
                    map.get("text"),
                    buttonList,
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
                    List<Long> permission = new ArrayList<>();
                    permission.add(241022301L); //Igor
                    //permission.add(290029195L); //Ura
                    if (!permission.contains(contextTelegram.getIdChat())) {
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
                                                "Кнопка",
                                                ServletResponseWriter.buildUrlQuery(
                                                        "/bt/",
                                                        new HashMapBuilder<String, String>().append("setup", "btn")
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
                                        .append(new Button(
                                                "Сохранить в блок перед матчем",
                                                ServletResponseWriter.buildUrlQuery(
                                                        "/bt/",
                                                        new HashMapBuilder<String, String>().append("setup", "inviteGame")
                                                )
                                        ))
                                        .append(new Button(
                                                "Сохранить в результат голосования",
                                                ServletResponseWriter.buildUrlQuery(
                                                        "/bt/",
                                                        new HashMapBuilder<String, String>().append("setup", "voteResult")
                                                )
                                        ))
                                        .append(new Button(
                                                "Сохранить в прогноз",
                                                ServletResponseWriter.buildUrlQuery(
                                                        "/bt/",
                                                        new HashMapBuilder<String, String>().append("setup", "prediction")
                                                )
                                        ))
                                        .append(new Button(
                                                "Сохранить в ставки",
                                                ServletResponseWriter.buildUrlQuery(
                                                        "/bt/",
                                                        new HashMapBuilder<String, String>().append("setup", "bets")
                                                )
                                        ))
                                ,
                                null
                        ));
                    } else if (contextTelegram.getUriParameters().containsKey("setup")) {
                        String setup = contextTelegram.getUriParameters().get("setup");
                        switch (setup) {
                            case "inviteGame", "voteResult", "prediction", "bets" -> {
                                promise.goTo("saveBroadcastTemplate");
                                return;
                            }
                            case "all" -> {
                                context.setAll(true);
                                // return что бы не скипалась рассылка
                                return;
                            }
                            case "test" -> {
                                context.setAll(false);
                                // return что бы не скипалась рассылка
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
                                String typed = switch (setup) {
                                    case "text" -> "Вводи:";
                                    case "btn" -> "Наименование; https://....";
                                    default -> "Грузи";
                                };
                                RegisterNotification.add(new TelegramNotification(
                                        contextTelegram.getIdChat(),
                                        contextTelegram.getTelegramBot().getBotUsername(),
                                        typed,
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
                .then("preHandle", (_, _, promise) -> {
                    Context context = promise.getRepositoryMapClass(Context.class);
                    TelegramCommandContext contextTelegram = promise.getRepositoryMapClass(TelegramCommandContext.class);
                    if (context.isAll()) {
                        //context.getListIdChat().clear();
                    } else {
                        context.getListIdChat().clear();
                        context.getListIdChat().add(290029195L); // Ura
                        context.getListIdChat().add(241022301L); // Igor
                        context.getListIdChat().add(294097034L); // Alex
                        context.getListIdChat().add(-4739098379L); // Group
                    }
                    if (!contextTelegram.getUriParameters().containsKey("yes")) {
                        RegisterNotification.add(new TelegramNotification(
                                contextTelegram.getIdChat(),
                                contextTelegram.getTelegramBot().getBotUsername(),
                                "Точно разослать уведомления?........................................",
                                new ArrayListBuilder<Button>()
                                        .append(new Button(
                                                "Да, разослать " + context.getListIdChat().size() + " уведомлений!",
                                                ServletResponseWriter.buildUrlQuery(
                                                        "/bt/",
                                                        new HashMapBuilder<>(contextTelegram.getUriParameters())
                                                                .append("yes", "true")
                                                )
                                        )),
                                null
                        ));
                        promise.skipAllStep("--");
                    }
                })
                .then("handle", (_, _, promise) -> {
                    Context context = promise.getRepositoryMapClass(Context.class);
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
                    // Что бы туда дойти, надо выбрать сохранение promise.goTo("saveBroadcastTemplate");
                    promise.skipAllStep("skip saveBroadcastTemplate");
                })
                .thenWithResource("saveBroadcastTemplate", JdbcResource.class, (_, _, promise, jdbcResource) -> {
                    TelegramCommandContext contextTelegram = promise.getRepositoryMapClass(TelegramCommandContext.class);
                    String setup = contextTelegram.getUriParameters().get("setup");
                    List<String> available = new ArrayListBuilder<String>()
                            .append("inviteGame")
                            .append("voteResult")
                            .append("prediction")
                            .append("bets");
                    if (!available.contains(setup)) {
                        return;
                    }
                    CreateMessage session = mapSession.computeIfAbsent(contextTelegram.getIdChat(), aLong -> new CreateMessage(
                            contextTelegram.getIdChat(),
                            contextTelegram.getTelegramBot().getBotUsername()
                    ));
                    TelegramNotification telegramNotification = session.get();
                    List<Map<String, Object>> execute = jdbcResource.execute(new JdbcRequest(JTBroadcastTemplate.UPDATE)
                            .addArg("key", setup)
                            .addArg("telegram_notification", UtilJson.toStringPretty(new HashMapBuilder<String, Object>()
                                            .append("message", telegramNotification.getMessage())
                                            .append("buttons", telegramNotification.getButtons())
                                            .append("idImage", telegramNotification.getIdImage())
                                            .append("idVideo", telegramNotification.getIdVideo())
                                    ,
                                    "{}"))
                    );
                    RegisterNotification.add(new TelegramNotification(
                            contextTelegram.getIdChat(),
                            contextTelegram.getTelegramBot().getBotUsername(),
                            "Сохранено в: " + setup,
                            null,
                            null
                    ));
                })
                .extension(NhlStatisticApplication::addOnError);
        return gen;
    }

}
