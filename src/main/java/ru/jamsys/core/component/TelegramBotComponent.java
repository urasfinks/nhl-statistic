package ru.jamsys.core.component;

import lombok.Getter;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;
import ru.jamsys.NhlStatisticApplication;
import ru.jamsys.core.App;
import ru.jamsys.core.component.manager.ManagerExpiration;
import ru.jamsys.core.extension.LifeCycleComponent;
import ru.jamsys.core.flat.util.Util;
import ru.jamsys.telegram.AbstractBot;
import ru.jamsys.telegram.bot.NhlStatisticsBot;
import ru.jamsys.telegram.bot.OviGoalsBot;
import ru.jamsys.telegram.handler.NhlStatisticsBotCommandHandler;
import ru.jamsys.telegram.handler.OviGoalsBotCommandHandler;

import java.util.HashMap;
import java.util.Map;

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
    private final Map<String, AbstractBot> botRepository = new HashMap<>();

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
            ServiceProperty serviceProperty = App.get(ServiceProperty.class);
            String commonSecurityAlias = serviceProperty.get("telegram.bot.common.security.alias");
            String commonName = serviceProperty.get("telegram.bot.common.name");
            String oviSecurityAlias = serviceProperty.get("telegram.bot.ovi.security.alias");
            String oviName = serviceProperty.get("telegram.bot.ovi.name");

            Util.logConsole("commonName: "+commonName);
            Util.logConsole("commonSecurityAlias: "+commonSecurityAlias);

            Util.logConsole("oviName: "+oviName);
            Util.logConsole("oviSecurityAlias: "+oviSecurityAlias);
            try {
                nhlStatisticsBot = new NhlStatisticsBot(
                        commonName,
                        new String(securityComponent.get(commonSecurityAlias)),
                        routeGenerator.getRouterRepository(NhlStatisticsBotCommandHandler.class)
                );
                botRepository.put(nhlStatisticsBot.getBotUsername(), nhlStatisticsBot);
                api.registerBot(nhlStatisticsBot);
            } catch (TelegramApiException e) {
                throw new RuntimeException(e);
            }

            try {
                oviGoalsBot = new OviGoalsBot(
                        oviName,
                        new String(securityComponent.get(oviSecurityAlias)),
                        routeGenerator.getRouterRepository(OviGoalsBotCommandHandler.class)
                );
                botRepository.put(oviGoalsBot.getBotUsername(), oviGoalsBot);
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
