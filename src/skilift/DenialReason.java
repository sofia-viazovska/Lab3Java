package skilift;

/** Причина відмови в проході через турнікет. */
public enum DenialReason {
    READ_ERROR("не вдалося зчитати дані картки"),
    UNKNOWN_CARD("картка не зареєстрована в системі"),
    BLOCKED("картка заблокована"),
    EXPIRED("термін дії картки закінчився"),
    OUT_OF_SCHEDULE("картка не діє в цей день/час"),
    NO_RIDES_LEFT("на картці не залишилось підйомів");

    private final String description;

    DenialReason(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
