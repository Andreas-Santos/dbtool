package com.example.dbtool.ui;

import com.example.dbtool.database.MetadataServiceFactory;
import com.example.dbtool.hotkey.AutocompleteController;
import com.example.dbtool.hotkey.GlobalHotkeyListener;
import com.example.dbtool.hotkey.SyncManualRelationshipsController;
import com.example.dbtool.hotkey.TrayIconController;
import com.github.kwhat.jnativehook.keyboard.NativeKeyEvent;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class MainApp extends Application {

    private GlobalHotkeyListener hotkeyListener;

    @Override
    public void start(Stage stage) {
        stage.setTitle("DB Tool");
        stage.setScene(new Scene(new MainWindow(), 480, 420));
        stage.show();

        Platform.setImplicitExit(false);
        stage.setOnCloseRequest(e -> {
            e.consume();
            stage.hide();
        });

        setupBackgroundHotkeys(stage);
    }

    private void setupBackgroundHotkeys(Stage stage) {
        TrayIconController tray = new TrayIconController();
        tray.install(
                () -> Platform.runLater(() -> {
                    stage.show();
                    stage.toFront();
                }),
                () -> {
                    if (hotkeyListener != null) {
                        hotkeyListener.unregister();
                    }
                    Platform.exit();
                    System.exit(0);
                }
        );

        MetadataServiceFactory factory = new MetadataServiceFactory();
        AutocompleteController[] autocomplete = new AutocompleteController[1];
        SyncManualRelationshipsController[] sync = new SyncManualRelationshipsController[1];

        hotkeyListener = new GlobalHotkeyListener();
        hotkeyListener.bind(NativeKeyEvent.VC_Z, () -> {
            try {
                if (autocomplete[0] == null) {
                    autocomplete[0] = new AutocompleteController(factory.create(), tray::showInfo, tray::showError);
                }
                autocomplete[0].onHotkeyPressed();
            } catch (Exception e) {
                tray.showError(e.getMessage());
            }
        });
        hotkeyListener.bind(NativeKeyEvent.VC_X, () -> {
            try {
                if (sync[0] == null) {
                    sync[0] = new SyncManualRelationshipsController(
                            factory.manualRelationships(), tray::showInfo, tray::showError);
                }
                sync[0].onHotkeyPressed();
            } catch (Exception e) {
                tray.showError(e.getMessage());
            }
        });
        hotkeyListener.start();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
