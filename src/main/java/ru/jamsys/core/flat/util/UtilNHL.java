package ru.jamsys.core.flat.util;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.Month;
import java.time.ZoneId;
import java.util.Date;

public class UtilNHL {

    // Метод для определения сезона по дате
    public static Integer getActiveSeasonOrNext() {
        return getActiveSeasonOrNext(LocalDate.ofInstant(new Date().toInstant(), ZoneId.systemDefault()));
    }

    public static Integer getActiveSeasonOrNext(String date, String format) throws ParseException {
        DateFormat dateFormat = new SimpleDateFormat(format);
        Date input = dateFormat.parse(date);
        return getActiveSeasonOrNext(LocalDate.ofInstant(input.toInstant(), ZoneId.systemDefault()));
    }

    public static String seasonFormat(Integer season) {
        if (season == null || String.valueOf(season).length() < 4) {
            return "Сезон не определён";
        }
        int lastSeason = season - 1;
        return lastSeason + "/" + String.valueOf(season).substring(2, 4);
    }

    public static Integer getActiveSeasonOrNext(LocalDate date) {
        int year = date.getYear();
        if (date.getMonthValue() <= Month.APRIL.getValue()) {
            return year;
        } else if (date.getMonthValue() >= Month.OCTOBER.getValue()) {
            return year + 1;
        } else { // Потому что возможно ещё не сформировано расписание, нет смысла раньше времени
            return null;
        }
    }

    public static String getCurrentDateEpoch() {
        return UtilDate.timestampFormatUTC(UtilDate.getTimestamp(), "yyyyMMdd");
    }

}
