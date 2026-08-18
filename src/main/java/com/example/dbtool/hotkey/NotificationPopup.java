package com.example.dbtool.hotkey;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JWindow;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import java.awt.Color;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;

/**
 * Renders hotkey feedback as our own small popup near the bottom-right of the screen,
 * instead of an OS tray balloon. TrayIcon's balloon tips queue on Windows — calling
 * displayMessage while one is still showing makes the new one wait its turn instead of
 * appearing right away, which makes back-to-back hotkey presses feel laggy. Since this
 * popup is fully ours, showing a new message simply replaces whatever is currently
 * displayed instead of waiting for it to disappear first.
 */
public class NotificationPopup {

    private static final int DISPLAY_MILLIS = 3500;
    private static final Color INFO_BACKGROUND = new Color(0x2E, 0x6F, 0xDB);
    private static final Color ERROR_BACKGROUND = new Color(0xB3, 0x26, 0x1E);

    private JWindow window;
    private Timer hideTimer;

    public void showInfo(String message) {
        show(message, INFO_BACKGROUND);
    }

    public void showError(String message) {
        show(message, ERROR_BACKGROUND);
    }

    /**
     * Must run on the Swing event dispatch thread — hotkey triggers call this from
     * their own background thread, so every access to the shared window/timer state
     * is funneled through invokeLater instead of touching Swing components directly.
     */
    private void show(String message, Color background) {
        SwingUtilities.invokeLater(() -> {
            closeCurrent();
            window = buildWindow(message, background);
            window.setVisible(true);

            hideTimer = new Timer(DISPLAY_MILLIS, e -> closeCurrent());
            hideTimer.setRepeats(false);
            hideTimer.start();
        });
    }

    private void closeCurrent() {
        if (hideTimer != null) {
            hideTimer.stop();
        }
        if (window != null) {
            window.dispose();
        }
    }

    private JWindow buildWindow(String message, Color background) {
        JLabel label = new JLabel("<html><body style='width: 280px'>" + escapeHtml(message) + "</body></html>");
        label.setForeground(Color.WHITE);

        JPanel content = new JPanel();
        content.setBackground(background);
        content.setBorder(BorderFactory.createEmptyBorder(10, 14, 10, 14));
        content.add(label);

        JWindow popup = new JWindow();
        popup.setContentPane(content);
        popup.setAlwaysOnTop(true);
        popup.pack();
        positionBottomRight(popup);
        return popup;
    }

    private void positionBottomRight(JWindow popup) {
        Rectangle screen = GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();
        int x = screen.x + screen.width - popup.getWidth() - 16;
        int y = screen.y + screen.height - popup.getHeight() - 16;
        popup.setLocation(x, y);
    }

    private String escapeHtml(String text) {
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\n", "<br>");
    }
}
