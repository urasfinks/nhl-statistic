package ru.jamsys.core.handler.telegram.ovi;

import lombok.Getter;
import lombok.Setter;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestMapping;
import ru.jamsys.core.component.ServicePromise;
import ru.jamsys.core.handler.promise.Vote;
import ru.jamsys.core.promise.Promise;
import ru.jamsys.core.promise.PromiseGenerator;
import ru.jamsys.telegram.handler.OviGoalsBotCommandHandler;

@SuppressWarnings("unused")
@Setter
@Getter
@Component
@RequestMapping("/poll_results/**")
public class PollResults extends Vote implements PromiseGenerator, OviGoalsBotCommandHandler {

    public PollResults(ServicePromise servicePromise) {
        super(servicePromise);
    }

    @Override
    public Promise generate() {
        Promise promise = super.generate();
        promise
                .getRepositoryMapClass(Context.class)
                .setExtraText("Побьет ли Александр Овечкин рекорд Уэйна Гретцки в этом сезоне?\n\n");
        return promise;
    }

}
