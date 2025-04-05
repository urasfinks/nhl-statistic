package ru.jamsys.core.flat.util;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class OviStatisticMessageTest {
    @Test
    void xx(){
        Assertions.assertEquals("До нового рекорда: 1 гол", OviStatisticMessage.get(894));
        Assertions.assertEquals("Рекорд Грецки побит на 1 гол", OviStatisticMessage.get(895));
        Assertions.assertEquals("Рекорд Грецки побит на 2 гола", OviStatisticMessage.get(896));
        Assertions.assertEquals("Рекорд Грецки побит на 3 гола", OviStatisticMessage.get(897));
    }

}