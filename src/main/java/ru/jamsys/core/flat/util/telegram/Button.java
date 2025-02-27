package ru.jamsys.core.flat.util.telegram;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;

@Getter
@Setter
@Accessors(chain = true)
@ToString
public class Button {

    private final String data;

    private String callback;

    private String url;

    private String webapp;

    public Button(String data) {
        this.data = data;
    }

    public Button(String data, String callback) {
        this.data = data;
        this.callback = callback;
    }

}