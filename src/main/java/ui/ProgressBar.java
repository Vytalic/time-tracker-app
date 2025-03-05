package ui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.ArrayList;


import ui.TimeTrackerFrame;

public class ProgressBar extends JPanel {
    private static final int TOTAL_MINUTES = 1440;
    private List<TimeBlock> timeBlocks;
    private LocalTime startOfDay;
    private Color fontColor;
    private Color progressFillColor;
    private Color timelineColor;
    private Color currentTimeColor;
    private Color blockColor;
    private Color blockHoverColor;
    private Color blockBorderColor;
    private TimeBlock hoveredBlock = null;

    private Font timelineFont = new Font("Arial", Font.BOLD, 14);
    private Font blockFont = new Font("Arial", Font.BOLD, 14);

    private static final int PADDING = 20;
    private static final int VERTICAL_PADDING = 20;

    public ProgressBar(List<TimeBlock> timeBlocks,
                       LocalTime startOfDay,
                       Color progressFillColor,
                       Color timelineColor,
                       Color currentTimeColor,
                       Color blockColor,
                       Color blockHoverColor,
                       Color blockBorderColor) {
        this.timeBlocks = timeBlocks;
        this.startOfDay = startOfDay;
        this.fontColor = fontColor;
        this.progressFillColor = progressFillColor;
        this.timelineColor = timelineColor;
        this.currentTimeColor = currentTimeColor;
        this.blockColor = (blockColor != null) ? blockColor : Color.LIGHT_GRAY;
        this.blockHoverColor = blockHoverColor;
        this.blockBorderColor = blockBorderColor;

        // Refresh every minute to update the progress fill and current time indicator.
        Timer timer = new Timer(60000, e -> repaint());
        timer.start();

        // Mouse listener to detect when the mouse hovers over a time block.
        MouseAdapter mouseAdapter = new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                TimeBlock newHover = findBlockAt(e.getX());
                if (newHover != hoveredBlock) {
                    hoveredBlock = newHover;
                    repaint();
                }
            }

