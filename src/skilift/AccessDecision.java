package skilift;

import java.time.LocalDateTime;

/** Результат перевірки турнікетом однієї спроби проходу. */
public final class AccessDecision {

    private final boolean allowed;
    private final DenialReason denialReason; // null, якщо allowed == true
    private final SkiPassType type;          // null, якщо тип встановити не вдалося (помилка зчитування/невідома картка)
    private final LocalDateTime timestamp;

    private AccessDecision(boolean allowed, DenialReason denialReason, SkiPassType type, LocalDateTime timestamp) {
        this.allowed = allowed;
        this.denialReason = denialReason;
        this.type = type;
        this.timestamp = timestamp;
    }

    public static AccessDecision allowed(SkiPassType type, LocalDateTime timestamp) {
        return new AccessDecision(true, null, type, timestamp);
    }

    public static AccessDecision denied(DenialReason reason, SkiPassType type, LocalDateTime timestamp) {
        return new AccessDecision(false, reason, type, timestamp);
    }

    public boolean isAllowed() {
        return allowed;
    }

    public DenialReason getDenialReason() {
        return denialReason;
    }

    public SkiPassType getType() {
        return type;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    @Override
    public String toString() {
        if (allowed) {
            return "ДОЗВОЛЕНО (" + (type != null ? type.getDescription() : "?") + ")";
        }
        String typeInfo = type != null ? type.getDescription() : "тип невідомий";
        return "ВІДМОВЛЕНО [" + denialReason.getDescription() + "] (" + typeInfo + ")";
    }
}
