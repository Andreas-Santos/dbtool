package com.example.dbtool.ui;

/**
 * Entry point that does NOT extend javafx.application.Application. The Java
 * launcher refuses to start an Application subclass directly unless JavaFX is on
 * the module path (mvn javafx:run sets that up, but running the jar or an IDE's
 * "Run" button on MainApp does not) — going through this indirection avoids that
 * "JavaFX runtime components are missing" error.
 */
public class Launcher {

    public static void main(String[] args) {
        MainApp.main(args);
    }
}
