package com.example.dbtool;

import com.example.dbtool.config.ConfigLoader;
import com.example.dbtool.database.MetadataServiceFactory;
import com.example.dbtool.hotkey.AutocompleteController;
import com.example.dbtool.hotkey.GlobalHotkeyListener;
import com.example.dbtool.hotkey.GroupByController;
import com.example.dbtool.hotkey.SyncManualRelationshipsController;
import com.example.dbtool.hotkey.TrayIconController;
import com.example.dbtool.ui.DbConfigWindow;
import com.github.kwhat.jnativehook.keyboard.NativeKeyEvent;

/**
 * Entry point. Runs entirely in the background via a tray icon and global hotkeys —
 * there is no window, so it never appears in the taskbar/window switcher and never
 * steals focus from DBeaver. Ctrl+Alt+Z completes a JOIN, Ctrl+Alt+X syncs manual
 * relationships from the editor, Ctrl+Alt+A generates a GROUP BY.
 */
public class Main {

    private final MetadataServiceFactory factory = new MetadataServiceFactory();
    private final TrayIconController tray = new TrayIconController();
    private final GlobalHotkeyListener hotkeyListener = new GlobalHotkeyListener();

    private AutocompleteController autocompleteController;
    private SyncManualRelationshipsController syncController;
    private GroupByController groupByController;

    public static void main(String[] args) {
        new Main().start();
    }

    private void start() {
        if (new ConfigLoader().tryLoad() == null) {
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

        hotkeyListener.bind(NativeKeyEvent.VC_Z, () -> runSafely(this::autocomplete));
        hotkeyListener.bind(NativeKeyEvent.VC_X, () -> runSafely(this::syncManualRelationships));
        hotkeyListener.bind(NativeKeyEvent.VC_A, () -> runSafely(this::groupBy));
        hotkeyListener.start();
    }

    /**
     * Reopening from the tray must forget the cached AutocompleteController — it's the
     * only one holding a DB connection built from the old settings — so the next JOIN
     * hotkey rebuilds it against whatever was just saved.
     */
    private void openConfigWindow() {
        DbConfigWindow.showOnEventThread(config -> autocompleteController = null);
    }

    private void autocomplete() {
        if (autocompleteController == null) {
            autocompleteController = new AutocompleteController(factory.create(), tray::showInfo, tray::showError);
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
            groupByController = new GroupByController(tray::showInfo, tray::showError);
        }
        groupByController.onHotkeyPressed();
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
