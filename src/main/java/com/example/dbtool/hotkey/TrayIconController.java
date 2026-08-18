package com.example.dbtool.hotkey;

import java.awt.AWTException;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.MenuItem;
import java.awt.PopupMenu;
import java.awt.SystemTray;
import java.awt.TrayIcon;
import java.awt.image.BufferedImage;

/**
 * Keeps the app reachable while it has no window at all: a tray icon with a menu to
 * quit, so the user can tell the background hotkey listener is active. Adding it to
 * the SystemTray is also what keeps the JVM alive, since AWT's tray implementation
 * runs on a non-daemon thread.
 */
public class TrayIconController {

    private final NotificationPopup notifications = new NotificationPopup();
    private TrayIcon trayIcon;

    public void install(Runnable onExit) {
        if (!SystemTray.isSupported()) {
            throw new IllegalStateException("System tray is not supported on this platform");
        }

        PopupMenu menu = new PopupMenu();
        MenuItem exitItem = new MenuItem("Sair");
        exitItem.addActionListener(e -> onExit.run());
        menu.add(exitItem);

        trayIcon = new TrayIcon(createIconImage(),
                "DB Tool (Ctrl+Alt+Z completa JOIN, Ctrl+Alt+X sincroniza manual, Ctrl+Alt+A gera GROUP BY)", menu);
        trayIcon.setImageAutoSize(true);

        try {
            SystemTray.getSystemTray().add(trayIcon);
        } catch (AWTException e) {
            throw new IllegalStateException("Failed to add the tray icon", e);
        }
    }

    public void showError(String message) {
        notifications.showError(message);
    }

    public void showInfo(String message) {
        notifications.showInfo(message);
    }

    private Image createIconImage() {
        BufferedImage image = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        g.setColor(new Color(0x2E, 0x6F, 0xDB));
        g.fillOval(0, 0, 16, 16);
        g.setColor(Color.WHITE);
        g.setFont(new Font("SansSerif", Font.BOLD, 9));
        g.drawString("D", 4, 12);
        g.dispose();
        return image;
    }
}
