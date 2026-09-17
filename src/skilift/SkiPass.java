package skilift;

import java.time.LocalDateTime;

/**
 * Дані, що зберігаються на самій картці ski-pass: унікальний ідентифікатор,
 * тип, термін дії, кількість підйомів, що залишились, та статус.
 *
 * Блокування (block) і списання поїздки (consumeRide) навмисно package-private:
 * ці дії виконує лише система (SkiPassRegistry) чи турнікет (Turnstile), а не
 * довільний код, що тримає посилання на картку.
 */
public class SkiPass {

    private final String id;
    private final SkiPassType type;
    private final LocalDateTime issuedAt;
    private final LocalDateTime validUntil;
    private int remainingRides; // -1, якщо тип не обмежений кількістю підйомів
    private SkiPassStatus status;

    public SkiPass(String id, SkiPassType type, LocalDateTime issuedAt) {
        this.id = id;
        this.type = type;
        this.issuedAt = issuedAt;
        this.validUntil = type.computeValidUntil(issuedAt);
        this.remainingRides = type.isRideLimited() ? type.getRideLimit() : -1;
        this.status = SkiPassStatus.ACTIVE;
    }

    public String getId() {
        return id;
    }

    public SkiPassType getType() {
        return type;
    }

    public LocalDateTime getIssuedAt() {
        return issuedAt;
    }

    public LocalDateTime getValidUntil() {
        return validUntil;
    }

    public int getRemainingRides() {
        return remainingRides;
    }

    public SkiPassStatus getStatus() {
        return status;
    }

    public boolean isBlocked() {
        return status == SkiPassStatus.BLOCKED;
    }

    void block() {
        this.status = SkiPassStatus.BLOCKED;
    }

    public boolean hasRidesLeft() {
        return !type.isRideLimited() || remainingRides > 0;
    }

    /** Списує одну поїздку, якщо тип пропуску це передбачає. */
    void consumeRide() {
        if (type.isRideLimited()) {
            if (remainingRides <= 0) {
                throw new IllegalStateException("На картці " + id + " не залишилось підйомів");
            }
            remainingRides--;
        }
    }

    @Override
    public String toString() {
        String rides = type.isRideLimited() ? (remainingRides + "/" + type.getRideLimit()) : "необмежено";
        return String.format("SkiPass{id=%s, тип=%s, дійсний до=%s, підйомів=%s, статус=%s}",
                id, type.getDescription(), validUntil, rides, status);
    }
}
