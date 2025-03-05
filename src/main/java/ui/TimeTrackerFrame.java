package ui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.LocalTime;
import java.util.List;
import java.util.ArrayList;
import utils.TimeUtils;

import java.util.Objects;
import java.util.Properties;
import java.nio.file.*;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.FileWriter;
import java.io.File;
import org.json.JSONArray;
import org.json.JSONObject;

public class TimeTrackerFrame extends JFrame {
    private Point initialClick;
    private List<TimeBlock> schedule;
    private final ProgressBar progressBar;
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


    private static final String SETTINGS_FILE = "settings.properties";
    private static final String TIMEBLOCKS_FILE = "timeblocks.json";

    private final JLabel blockLabel;
    private final JLabel timeLeftLabel;


    public TimeTrackerFrame() {
        // Load settings and schedule.
        loadSettings();
        loadTimeBlocks();

        setTitle("Time Tracker Overlay");
        setSize(frameWidth, frameHeight);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setUndecorated(true);
        setAlwaysOnTop(false);
        setLayout(new BorderLayout());

        // Buttons panel
        JPanel buttonPanel = new JPanel(new BorderLayout());
        buttonPanel.setBackground(Color.DARK_GRAY);
        buttonPanel.setPreferredSize(new Dimension(getWidth(), 40));

        // Left panel for Menu Button
        JPanel leftPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 5));
        leftPanel.setOpaque(false);

        // Menu button properties
        JButton menuButton = new JButton("☰ Menu");
        menuButton.setFont(new Font("SansSerif", Font.BOLD, 14));
        menuButton.setFocusPainted(false);
        menuButton.setBorderPainted(false);
        menuButton.setBackground(Color.GRAY);
        menuButton.setOpaque(true);
        menuButton.setPreferredSize(new Dimension(100, 30));
        menuButton.addActionListener(e -> showMenu(menuButton));
        leftPanel.add(menuButton);

        /// Center panel for Time Label, Current Block Label, and Time Left
        JPanel centerPanel = new JPanel(new GridBagLayout());
        centerPanel.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();

// Time label (Left-aligned)
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1.0; // Allows stretching
        gbc.anchor = GridBagConstraints.WEST; // Left-align
        JLabel timeLabel = new JLabel();
        timeLabel.setForeground(Color.WHITE);
        timeLabel.setFont(new Font("Arial", Font.BOLD, 28));
        centerPanel.add(timeLabel, gbc);

// Current block label (Center-aligned)
        gbc.gridx = 1;
        gbc.anchor = GridBagConstraints.CENTER; // Center-align
        blockLabel = new JLabel("No active block");
        blockLabel.setForeground(Color.WHITE);
        blockLabel.setFont(new Font("Arial", Font.BOLD, 28));
        centerPanel.add(blockLabel, gbc);

