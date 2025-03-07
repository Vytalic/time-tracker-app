import utils.StartupManager;

public class Main {
    public static void main(String[] args) {
        // Startup Logic before launching UI
        StartupManager.checkAndHandleStartup();

        new ui.TimeTrackerFrame();
    }
}
