package com.example.dbtool.hotkey;

import java.awt.AWTException;
import java.awt.Robot;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.KeyEvent;
import java.io.IOException;

/**
 * Drives the focused text editor via simulated keystrokes and the system clipboard.
 *
 * <p>Selecting back to the document start and then pressing Right is meant to collapse
 * the selection to its original (rightmost) end without moving the caret — that's the
 * standard "collapse to selection end" convention nearly every text widget follows, and
 * how we read "everything before the cursor" while still landing back at that same
 * spot. Escape is sent first because SQL editors typically show a content-assist popup
 * right after "alias." — if left open it can swallow the Home/Right keys meant for the
 * text widget itself.
 */
public class EditorAutomation {

    private final Robot robot;

    public EditorAutomation() {
        try {
            robot = new Robot();
            robot.setAutoDelay(20);
        } catch (AWTException e) {
            throw new IllegalStateException("Failed to create a Robot for keyboard automation", e);
        }
    }

    /**
     * Forces Ctrl/Alt/Shift to a released state before any automation runs. The hotkey
     * that triggered us IS Ctrl+Alt+<key>, and if the user hasn't physically released
     * those keys yet, our own Ctrl+C/Ctrl+A get sent on top of a real, still-held
     * Ctrl+Alt — some keyboard layouts (e.g. ABNT2) treat that overlap as an AltGr
     * dead-key combo and produce a stray special character instead of a clean copy.
     */
    public void releaseHotkeyModifiers() {
        robot.keyRelease(KeyEvent.VK_CONTROL);
        robot.keyRelease(KeyEvent.VK_ALT);
        robot.keyRelease(KeyEvent.VK_SHIFT);
    }

    public void dismissPopup() {
        tap(KeyEvent.VK_ESCAPE);
    }

    public void selectToDocumentStart() {
        pressCombo(KeyEvent.VK_CONTROL, KeyEvent.VK_SHIFT, KeyEvent.VK_HOME);
    }

    public void collapseSelectionForward() {
        tap(KeyEvent.VK_RIGHT);
    }

    public void selectAll() {
        pressCombo(KeyEvent.VK_CONTROL, KeyEvent.VK_A);
    }

    public void moveToDocumentEnd() {
        tap(KeyEvent.VK_ESCAPE);
        pressCombo(KeyEvent.VK_CONTROL, KeyEvent.VK_END);
    }

    public void copy() {
        pressCombo(KeyEvent.VK_CONTROL, KeyEvent.VK_C);
    }

    public String readClipboard() {
        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        try {
            Object data = clipboard.getData(DataFlavor.stringFlavor);
            return data instanceof String s ? s : "";
        } catch (UnsupportedFlavorException | IOException | IllegalStateException e) {
            return "";
        }
    }

    public void writeClipboard(String text) {
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(text), null);
    }

    private void pressCombo(int... keys) {
        for (int key : keys) {
            robot.keyPress(key);
        }
        for (int i = keys.length - 1; i >= 0; i--) {
            robot.keyRelease(keys[i]);
        }
    }

    private void tap(int key) {
        robot.keyPress(key);
        robot.keyRelease(key);
    }
}
