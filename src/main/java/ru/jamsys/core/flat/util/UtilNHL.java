package ru.jamsys.core.flat.util;

import ru.jamsys.tank.data.NHLPlayerList;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.Month;
import java.time.ZoneId;
import java.util.Date;
import java.util.Locale;

public class UtilNHL {

    public static int getScoreGretzky() { // Кол-во голов у Gretzky
        return 894;
    }

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

    public static NHLPlayerList.Player getOvi() {
        return new NHLPlayerList.Player()
                .setPlayerID("3101")
                .setPos("LW")
                .setLongName("Alex Ovechkin")
                .setTeam("WSH")
                .setTeamID("31");
    }

    public static int getOviScoreLastSeason() {
        return 853;
    }

    public static boolean isOvi(String idPlayer) {
        return getOvi().getPlayerID().equals(idPlayer);
    }

    public static boolean isOviGame(String idGame) {
        String team = getOvi().getTeam();
        if (idGame == null) {
            return false;
        }
        if (idGame.endsWith("@" + team)) {
            return true;
        }
        return idGame.contains("_" + team + "@");
    }

    public static String formatDate(long timestampEpoch) throws ParseException {
        String format = "dd.MM.yyyy HH:mm";
        String date = UtilDate.timestampFormat(timestampEpoch, format);
        Date jud = new SimpleDateFormat(format).parse(date);
        return new SimpleDateFormat("dd MMMM 'в' HH:mm '(МСК)'", Locale.of("ru")).format(jud);
    }

}