            @Override
            public void mouseExited(MouseEvent e) {
                hoveredBlock = null;
                repaint();
            }
        };
        addMouseListener(mouseAdapter);
        addMouseMotionListener(mouseAdapter);
    }

    // Helper method to calculate the x-coordinate for a given time (in minutes) relative to the current startOfDay.
    private int getXForTime(LocalTime time) {
        long minutesFromStart = ChronoUnit.MINUTES.between(startOfDay, time);
        if (minutesFromStart < 0) {
            minutesFromStart += TOTAL_MINUTES;
        }
        return (int) ((minutesFromStart / (double) TOTAL_MINUTES) * (getWidth() - 2 * PADDING));
    }

    // Finds which time block (if any) the given x coordinate falls within.
    private TimeBlock findBlockAt(int mouseX) {
        int width = getWidth();
        for (TimeBlock block : timeBlocks) {
            int xStart = PADDING + getXForTime(block.start);
            int xEnd = PADDING + getXForTime(block.end);

            // If a block spans midnight, xEnd may be less than xStart.
            if (xEnd < xStart) {
                xEnd += width;
            }
            if (mouseX >= xStart && mouseX <= xEnd) {
                return block;
            }
        }
        return null;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        int panelWidth = getWidth() - 2 * PADDING;
        int panelHeight = getHeight() - 2 * VERTICAL_PADDING;
        int timelineHeight = 20;  // Bottom area for the timeline
        int barHeight = panelHeight - timelineHeight;

        // Draw the progress fill (background from startOfDay to current time)
        LocalTime now = LocalTime.now();
        int currentX = PADDING + getXForTime(now);
        g.setColor(progressFillColor);
        g.fillRect(PADDING, VERTICAL_PADDING, currentX - PADDING, barHeight);

        // Draw the timeline
        g.setColor(timelineColor);
        g.setFont(timelineFont);
        for (int hour = 0; hour <= 24; hour++) {
            LocalTime tickTime = startOfDay.plusHours(hour);
            int x = PADDING + (int) ((hour / 24.0) * (getWidth() - 2 * PADDING));
            g.drawLine(x, barHeight + VERTICAL_PADDING, x, barHeight + VERTICAL_PADDING + 5);
            String tickLabel = tickTime.format(java.time.format.DateTimeFormatter.ofPattern("ha"));
            g.drawString(tickLabel, x - 10, panelHeight + VERTICAL_PADDING - 3);
        }

        // Draw the time blocks (skip hovered block)
        g.setFont(blockFont);
        for (TimeBlock block : timeBlocks) {
            if (block.equals(hoveredBlock)) {
                continue;
            }

            int xStart = getXForTime(block.start) + PADDING;
            int xEnd = getXForTime(block.end) + PADDING;
            xEnd = Math.min(xEnd, getWidth() - PADDING);

            int blockWidth = xEnd - xStart;
            if (blockWidth < 0) {
                blockWidth = panelWidth - xStart + xEnd;
            }
            // If a block is hovered, make other blocks more transparent
            Color fadedBlockColor = (hoveredBlock != null && !block.equals(hoveredBlock))
                    ? new Color(blockColor.getRed(), blockColor.getGreen(), blockColor.getBlue(), 100)
                    : blockColor;

            Color fadedTextColor = (hoveredBlock != null && !block.equals(hoveredBlock))
                    ? new Color(0, 0, 0, 100)
                    : Color.BLACK;

            // Make the hovered block have exact color (34, 34, 139, 128)
            if (block.equals(hoveredBlock)) {
                fadedBlockColor = new Color(34, 34, 139, 128);
            }


            // Draw the transparent block
            g.setColor(fadedBlockColor);
            g.fillRect(xStart, PADDING, blockWidth, barHeight);

            // Draw the block border
            g.setColor(blockBorderColor);
            g.drawRect(xStart, PADDING, blockWidth - 1, barHeight - 1);

            // Draw the transparent text
            String label = block.label;
            int maxTextWidth = blockWidth - 10;
            if (g.getFontMetrics().stringWidth(label) > maxTextWidth) {
                while (g.getFontMetrics().stringWidth(label + "...") > maxTextWidth && !label.isEmpty()) {
                    label = label.substring(0, label.length() - 1);
                }
                label += "...";
            }

            // Draw text inside the time blocks
            if (blockWidth > 30) {
                g.setColor(fadedTextColor);
                List<String> wrappedLines = wrapText(g, label, maxTextWidth);
                int lineHeight = g.getFontMetrics().getHeight();
                int yPosition = PADDING + (barHeight / 2) - (wrappedLines.size() * lineHeight / 2);

                for (String line : wrappedLines) {
                    g.drawString(line, xStart + 5, yPosition);
                    yPosition += lineHeight;
                }
            }
        }

        // Draw the current time indicator
        g.setColor(currentTimeColor);
        g.fillRect(currentX - 1, VERTICAL_PADDING - 10, 2, panelHeight);  // (x-position, y-position, thickness, height)


        // Draw the hovered block LAST so it's on top
        if (hoveredBlock != null) {
            int xStart = PADDING + getXForTime(hoveredBlock.start);
            int xEnd = PADDING + getXForTime(hoveredBlock.end);
            xEnd = Math.min(xEnd, getWidth() - PADDING);
            int blockWidth = xEnd - xStart;
            if (blockWidth < 0) {
                blockWidth = panelWidth - xStart + xEnd;
            }

            // Shadow Effect (Draw first so it appears underneath)
            g.setColor(new Color(100, 100, 100, 150)); // Semi-transparent gray shadow
            g.fillRect(xStart, 20, blockWidth, barHeight); // Shadow 5px below the block

            // Pop-out effect (Draw the actual hovered block)
            g.setColor(blockHoverColor);
            g.fillRect(xStart + 5, 10, blockWidth, barHeight);

            g.setColor(blockBorderColor);
            g.drawRect(xStart + 5, 10, blockWidth, barHeight - 1);

            // Draw highlight behind text
            String fullLabel = hoveredBlock.label;
            int textWidth = g.getFontMetrics().stringWidth(fullLabel);
            int textHeight = g.getFontMetrics().getHeight();
            Color highlight = new Color(20, 20, 20, 255);
            g.setColor(highlight);
            g.fillRect(xStart + 10, barHeight / 2 - textHeight, textWidth + 5, textHeight + 10);

            // Draw text AFTER highlight so it appears on top
            g.setColor(Color.WHITE);
            g.drawString(fullLabel, xStart + 10, barHeight / 2);
        }



    }

    // Wrap text within a given width
    private List<String> wrapText(Graphics g, String text, int maxWidth) {
        List<String> lines = new ArrayList<>();
        StringBuilder currentLine = new StringBuilder();
        String[] words = text.split(" ");

        for (String word : words) {
            String testLine = currentLine.isEmpty() ? word : currentLine + " " + word;
            if (g.getFontMetrics().stringWidth(testLine) > maxWidth) {
                lines.add(currentLine.toString());
                currentLine = new StringBuilder(word);
            } else {
                currentLine.append(currentLine.isEmpty() ? "" : " ").append(word);
            }
        }
        if (!currentLine.isEmpty()) {
            lines.add(currentLine.toString());
        }
        return lines;
    }

    // Used for determining the current time block
    protected TimeBlock getCurrentTimeBlock() {
        LocalTime now = LocalTime.now();
        for (TimeBlock block : timeBlocks) {
            if (!now.isBefore(block.start) && now.isBefore(block.end)) {
                return block; // The current time is within this block
            }
        }
        return null; // No active time block
    }

    protected String getTimeRemaining(TimeBlock block) {
        if (block == null) return "No active block";

        LocalTime now = LocalTime.now();
        long minutesRemaining = ChronoUnit.MINUTES.between(now, block.end);

        long hours = minutesRemaining / 60;
        long minutes = minutesRemaining % 60;

        return (hours > 0) ? String.format("%d hr %d min left", hours, minutes) : String.format("%d min left", minutes);
    }


    public void updateSettings(Color fontColor, Color progressFillColor, Color timelineColor,
                               Color currentTimeColor, LocalTime startOfDay,
                               Color blockColor, Color blockHoverColor, Color blockBorderColor) {
        this.fontColor = fontColor;
        this.progressFillColor = progressFillColor;
        this.timelineColor = timelineColor;
        this.currentTimeColor = currentTimeColor;
        this.startOfDay = startOfDay;
        this.blockColor = blockColor;
        this.blockHoverColor = blockHoverColor;
        this.blockBorderColor = blockBorderColor;
        repaint();
    }

}
