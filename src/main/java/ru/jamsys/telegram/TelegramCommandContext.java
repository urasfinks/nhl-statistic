package ru.jamsys.telegram;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.util.List;
import java.util.Map;

@Setter
@Getter
@Accessors(chain = true)
@ToString
public class TelegramCommandContext {

    private long idChat;

    private String uriPath;

    private Map<String, List<String>> uriParameters;

    @ToString.Exclude
    @JsonIgnore
    private Update msg;

    @ToString.Exclude
    @JsonIgnore
    private Map<Long, String> stepHandler;

    @ToString.Exclude
    @JsonIgnore
    private TelegramBot telegramBot;

}