// Time Left label (Right-aligned)
        gbc.gridx = 2;
        gbc.anchor = GridBagConstraints.EAST; // Right-align
        timeLeftLabel = new JLabel("");
        timeLeftLabel.setForeground(Color.WHITE);
        timeLeftLabel.setFont(new Font("Arial", Font.BOLD, 28));
        centerPanel.add(timeLeftLabel, gbc);



        // Right panel for Pin, Minimize, and Close Buttons
        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 5));
        rightPanel.setOpaque(false);

        // Declare pinButton & set properties
        JButton pinButton = new JButton("Pin");
        pinButton.setPreferredSize(new Dimension(80, 30));
        pinButton.setBackground(Color.LIGHT_GRAY);

        // Add action listener for pinButton
        pinButton.addActionListener(e -> {
            boolean isPinned = !isAlwaysOnTop();
            setAlwaysOnTop(isPinned);
            pinButton.setText(isPinned ? "Unpin" : "Pin");
        });

        JButton minimizeButton = createButton("-", 50, 30, Color.GRAY, e -> setState(JFrame.ICONIFIED));
        JButton closeButton = createButton("Close", 80, 30, Color.RED, e -> System.exit(0));
        rightPanel.add(pinButton);
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

        // Automatically update the title, block label, and eta every minute.
        Timer timer = new Timer(60000, e -> {
            updateTimeLabel(timeLabel);
            updateCurrentBlockInfo();
        });
        timer.start();
        updateTimeLabel(timeLabel);
        updateCurrentBlockInfo();


        enableDragging();
        setVisible(true);
    }

    private void showMenu(JButton menuButton) {
        JPopupMenu menu = new JPopupMenu();

        // First entry in menu
        JMenuItem schedule = new JMenuItem("Edit Schedule");
        schedule.addActionListener(e -> openScheduleDialog());

        // Second entry in menu
        JMenuItem changeSize = new JMenuItem("Change Size");
        changeSize.addActionListener(e -> openSizeDialog());

        // Third entry in menu
        JMenuItem settings = new JMenuItem("Settings");
        settings.addActionListener(e -> openSettingsDialog());

        menu.add(schedule);
        menu.add(changeSize);
        menu.add(settings);

        // Show below the Menu button
        menu.show(menuButton, 0, menuButton.getHeight());
    }

    private void updateCurrentBlockInfo() {
        TimeBlock currentBlock = progressBar.getCurrentTimeBlock();

        if (currentBlock != null) {
            blockLabel.setText("" + currentBlock.label);
            timeLeftLabel.setText(progressBar.getTimeRemaining(currentBlock));
        } else {
            blockLabel.setText("No active block");
            timeLeftLabel.setText("");
        }
    }


    // Loads settings from file
    private void loadSettings() {
        Properties properties = new Properties();
        try (FileInputStream input = new FileInputStream(SETTINGS_FILE)) {
            properties.load(input);

            fontColor = new Color(Integer.parseInt(properties.getProperty("fontColor", String.valueOf(Color.BLACK.getRGB()))), true);
            progressBarColor = new Color(Integer.parseInt(properties.getProperty("progressBarColor", String.valueOf(new Color(18, 97, 150, 170).getRGB()))), true);
            timelineColor = new Color(Integer.parseInt(properties.getProperty("timelineColor", String.valueOf(new Color(140, 0, 0, 255).getRGB()))), true);
            currentTimeColor = new Color(Integer.parseInt(properties.getProperty("currentTimeColor", String.valueOf(new Color(255, 0, 0, 255).getRGB()))), true);

            // Formatting for startOfDay
            startOfDay = LocalTime.parse(properties.getProperty("startOfDay", "06:00"));

            // Default frame width and height
            frameWidth = Integer.parseInt(properties.getProperty("frameWidth", "1200"));
            frameHeight = Integer.parseInt(properties.getProperty("frameHeight", "200"));

            blockColor = new Color(Integer.parseInt(properties.getProperty("blockColor", String.valueOf(new Color(255, 255, 255, 128).getRGB()))), true);
            blockHoverColor = new Color(Integer.parseInt(properties.getProperty("blockHoverColor", String.valueOf(new Color(34, 34, 139, 200).getRGB()))), true);
            blockBorderColor = new Color(Integer.parseInt(properties.getProperty("blockBorderColor", String.valueOf(Color.BLACK.getRGB()))), true);

        } catch (IOException e) {
            System.out.println("No previous settings found, using defaults.");

            // Set default colors with correct transparency
            fontColor = Color.BLACK;
            progressBarColor = new Color(18, 97, 150, 170);
            timelineColor = new Color(140, 0, 0, 255);
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

    private void addNewTimeBlock(DefaultListModel<TimeBlock> listModel) {
        JTextField startTimeField = new JTextField(5);
        JTextField endTimeField = new JTextField(5);
        JTextField labelField = new JTextField(10);

        JPanel panel = new JPanel(new GridLayout(3, 2));
        panel.add(new JLabel("Start Time (HH:mm):"));
        panel.add(startTimeField);
        panel.add(new JLabel("End Time (HH:mm):"));
        panel.add(endTimeField);
        panel.add(new JLabel("Label:"));
        panel.add(labelField);

        int result = JOptionPane.showConfirmDialog(null, panel, "Add Time Block", JOptionPane.OK_CANCEL_OPTION);
        if (result == JOptionPane.OK_OPTION) {
            try {
                LocalTime start = LocalTime.parse(startTimeField.getText().trim());
                LocalTime end = LocalTime.parse(endTimeField.getText().trim());
                String label = labelField.getText().trim();

                if (label.isEmpty()) {
                    JOptionPane.showMessageDialog(null, "Label cannot be empty.", "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                TimeBlock newBlock = new TimeBlock(start, end, label);
                schedule.add(newBlock);
                listModel.addElement(newBlock);

                saveTimeBlocks();
                progressBar.repaint();
            } catch (Exception e) {
                JOptionPane.showMessageDialog(null, "Invalid time format! Use HH:mm.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }


    private void openScheduleDialog() {
        setAlwaysOnTop(false);

        JFrame scheduleFrame = new JFrame("Manage Schedule");
        scheduleFrame.setSize(450, 450);
        scheduleFrame.setLayout(new BorderLayout());
        scheduleFrame.setAlwaysOnTop(true);
        scheduleFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        // Panel to display existing time blocks
        DefaultListModel<TimeBlock> listModel = new DefaultListModel<>();
        JList<TimeBlock> timeBlockList = new JList<>(listModel);
        timeBlockList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        for (TimeBlock block : schedule) {
            listModel.addElement(block);
        }

        JScrollPane scrollPane = new JScrollPane(timeBlockList);
        scheduleFrame.add(scrollPane, BorderLayout.CENTER);

        // Buttons for Adding and Deleting
        JButton addButton = new JButton("Add Time Block");
        JButton editButton = new JButton("Edit");
        JButton deleteButton = new JButton("Delete Selected");
        JButton saveButton = new JButton("Save");

        // Add new time block
        addButton.addActionListener(e -> addNewTimeBlock(listModel));

        // Edit selected time block
        editButton.addActionListener(e -> {
            TimeBlock selectedBlock = timeBlockList.getSelectedValue();
            if (selectedBlock != null) {
                editTimeBlock(selectedBlock, listModel);
            } else {
                JOptionPane.showMessageDialog(scheduleFrame, "Please select a time block to edit.", "No Selection", JOptionPane.WARNING_MESSAGE);
            }
        });

        // Delete selected time block
        deleteButton.addActionListener(e -> {
            TimeBlock selectedBlock = timeBlockList.getSelectedValue();
            if (selectedBlock != null) {
                schedule.remove(selectedBlock);
                listModel.removeElement(selectedBlock);
                saveTimeBlocks();
                progressBar.repaint();
            }
        });

        saveButton.addActionListener(e -> {
            saveTimeBlocks();
            progressBar.repaint();
            JOptionPane.showMessageDialog(scheduleFrame, "Schedule saved successfully.", "Saved", JOptionPane.INFORMATION_MESSAGE);
            scheduleFrame.dispose();
        });



        JPanel buttonPanel = new JPanel();
        buttonPanel.add(addButton);
        buttonPanel.add(editButton);
        buttonPanel.add(deleteButton);
        buttonPanel.add(saveButton);

        scheduleFrame.add(buttonPanel, BorderLayout.SOUTH);

        // Restore "Always on Top" when closed
        scheduleFrame.addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosed(java.awt.event.WindowEvent e) {
                setAlwaysOnTop(true);
            }
        });

        scheduleFrame.setVisible(true);
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

    private void editTimeBlock(TimeBlock block, DefaultListModel<TimeBlock> listModel) {
        JTextField startTimeField = new JTextField(block.start.toString(), 5);
        JTextField endTimeField = new JTextField(block.end.toString(), 5);
        JTextField labelField = new JTextField(block.label, 10);

        JPanel panel = new JPanel(new GridLayout(3, 2));
        panel.add(new JLabel("Start Time (HH:mm):"));
        panel.add(startTimeField);
        panel.add(new JLabel("End Time (HH:mm):"));
        panel.add(endTimeField);
        panel.add(new JLabel("Label:"));
        panel.add(labelField);

        int result = JOptionPane.showConfirmDialog(null, panel, "Edit Time Block", JOptionPane.OK_CANCEL_OPTION);
        if (result == JOptionPane.OK_OPTION) {
            try {
                LocalTime newStart = LocalTime.parse(startTimeField.getText().trim());
                LocalTime newEnd = LocalTime.parse(endTimeField.getText().trim());
                String newLabel = labelField.getText().trim();

                if (newLabel.isEmpty()) {
                    JOptionPane.showMessageDialog(null, "Label cannot be empty.", "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                // Update the block
                block.start = newStart;
                block.end = newEnd;
                block.label = newLabel;

                // Refresh the list
                listModel.setElementAt(block, listModel.indexOf(block));

                // Save updates
                saveTimeBlocks();
                progressBar.repaint();
            } catch (Exception e) {
                JOptionPane.showMessageDialog(null, "Invalid time format! Use HH:mm.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }


    // Load time blocks from file
    private void loadTimeBlocks() {
        schedule = new ArrayList<>();
        File file = new File(TIMEBLOCKS_FILE);

        // If the file doesn't exist or is empty, create a default schedule
        if (!file.exists() || file.length() == 0) {
            System.out.println("No valid time blocks found, creating defaults.");
            schedule = TimeUtils.getDefaultSchedule();
            return;
        }

        try {
            String json = new String(Files.readAllBytes(Paths.get(TIMEBLOCKS_FILE))).trim();

            // Check if JSON is empty
            if (json.isEmpty() || json.equals("null")) {
                System.out.println("Empty JSON file, creating default schedule.");
                schedule = TimeUtils.getDefaultSchedule();
                return;
            }

            JSONArray jsonArray = new JSONArray(json);
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject obj = jsonArray.getJSONObject(i);
                LocalTime start = LocalTime.parse(obj.getString("start"));
                LocalTime end = LocalTime.parse(obj.getString("end"));
                String label = obj.getString("label");

                schedule.add(new TimeBlock(start, end, label));
            }
        } catch (IOException e) {
            System.out.println("Error reading timeblocks.json, using defaults.");
            schedule = TimeUtils.getDefaultSchedule();
        } catch (Exception e) {
            System.out.println("Corrupt JSON detected. Resetting to default.");
            e.printStackTrace();
            schedule = TimeUtils.getDefaultSchedule();
        }
    }



    private void saveTimeBlocks() {
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
            JOptionPane.showMessageDialog(null, "Error saving schedule!", "Save Error", JOptionPane.ERROR_MESSAGE);
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
        // Prevent JFrame from staying on top of new dialog boxes
        setAlwaysOnTop(false);

        JFrame settingsFrame = new JFrame("Settings");
        settingsFrame.setSize(400, 300);
        settingsFrame.setLayout(new GridLayout(6, 2));
        getContentPane().setBackground(new Color(211, 211, 211)); // Light Gray background
        settingsFrame.setAlwaysOnTop(true);
        settingsFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        // Color Pickers
        JButton fontColorButton = new JButton("Font Color");
        JButton progressBarColorButton = new JButton("Progress Bar Color");
        JButton timelineColorButton = new JButton("Timeline Color");
        JButton currentTimeColorButton = new JButton("Current Time Color");
        JButton blockHoverColorButton = new JButton("Block Hover Color");

        fontColorButton.addActionListener(e -> fontColor = JColorChooser.showDialog(null, "Choose Font Color", fontColor));
        progressBarColorButton.addActionListener(e -> progressBarColor = JColorChooser.showDialog(null, "Choose Progress Bar Color", progressBarColor));
        timelineColorButton.addActionListener(e -> timelineColor = JColorChooser.showDialog(null, "Choose Timeline Color", timelineColor));
        currentTimeColorButton.addActionListener(e -> currentTimeColor = JColorChooser.showDialog(null, "Choose Current Time Color", currentTimeColor));
        blockHoverColorButton.addActionListener(e -> blockHoverColor = JColorChooser.showDialog(null, "Choose Block Hover Color", blockHoverColor));

        // Start of Day Selection
        JComboBox<String> startOfDayBox = new JComboBox<>();
        for (int hour = 0; hour < 24; hour++) {
            startOfDayBox.addItem(String.format("%02d:00", hour));
        }
        startOfDayBox.setSelectedItem(startOfDay.toString()); // Load saved setting
        startOfDayBox.addActionListener(e -> startOfDay = LocalTime.parse((String) Objects.requireNonNull(startOfDayBox.getSelectedItem())));

        // Save & Apply Button
        JButton applyButton = new JButton("Apply");
        applyButton.addActionListener(e -> {
            progressBar.updateSettings(fontColor, progressBarColor, timelineColor, currentTimeColor, startOfDay,
                    blockColor, blockHoverColor, blockBorderColor);
            saveSettings();
            settingsFrame.dispose();
        });

        // Reset Button (Deletes settings file and reloads defaults)
        JButton resetButton = new JButton("Reset to Default");
        resetButton.setBackground(Color.RED);
        resetButton.setForeground(Color.WHITE);
        resetButton.setOpaque(true);
        resetButton.setFont(new Font("Arial", Font.BOLD, 12));

        resetButton.addActionListener(e -> {
            int confirm = JOptionPane.showConfirmDialog(null, "Are you sure you want to reset all settings to default?",
                    "Reset Confirmation", JOptionPane.YES_NO_OPTION);
            if (confirm == JOptionPane.YES_OPTION) {
                resetSettings();
                settingsFrame.dispose();
            }
        });


        // Add components
        settingsFrame.add(new JLabel("Select Start of Day:"));
        settingsFrame.add(startOfDayBox);
        settingsFrame.add(fontColorButton);
        settingsFrame.add(progressBarColorButton);
        settingsFrame.add(timelineColorButton);
        settingsFrame.add(currentTimeColorButton);
        settingsFrame.add(blockHoverColorButton);
        settingsFrame.add(new JLabel()); // Spacer
        settingsFrame.add(applyButton);
        settingsFrame.add(resetButton);

        // Re-enable Always on top when settings closes
        settingsFrame.addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosed(java.awt.event.WindowEvent windowEvent) {
                setAlwaysOnTop(true);
            }
        });

        settingsFrame.setVisible(true);
    }

    private void resetSettings() {
        // Delete the settings file
        try {
            Files.deleteIfExists(Paths.get(SETTINGS_FILE));
            JOptionPane.showMessageDialog(null, "Settings reset to default. Restarting the application.",
                    "Reset Successful", JOptionPane.INFORMATION_MESSAGE);
        } catch (IOException e) {
            JOptionPane.showMessageDialog(null, "Failed to reset settings!", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // Reload default settings
        loadSettings();
        progressBar.updateSettings(fontColor, progressBarColor, timelineColor, currentTimeColor, startOfDay,
                blockColor, blockHoverColor, blockBorderColor);

        // Repaint UI to reflect default settings
        progressBar.repaint();
    }


    private void updateTimeLabel(JLabel timeLabel) {
        String currentTime = java.time.LocalTime.now().format(java.time.format.DateTimeFormatter.ofPattern("hh:mm a"));
        timeLabel.setText(currentTime);
    }


    public static void main(String[] args) {
        SwingUtilities.invokeLater(TimeTrackerFrame::new);
    }
}