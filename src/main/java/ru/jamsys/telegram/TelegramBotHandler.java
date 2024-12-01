package ru.jamsys.telegram;

import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;
import org.telegram.telegrambots.meta.api.objects.commands.scope.BotCommandScopeDefault;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import ru.jamsys.core.App;
import ru.jamsys.core.component.TelegramBotComponent;
import ru.jamsys.core.flat.util.UtilJson;
import ru.jamsys.telegram.command.TelegramContext;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class TelegramBotHandler extends TelegramLongPollingBot {

    public TelegramBotHandler(String botToken) throws TelegramApiException {
        super(botToken);
        List<BotCommand> list = new ArrayList<>();
        list.add(new BotCommand("/subscribe_to_player", "Follow a player to stay updated"));
        list.add(new BotCommand("/my_subscriptions", "See the list of players you're following"));
        list.add(new BotCommand("/remove_subscription", "Unfollow a player from your list"));
        this.execute(new SetMyCommands(list, new BotCommandScopeDefault(), null));
    }

    @Override
    public void onUpdateReceived(Update msg) {
        try {
            System.out.println(UtilJson.toStringPretty(msg, "{}"));
            if (msg != null) {
                if (msg.hasCallbackQuery()) {
                    AnswerCallbackQuery answerCallbackQuery = new AnswerCallbackQuery();
                    answerCallbackQuery.setCallbackQueryId(msg.getCallbackQuery().getId());
                    answerCallbackQuery.setText("");
                    execute(answerCallbackQuery);

                    handler(msg.getCallbackQuery().getMessage().getChatId(), msg);

                } else if (msg.hasMessage()) {
                    handler(msg.getMessage().getChatId(), msg);
                }
            }
        } catch (Throwable e) {
            App.error(e);
        }
    }

    @SuppressWarnings("unused")
    public void removeMessage(Update msg) {
        // Удаляем контекстное меню, что ранее нарисовали
        DeleteMessage editMessage = new DeleteMessage();
        editMessage.setChatId(getIdChat(msg));
        editMessage.setMessageId(getIdMessage(msg));
        try {
            execute(editMessage);
        } catch (Throwable th) {
            App.error(th);
        }
    }

    @SuppressWarnings("unused")
    public void changeMessage(Update msg, String data) {
        // Удаляем контекстное меню, что ранее нарисовали
        EditMessageText editMessage = new EditMessageText();
        editMessage.setChatId(getIdChat(msg));
        editMessage.setMessageId(getIdMessage(msg));
        editMessage.setText(data);
        try {
            execute(editMessage);
        } catch (Throwable th) {
            App.error(th);
        }
    }

    @SuppressWarnings("unused")
    public Integer getIdMessage(Update msg) {
        if (msg.hasCallbackQuery()) {
            return msg.getCallbackQuery().getMessage().getMessageId();
        } else if (msg.hasMessage()) {
            return msg.getMessage().getMessageId();
        }
        return null;
    }

    @SuppressWarnings("unused")
    public Long getIdChat(Update msg) {
        if (msg.hasCallbackQuery()) {
            return msg.getCallbackQuery().getMessage().getChatId();
        } else if (msg.hasMessage()) {
            return msg.getMessage().getChatId();
        }
        return null;
    }

    @SuppressWarnings("unused")
    public String getData(Update msg) {
        if (msg.hasCallbackQuery()) {
            return msg.getCallbackQuery().getData();
        } else if (msg.hasMessage()) {
            return msg.getMessage().getText();
        }
        return null;
    }

    private void handler(long idChat, Update msg) throws Throwable {
        Map<Long, TelegramContext> map = App.get(TelegramBotComponent.class).getMap();
        if (map.containsKey(idChat)) { //Мы имеем контекст
            map.get(idChat).onNext(msg);
        } else if (msg.hasMessage() && msg.getMessage().getText().startsWith("/")) {
            TelegramCommand telegramCommand = TelegramCommand.valueOfCommand(msg.getMessage().getText());
            if (telegramCommand != null) {
                TelegramContext telegramContext = telegramCommand.getTelegramContext(idChat);
                map.put(idChat, telegramContext);
                telegramContext.start();
            } else {
                SendMessage message = new SendMessage();
                message.setChatId(idChat);
                message.setText("Command does not exist");
            }
        }
    }

    public void send(Long idChat, String data, List<Button> listButtons) throws TelegramApiException {
        SendMessage message = new SendMessage();
        message.setChatId(idChat);
        message.setText(data);
        if (listButtons != null && !listButtons.isEmpty()) {
            addMessageButton(message, listButtons);
        }
        execute(message);
    }


    public static void addMessageButton(SendMessage message, List<Button> listButtons) {
        List<List<InlineKeyboardButton>> list = new ArrayList<>();
        listButtons.forEach(button -> {
            InlineKeyboardButton markupInline = new InlineKeyboardButton(button.getData());
            markupInline.setCallbackData(button.getCallback());
            list.add(List.of(markupInline));
        });
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        markup.setKeyboard(list);
        message.setReplyMarkup(markup);
    }

    @Override
    public String getBotUsername() {
        return "ovi_goals_bot";
    }

}
