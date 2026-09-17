package skilift;

import org.junit.Before;
import org.junit.Test;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.TemporalAdjusters;
import java.util.NoSuchElementException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class TurnstileTest {

    private SkiPassRegistry registry;
    private Turnstile turnstile;
    private LocalDateTime monday10;
    private LocalDateTime thursday10;
    private LocalDateTime saturday10;

    @Before
    public void setUp() {
        registry = new SkiPassRegistry();
        turnstile = new Turnstile(registry);

        LocalDate mondayDate = LocalDate.of(2027, 1, 1).with(TemporalAdjusters.nextOrSame(DayOfWeek.MONDAY));
        LocalDate thursdayDate = mondayDate.plusDays(3);
        LocalDate saturdayDate = mondayDate.with(TemporalAdjusters.next(DayOfWeek.SATURDAY));
        monday10 = LocalDateTime.of(mondayDate, LocalTime.of(10, 0));
        thursday10 = LocalDateTime.of(thursdayDate, LocalTime.of(10, 0));
        saturday10 = LocalDateTime.of(saturdayDate, LocalTime.of(10, 0));
    }

    @Test
    public void registryIssuesActivePassWithCorrectRideLimit() {
        SkiPass pass = registry.issuePass(SkiPassType.WEEKDAY_RIDES_10, monday10);

        assertEquals(SkiPassStatus.ACTIVE, pass.getStatus());
        assertEquals(10, pass.getRemainingRides());
        assertTrue(pass.hasRidesLeft());
    }

    @Test
    public void registryBlockingUnknownIdThrows() {
        try {
            registry.blockPass("немає-такого-id");
            fail("Очікувався NoSuchElementException");
        } catch (NoSuchElementException expected) {
            // ок
        }
    }

    @Test
    public void allowsEntryWithValidWeekdayDayPass() {
        SkiPass pass = registry.issuePass(SkiPassType.WEEKDAY_ONE_DAY, monday10);

        AccessDecision decision = turnstile.checkAccess(new SimpleCardReader(pass), monday10);

        assertTrue(decision.isAllowed());
        assertEquals(1, turnstile.getTotalAllowed());
        assertEquals(0, turnstile.getTotalDenied());
    }

    @Test
    public void deniesBlockedCard() {
        SkiPass pass = registry.issuePass(SkiPassType.SEASON, monday10);
        registry.blockPass(pass.getId());

        AccessDecision decision = turnstile.checkAccess(new SimpleCardReader(pass), monday10);

        assertFalse(decision.isAllowed());
        assertEquals(DenialReason.BLOCKED, decision.getDenialReason());
    }

    @Test
    public void deniesExpiredCard() {
        SkiPass pass = registry.issuePass(SkiPassType.WEEKDAY_ONE_DAY, monday10);

        AccessDecision decision = turnstile.checkAccess(new SimpleCardReader(pass), monday10.plusDays(3));

        assertFalse(decision.isAllowed());
        assertEquals(DenialReason.EXPIRED, decision.getDenialReason());
    }

    @Test
    public void deniesWeekdayPassUsedOnWeekend() {
        // 5-денний робочий пропуск, виданий у четвер, охоплює чт-пт-сб-нд-пн,
        // тобто термін дії (до наступного понеділка) ще не сплив у суботу,
        // але субота - вихідний день, не дозволений для робочого пропуску.
        SkiPass pass = registry.issuePass(SkiPassType.WEEKDAY_FIVE_DAYS, thursday10);

        AccessDecision decision = turnstile.checkAccess(new SimpleCardReader(pass), saturday10);

        assertFalse(decision.isAllowed());
        assertEquals(DenialReason.OUT_OF_SCHEDULE, decision.getDenialReason());
    }

    @Test
    public void halfDayPassAllowedInsideWindowSameDay() {
        SkiPass pass = registry.issuePass(SkiPassType.WEEKDAY_HALF_MORNING, monday10);

        AccessDecision decision = turnstile.checkAccess(new SimpleCardReader(pass), monday10.withHour(11));

        assertTrue(decision.isAllowed());
    }

    @Test
    public void halfDayPassDeniedOutsideWindowSameDay() {
        // Видано о 7:00 (вікно 9:00-13:00, тому термін дії - до 13:00 того ж дня)
        SkiPass pass = registry.issuePass(SkiPassType.WEEKDAY_HALF_MORNING, monday10.withHour(7));

        // 8:00 того ж дня: ще не прострочено (до 13:00), але поза вікном 9:00-13:00
        AccessDecision decision = turnstile.checkAccess(new SimpleCardReader(pass), monday10.withHour(8));

        assertFalse(decision.isAllowed());
        assertEquals(DenialReason.OUT_OF_SCHEDULE, decision.getDenialReason());
    }

    @Test
    public void halfDayPassDeniedNextDay() {
        SkiPass pass = registry.issuePass(SkiPassType.WEEKDAY_HALF_MORNING, monday10);

        AccessDecision decision = turnstile.checkAccess(new SimpleCardReader(pass), monday10.plusDays(1).withHour(10));

        assertFalse(decision.isAllowed());
        // наступний робочий день все ще у межах терміну дії (validUntil = кінець дня видачі),
        // тож причина відмови саме "прострочено", а не розклад
        assertEquals(DenialReason.EXPIRED, decision.getDenialReason());
    }

    @Test
    public void rideLimitedPassConsumesCreditsAndDeniesWhenExhausted() {
        SkiPass pass = registry.issuePass(SkiPassType.WEEKDAY_RIDES_10, monday10);

        for (int i = 0; i < 10; i++) {
            AccessDecision decision = turnstile.checkAccess(new SimpleCardReader(pass), monday10.plusMinutes(i));
            assertTrue("Прохід #" + (i + 1) + " мав бути дозволений", decision.isAllowed());
        }
        assertEquals(0, pass.getRemainingRides());

        AccessDecision eleventh = turnstile.checkAccess(new SimpleCardReader(pass), monday10.plusMinutes(11));
        assertFalse(eleventh.isAllowed());
        assertEquals(DenialReason.NO_RIDES_LEFT, eleventh.getDenialReason());
    }

    @Test
    public void deniesOnReadError() {
        AccessDecision decision = turnstile.checkAccess(new FaultyCardReader(), monday10);

        assertFalse(decision.isAllowed());
        assertEquals(DenialReason.READ_ERROR, decision.getDenialReason());
        assertEquals(1, turnstile.getDeniedWithUnknownType());
    }

    @Test
    public void deniesUnknownCard() {
        AccessDecision decision = turnstile.checkAccess(new SimpleCardReader("не-в-реєстрі"), monday10);

        assertFalse(decision.isAllowed());
        assertEquals(DenialReason.UNKNOWN_CARD, decision.getDenialReason());
    }

    @Test
    public void seasonPassAllowedOnWeekend() {
        SkiPass pass = registry.issuePass(SkiPassType.SEASON, monday10);

        AccessDecision decision = turnstile.checkAccess(new SimpleCardReader(pass), saturday10);

        assertTrue(decision.isAllowed());
    }

    @Test
    public void statisticsAreTrackedTotalAndByType() {
        SkiPass ok = registry.issuePass(SkiPassType.WEEKDAY_ONE_DAY, monday10);
        SkiPass blocked = registry.issuePass(SkiPassType.SEASON, monday10);
        registry.blockPass(blocked.getId());

        turnstile.checkAccess(new SimpleCardReader(ok), monday10);
        turnstile.checkAccess(new SimpleCardReader(blocked), monday10);
        turnstile.checkAccess(new FaultyCardReader(), monday10);

        assertEquals(3, turnstile.getTotalAttempts());
        assertEquals(1, turnstile.getTotalAllowed());
        assertEquals(2, turnstile.getTotalDenied());
        assertEquals(Integer.valueOf(1), turnstile.getAllowedByType().get(SkiPassType.WEEKDAY_ONE_DAY));
        assertEquals(Integer.valueOf(1), turnstile.getDeniedByType().get(SkiPassType.SEASON));
        assertEquals(1, turnstile.getDeniedWithUnknownType());
    }
}
