package ru.jamsys.core.handler.telegram.ovi;

import lombok.Getter;
import lombok.Setter;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestMapping;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.webapp.WebAppInfo;
import ru.jamsys.NhlStatisticApplication;
import ru.jamsys.core.component.ServicePromise;
import ru.jamsys.core.promise.Promise;
import ru.jamsys.core.promise.PromiseGenerator;
import ru.jamsys.core.resource.jdbc.JdbcResource;
import ru.jamsys.telegram.TelegramCommandContext;
import ru.jamsys.telegram.handler.OviGoalsBotCommandHandler;

import java.util.List;

@SuppressWarnings("unused")
@Setter
@Getter
@Component
@RequestMapping("/quiz")
public class Quiz implements PromiseGenerator, OviGoalsBotCommandHandler {

    private final ServicePromise servicePromise;

    public Quiz(ServicePromise servicePromise) {
        this.servicePromise = servicePromise;
    }

    @Override
    public Promise generate() {
        return servicePromise.get(getClass().getSimpleName(), 12_000L)
                .extension(promise -> promise.setRepositoryMapClass(Quiz.class, this))
                .thenWithResource("quiz", JdbcResource.class, (_, _, promise, jdbcResource) -> {
                    TelegramCommandContext context = promise.getRepositoryMapClass(TelegramCommandContext.class);
                    SendMessage message = new SendMessage();
                    message.setChatId(context.getIdChat());
                    message.setText("""
                            Пройди квиз и узнай, насколько хорошо ты знаешь Александра Овечкина!
                            
                            ⚡️ 10 вопросов, 4 варианта ответов, только самые увлекательные факты и моменты из его биографии.\s
                            
                            Не забывай делиться результатами с друзьями! Уверены, что им тоже будет интересно.""");

                    InlineKeyboardButton webAppButton = new InlineKeyboardButton();
                    webAppButton.setText("Поехали 🚀");
                    webAppButton.setWebApp(new WebAppInfo("https://quiz.ovechkingoals.ru/?utm_source=bot_menu&mode=tg"));

                    InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
                    markup.setKeyboard(List.of(List.of(webAppButton)));
                    message.setReplyMarkup(markup);

                    context.getTelegramBot().send(message, context.getIdChat());
                })
                .extension(NhlStatisticApplication::addOnError);
    }

}
