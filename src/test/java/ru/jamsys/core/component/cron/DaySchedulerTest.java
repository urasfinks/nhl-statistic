package ru.jamsys.core.component.cron;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import ru.jamsys.core.flat.template.cron.Cron;
import ru.jamsys.core.flat.util.UtilDate;

class DaySchedulerTest {

    @Test
    void getCronTemplate() {
        long curTime = 1709734264056L; //2024-03-06T17:11:04.056

        String cronTemplate = new DayScheduler().getCronTemplate();
        Assertions.assertEquals("Template({Second=[0], Minute=[0], HourOfDay=[12], DayOfMonth=[], Month=[], DayOfWeek=[]})", new Cron(cronTemplate).toString());
        Assertions.assertEquals("2024-03-07T12:00:00.000", UtilDate.msFormat(new Cron(cronTemplate).compile(curTime).getNextTimestamp()));
    }

}