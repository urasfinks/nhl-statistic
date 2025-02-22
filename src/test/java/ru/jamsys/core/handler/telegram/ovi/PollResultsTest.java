package ru.jamsys.core.handler.telegram.ovi;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import ru.jamsys.core.extension.builder.ArrayListBuilder;
import ru.jamsys.core.extension.builder.HashMapBuilder;

import java.util.Map;

class PollResultsTest {

    @Test
    void getStat() {
        Assertions.assertEquals("""
                Статистика голосования:
                
                Да 🔥 – 0,00%
                Нет ⛔ – 0,00%""", PollResults.getStat(new ArrayListBuilder<Map<String, Object>>()
                .append(new HashMapBuilder<String, Object>()
                        .append("true", null)
                        .append("false", null)
                ), ""));

        Assertions.assertEquals("""
                        Статистика голосования:
                        
                        Да 🔥 – 0,00%
                        Нет ⛔ – 0,00%""",
                PollResults.getStat(new ArrayListBuilder<Map<String, Object>>()
                                .append(new HashMapBuilder<String, Object>().append("unit", null))
                                .append(new HashMapBuilder<String, Object>().append("unit", null))
                        , ""));

        Assertions.assertEquals("""
                        Статистика голосования:
                        
                        Да 🔥 – 0,00%
                        Нет ⛔ – 0,00%""",
                PollResults.getStat(new ArrayListBuilder<Map<String, Object>>()
                                .append(new HashMapBuilder<String, Object>().append("unit", 1))
                                .append(new HashMapBuilder<String, Object>().append("unit", null))
                        , ""));

        Assertions.assertEquals("""
                        Статистика голосования:
                        
                        Да 🔥 – 0,00%
                        Нет ⛔ – 0,00%""",
                PollResults.getStat(new ArrayListBuilder<Map<String, Object>>()
                                .append(new HashMapBuilder<String, Object>().append("unit", "true"))
                                .append(new HashMapBuilder<String, Object>().append("unit", "false"))
                        , ""));

        Assertions.assertEquals("""
                        Статистика голосования:
                        
                        Да 🔥 – 0,00%
                        Нет ⛔ – 0,00%""",
                PollResults.getStat(new ArrayListBuilder<Map<String, Object>>()
                                .append(new HashMapBuilder<String, Object>().append("unit", "true"))

                        , ""));

        Assertions.assertEquals("""
                        Статистика голосования:
                        
                        Да 🔥 – 0,00%
                        Нет ⛔ – 0,00%""",
                PollResults.getStat(new ArrayListBuilder<Map<String, Object>>()
                                .append(new HashMapBuilder<String, Object>().append("unit", "false"))

                        , ""));

        Assertions.assertEquals("""
                        Статистика голосования:
                        
                        Да 🔥 – 0,00%
                        Нет ⛔ – 0,00%""",
                PollResults.getStat(new ArrayListBuilder<Map<String, Object>>()
                                .append(new HashMapBuilder<String, Object>().append("unit", "true").append("count", 0))
                                .append(new HashMapBuilder<String, Object>().append("unit", "false").append("count", 0))
                        , ""));

        Assertions.assertEquals("""
                        Статистика голосования:
                        
                        Да 🔥 – 100,00%
                        Нет ⛔ – 0,00%""",
                PollResults.getStat(new ArrayListBuilder<Map<String, Object>>()
                                .append(new HashMapBuilder<String, Object>().append("unit", "true").append("count", 1))
                                .append(new HashMapBuilder<String, Object>().append("unit", "false").append("count", 0))
                        , ""));

        Assertions.assertEquals("""
                        Статистика голосования:
                        
                        Да 🔥 – 50,00%
                        Нет ⛔ – 50,00%""",
                PollResults.getStat(new ArrayListBuilder<Map<String, Object>>()
                                .append(new HashMapBuilder<String, Object>().append("unit", "true").append("count", 1))
                                .append(new HashMapBuilder<String, Object>().append("unit", "false").append("count", 1))
                        , ""));

        Assertions.assertEquals("""
                        Статистика голосования:
                        
                        Да 🔥 – 66,67%
                        Нет ⛔ – 33,33%""",
                PollResults.getStat(new ArrayListBuilder<Map<String, Object>>()
                                .append(new HashMapBuilder<String, Object>().append("unit", "true").append("count", 10))
                                .append(new HashMapBuilder<String, Object>().append("unit", "false").append("count", 5))
                        , ""));
    }
}