package skilift;

import java.time.DayOfWeek;
import java.time.LocalDate;

/** Категорія днів, у які дійсний ski-pass. */
public enum DayCategory {
    WEEKDAY,
    WEEKEND,
    ANY; // сезонний абонемент дійсний у будь-який день

    public boolean matches(LocalDate date) {
        boolean isWeekend = date.getDayOfWeek() == DayOfWeek.SATURDAY
                || date.getDayOfWeek() == DayOfWeek.SUNDAY;
        switch (this) {
            case WEEKDAY:
                return !isWeekend;
            case WEEKEND:
                return isWeekend;
            case ANY:
            default:
                return true;
        }
    }
}
