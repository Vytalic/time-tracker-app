package utils;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;
import ui.TimeBlock;

public class TimeUtils {
    private static final String TIMEBLOCKS_FILE = "timeblocks.json";

    public static List<TimeBlock> getDefaultSchedule() {
        File file = new File(TIMEBLOCKS_FILE);

        // If the file is missing OR empty OR corrupt, regenerate defaults
        if (!file.exists() || file.length() == 0 || isJsonCorrupt()) {
            System.out.println("Creating default schedule...");
            List<TimeBlock> schedule = createDefaultSchedule();
            saveScheduleToJson(schedule);
            return schedule;
        }

        // If the file is valid, load it
        return loadScheduleFromJson();
    }

    // Check if JSON file is corrupt
    private static boolean isJsonCorrupt() {
        try {
            String json = new String(Files.readAllBytes(Paths.get(TIMEBLOCKS_FILE))).trim();
            if (json.isEmpty() || json.equals("null") || json.equals("{}")) {
                return true;
            }
            new JSONArray(json); // If parsing fails, the file is corrupt
            return false;
        } catch (Exception e) {
            return true;
        }
    }

    // Create default schedule without saving
    private static List<TimeBlock> createDefaultSchedule() {
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

    // Load Schedule from JSON File
    private static List<TimeBlock> loadScheduleFromJson() {
        List<TimeBlock> schedule = new ArrayList<>();

        try {
            String json = new String(Files.readAllBytes(Paths.get(TIMEBLOCKS_FILE))).trim();
            JSONArray jsonArray = new JSONArray(json);

            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject obj = jsonArray.getJSONObject(i);
                LocalTime start = LocalTime.parse(obj.getString("start"));
                LocalTime end = LocalTime.parse(obj.getString("end"));
                String label = obj.getString("label");

                schedule.add(new TimeBlock(start, end, label));
            }
        } catch (IOException e) {
            System.out.println("Error loading schedule from JSON, using defaults.");
            schedule = createDefaultSchedule();
        } catch (Exception e) {
            System.out.println("Corrupt JSON detected. Resetting to default.");
            e.printStackTrace();
            schedule = createDefaultSchedule();
        }

        return schedule;
    }

    // Save Schedule to JSON File
    private static void saveScheduleToJson(List<TimeBlock> schedule) {
        JSONArray jsonArray = new JSONArray();

        for (TimeBlock block : schedule) {
            JSONObject obj = new JSONObject();
            obj.put("start", block.start.toString());
            obj.put("end", block.end.toString());
            obj.put("label", block.label);
            jsonArray.put(obj);
        }

        try (FileWriter file = new FileWriter(TIMEBLOCKS_FILE, false)) {
            file.write(jsonArray.toString(4));
            file.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
