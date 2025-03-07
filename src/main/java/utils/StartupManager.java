package utils;

import java.io.*;
import java.nio.file.*;
import java.util.Properties;


public class StartupManager {
    private static final String SETTINGS_FILE = "settings.properties";
    private static final String STARTUP_KEY = "runOnStartup";

    public static void checkAndHandleStartup() {
        if (loadStartupPreference()) {
            addToStartup();
        }
    }

    public static boolean loadStartupPreference() {
        Properties properties = new Properties();
        try (FileInputStream input = new FileInputStream(SETTINGS_FILE)) {
            properties.load(input);
            return Boolean.parseBoolean(properties.getProperty(STARTUP_KEY, "false"));
        } catch (IOException e) {
            return false;
        }
    }

    public static void saveStartupPreference(boolean enabled) {
        Properties properties = new Properties();
        try (FileInputStream input = new FileInputStream(SETTINGS_FILE)) {
            properties.load(input);
        } catch (IOException ignored) {
        }

        properties.setProperty(STARTUP_KEY, String.valueOf(enabled));

        try (FileOutputStream output = new FileOutputStream(SETTINGS_FILE)) {
            properties.store(output, "Startup Settings");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void addToStartup() {
        String os = System.getProperty("os.name").toLowerCase();
        String jarPath = new File(StartupManager.class.getProtectionDomain().getCodeSource().getLocation().getPath()).getAbsolutePath();

        try {
            if (os.contains("win")) {
                // Windows: Use ProcessBuilder instead of exec(command)
                ProcessBuilder pb = new ProcessBuilder(
                        "reg", "add", "HKCU\\Software\\Microsoft\\Windows\\CurrentVersion\\Run",
                        "/v", "MyJavaApp", "/t", "REG_SZ", "/d", jarPath, "/f"
                );
                pb.start();
            } else if (os.contains("linux")) {
                // Linux: Write .desktop file
                String autostartPath = System.getProperty("user.home") + "/.config/autostart/MyJavaApp.desktop";
                String desktopEntry = "[Desktop Entry]\nType=Application\nExec=java -jar " + jarPath + "\nHidden=false\nNoDisplay=false\nX-GNOME-Autostart-enabled=true\nName=MyJavaApp";
                Files.write(Paths.get(autostartPath), desktopEntry.getBytes());
            } else if (os.contains("mac")) {
                // macOS: Write plist file
                String plistPath = System.getProperty("user.home") + "/Library/LaunchAgents/com.myjavaapp.plist";
                String plistContent = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                        + "<!DOCTYPE plist PUBLIC \"-//Apple//DTD PLIST 1.0//EN\" \"http://www.apple.com/DTDs/PropertyList-1.0.dtd\">\n"
                        + "<plist version=\"1.0\">\n"
                        + "<dict>\n"
                        + "    <key>Label</key>\n"
                        + "    <string>com.myjavaapp</string>\n"
                        + "    <key>ProgramArguments</key>\n"
                        + "    <array>\n"
                        + "        <string>/usr/bin/java</string>\n"
                        + "        <string>-jar</string>\n"
                        + "        <string>" + jarPath + "</string>\n"
                        + "    </array>\n"
                        + "    <key>RunAtLoad</key>\n"
                        + "    <true/>\n"
                        + "</dict>\n"
                        + "</plist>";
                Files.write(Paths.get(plistPath), plistContent.getBytes());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public static void removeFromStartup() {
        String os = System.getProperty("os.name").toLowerCase();

        try {
            if (os.contains("win")) {
                // Windows: Use ProcessBuilder to delete registry key
                ProcessBuilder pb = new ProcessBuilder(
                        "reg", "delete", "HKCU\\Software\\Microsoft\\Windows\\CurrentVersion\\Run",
                        "/v", "MyJavaApp", "/f"
                );
                pb.start();
            } else if (os.contains("linux")) {
                // Linux: Delete .desktop file
                String autostartPath = System.getProperty("user.home") + "/.config/autostart/MyJavaApp.desktop";
                Files.deleteIfExists(Paths.get(autostartPath));
            } else if (os.contains("mac")) {
                // macOS: Delete plist file
                String plistPath = System.getProperty("user.home") + "/Library/LaunchAgents/com.myjavaapp.plist";
                Files.deleteIfExists(Paths.get(plistPath));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
