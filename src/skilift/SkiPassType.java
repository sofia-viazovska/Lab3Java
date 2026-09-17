package skilift;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * Усі типи ski-pass згідно з умовою завдання: за днями тижня (робочі/вихідні),
 * за способом обліку (без обліку кількості підйомів / за кількістю підйомів)
 * та сезонний абонемент.
 *
 * Припущення, не деталізовані в умові, тому зафіксовані тут явно:
 *  - "південний" (напівденний) пропуск діє лише в день його видачі,
 *    у межах свого часового вікна (9:00-13:00 або 13:00-17:00);
 *  - багатоденний пропуск (2 чи 5 днів) прив'язаний до категорії днів: якщо
 *    на день спроби проходу випадає день іншої категорії (наприклад вихідний
 *    посеред 5-денного робочого пропуску), прохід у цей день не дозволяється;
 *  - пропуски за кількістю підйомів діють до кінця сезону (SEASON_LENGTH_DAYS
 *    від дати видачі), обмежені лише категорією днів і кількістю підйомів, що залишились;
 *  - сезонний абонемент діє SEASON_LENGTH_DAYS від дати видачі, у будь-який день.
 */
public enum SkiPassType {

    WEEKDAY_HALF_MORNING("Робочий день, півдня (09:00-13:00)", DayCategory.WEEKDAY, HalfDayWindow.MORNING, 0, 0),
    WEEKDAY_HALF_AFTERNOON("Робочий день, півдня (13:00-17:00)", DayCategory.WEEKDAY, HalfDayWindow.AFTERNOON, 0, 0),
    WEEKDAY_ONE_DAY("Робочий день, 1 день", DayCategory.WEEKDAY, null, 1, 0),
    WEEKDAY_TWO_DAYS("Робочі дні, 2 дні", DayCategory.WEEKDAY, null, 2, 0),
    WEEKDAY_FIVE_DAYS("Робочі дні, 5 днів", DayCategory.WEEKDAY, null, 5, 0),
    WEEKDAY_RIDES_10("Робочі дні, 10 підйомів", DayCategory.WEEKDAY, null, 150, 10),
    WEEKDAY_RIDES_20("Робочі дні, 20 підйомів", DayCategory.WEEKDAY, null, 150, 20),
    WEEKDAY_RIDES_50("Робочі дні, 50 підйомів", DayCategory.WEEKDAY, null, 150, 50),
    WEEKDAY_RIDES_100("Робочі дні, 100 підйомів", DayCategory.WEEKDAY, null, 150, 100),

    WEEKEND_HALF_MORNING("Вихідний день, півдня (09:00-13:00)", DayCategory.WEEKEND, HalfDayWindow.MORNING, 0, 0),
    WEEKEND_HALF_AFTERNOON("Вихідний день, півдня (13:00-17:00)", DayCategory.WEEKEND, HalfDayWindow.AFTERNOON, 0, 0),
    WEEKEND_ONE_DAY("Вихідний день, 1 день", DayCategory.WEEKEND, null, 1, 0),
    WEEKEND_TWO_DAYS("Вихідні дні, 2 дні", DayCategory.WEEKEND, null, 2, 0),
    WEEKEND_RIDES_10("Вихідні дні, 10 підйомів", DayCategory.WEEKEND, null, 150, 10),
    WEEKEND_RIDES_20("Вихідні дні, 20 підйомів", DayCategory.WEEKEND, null, 150, 20),
    WEEKEND_RIDES_50("Вихідні дні, 50 підйомів", DayCategory.WEEKEND, null, 150, 50),
    WEEKEND_RIDES_100("Вихідні дні, 100 підйомів", DayCategory.WEEKEND, null, 150, 100),

    SEASON("Сезонний абонемент", DayCategory.ANY, null, 150, 0);

    /** Тривалість "сезону" в днях: використовується для пропусків без явного терміну (за підйомами) і сезонного абонемента. */
    public static final int SEASON_LENGTH_DAYS = 150;

    private final String description;
    private final DayCategory dayCategory;
    private final HalfDayWindow halfDayWindow; // null, якщо це не "південний" тип
    private final int validityDays;            // використовується для обчислення терміну дії
    private final int rideLimit;                // 0, якщо тип не обмежений кількістю підйомів

    SkiPassType(String description, DayCategory dayCategory, HalfDayWindow halfDayWindow,
                int validityDays, int rideLimit) {
        this.description = description;
        this.dayCategory = dayCategory;
        this.halfDayWindow = halfDayWindow;
        this.validityDays = validityDays;
        this.rideLimit = rideLimit;
    }

    public String getDescription() {
        return description;
    }

    public DayCategory getDayCategory() {
        return dayCategory;
    }

    public boolean isHalfDay() {
        return halfDayWindow != null;
    }

    public boolean isRideLimited() {
        return rideLimit > 0;
    }

    public int getRideLimit() {
        return rideLimit;
    }

    /** Обчислює дату/час завершення дії пропуску, виданого в момент issuedAt. */
    public LocalDateTime computeValidUntil(LocalDateTime issuedAt) {
        if (halfDayWindow != null) {
            return LocalDateTime.of(issuedAt.toLocalDate(), halfDayWindow.getEnd());
        }
        LocalDate lastValidDate = issuedAt.toLocalDate().plusDays(validityDays - 1L);
        return LocalDateTime.of(lastValidDate, LocalTime.of(23, 59, 59));
    }

    /**
     * Перевіряє, чи дозволений прохід саме зараз з огляду на категорію днів
     * та (для "південних" пропусків) часове вікно і день видачі.
     */
    public boolean isWithinSchedule(LocalDateTime issuedAt, LocalDateTime now) {
        if (!dayCategory.matches(now.toLocalDate())) {
            return false;
        }
        if (halfDayWindow != null) {
            return now.toLocalDate().equals(issuedAt.toLocalDate()) && halfDayWindow.contains(now.toLocalTime());
        }
        return true;
    }
}
