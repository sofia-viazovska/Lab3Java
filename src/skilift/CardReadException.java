package skilift;

/** Помилка зчитування даних з фізичної картки турнікетом. */
public class CardReadException extends Exception {

    public CardReadException(String message) {
        super(message);
    }
}
