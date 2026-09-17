package skilift;

/** Справний зчитувач: коректно повертає ідентифікатор конкретної картки. */
public class SimpleCardReader implements CardReader {

    private final String cardId;

    public SimpleCardReader(String cardId) {
        this.cardId = cardId;
    }

    public SimpleCardReader(SkiPass pass) {
        this(pass.getId());
    }

    @Override
    public String readCardId() {
        return cardId;
    }
}
