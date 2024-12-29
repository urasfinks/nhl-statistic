package ru.jamsys.core.component;

import lombok.Getter;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;
import ru.jamsys.NhlStatisticApplication;
import ru.jamsys.core.component.manager.ManagerExpiration;
import ru.jamsys.core.extension.LifeCycleComponent;
import ru.jamsys.telegram.bot.NhlStatisticsBot;
import ru.jamsys.telegram.bot.OviGoalsBot;
import ru.jamsys.telegram.handler.NhlStatisticsBotCommandHandler;
import ru.jamsys.telegram.handler.OviGoalsBotCommandHandler;

@SuppressWarnings("unused")
@Component
@Lazy
public class TelegramBotComponent implements LifeCycleComponent {

    private final TelegramBotsApi api;

    private final SecurityComponent securityComponent;

    private final RouteGenerator routeGenerator;

    @Getter
    private NhlStatisticsBot nhlStatisticsBot;

    @Getter
    private OviGoalsBot oviGoalsBot;

    public TelegramBotComponent(
            SecurityComponent securityComponent,
            ManagerExpiration managerExpiration,
            RouteGenerator routeGenerator
    ) throws TelegramApiException {
        this.securityComponent = securityComponent;
        api = new TelegramBotsApi(DefaultBotSession.class);
        this.routeGenerator = routeGenerator;
    }

    @Override
    public int getInitializationIndex() {
        return 900;
    }

    @Override
    public void run() {
        if (NhlStatisticApplication.startTelegramListener) {
            try {
                nhlStatisticsBot = new NhlStatisticsBot(
                        "nhl_statistics_bot",
                        new String(securityComponent.get("telegram.api.token.nhl_statistics_bot")),
                        routeGenerator.getRouterRepository(NhlStatisticsBotCommandHandler.class)
                );
                api.registerBot(nhlStatisticsBot);
            } catch (TelegramApiException e) {
                throw new RuntimeException(e);
            }

            try {
                oviGoalsBot = new OviGoalsBot(
                        "ovi_goals_bot",
                        new String(securityComponent.get("telegram.api.token.ovi_goals_bot")),
                        routeGenerator.getRouterRepository(OviGoalsBotCommandHandler.class)
                );
                api.registerBot(oviGoalsBot);
            } catch (TelegramApiException e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Override
    public void shutdown() {
    }

}
