package ui;

import java.time.LocalTime;

public class TimeBlock {
    public LocalTime start;
    public LocalTime end;
    public String label;

    public TimeBlock(LocalTime start, LocalTime end, String label) {
        this.start = start;
        this.end = end;
        this.label = label;
    }

    @Override
    public String toString() {
        return start + " - " + end + " (" + label + ")";
    }
}
