package com.example.dbtool;

import com.example.dbtool.config.ConfigLoader;
import com.example.dbtool.config.DbConfig;
import com.example.dbtool.database.MetadataServiceFactory;
import com.example.dbtool.hotkey.AutocompleteController;
import com.example.dbtool.hotkey.FormatQueryController;
import com.example.dbtool.hotkey.GlobalHotkeyListener;
import com.example.dbtool.hotkey.GroupByController;
import com.example.dbtool.hotkey.SyncManualRelationshipsController;
import com.example.dbtool.hotkey.TrayIconController;
import com.example.dbtool.ui.DbConfigWindow;
import com.github.kwhat.jnativehook.keyboard.NativeKeyEvent;

/**
 * Entry point. Runs entirely in the background via a tray icon and global hotkeys —
 * there is no window, so it never appears in the taskbar/window switcher and never
 * steals focus from DBeaver. Alt+C completes a JOIN, Alt+V syncs manual
 * relationships from the editor, Alt+G generates a GROUP BY, Alt+F formats the
 * statement the cursor is in.
 */
public class Main {

    private final MetadataServiceFactory factory = new MetadataServiceFactory();
    private final TrayIconController tray = new TrayIconController();
    private final GlobalHotkeyListener hotkeyListener = new GlobalHotkeyListener();
    private final ConfigLoader configLoader = new ConfigLoader();

    private AutocompleteController autocompleteController;
    private SyncManualRelationshipsController syncController;
    private GroupByController groupByController;
    private FormatQueryController formatQueryController;

    public static void main(String[] args) {
        new Main().start();
    }

    private void start() {
        if (configLoader.tryLoad() == null) {
            DbConfigWindow.showOnEventThread(config -> startBackgroundServices());
        } else {
            startBackgroundServices();
        }
    }

    private void startBackgroundServices() {
        tray.install(this::openConfigWindow, () -> {
            hotkeyListener.unregister();
            System.exit(0);
        });

        hotkeyListener.bind(NativeKeyEvent.VC_C, () -> runSafely(this::autocomplete));
        hotkeyListener.bind(NativeKeyEvent.VC_V, () -> runSafely(this::syncManualRelationships));
        hotkeyListener.bind(NativeKeyEvent.VC_G, () -> runSafely(this::groupBy));
        hotkeyListener.bind(NativeKeyEvent.VC_F, () -> runSafely(this::formatQuery));
        hotkeyListener.start();
    }

    /**
     * Reopening from the tray must forget every cached controller — each one bakes in
     * either a DB connection or the owner, both built from the settings as they stood
     * when it was first created — so the next hotkey press rebuilds whichever one it
     * needs against whatever was just saved.
     */
    private void openConfigWindow() {
        DbConfigWindow.showOnEventThread(config -> {
            autocompleteController = null;
            groupByController = null;
            formatQueryController = null;
        });
    }

    private String currentOwner() {
        DbConfig config = configLoader.tryLoad();
        return config == null ? "" : config.owner();
    }

    private void autocomplete() {
        if (autocompleteController == null) {
            autocompleteController = new AutocompleteController(
                    factory.create(), currentOwner(), tray::showInfo, tray::showError);
        }
        autocompleteController.onHotkeyPressed();
    }

    private void syncManualRelationships() {
        if (syncController == null) {
            syncController = new SyncManualRelationshipsController(
                    factory.manualRelationships(), tray::showInfo, tray::showError);
        }
        syncController.onHotkeyPressed();
    }

    private void groupBy() {
        if (groupByController == null) {
            groupByController = new GroupByController(currentOwner(), tray::showInfo, tray::showError);
        }
        groupByController.onHotkeyPressed();
    }

    private void formatQuery() {
        if (formatQueryController == null) {
            formatQueryController = new FormatQueryController(currentOwner(), tray::showInfo, tray::showError);
        }
        formatQueryController.onHotkeyPressed();
    }

    /**
     * Each hotkey trigger runs on its own background thread (see GlobalHotkeyListener) —
     * an uncaught exception there would otherwise vanish silently instead of surfacing
     * to the user via the tray balloon.
     */
    private void runSafely(Runnable action) {
        try {
            action.run();
        } catch (Exception e) {
            tray.showError(e.getMessage());
        }
    }
}
