package ui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.LocalTime;
import java.util.List;
import java.util.ArrayList;
import utils.TimeUtils;
import java.util.Properties;
import java.nio.file.*;
import com.google.gson.*;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.FileWriter;


import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import utils.TimeUtils;
import ui.TimeBlock;


public class TimeTrackerFrame extends JFrame {
    private Point initialClick;
    private List<TimeBlock> schedule;
    private ProgressBar progressBar;
    private Color fontColor;
    private Color progressBarColor;
    private Color timelineColor;
    private Color currentTimeColor;
    private Color blockColor;
    private Color blockHoverColor;
    private Color blockBorderColor;
    private LocalTime startOfDay;
    private int frameWidth;
    private int frameHeight;

    // Store time for the label in titlebar
    private JLabel timeLabel;

    private static final String SETTINGS_FILE = "settings.properties";
    private static final String TIMEBLOCKS_FILE = "timeblocks.json";

    public TimeTrackerFrame() {
        // Load settings and schedule.
        loadSettings();
        loadTimeBlocks();

        setTitle("Time Tracker Overlay");
        setSize(frameWidth, frameHeight);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setUndecorated(true);
        setAlwaysOnTop(true);
        setLayout(new BorderLayout());

        // Buttons panel
        JPanel buttonPanel = new JPanel(new BorderLayout());
        buttonPanel.setBackground(Color.DARK_GRAY);
        buttonPanel.setPreferredSize(new Dimension(getWidth(), 40));

        // Left panel for Menu Button
        JPanel leftPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 5));
        leftPanel.setOpaque(false);

        // Menu button properties
        JButton menuButton = new JButton("\u2630 Menu");
        menuButton.setFont(new Font("SansSerif", Font.BOLD, 14));
        menuButton.setFocusPainted(false);
        menuButton.setBorderPainted(false);
        menuButton.setBackground(Color.GRAY);
        menuButton.setOpaque(true);
        menuButton.setPreferredSize(new Dimension(100, 30));
        menuButton.addActionListener(e -> showMenu(menuButton));
        leftPanel.add(menuButton);

        // Center panel for Time Label
        JPanel centerPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 5, 5));
        centerPanel.setOpaque(false);
        JLabel timeLabel = new JLabel();
        timeLabel.setForeground(Color.WHITE);
        timeLabel.setFont(new Font("Arial", Font.BOLD, 28));
        centerPanel.add(timeLabel);


        // Right panel for Minimize and Close Buttons
        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 5));
        rightPanel.setOpaque(false);
        JButton minimizeButton = createButton("-", 50, 30, Color.LIGHT_GRAY, e -> setState(JFrame.ICONIFIED));
        JButton closeButton = createButton("Close", 80, 30, Color.RED, e -> System.exit(0));
        rightPanel.add(minimizeButton);
        rightPanel.add(closeButton);

        // Add panels to buttonPanel
        buttonPanel.add(leftPanel, BorderLayout.WEST);
        buttonPanel.add(centerPanel, BorderLayout.CENTER);
        buttonPanel.add(rightPanel, BorderLayout.EAST);
        add(buttonPanel, BorderLayout.NORTH);


        // Create the ProgressBar
        progressBar = new ProgressBar(
                schedule,
                startOfDay,
                progressBarColor,
                timelineColor,
                currentTimeColor,
                blockColor,
                blockHoverColor,
                blockBorderColor
        );
        add(progressBar, BorderLayout.CENTER);

        // Automatically update the title every minute.
        Timer timer = new Timer(60000, e -> updateTimeLabel(timeLabel));
        timer.start();
        updateTimeLabel(timeLabel);

        enableDragging();
        setVisible(true);
    }

    private void showMenu(JButton menuButton) {
        JPopupMenu menu = new JPopupMenu();

        JMenuItem changeSize = new JMenuItem("Change Size");
        changeSize.addActionListener(e -> openSizeDialog());

        JMenuItem settings = new JMenuItem("Settings");
        settings.addActionListener(e -> openSettingsDialog());

        menu.add(changeSize);
        menu.add(settings);

        // Show below the Menu button
        menu.show(menuButton, 0, menuButton.getHeight());
    }

    // Loads settings from file
    private void loadSettings() {
        Properties properties = new Properties();
        try (FileInputStream input = new FileInputStream(SETTINGS_FILE)) {
            properties.load(input);

            fontColor = new Color(Integer.parseInt(properties.getProperty("fontColor", String.valueOf(Color.BLACK.getRGB()))), true);
            progressBarColor = new Color(Integer.parseInt(properties.getProperty("progressBarColor", String.valueOf(new Color(0, 100, 0, 128).getRGB()))), true);
            timelineColor = new Color(Integer.parseInt(properties.getProperty("timelineColor", String.valueOf(new Color(0, 0, 139).getRGB()))), true);
            currentTimeColor = new Color(Integer.parseInt(properties.getProperty("currentTimeColor", String.valueOf(Color.RED.getRGB()))), true);

            // Formatting for startOfDay
            startOfDay = LocalTime.parse(properties.getProperty("startOfDay", "06:00"));

            // Default frame width and height
            frameWidth = Integer.parseInt(properties.getProperty("frameWidth", "1200"));
            frameHeight = Integer.parseInt(properties.getProperty("frameHeight", "200"));

            blockColor = new Color(Integer.parseInt(properties.getProperty("blockColor", String.valueOf(new Color(150, 150, 150, 128).getRGB()))), true);
            blockHoverColor = new Color(Integer.parseInt(properties.getProperty("blockHoverColor", String.valueOf(new Color(34, 34, 139, 200).getRGB()))), true);
            blockBorderColor = new Color(Integer.parseInt(properties.getProperty("blockBorderColor", String.valueOf(Color.BLACK.getRGB()))), true);

        } catch (IOException e) {
            System.out.println("No previous settings found, using defaults.");

            // Set default colors with correct transparency
            fontColor = Color.BLACK;
            progressBarColor = new Color(0, 100, 0, 128);
            timelineColor = new Color(0, 0, 139, 128);
            currentTimeColor = new Color(255, 0, 0, 255);

            startOfDay = LocalTime.of(6, 0);
            frameWidth = 1200;
            frameHeight = 200;

            blockColor = new Color(255, 255, 255, 128);
            blockHoverColor = new Color(34, 34, 139, 200);
            blockBorderColor = Color.BLACK;
        }
    }

    // Saves settings to file
    private void saveSettings() {
        Properties properties = new Properties();
        properties.setProperty("fontColor", String.valueOf(fontColor.getRGB()));
        properties.setProperty("progressBarColor", String.valueOf(progressBarColor.getRGB()));
        properties.setProperty("timelineColor", String.valueOf(timelineColor.getRGB()));
        properties.setProperty("currentTimeColor", String.valueOf(currentTimeColor.getRGB()));

        // Save startOfDay Correctly
        properties.setProperty("startOfDay", startOfDay.format(java.time.format.DateTimeFormatter.ofPattern("HH:mm")));

        // Save frame dimensions
        properties.setProperty("frameWidth", String.valueOf(frameWidth));
        properties.setProperty("frameHeight", String.valueOf(frameHeight));

        // Save colors
        properties.setProperty("blockColor", String.valueOf(blockColor.getRGB()));
        properties.setProperty("blockHoverColor", String.valueOf(blockHoverColor.getRGB()));
        properties.setProperty("blockBorderColor", String.valueOf(blockBorderColor.getRGB()));

        try (FileOutputStream output = new FileOutputStream(SETTINGS_FILE)) {
            properties.store(output, "User Settings");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Open the Change Size Dialog
    private void openSizeDialog() {
        JTextField widthField = new JTextField(String.valueOf(frameWidth), 5);
        JTextField heightField = new JTextField(String.valueOf(frameHeight), 5);

        JPanel panel = new JPanel(new GridLayout(2, 2));
        panel.add(new JLabel("Width:"));
        panel.add(widthField);
        panel.add(new JLabel("Height:"));
        panel.add(heightField);

        int result = JOptionPane.showConfirmDialog(null, panel, "Change Frame Size", JOptionPane.OK_CANCEL_OPTION);
        if (result == JOptionPane.OK_OPTION) {
            try {
                int newWidth = Integer.parseInt(widthField.getText().trim());
                int newHeight = Integer.parseInt(heightField.getText().trim());

                if (newWidth > 0 && newHeight > 0) {
                    frameWidth = newWidth;
                    frameHeight = newHeight;
                    setSize(frameWidth, frameHeight);
                    saveSettings();
                } else {
                    JOptionPane.showMessageDialog(null, "Width and Height must be positive numbers!", "Invalid Input", JOptionPane.ERROR_MESSAGE);
                }
            } catch (NumberFormatException e) {
                JOptionPane.showMessageDialog(null, "Invalid input! Please enter valid numbers.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    // Load time blocks from file
    private void loadTimeBlocks() {
        try {
            String json = Files.readString(Paths.get(TIMEBLOCKS_FILE));
            TimeBlock[] blocks = new Gson().fromJson(json, TimeBlock[].class);
            schedule = new ArrayList<>(List.of(blocks));
        } catch (IOException e) {
            System.out.println("No previous time blocks found, using defaults.");
            schedule = new ArrayList<>(TimeUtils.getDefaultSchedule());
        }
    }

    // Save time blocks to file
    private void saveTimeBlocks() {
        try (FileWriter writer = new FileWriter(TIMEBLOCKS_FILE)) {
            new Gson().toJson(schedule, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private JButton createButton(String text, int width, int height, Color color, ActionListener action) {
        JButton button = new JButton(text);
        button.setFont(new Font("Arial", Font.BOLD, 12));
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setBackground(color);
        button.setOpaque(true);
        button.setPreferredSize(new Dimension(width, height));
        button.addActionListener(action);
        return button;
    }
    private void enableDragging() {
        MouseAdapter dragListener = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                initialClick = e.getPoint();
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                int thisX = getLocation().x;
                int thisY = getLocation().y;
                int xMoved = e.getX() - initialClick.x;
                int yMoved = e.getY() - initialClick.y;
                setLocation(thisX + xMoved, thisY + yMoved);
            }
        };

        addMouseListener(dragListener);
        addMouseMotionListener(dragListener);
    }

    private void openSettingsDialog() {
        JFrame settingsFrame = new JFrame("Settings");
        settingsFrame.setSize(400, 300);
        settingsFrame.setLayout(new GridLayout(6, 2));
        getContentPane().setBackground(new Color(211, 211, 211)); // Light Gray background
        settingsFrame.setAlwaysOnTop(true);

        // Color Pickers
        JButton fontColorButton = new JButton("Font Color");
        JButton progressBarColorButton = new JButton("Progress Bar Color");
        JButton timelineColorButton = new JButton("Timeline Color");
        JButton currentTimeColorButton = new JButton("Current Time Color");

        fontColorButton.addActionListener(e -> fontColor = JColorChooser.showDialog(null, "Choose Font Color", fontColor));
        progressBarColorButton.addActionListener(e -> progressBarColor = JColorChooser.showDialog(null, "Choose Progress Bar Color", progressBarColor));
        timelineColorButton.addActionListener(e -> timelineColor = JColorChooser.showDialog(null, "Choose Timeline Color", timelineColor));
        currentTimeColorButton.addActionListener(e -> currentTimeColor = JColorChooser.showDialog(null, "Choose Current Time Color", currentTimeColor));

        // Start of Day Selection
        JComboBox<String> startOfDayBox = new JComboBox<>();
        for (int hour = 0; hour < 24; hour++) {
            startOfDayBox.addItem(String.format("%02d:00", hour));
        }
        startOfDayBox.setSelectedItem(startOfDay.toString()); // Load saved setting
        startOfDayBox.addActionListener(e -> startOfDay = LocalTime.parse((String) startOfDayBox.getSelectedItem()));

        // Save & Apply Button
        JButton applyButton = new JButton("Apply");
        applyButton.addActionListener(e -> {
            progressBar.updateSettings(fontColor, progressBarColor, timelineColor, currentTimeColor, startOfDay,
                    blockColor, blockHoverColor, blockBorderColor);
            saveSettings();
            settingsFrame.dispose();
        });

        // Add components
        settingsFrame.add(new JLabel("Select Start of Day:"));
        settingsFrame.add(startOfDayBox);
        settingsFrame.add(fontColorButton);
        settingsFrame.add(progressBarColorButton);
        settingsFrame.add(timelineColorButton);
        settingsFrame.add(currentTimeColorButton);
        settingsFrame.add(new JLabel()); // Spacer
        settingsFrame.add(applyButton);

        settingsFrame.setVisible(true);
    }

    private void updateTimeLabel(JLabel timeLabel) {
        String currentTime = java.time.LocalTime.now().format(java.time.format.DateTimeFormatter.ofPattern("hh:mm a"));
        timeLabel.setText(currentTime);
    }


    public static void main(String[] args) {
        SwingUtilities.invokeLater(TimeTrackerFrame::new);
    }
}