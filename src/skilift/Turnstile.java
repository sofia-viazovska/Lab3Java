package skilift;

import java.time.LocalDateTime;
import java.util.EnumMap;
import java.util.Map;

/**
 * Турнікет підйомника: зчитує картку, звіряється із системою реєстрації
 * (SkiPassRegistry) і веде облік дозволів/відмов проходу - сумарно
 * та розбитих по типах ski-pass.
 */
public class Turnstile {

    private final SkiPassRegistry registry;

    private int totalAllowed;
    private int totalDenied;
    private final Map<SkiPassType, Integer> allowedByType = new EnumMap<>(SkiPassType.class);
    private final Map<SkiPassType, Integer> deniedByType = new EnumMap<>(SkiPassType.class);
    private int deniedWithUnknownType; // помилки зчитування / невідомі картки - типу немає

    public Turnstile(SkiPassRegistry registry) {
        this.registry = registry;
    }

    /** Основна операція турнікета: спроба проходу з карткою, зчитаною через reader, у момент now. */
    public AccessDecision checkAccess(CardReader reader, LocalDateTime now) {
        String cardId;
        try {
            cardId = reader.readCardId();
        } catch (CardReadException e) {
            return recordDenied(DenialReason.READ_ERROR, null, now);
        }

        SkiPass pass = registry.findById(cardId);
        if (pass == null) {
            return recordDenied(DenialReason.UNKNOWN_CARD, null, now);
        }

        SkiPassType type = pass.getType();

        if (pass.isBlocked()) {
            return recordDenied(DenialReason.BLOCKED, type, now);
        }
        if (now.isAfter(pass.getValidUntil())) {
            return recordDenied(DenialReason.EXPIRED, type, now);
        }
        if (!type.isWithinSchedule(pass.getIssuedAt(), now)) {
            return recordDenied(DenialReason.OUT_OF_SCHEDULE, type, now);
        }
        if (!pass.hasRidesLeft()) {
            return recordDenied(DenialReason.NO_RIDES_LEFT, type, now);
        }

        pass.consumeRide();
        return recordAllowed(type, now);
    }

    private AccessDecision recordAllowed(SkiPassType type, LocalDateTime now) {
        totalAllowed++;
        allowedByType.merge(type, 1, Integer::sum);
        return AccessDecision.allowed(type, now);
    }

    private AccessDecision recordDenied(DenialReason reason, SkiPassType type, LocalDateTime now) {
        totalDenied++;
        if (type != null) {
            deniedByType.merge(type, 1, Integer::sum);
        } else {
            deniedWithUnknownType++;
        }
        return AccessDecision.denied(reason, type, now);
    }

    // ---- Облік дозволів/відмов проходу ----

    public int getTotalAllowed() {
        return totalAllowed;
    }

    public int getTotalDenied() {
        return totalDenied;
    }

    public int getTotalAttempts() {
        return totalAllowed + totalDenied;
    }

    public Map<SkiPassType, Integer> getAllowedByType() {
        return new EnumMap<>(allowedByType);
    }

    public Map<SkiPassType, Integer> getDeniedByType() {
        return new EnumMap<>(deniedByType);
    }

    public int getDeniedWithUnknownType() {
        return deniedWithUnknownType;
    }

    /** 1) Сумарні дані по проходах (як вимагає умова: "видавати по запиту сумарні дані"). */
    public String summaryReport() {
        return String.format(
                "Усього спроб: %d | Дозволено: %d | Відмовлено: %d",
                getTotalAttempts(), totalAllowed, totalDenied);
    }

    /** 2) Дані, розбиті по типах ski-pass (як вимагає умова: "дані розбиті по типах ski-pass"). */
    public String byTypeReport() {
        StringBuilder sb = new StringBuilder("Статистика по типах ski-pass:\n");
        for (SkiPassType type : SkiPassType.values()) {
            int allowed = allowedByType.getOrDefault(type, 0);
            int denied = deniedByType.getOrDefault(type, 0);
            if (allowed == 0 && denied == 0) {
                continue;
            }
            sb.append(String.format("  %-45s дозволено=%-4d відмовлено=%-4d%n",
                    type.getDescription(), allowed, denied));
        }
        if (deniedWithUnknownType > 0) {
            sb.append(String.format("  %-45s дозволено=%-4d відмовлено=%-4d%n",
                    "(невідома/непрочитана картка)", 0, deniedWithUnknownType));
        }
        return sb.toString();
    }
}
