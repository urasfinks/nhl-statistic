package ru.jamsys.telegram;

import lombok.Getter;
import lombok.Setter;
import ru.jamsys.core.App;
import ru.jamsys.core.component.ServiceProperty;

@Getter
@Setter
public class BotProperty {

    private final String name;

    private final String securityAlias;

    public BotProperty(String name, String securityAlias) {
        this.name = name;
        this.securityAlias = securityAlias;
    }

    public static BotProperty getInstance(String propertyClass) {
        ServiceProperty serviceProperty = App.get(ServiceProperty.class);
        return new BotProperty(
                serviceProperty.get(propertyClass + ".name"),
                serviceProperty.get(propertyClass + ".security.alias")
        );
    }

}
