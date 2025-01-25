package ru.jamsys.core.handler.telegram.common;

import lombok.Getter;
import lombok.Setter;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestMapping;
import ru.jamsys.core.component.ServicePromise;
import ru.jamsys.core.handler.promise.Vote;
import ru.jamsys.core.promise.PromiseGenerator;
import ru.jamsys.telegram.handler.NhlStatisticsBotCommandHandler;

@SuppressWarnings("unused")
@Setter
@Getter
@Component
@RequestMapping("/vote/**")
public class VoteCommon extends Vote implements PromiseGenerator, NhlStatisticsBotCommandHandler {

    public VoteCommon(ServicePromise servicePromise) {
        super(servicePromise);
    }

}
