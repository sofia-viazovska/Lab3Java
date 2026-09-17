package skilift;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;

/**
 * Демонстраційний сценарій роботи турнікета лижного підйомника (завдання 3.3):
 * випуск карток різних типів через SkiPassRegistry, спроби проходу через
 * Turnstile у різних ситуаціях (дозвіл, вичерпані підйоми, блокування,
 * прострочення, невідповідний день/час, помилка зчитування, невідома картка),
 * і підсумкові звіти турнікета - сумарний та по типах ski-pass.
 */
public class Main {

    public static void main(String[] args) {
        SkiPassRegistry registry = new SkiPassRegistry();
        Turnstile turnstile = new Turnstile(registry);

        // Знаходимо конкретний робочий день (понеділок) і вихідний (субота) для сценаріїв,
        // не прив'язуючись до дати запуску програми.
        LocalDate mondayDate = LocalDate.of(2027, 1, 1).with(TemporalAdjusters.nextOrSame(DayOfWeek.MONDAY));
        LocalDate thursdayDate = mondayDate.plusDays(3);
        LocalDate saturdayDate = mondayDate.with(TemporalAdjusters.next(DayOfWeek.SATURDAY));
        LocalDateTime monday10 = LocalDateTime.of(mondayDate, java.time.LocalTime.of(10, 0));
        LocalDateTime thursday10 = LocalDateTime.of(thursdayDate, java.time.LocalTime.of(10, 0));
        LocalDateTime saturday10 = LocalDateTime.of(saturdayDate, java.time.LocalTime.of(10, 0));

        System.out.println("=== 1. Звичайний прохід за одноденним робочим пропуском ===");
        SkiPass oneDayPass = registry.issuePass(SkiPassType.WEEKDAY_ONE_DAY, monday10);
        System.out.println(oneDayPass);
        print(turnstile.checkAccess(new SimpleCardReader(oneDayPass), monday10));

        System.out.println("\n=== 2. Пропуск на 10 підйомів: витрачаємо всі й пробуємо ще раз ===");
        SkiPass rides10 = registry.issuePass(SkiPassType.WEEKDAY_RIDES_10, monday10);
        for (int i = 1; i <= 10; i++) {
            print(turnstile.checkAccess(new SimpleCardReader(rides10), monday10.plusMinutes(i)));
        }
        System.out.println("Спроба #11 (підйомів вже не залишилось):");
        print(turnstile.checkAccess(new SimpleCardReader(rides10), monday10.plusMinutes(11)));

        System.out.println("\n=== 3. Заблокована картка (порушення правил підйому) ===");
        SkiPass toBlock = registry.issuePass(SkiPassType.SEASON, monday10);
        registry.blockPass(toBlock.getId());
        print(turnstile.checkAccess(new SimpleCardReader(toBlock), monday10));

        System.out.println("\n=== 4. Прострочена картка (напівденний пропуск, спроба наступного дня) ===");
        SkiPass halfDay = registry.issuePass(SkiPassType.WEEKDAY_HALF_MORNING, monday10);
        print(turnstile.checkAccess(new SimpleCardReader(halfDay), monday10.plusDays(1)));

        System.out.println("\n=== 5. Робочий пропуск, використаний у вихідний день (в межах терміну дії) ===");
        // 5-денний робочий пропуск, виданий у четвер, охоплює чт-пт-сб-нд-пн:
        // у суботу термін дії ще не сплив, але субота - вихідний, не дозволений для робочого пропуску
        SkiPass weekdayPass = registry.issuePass(SkiPassType.WEEKDAY_FIVE_DAYS, thursday10);
        print(turnstile.checkAccess(new SimpleCardReader(weekdayPass), saturday10));

        System.out.println("\n=== 6. Не вдалося зчитати дані картки ===");
        print(turnstile.checkAccess(new FaultyCardReader(), monday10));

        System.out.println("\n=== 7. Невідома картка (немає в реєстрі системи) ===");
        print(turnstile.checkAccess(new SimpleCardReader("невідомий-id-12345"), monday10));

        System.out.println("\n=== 8. Сезонний абонемент діє в будь-який день ===");
        SkiPass season = registry.issuePass(SkiPassType.SEASON, monday10);
        print(turnstile.checkAccess(new SimpleCardReader(season), saturday10));

        System.out.println("\n\n--- Звіт турнікета ---");
        System.out.println("1) Сумарні дані: " + turnstile.summaryReport());
        System.out.println("2) Дані по типах ski-pass:");
        System.out.println(turnstile.byTypeReport());
    }

    private static void print(AccessDecision decision) {
        System.out.println(decision);
    }
}
