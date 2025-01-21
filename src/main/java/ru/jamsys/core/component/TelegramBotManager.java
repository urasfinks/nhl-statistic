package ru.jamsys.core.component;

import lombok.Getter;
import lombok.Setter;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands;
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;
import org.telegram.telegrambots.meta.api.objects.commands.scope.BotCommandScopeDefault;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import ru.jamsys.NhlStatisticApplication;
import ru.jamsys.core.App;
import ru.jamsys.core.component.manager.item.RouteGeneratorRepository;
import ru.jamsys.core.extension.LifeCycleComponent;
import ru.jamsys.core.extension.builder.ArrayListBuilder;
import ru.jamsys.core.extension.functional.SupplierThrowing;
import ru.jamsys.core.flat.util.UtilFile;
import ru.jamsys.core.flat.util.UtilFileResource;
import ru.jamsys.core.flat.util.UtilTelegram;
import ru.jamsys.telegram.*;
import ru.jamsys.telegram.handler.NhlStatisticsBotCommandHandler;
import ru.jamsys.telegram.handler.OviGoalsBotCommandHandler;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@SuppressWarnings("unused")
@Component
@Getter
@Setter
@Lazy
public class TelegramBotManager implements LifeCycleComponent {

    private final BotProperty commonBotProperty = BotProperty.getInstance("telegram.bot.common");

    private final BotProperty oviBotProperty = BotProperty.getInstance("telegram.bot.ovi");

    private final Map<String, TelegramSender> repository = new HashMap<>(); //key - name bot; value - sender

    public enum Type {
        HTTP_SENDER,
        LIB_SENDER
    }

    public List<String> getListBotName() {
        return new ArrayListBuilder<String>()
                .append(getCommonBotProperty().getName())
                .append(getOviBotProperty().getName());
    }

    @Override
    public void run() {
        if (NhlStatisticApplication.startTelegramListener) {
            try {
                init(
                        Type.LIB_SENDER,
                        getCommonBotProperty(),
                        App.get(RouteGenerator.class).getRouterRepository(NhlStatisticsBotCommandHandler.class)
                )
                        .setMyCommands(
                                new SetMyCommands(new ArrayListBuilder<BotCommand>()
                                        .append(new BotCommand("/subscribe", "Подписаться на игрока"))
                                        .append(new BotCommand("/schedule", "Расписание"))
                                        .append(new BotCommand("/remove", "Удалить подписку")),
                                        new BotCommandScopeDefault(), null));
                init(
                        Type.LIB_SENDER,
                        getOviBotProperty(),
                        App.get(RouteGenerator.class).getRouterRepository(OviGoalsBotCommandHandler.class)
                ).setMyCommands(
                        new SetMyCommands(new ArrayListBuilder<BotCommand>()
                                .append(new BotCommand("/start", "Включить уведомления"))
                                .append(new BotCommand("/stats", "Текущая статистика: количество голов, оставшихся до рекорда, и статистика по сезону"))
                                .append(new BotCommand("/poll_results", "Статистика голосования"))
                                .append(new BotCommand("/schedule", "Ближайшие игры Александра Овечкина и команды Washington Capitals"))
                                .append(new BotCommand("/stop", "Отключить уведомления")),
                                new BotCommandScopeDefault(), null));

                init(Type.HTTP_SENDER, getCommonBotProperty());
                init(Type.HTTP_SENDER, getOviBotProperty());

            } catch (TelegramApiException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public TelegramSender get(String botName, Type type) {
        return repository.get(botName + "." + type.toString());
    }

    public TelegramSender init(String botName, Type type, SupplierThrowing<TelegramSender> getter) {
        String key = botName + "." + type.toString();
        if (!repository.containsKey(key) && getter == null) {
            return null;
        }
        return repository.computeIfAbsent(key, _ -> {
            try {
                return getter.get();
            } catch (Throwable e) {
                throw new RuntimeException(e);
            }
        });
    }

    @SuppressWarnings("all")
    public static TelegramSender init(Type type, BotProperty botProperty) {
        return init(type, botProperty, null);
    }

    public static TelegramSender init(Type type, BotProperty botProperty, RouteGeneratorRepository routerRepository) {
        ServiceProperty serviceProperty = App.get(ServiceProperty.class);
        String botName = serviceProperty.get(botProperty + ".name");
        return App
                .get(TelegramBotManager.class)
                .init(botName, type, () -> switch (type) {
                    case HTTP_SENDER -> new TelegramBotHttpSender(botProperty);
                    case LIB_SENDER -> TelegramBotLibSender.getInstance(
                            botProperty,
                            routerRepository
                    );
                });
    }

    public UtilTelegram.Result send(NotificationObject notificationObject, Type type) {
        UtilTelegram.Result telegramResult = new UtilTelegram.Result();
        TelegramSender telegramSender = get(notificationObject.getBot(), type);
        if (telegramSender == null) {
            return telegramResult
                    .setException(UtilTelegram.ResultException.SENDER_NULL)
                    .setCause("TelegramSender is null");
        }
        if (
                notificationObject.getPathImage() == null
                        || notificationObject.getPathImage().isEmpty()
        ) {
            return telegramSender.send(
                    notificationObject.getIdChat(),
                    notificationObject.getMessage(),
                    notificationObject.getButtons()
            );
        } else {
            try { // Потому что UtilFileResource.get throw exception
                return telegramSender.sendImage(
                        notificationObject.getIdChat(),
                        UtilFileResource.get(
                                notificationObject.getPathImage(),
                                UtilFileResource.Direction.PROJECT
                        ),
                        UtilFile.getFileName(notificationObject.getPathImage()),
                        notificationObject.getMessage()
                );
            } catch (Throwable th) {
                telegramResult
                        .setException(UtilTelegram.ResultException.OTHER)
                        .setCause(th.getMessage());
                App.error(th);
            }
        }
        return telegramResult;
    }

    @Override
    public int getInitializationIndex() {
        return 900;
    }

    @Override
    public void shutdown() {

    }

}
