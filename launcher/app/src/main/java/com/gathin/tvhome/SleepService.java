package com.gathin.tvhome;

import android.accessibilityservice.AccessibilityService;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.view.accessibility.AccessibilityEvent;

/**
 * Puts the box in standby for the control center's Sleep tile and sleep timer. An app cannot call
 * PowerManager.goToSleep, but an accessibility service can ask for GLOBAL_ACTION_LOCK_SCREEN, which on TV
 * goes to sleep. Opt-in like the other helpers (settings put secure enabled_accessibility_services); while it
 * is off, sleep falls back to the screensaver.
 */
public final class SleepService extends AccessibilityService {
    private static SleepService instance;

    @Override
    protected void onServiceConnected() {
        instance = this;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        instance = null;
        return super.onUnbind(intent);
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {}

    @Override
    public void onInterrupt() {}

    static void sleepNow(Context context) {
        if (instance != null && instance.performGlobalAction(GLOBAL_ACTION_LOCK_SCREEN)) return;
        // SystemUI's Somnambulator starts the screensaver now.
        try {
            context.startActivity(new Intent(Intent.ACTION_MAIN)
                    .setComponent(new ComponentName("com.android.systemui", "com.android.systemui.Somnambulator"))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        } catch (ActivityNotFoundException | SecurityException ignored) {
        }
    }
}
