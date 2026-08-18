package com.example.dbtool.hotkey;

import com.github.kwhat.jnativehook.GlobalScreen;
import com.github.kwhat.jnativehook.NativeHookException;
import com.github.kwhat.jnativehook.NativeInputEvent;
import com.github.kwhat.jnativehook.keyboard.NativeKeyEvent;
import com.github.kwhat.jnativehook.keyboard.NativeKeyListener;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Registers system-wide Ctrl+Alt+&lt;key&gt; hotkeys via JNativeHook, so they fire even
 * while another application (DBeaver) has focus. Each trigger runs on a background
 * thread — it drives Robot key events that must never block the native hook thread.
 */
public class GlobalHotkeyListener {

    private final List<Binding> bindings = new ArrayList<>();
    private final ExecutorService triggerExecutor = Executors.newSingleThreadExecutor(r -> {
        Thread thread = new Thread(r, "dbtool-hotkey-trigger");
        thread.setDaemon(true);
        return thread;
    });

    /**
     * Registers a Ctrl+Alt+&lt;keyCode&gt; binding (use NativeKeyEvent.VC_* constants).
     * Call before {@link #start()}.
     */
    public void bind(int keyCode, Runnable onTrigger) {
        bindings.add(new Binding(keyCode, onTrigger));
    }

    public void start() {
        silenceJNativeHookLogging();
        try {
            GlobalScreen.registerNativeHook();
        } catch (NativeHookException e) {
            throw new IllegalStateException("Failed to register the global hotkeys", e);
        }

        GlobalScreen.addNativeKeyListener(new NativeKeyListener() {
            @Override
            public void nativeKeyPressed(NativeKeyEvent e) {
                boolean ctrl = (e.getModifiers() & NativeInputEvent.CTRL_MASK) != 0;
                boolean alt = (e.getModifiers() & NativeInputEvent.ALT_MASK) != 0;
                if (!ctrl || !alt) {
                    return;
                }
                for (Binding binding : bindings) {
                    if (e.getKeyCode() == binding.keyCode()) {
                        triggerExecutor.submit(binding.onTrigger());
                    }
                }
            }
        });
    }

    public void unregister() {
        try {
            GlobalScreen.unregisterNativeHook();
        } catch (NativeHookException ignored) {
            // best-effort on shutdown
        }
        triggerExecutor.shutdownNow();
    }

    private void silenceJNativeHookLogging() {
        Logger logger = Logger.getLogger(GlobalScreen.class.getPackage().getName());
        logger.setLevel(Level.OFF);
        logger.setUseParentHandlers(false);
    }

    private record Binding(int keyCode, Runnable onTrigger) {
    }
}
