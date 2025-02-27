package ru.jamsys.core.handler.telegram.ovi;

import lombok.Getter;
import lombok.Setter;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestMapping;
import ru.jamsys.core.component.ServicePromise;
import ru.jamsys.core.handler.promise.Vote;
import ru.jamsys.core.promise.PromiseGenerator;
import ru.jamsys.telegram.handler.OviGoalsBotCommandHandler;

@SuppressWarnings("unused")
@Setter
@Getter
@Component
@RequestMapping("/vote/**")
public class VoteOvi extends Vote implements PromiseGenerator, OviGoalsBotCommandHandler {

    public VoteOvi(ServicePromise servicePromise) {
        super(servicePromise, false);
    }

}
