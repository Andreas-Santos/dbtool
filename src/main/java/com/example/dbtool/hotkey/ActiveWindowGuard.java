package com.example.dbtool.hotkey;

import com.sun.jna.Native;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef.HWND;

/**
 * Safety check so the hotkey only acts while DBeaver is the focused window —
 * otherwise the simulated keystrokes could land in whatever else is active.
 */
public class ActiveWindowGuard {

    public boolean isDBeaverActive() {
        HWND hwnd = User32.INSTANCE.GetForegroundWindow();
        if (hwnd == null) {
            return false;
        }
        char[] buffer = new char[1024];
        User32.INSTANCE.GetWindowText(hwnd, buffer, buffer.length);
        String title = Native.toString(buffer);
        return title.toLowerCase().contains("dbeaver");
    }
}
