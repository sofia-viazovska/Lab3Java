package skilift;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.UUID;

/**
 * Система реєстрації ski-pass, з якою зв'язаний турнікет: випуск карток
 * та їх блокування через порушення правил підйому (Крок з умови завдання:
 * "1. випустити ski-pass; 2. заблокувати ski-pass").
 */
public class SkiPassRegistry {

    private final Map<String, SkiPass> issuedPasses = new HashMap<>();

    /** Випускає новий ski-pass заданого типу, виданий у момент issuedAt. */
    public SkiPass issuePass(SkiPassType type, LocalDateTime issuedAt) {
        String id = UUID.randomUUID().toString();
        SkiPass pass = new SkiPass(id, type, issuedAt);
        issuedPasses.put(id, pass);
        return pass;
    }

    /** Блокує ski-pass за ідентифікатором (наприклад, через порушення правил підйому). */
    public void blockPass(String id) {
        SkiPass pass = issuedPasses.get(id);
        if (pass == null) {
            throw new NoSuchElementException("Ski-pass з ідентифікатором " + id + " не зареєстрований");
        }
        pass.block();
    }

    /** Пошук зареєстрованого ski-pass за ідентифікатором. Null, якщо картка невідома системі. */
    public SkiPass findById(String id) {
        return issuedPasses.get(id);
    }

    public int totalIssued() {
        return issuedPasses.size();
    }
}
