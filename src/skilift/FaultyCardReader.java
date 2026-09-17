package skilift;

/** Емулює несправну картку чи зчитувач: дані прочитати не вдається. */
public class FaultyCardReader implements CardReader {

    @Override
    public String readCardId() throws CardReadException {
        throw new CardReadException("Не вдалося зчитати дані картки");
    }
}
