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
import ru.jamsys.core.flat.util.UtilTelegramResponse;
import ru.jamsys.telegram.*;
import ru.jamsys.telegram.handler.EyeBotCommandHandler;
import ru.jamsys.telegram.handler.MotherBotCommandHandler;
import ru.jamsys.telegram.handler.NhlStatisticsBotCommandHandler;
import ru.jamsys.telegram.handler.OviGoalsBotCommandHandler;

import java.io.FileInputStream;
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

    private final BotProperty motherBotProperty = BotProperty.getInstance("telegram.bot.mother");

    private final BotProperty eyeBotProperty = BotProperty.getInstance("telegram.bot.eye");

    private final Map<String, TelegramSender> repository = new HashMap<>(); //key - name bot; value - sender

    public enum TypeSender {
        HTTP,
        EMBEDDED
    }

    public List<String> getListBotName() {
        return new ArrayListBuilder<String>()
                .append(getCommonBotProperty().getName())
                .append(getOviBotProperty().getName())
                .append(getMotherBotProperty().getName())
                .append(getEyeBotProperty().getName())
                ;
    }

    @Override
    public void run() {
        if (NhlStatisticApplication.startTelegramListener) {
            try {
                init(
                        TypeSender.EMBEDDED,
                        getCommonBotProperty(),
                        App.get(RouteGenerator.class).getRouterRepository(NhlStatisticsBotCommandHandler.class)
                )
                        .setMyCommands(new SetMyCommands(new ArrayListBuilder<BotCommand>()
                                .append(new BotCommand("/subscribe", "Подписаться на игрока"))
                                .append(new BotCommand("/schedule", "Расписание"))
                                .append(new BotCommand("/remove", "Удалить подписку")),
                                new BotCommandScopeDefault(),
                                null
                        ));
                init(
                        TypeSender.EMBEDDED,
                        getOviBotProperty(),
                        App.get(RouteGenerator.class).getRouterRepository(OviGoalsBotCommandHandler.class)
                )
                        .setMyCommands(new SetMyCommands(new ArrayListBuilder<BotCommand>()
//                                .append(new BotCommand("/start", "Включить уведомления"))
//                                .append(new BotCommand("/stats", "Текущая статистика: количество голов, оставшихся до рекорда, и статистика по сезону"))
//                                .append(new BotCommand("/poll_results", "Статистика голосования"))
//                                .append(new BotCommand("/schedule", "Ближайшие игры Александра Овечкина и команды Washington Capitals"))
//                                .append(new BotCommand("/prediction", "Когда Овечкин побьет рекорд Гретцки?"))
//                                .append(new BotCommand("/quiz", "Насколько хорошо ты знаешь Александра Овечкина?"))
//                                .append(new BotCommand("/bets", "Ставки на Овечкина"))
//                                .append(new BotCommand("/stop", "Отключить уведомления"))
                                .append(new BotCommand("/all", "Посмотреть все команды"))
                                .append(new BotCommand("/stats", "Статистика Овечкина"))
                                .append(new BotCommand("/schedule", "Расписание матчей"))
                                .append(new BotCommand("/prediction", "Прогноз даты рекорда"))
                                .append(new BotCommand("/quiz", "Квиз"))
                                .append(new BotCommand("/bets", "Ставки на Овечкина"))

                                ,
                                new BotCommandScopeDefault(),
                                null
                        ));

                init(
                        TypeSender.EMBEDDED,
                        getMotherBotProperty(),
                        App.get(RouteGenerator.class).getRouterRepository(MotherBotCommandHandler.class)
                )
                        .setNotCommandPrefix("/ask_question/?question=")
                        .setMyCommands(new SetMyCommands(new ArrayListBuilder<BotCommand>()
                                .append(new BotCommand("/feedback", "Обратная связь"))
                                //.append(new BotCommand("/pay", "Купить подписку"))
                                ,
                                new BotCommandScopeDefault(),
                                null
                        ));

                init(
                        TypeSender.EMBEDDED,
                        getEyeBotProperty(),
                        App.get(RouteGenerator.class).getRouterRepository(EyeBotCommandHandler.class)
                )
                        .setMyCommands(new SetMyCommands(new ArrayListBuilder<BotCommand>()
                                .append(new BotCommand("/perform_spread", "Выполнить расклад"))
                                //.append(new BotCommand("/pay", "Купить подписку"))
                                ,
                                new BotCommandScopeDefault(),
                                null
                        ));

                init(TypeSender.HTTP, getCommonBotProperty());
                init(TypeSender.HTTP, getOviBotProperty());
                init(TypeSender.HTTP, getMotherBotProperty());
                init(TypeSender.HTTP, getEyeBotProperty());

            } catch (TelegramApiException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public TelegramSender get(String botName, TypeSender typeSender) {
        return repository.get(botName + "." + typeSender.toString());
    }

    public TelegramSender init(String botName, TypeSender typeSender, SupplierThrowing<TelegramSender> getter) {
        String key = botName + "." + typeSender.toString();
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
    public static TelegramSender init(TypeSender typeSender, BotProperty botProperty) {
        return init(typeSender, botProperty, null);
    }

    public static TelegramSender init(TypeSender typeSender, BotProperty botProperty, RouteGeneratorRepository routerRepository) {
        return App
                .get(TelegramBotManager.class)
                .init(botProperty.getName(), typeSender, () -> switch (typeSender) {
                    case HTTP -> new TelegramBotHttpSender(botProperty);
                    case EMBEDDED -> TelegramBotEmbedded.getInstance(
                            botProperty,
                            routerRepository
                    );
                });
    }

    public UtilTelegramResponse.Result send(TelegramNotification telegramNotification, TypeSender typeSender) {
        long startTime = System.currentTimeMillis();
        UtilTelegramResponse.Result telegramResult = new UtilTelegramResponse.Result();
        TelegramSender telegramSender = get(telegramNotification.getBotName(), typeSender);
        if (telegramSender == null) {
            return telegramResult
                    .setException(UtilTelegramResponse.ResultException.SENDER_NULL)
                    .setCause("TelegramSender is null");
        }
        if (telegramNotification.getPathImage() != null && !telegramNotification.getPathImage().isEmpty()) {
            try { // Потому что UtilFileResource.get throw exception
                if (telegramNotification.getPathImage().startsWith("file:/")) {
                    return telegramSender.sendImage(
                            telegramNotification.getIdChat(),
                            new FileInputStream(telegramNotification.getPathImage().substring(6)),
                            UtilFile.getFileName(telegramNotification.getPathImage()),
                            telegramNotification.getMessage()
                    );
                } else {
                    return telegramSender.sendImage(
                            telegramNotification.getIdChat(),
                            UtilFileResource.get(
                                    telegramNotification.getPathImage(),
                                    UtilFileResource.Direction.PROJECT
                            ),
                            UtilFile.getFileName(telegramNotification.getPathImage()),
                            telegramNotification.getMessage()
                    );
                }
            } catch (Throwable th) {
                telegramResult
                        .setException(UtilTelegramResponse.ResultException.OTHER)
                        .setCause(th.getMessage());
                App.error(th);
            }
        } else if (telegramNotification.getIdImage() != null && !telegramNotification.getIdImage().isEmpty()) {
            return telegramSender.sendImage(
                    telegramNotification.getIdChat(),
                    telegramNotification.getIdImage(),
                    telegramNotification.getMessage()
            );
        } else if (telegramNotification.getIdVideo() != null && !telegramNotification.getIdVideo().isEmpty()) {
            return telegramSender.sendVideo(
                    telegramNotification.getIdChat(),
                    telegramNotification.getIdVideo(),
                    telegramNotification.getMessage()
            );
        } else {
            return telegramSender.send(
                    telegramNotification.getIdChat(),
                    telegramNotification.getMessage(),
                    telegramNotification.getButtons()
            );
        }
        telegramResult.setRequestTiming(System.currentTimeMillis() - startTime);
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
