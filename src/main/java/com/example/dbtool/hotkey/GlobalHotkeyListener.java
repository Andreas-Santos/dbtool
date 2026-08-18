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
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Registers system-wide Ctrl+Alt+&lt;key&gt; hotkeys via JNativeHook, so they fire even
 * while another application (DBeaver) has focus. Each binding runs on its own
 * single-thread executor — it drives Robot key events that must never block the
 * native hook thread — and skips a new trigger while its previous one is still
 * running, rather than queuing it to fire late once the first finishes. Both choices
 * exist so pressing one hotkey never makes another feel sluggish: without a
 * per-binding executor, an unrelated key held a moment too long would make every
 * other hotkey wait behind it on a single shared thread; without the in-flight
 * check, a double press would queue up a second run instead of being dropped.
 */
public class GlobalHotkeyListener {

    private final List<Binding> bindings = new ArrayList<>();

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
                        binding.triggerIfIdle();
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
        bindings.forEach(Binding::shutdown);
    }

    private void silenceJNativeHookLogging() {
        Logger logger = Logger.getLogger(GlobalScreen.class.getPackage().getName());
        logger.setLevel(Level.OFF);
        logger.setUseParentHandlers(false);
    }

    private static final class Binding {
        private final int keyCode;
        private final Runnable onTrigger;
        private final AtomicBoolean running = new AtomicBoolean(false);
        private final ExecutorService executor;

        Binding(int keyCode, Runnable onTrigger) {
            this.keyCode = keyCode;
            this.onTrigger = onTrigger;
            this.executor = Executors.newSingleThreadExecutor(r -> {
                Thread thread = new Thread(r, "dbtool-hotkey-trigger-" + keyCode);
                thread.setDaemon(true);
                return thread;
            });
        }

        int keyCode() {
            return keyCode;
        }

        void triggerIfIdle() {
            if (!running.compareAndSet(false, true)) {
                return;
            }
            executor.submit(() -> {
                try {
                    onTrigger.run();
                } finally {
                    running.set(false);
                }
            });
        }

        void shutdown() {
            executor.shutdownNow();
        }
    }
}
