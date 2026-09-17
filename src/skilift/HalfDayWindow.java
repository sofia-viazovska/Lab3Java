package skilift;

import java.time.LocalTime;

/** Часове вікно дії "південного" (напівденного) ski-pass. */
public enum HalfDayWindow {
    MORNING(LocalTime.of(9, 0), LocalTime.of(13, 0)),
    AFTERNOON(LocalTime.of(13, 0), LocalTime.of(17, 0));

    private final LocalTime start;
    private final LocalTime end;

    HalfDayWindow(LocalTime start, LocalTime end) {
        this.start = start;
        this.end = end;
    }

    public LocalTime getStart() {
        return start;
    }

    public LocalTime getEnd() {
        return end;
    }

    /** Чи входить час у півдіапазон [start, end). */
    public boolean contains(LocalTime time) {
        return !time.isBefore(start) && time.isBefore(end);
    }
}
