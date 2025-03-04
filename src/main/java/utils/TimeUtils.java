package utils;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import ui.TimeBlock;

public class TimeUtils {
    public static List<TimeBlock> getDefaultSchedule() {
        List<TimeBlock> schedule = new ArrayList<>();
        schedule.add(new TimeBlock(LocalTime.of(6, 0), LocalTime.of(9, 0), "Leetcode & DSA"));
        schedule.add(new TimeBlock(LocalTime.of(9, 0), LocalTime.of(12, 0), "Real-World Project"));
        schedule.add(new TimeBlock(LocalTime.of(12, 0), LocalTime.of(15, 0), "Classwork & Study"));
        schedule.add(new TimeBlock(LocalTime.of(15, 0), LocalTime.of(17, 0), "Family & Workout"));
        schedule.add(new TimeBlock(LocalTime.of(17, 0), LocalTime.of(20, 0), "Portfolio & Open Source"));
        schedule.add(new TimeBlock(LocalTime.of(20, 0), LocalTime.of(22, 0), "Networking & Job Apps"));
        schedule.add(new TimeBlock(LocalTime.of(22, 0), LocalTime.of(23, 59), "Wind Down & Sleep"));

        return schedule;
    }
}
