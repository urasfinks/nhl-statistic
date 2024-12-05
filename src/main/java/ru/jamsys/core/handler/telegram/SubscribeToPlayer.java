package ru.jamsys.core.handler.telegram;

import lombok.Getter;
import lombok.Setter;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestMapping;
import ru.jamsys.core.promise.Promise;
import ru.jamsys.core.promise.PromiseGenerator;
import ru.jamsys.telegram.TelegramHandler;

@Setter
@Getter
@Component
@RequestMapping("/subscribe_to_player")
public class SubscribeToPlayer implements PromiseGenerator, TelegramHandler {

    private String index;

    @Override
    public Promise generate() {
        return null;
    }

}
