package com.example.dbtool.ui;

import javax.imageio.ImageIO;
import java.awt.Image;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;

/**
 * Single source for the app's icon (src/main/resources/icon.png), shared by the tray
 * icon and every window so they all show the same image instead of drifting apart.
 */
public final class AppIcon {

    private static final String RESOURCE_PATH = "/icon.png";

    private AppIcon() {
    }

    public static Image load() {
        try (InputStream in = AppIcon.class.getResourceAsStream(RESOURCE_PATH)) {
            if (in == null) {
                throw new IllegalStateException("Missing app icon resource: " + RESOURCE_PATH);
            }
            return ImageIO.read(in);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to load app icon", e);
        }
    }
}
